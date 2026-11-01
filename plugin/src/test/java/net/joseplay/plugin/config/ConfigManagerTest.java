package net.joseplay.plugin.config;

import net.joseplay.core.config.ConfigManager;
import net.joseplay.core.config.ConfigSerializers;
import net.joseplay.core.config.PluginConfig;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @org.junit.jupiter.api.BeforeAll
    static void initBukkitServer() {
        if (org.bukkit.Bukkit.getServer() == null) {
            org.bukkit.UnsafeValues unsafe = (org.bukkit.UnsafeValues) java.lang.reflect.Proxy.newProxyInstance(
                    org.bukkit.UnsafeValues.class.getClassLoader(),
                    new Class<?>[]{org.bukkit.UnsafeValues.class},
                    (proxy, method, args) -> {
                        if ("getDataVersion".equals(method.getName())) return 3700;
                        if ("getMaterial".equals(method.getName())) {
                            return org.bukkit.Material.getMaterial((String) args[0]);
                        }
                        return null;
                    }
            );
            org.bukkit.inventory.ItemFactory itemFactory = (org.bukkit.inventory.ItemFactory) java.lang.reflect.Proxy.newProxyInstance(
                    org.bukkit.inventory.ItemFactory.class.getClassLoader(),
                    new Class<?>[]{org.bukkit.inventory.ItemFactory.class},
                    (proxy, method, args) -> {
                        if ("equals".equals(method.getName())) return true;
                        return null;
                    }
            );
            org.bukkit.Server server = (org.bukkit.Server) java.lang.reflect.Proxy.newProxyInstance(
                    org.bukkit.Server.class.getClassLoader(),
                    new Class<?>[]{org.bukkit.Server.class},
                    (proxy, method, args) -> {
                        if ("getUnsafe".equals(method.getName())) return unsafe;
                        if ("getItemFactory".equals(method.getName())) return itemFactory;
                        if ("getLogger".equals(method.getName())) return java.util.logging.Logger.getLogger("test");
                        return null;
                    }
            );
            org.bukkit.Bukkit.setServer(server);
        }
    }

    @TempDir
    File tempDir;

    @Test
    void registerCachesSingleInstance() throws Exception {
        Files.writeString(new File(tempDir, "config.yml").toPath(), "debug: true\n", StandardCharsets.UTF_8);
        ConfigManager manager = new ConfigManager(tempDir, Logger.getLogger("test"));

        PluginConfig first = manager.register("config.yml");
        PluginConfig second = manager.register("config");

        assertSame(first, second);
        assertTrue(manager.isLoaded("config"));
        assertTrue(first.getBoolean("debug"));
    }

    @Test
    void setUpdatesMemoryAndSaveWritesFile() throws Exception {
        Files.writeString(new File(tempDir, "messages.yml").toPath(), "hello: world\n", StandardCharsets.UTF_8);
        ConfigManager manager = new ConfigManager(tempDir, Logger.getLogger("test"));
        PluginConfig config = manager.register("messages");

        config.set("hello", "updated");
        config.set("count", 12);
        config.set("ratio", 1.5);
        config.set("enabled", true);
        config.set("tags", List.of("a", "b"));
        assertEquals("updated", config.getString("hello"));
        assertEquals(12, config.getInt("count"));
        assertEquals(1.5f, config.getFloat("ratio"));
        assertEquals(12L, config.getNumber("count").longValue());
        assertEquals(List.of("a", "b"), config.getStringList("tags"));
        assertTrue(config.isDirty());

        assertTrue(config.save());
                assertFalse(config.isDirty());

        String written = Files.readString(config.getFile().toPath(), StandardCharsets.UTF_8);
        assertTrue(written.contains("updated"));
        assertTrue(written.contains("12"));
    }

    @Test
    void reloadReadsExternalChanges() throws Exception {
        File file = new File(tempDir, "menus.yml");
        Files.writeString(file.toPath(), "title: old\n", StandardCharsets.UTF_8);
        ConfigManager manager = new ConfigManager(tempDir, Logger.getLogger("test"));
        PluginConfig config = manager.register("menus");
        assertEquals("old", config.getString("title"));

        Files.writeString(file.toPath(), "title: new\n", StandardCharsets.UTF_8);
        assertTrue(manager.reload("menus"));
        assertEquals("new", config.getString("title"));
    }

    @Test
    void reloadKeepsUnsavedChanges() throws Exception {
        File file = new File(tempDir, "keep.yml");
        Files.writeString(file.toPath(), "value: disk\n", StandardCharsets.UTF_8);
        PluginConfig config = new PluginConfig(file, Logger.getLogger("test"));
        config.set("value", "memory");

        Files.writeString(file.toPath(), "value: external\n", StandardCharsets.UTF_8);
        assertFalse(config.reload(false));
        assertEquals("memory", config.getString("value"));
    }

    @Test
    void invalidYamlIsNotOverwritten() throws Exception {
        File file = new File(tempDir, "broken.yml");
        String invalid = "items: [\n  - broken";
        Files.writeString(file.toPath(), invalid, StandardCharsets.UTF_8);

        PluginConfig config = new PluginConfig(file, Logger.getLogger("test"));
        assertTrue(config.hasLoadFailed());
        config.set("items", List.of("x"));
        assertFalse(config.save());
        assertEquals(invalid, Files.readString(file.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    void nestedPathsAndLists() throws Exception {
        Files.writeString(new File(tempDir, "database.yml").toPath(), """
                database:
                  mysql:
                    host: localhost
                    port: 3306
                    flags: [true, false]
                    ports: [3306, 3307]
                """, StandardCharsets.UTF_8);

        PluginConfig config = new ConfigManager(tempDir, Logger.getLogger("test")).register("database");
        assertEquals("localhost", config.getString("database.mysql.host"));
        assertEquals(3306, config.getInt("database.mysql.port"));
        assertEquals(List.of(3306, 3307), config.getIntegerList("database.mysql.ports"));
        assertEquals(List.of(true, false), config.getBooleanList("database.mysql.flags"));
        assertTrue(config.contains("database.mysql.host"));
        assertTrue(config.getKeys("database.mysql", false).contains("host"));
        config.remove("database.mysql.host");
        assertFalse(config.isSet("database.mysql.host"));
    }

    @Test
    void locationRoundTrip() throws Exception {
        Files.writeString(new File(tempDir, "worlds.yml").toPath(), "", StandardCharsets.UTF_8);
        PluginConfig config = new PluginConfig(new File(tempDir, "worlds.yml"), Logger.getLogger("test"));

        Location original = new Location(null, 10.5, 64.0, -3.25, 90.0f, -15.0f);
        config.setLocation("spawn", original);
        Location loaded = config.getLocation("spawn");

        assertNotNull(loaded);
        assertEquals(10.5, loaded.getX());
        assertEquals(64.0, loaded.getY());
        assertEquals(-3.25, loaded.getZ());
        assertEquals(90.0f, loaded.getYaw());
        assertEquals(-15.0f, loaded.getPitch());
        assertNull(ConfigSerializers.deserializeLocation(config.getConfigurationSection("missing")));
    }

    @Test
    void messagePlaceholders() {
        String formatted = Messages.apply("Hello %player%, price %price%", "%player%", "Alex", "%price%", "50");
        assertEquals("Hello Alex, price 50", formatted);
        assertEquals("&aHi", Messages.apply("&aHi"));
    }

    @Test
    void reloadMultipleFiles() throws Exception {
        File file1 = new File(tempDir, "f1.yml");
        File file2 = new File(tempDir, "f2.yml");
        Files.writeString(file1.toPath(), "val: 1\n", StandardCharsets.UTF_8);
        Files.writeString(file2.toPath(), "val: 2\n", StandardCharsets.UTF_8);

        ConfigManager manager = new ConfigManager(tempDir, Logger.getLogger("test"));
        PluginConfig c1 = manager.register("f1");
        PluginConfig c2 = manager.register("f2");

        assertEquals(1, c1.getInt("val"));
        assertEquals(2, c2.getInt("val"));

        // Update both on disk
        Files.writeString(file1.toPath(), "val: 10\n", StandardCharsets.UTF_8);
        Files.writeString(file2.toPath(), "val: 20\n", StandardCharsets.UTF_8);

        boolean reloaded = manager.reloadAll(false);
        assertTrue(reloaded);
        assertEquals(10, c1.getInt("val"));
        assertEquals(20, c2.getInt("val"));
    }

    @Test
    void itemStackRoundTrip() throws Exception {
        File file = new File(tempDir, "items.yml");
        Files.writeString(file.toPath(), "", StandardCharsets.UTF_8);
        PluginConfig config = new PluginConfig(file, Logger.getLogger("test"));

        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.APPLE, 5);
        config.setItemStack("reward", item);
        assertTrue(config.save());
        
        PluginConfig loaded = new PluginConfig(file, Logger.getLogger("test"));
                org.bukkit.inventory.ItemStack result = loaded.getItemStack("reward");
        assertNotNull(result);
        assertEquals(org.bukkit.Material.APPLE, result.getType());
        assertEquals(5, result.getAmount());
    }

    @Test
    void recoveryOfNonExistentFile() {
        ConfigManager manager = new ConfigManager(tempDir, Logger.getLogger("test"));
        File missingFile = new File(tempDir, "newfile.yml");
        assertFalse(missingFile.exists());

        PluginConfig config = manager.register("newfile");
        assertNotNull(config);
        assertFalse(config.hasLoadFailed());
        assertTrue(missingFile.exists(), "Registering non-existent file creates it cleanly");
    }
}