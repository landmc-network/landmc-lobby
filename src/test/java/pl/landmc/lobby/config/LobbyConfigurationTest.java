package pl.landmc.lobby.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.landmc.platform.component.ComponentFormatter;
import pl.landmc.platform.config.ConfigService;
import pl.landmc.platform.database.DatabaseType;
import pl.landmc.platform.notice.PlatformNotice;
import pl.landmc.platform.paper.notice.PaperNoticeService;

/**
 * Configuration loading in the order {@code LobbyBootstrap} uses.
 *
 * <p>The notice service provides the serdes pack that lets {@code Notice} fields be written at
 * all, and it reads the very file that pack helps load - so the service is built first against a
 * provider filled in afterwards. If that order breaks, the plugin fails on enable; this catches
 * it here.
 */
class LobbyConfigurationTest {

    @Test
    void loadsBothFiles(@TempDir Path directory) {
        Loaded loaded = load(directory);

        assertEquals("lobby-1", loaded.config().lobby.serverId);
        assertEquals(300, loaded.config().lobby.autosaveSeconds);
        assertTrue(loaded.config().spawn.teleportOnJoin);
        assertTrue(loaded.config().messaging.enabled);
    }

    @Test
    void embedsThePlatformDatabaseSection(@TempDir Path directory) throws IOException {
        Loaded loaded = load(directory);

        String yaml = Files.readString(directory.resolve("config.yml"));

        assertTrue(yaml.contains("database:"), yaml);
        assertEquals(DatabaseType.H2, loaded.config().database.type,
                "a fresh lobby must work with no database server to set up");
    }

    @Test
    void writesNoticeFieldsAsYaml(@TempDir Path directory) throws IOException {
        load(directory);

        String yaml = Files.readString(directory.resolve("messages.yml"));

        assertTrue(yaml.contains("spawn-teleported:"), yaml);
        assertTrue(yaml.contains("profile-header:"), yaml);
        assertTrue(yaml.contains("Przeniesiono na spawn"), yaml);
    }

    @Test
    void embedsThePlatformTechnicalMessages(@TempDir Path directory) throws IOException {
        Loaded loaded = load(directory);

        assertTrue(Files.readString(directory.resolve("messages.yml")).contains("platform:"));
        assertEquals(
                "<red>Błąd> <gray>Nie posiadasz uprawnień do tej komendy.",
                loaded.messages().platform.message(PlatformNotice.COMMAND_NO_PERMISSION));
    }

    @Test
    void spawnIsUnsetUntilConfigured(@TempDir Path directory) {
        assertEquals("", load(directory).config().spawn.world);
    }

    @Test
    void spawnSurvivesBeingWrittenBack(@TempDir Path directory) throws IOException {
        ConfigService configs = new ConfigService(notices().okaeriSerdes());
        LobbyConfig config = configs.load(directory, "config.yml", LobbyConfig.class);

        config.spawn.world = "lobby";
        config.spawn.x = 12.5;
        config.spawn.yaw = 90.0f;
        configs.save(config);

        LobbyConfig reloaded = new ConfigService(notices().okaeriSerdes())
                .load(directory, "config.yml", LobbyConfig.class);

        assertEquals("lobby", reloaded.spawn.world);
        assertEquals(12.5, reloaded.spawn.x);
        assertEquals(90.0f, reloaded.spawn.yaw);
        assertTrue(Files.readString(directory.resolve("config.yml")).contains("world: lobby"));
    }

    private static PaperNoticeService<LobbyMessages> notices() {
        return new PaperNoticeService<>(locale -> new LobbyMessages(), ComponentFormatter.standard());
    }

    private static Loaded load(Path directory) {
        LobbyMessages[] holder = new LobbyMessages[1];

        PaperNoticeService<LobbyMessages> notices =
                new PaperNoticeService<>(locale -> holder[0], ComponentFormatter.standard());

        ConfigService configs = new ConfigService(notices.okaeriSerdes());
        LobbyConfig config = configs.load(directory, "config.yml", LobbyConfig.class);
        holder[0] = configs.load(directory, "messages.yml", LobbyMessages.class);

        return new Loaded(config, holder[0]);
    }

    private record Loaded(LobbyConfig config, LobbyMessages messages) {
    }
}
