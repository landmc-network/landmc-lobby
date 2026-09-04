package pl.landmc.lobby.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.landmc.platform.database.DatabaseConfig;
import pl.landmc.platform.database.DatabaseService;
import pl.landmc.platform.database.DatabaseType;

/**
 * The join → cache → autosave → quit lifecycle, against a real database.
 *
 * <p>The "main thread" is a direct executor here, which makes the hop back deterministic without
 * changing what is being tested: the point is which data crosses the boundary and when it is
 * written, not which thread runs the continuation.
 */
class ProfileServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileServiceTest.class);

    private DatabaseService database;
    private ProfileRepository repository;
    private ProfileService profiles;

    @BeforeEach
    void open(@TempDir Path directory) {
        DatabaseConfig config = new DatabaseConfig();
        config.type = DatabaseType.H2;
        config.fileName = "lobby-service-test";
        config.poolSize = 2;

        this.database = new DatabaseService("lobby-service-test", config, directory, LOGGER);
        this.database.enable();

        this.repository = new ProfileRepository(this.database);
        this.repository.createTables();
        this.profiles = new ProfileService(this.repository, this.database, Runnable::run, LOGGER);
    }

    @AfterEach
    void close() {
        if (this.database != null) {
            this.database.close();
        }
    }

    @Test
    void joinLoadsTheProfileIntoTheCache() {
        UUID playerId = UUID.randomUUID();

        this.profiles.loadOnJoin(playerId, "Crispi", 1_000L).join();

        LobbyProfile profile = this.profiles.find(playerId).orElseThrow();
        assertEquals("Crispi", profile.name());
        assertEquals(1, this.profiles.cached());
    }

    @Test
    void everyJoinCountsAsAVisit() {
        UUID playerId = UUID.randomUUID();

        this.profiles.loadOnJoin(playerId, "Crispi", 1_000L).join();
        assertEquals(2, this.profiles.find(playerId).orElseThrow().visits(),
                "the row is created with one visit and the join adds the current one");

        this.profiles.unload(playerId, 2_000L).join();
        this.profiles.loadOnJoin(playerId, "Crispi", 3_000L).join();

        assertEquals(3, this.profiles.find(playerId).orElseThrow().visits());
    }

    @Test
    void aRenamedPlayerKeepsTheirProfile() {
        UUID playerId = UUID.randomUUID();

        this.profiles.loadOnJoin(playerId, "OldName", 1_000L).join();
        this.profiles.unload(playerId, 2_000L).join();
        this.profiles.loadOnJoin(playerId, "NewName", 3_000L).join();

        LobbyProfile profile = this.profiles.find(playerId).orElseThrow();
        assertEquals("NewName", profile.name());
        assertEquals(1_000L, profile.firstJoin());
    }

    @Test
    void quitPersistsAndForgets() throws Exception {
        UUID playerId = UUID.randomUUID();
        this.profiles.loadOnJoin(playerId, "Crispi", 1_000L).join();

        this.profiles.unload(playerId, 5_000L).join();

        assertEquals(0, this.profiles.cached());
        assertTrue(this.profiles.find(playerId).isEmpty());
        assertEquals(5_000L, this.repository.findOrCreate(playerId, "Crispi", 9_000L).lastSeen());
    }

    @Test
    void unloadingSomeoneWhoWasNeverLoadedIsHarmless() {
        this.profiles.unload(UUID.randomUUID(), 1_000L).join();

        assertEquals(0, this.profiles.cached());
    }

    /** The autosave must write only what changed, not every online profile on a timer. */
    @Test
    void autosaveWritesOnlyDirtyProfiles() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        this.profiles.loadOnJoin(first, "First", 1_000L).join();
        this.profiles.loadOnJoin(second, "Second", 1_000L).join();

        assertEquals(2, this.profiles.saveDirty().join(), "both are dirty right after joining");
        assertEquals(0, this.profiles.saveDirty().join(), "nothing changed since, so nothing is written");

        this.profiles.find(first).orElseThrow().touch(2_000L);

        assertEquals(1, this.profiles.saveDirty().join());
    }

    @Test
    void autosavePersistsWhatItCollected() throws Exception {
        UUID playerId = UUID.randomUUID();
        this.profiles.loadOnJoin(playerId, "Crispi", 1_000L).join();
        this.profiles.find(playerId).orElseThrow().touch(4_242L);

        this.profiles.saveDirty().join();

        assertEquals(4_242L, this.repository.findOrCreate(playerId, "Crispi", 9_000L).lastSeen());
    }

    @Test
    void clearDropsTheCacheWithoutTouchingTheDatabase() throws Exception {
        UUID playerId = UUID.randomUUID();
        this.profiles.loadOnJoin(playerId, "Crispi", 1_000L).join();

        this.profiles.clear();

        assertEquals(0, this.profiles.cached());
        assertEquals(1, this.repository.count(), "the row created on join is still there");
    }

    @Test
    void aFailedLoadLeavesThePlayerWithoutAProfileRatherThanBreaking() {
        this.database.close();

        this.profiles.loadOnJoin(UUID.randomUUID(), "Crispi", 1_000L).join();

        assertEquals(0, this.profiles.cached());
    }

    @Test
    void snapshotClearsTheDirtyFlag() {
        LobbyProfile profile = new LobbyProfile(LobbyProfileSnapshot.firstJoin(UUID.randomUUID(), "A", 1L));

        assertFalse(profile.isDirty());
        profile.touch(2L);
        assertTrue(profile.isDirty());

        LobbyProfileSnapshot snapshot = profile.snapshotAndClean();

        assertFalse(profile.isDirty());
        assertEquals(2L, snapshot.lastSeen());
    }
}
