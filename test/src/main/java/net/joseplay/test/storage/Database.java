package net.joseplay.test.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.joseplay.test.config.PluginSettings;

import java.io.File;
import java.sql.*;

public class Database {
    private final PluginSettings settings;
    private HikariDataSource dataSource;
    public boolean mysql;

    @FunctionalInterface
    public interface ResultMapper<T> {

        T map(ResultSet resultSet) throws SQLException;
    }

    public Database(PluginSettings pluginSettings) {
        this.settings = pluginSettings;
        mysql = settings.mysql();
    }


    public void start(){
        HikariConfig config = new HikariConfig();

        config.setPoolName("AllianceMariage");
        config.setConnectionTimeout(10000);

        if (mysql){
            config.setJdbcUrl("jdbc:mysql://" + settings.mysqlHost() + ":" + settings.mysqlPort() + "/"
                    + settings.mysqlDatabase() + "?useSSL=false&characterEncoding=utf8");

            config.setUsername(settings.mysqlUsername());
            config.setPassword(settings.mysqlPassword());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setMaximumPoolSize(8);
        } else {
            File file = new File( "marriage.db");

            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1);
            config.setConnectionTestQuery("SELECT 1");
        }

        dataSource = new HikariDataSource(config);
        createSchema();
    }
    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }



    private void createSchema(){
        execute("""
                CREATE TABLE IF NOT EXISTS marriages (
                      playerUUID VARCHAR(36) PRIMARY KEY,
                      partnerUUID VARCHAR(36) NOT NULL
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
    }



    public void execute(String sql) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = connection();
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Falha ao preparar o banco: " + sql, exception);
        } finally {
            closeQuietly(statement, connection);
        }
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
