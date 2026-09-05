package pl.landmc.lobby.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.lobby.config.LobbyMessages;
import pl.landmc.platform.component.ComponentFormatter;
import pl.landmc.platform.config.ConfigPlaceholders;
import pl.landmc.platform.config.ConfigService;
import pl.landmc.platform.paper.notice.PaperNoticeService;

/**
 * A figure put up by the command has to still be there after a restart.
 *
 * <p>This exists because it was not. {@code /npc utworz} added the entry, spawned the figure and
 * reported success, and the file it wrote a moment later said the list was empty - so the whole
 * feature worked until the first restart and then quietly did not.
 *
 * <p>The test is about the round trip rather than about the command: add an entry to a loaded
 * configuration, write it, read it back in a new process's worth of objects, and expect to find
 * it. That is the only part the bug was in, and it needs no server to check.
 */
class NpcPersistenceTest {

    @Test
    void keepsAFigureAcrossASave(@TempDir Path directory) throws IOException {
        ConfigService configs = configs(directory);
        LobbyConfig config = configs.load(directory, "config.yml", LobbyConfig.class);

        LobbyConfig.NpcEntry entry = new LobbyConfig.NpcEntry();
        entry.id = "skyblock";
        entry.server = "skyblock";
        entry.name = "<green>SkyBlock";
        entry.addon = "Kliknij, aby dołączyć";
        entry.world = "lobby";
        entry.x = 1.5D;
        entry.y = 100.0D;
        entry.z = -2.5D;
        entry.yaw = -90.0F;

        config.npcs.list.add(entry);
        configs.save(config);

        String written = Files.readString(directory.resolve("config.yml"));
        assertTrue(written.contains("id: skyblock"), "the figure is missing from the file:\n" + written);

        LobbyConfig reloaded = configs(directory).load(directory, "config.yml", LobbyConfig.class);
        assertEquals(1, reloaded.npcs.list.size());
        assertEquals("skyblock", reloaded.npcs.list.get(0).server);
        assertEquals(-90.0F, reloaded.npcs.list.get(0).yaw);
    }

    @Test
    void shipsTheOutfitsTheOldServerHadInItsSource(@TempDir Path directory) {
        LobbyConfig config = configs(directory).load(directory, "config.yml", LobbyConfig.class);

        assertEquals(3, config.npcs.presets.size());
        assertEquals("skyblock", config.npcs.presets.get(0).id);
        assertEquals("GRASS_BLOCK", config.npcs.presets.get(0).item);
    }

    /**
     * Built exactly as the plugin builds it, placeholders included.
     *
     * <p>The placeholder layer rewrites the rendered YAML on the way out, putting {@code
     * ${LANDMC_DB_HOST}} back where the resolved value stands - so it is part of every save on a
     * real server and none of a save in a test that leaves it out.
     */
    private static ConfigService configs(Path directory) {
        LobbyMessages[] holder = new LobbyMessages[1];
        PaperNoticeService<LobbyMessages> notices =
                new PaperNoticeService<>(locale -> holder[0], ComponentFormatter.standard());
        return new ConfigService(
                ConfigPlaceholders.forPlugin(directory), notices.okaeriSerdes());
    }
}
