package net.joseplay.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.joseplay.core.config.PluginSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Database {
    private final PluginSettings settings;
    private final File dataFolder;
    private HikariDataSource dataSource;
    private final ExecutorService databaseExecutor;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @FunctionalInterface
    public interface ResultMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    public Database(PluginSettings pluginSettings, File dataFolder) {
        this.settings = pluginSettings != null ? pluginSettings : new PluginSettings(null);
        this.dataFolder = dataFolder;

        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "AllianceMarriage-DB-" + counter.incrementAndGet());
                thread.setDaemon(true);
                thread.setUncaughtExceptionHandler((t, e) -> {
                    System.err.println("Uncaught exception in database thread " + t.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                });
                return thread;
            }
        };

        this.databaseExecutor = Executors.newFixedThreadPool(mysql() ? 8 : 1, threadFactory);
    }

    public Database(PluginSettings pluginSettings, JavaPlugin plugin) {
        this(pluginSettings, plugin != null ? plugin.getDataFolder() : new File("."));
    }

    public boolean mysql(){
        return settings.mysql();
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    private void ensureInitialized() {
        if (!initialized.get()) {
            throw new IllegalStateException("Database has not been initialized yet.");
        }
    }

    public CompletableFuture<Void> start() {
        HikariConfig config = new HikariConfig();

        config.setPoolName("AllianceMarriage");
        config.setConnectionTimeout(10000);

        if (mysql()) {
            config.setJdbcUrl("jdbc:mysql://" + settings.mysqlHost() + ":" + settings.mysqlPort() + "/"
                    + settings.mysqlDatabase() + "?useSSL=false&characterEncoding=utf8");

            config.setUsername(settings.mysqlUsername());
            config.setPassword(settings.mysqlPassword());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setMaximumPoolSize(8);
        } else {
            File dir = dataFolder != null ? dataFolder : new File(".");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "marriage.db");

            if (!file.exists()) {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                try {
                    file.createNewFile();
                } catch (IOException ignored) {
                }
            }

            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1);
            config.setConnectionTestQuery("SELECT 1");
        }

        dataSource = new HikariDataSource(config);

        return createSchema().whenComplete((v, ex) -> {
            if (ex == null) {
                initialized.set(true);
            }
        });
    }

    public Connection connection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    public void shutdown() {
        databaseExecutor.shutdown();
        try {
            if (!databaseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                databaseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            databaseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        initialized.set(false);
    }

    public CompletableFuture<Void> createSchema() {
        return CompletableFuture.runAsync(() -> {
            execute("""
                    CREATE TABLE IF NOT EXISTS marriages (
                          playerUUID VARCHAR(36) PRIMARY KEY,
                          partnerUUID VARCHAR(36) NOT NULL,
                          UNIQUE (playerUUID)
                      );
                    """);

            execute("""
                    CREATE TABLE IF NOT EXISTS couples (
                          id VARCHAR(36) PRIMARY KEY,
                          partner1UUID VARCHAR(36) NOT NULL,
                          partner2UUID VARCHAR(36) NOT NULL,
                          anniversary TEXT,
                          created_at TEXT NOT NULL,
                          UNIQUE (partner1UUID, partner2UUID)
                      );
                    """);

            execute("""
                    CREATE TABLE IF NOT EXISTS couples_features (
                          couple_id VARCHAR(36) NOT NULL,
                          feature TEXT NOT NULL,
                          type TEXT NOT NULL,
                          value TEXT,
                          PRIMARY KEY (couple_id, feature)
                      );
                    """);
        }, databaseExecutor);
    }

    public void execute(String sql) {
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao preparar o banco: " + sql, exception);
        }
    }

    public CompletableFuture<Void> executeAsync(String sql) {
        ensureInitialized();
        return CompletableFuture.runAsync(() -> {
            execute(sql);
        }, databaseExecutor);
    }

    public int executeUpdate(String sql, Object... params) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Falha ao executar operação no banco: " + sql,
                    exception
            );
        }
    }

    public CompletableFuture<Void> executeUpdateAsync(String sql, Object... params) {
        ensureInitialized();
        return CompletableFuture.runAsync(() -> {
            executeUpdate(sql, params);
        }, databaseExecutor);
    }

    public CompletableFuture<Integer> executeUpdateRowsAsync(String sql, Object... params) {
        ensureInitialized();
        return CompletableFuture.supplyAsync(() -> executeUpdate(sql, params), databaseExecutor);
    }

    public <T> T executeQuery(
            String sql,
            ResultMapper<T> mapper,
            Object... params
    ) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapper.map(resultSet);
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Falha ao consultar o banco: " + sql,
                    exception
            );
        }
    }

    public <T> CompletableFuture<T> executeQueryAsync(
            String sql,
            ResultMapper<T> mapper,
            Object... parameters
    ) {
        ensureInitialized();
        return CompletableFuture.supplyAsync(
                () -> executeQuery(sql, mapper, parameters),
                databaseExecutor
        );
    }

    public <T> CompletableFuture<T> executeTransactionAsync(TransactionFunction<T> function) {
        ensureInitialized();
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = connection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    T result = function.apply(connection);
                    connection.commit();
                    return result;
                } catch (Exception ex) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackEx) {
                        ex.addSuppressed(rollbackEx);
                    }
                    if (ex instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    throw new CompletionException(ex);
                } finally {
                    try {
                        connection.setAutoCommit(originalAutoCommit);
                    } catch (SQLException ignored) {
                    }
                }
            } catch (SQLException ex) {
                throw new CompletionException("Failed to execute database transaction", ex);
            }
        }, databaseExecutor);
    }

    static void closeQuietly(AutoCloseable... closeables) {
        for (AutoCloseable closeable : closeables) {
            if (closeable == null) {
                continue;
            }
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
