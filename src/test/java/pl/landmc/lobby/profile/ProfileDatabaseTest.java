package pl.landmc.lobby.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
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
 * The profile layer against a real embedded database.
 *
 * <p>This is the first place anything actually runs {@code platform-database} outside the
 * platform's own tests: a real Hikari pool, a real ORMLite connection source, a real table. The
 * parts worth covering here - the row created on first join, a transaction that spans several
 * writes, ordering done by SQL rather than in Java - only behave correctly against a driver.
 */
class ProfileDatabaseTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileDatabaseTest.class);

    private DatabaseService database;
    private ProfileRepository repository;

    @BeforeEach
    void openDatabase(@TempDir Path directory) {
        DatabaseConfig config = new DatabaseConfig();
        config.type = DatabaseType.H2;
        config.fileName = "lobby-test";
        config.poolSize = 2;

        this.database = new DatabaseService("landmc-lobby-test", config, directory, LOGGER);
        this.database.enable();

        this.repository = new ProfileRepository(this.database);
        this.repository.createTables();
    }

    @AfterEach
    void closeDatabase() {
        if (this.database != null) {
            this.database.close();
        }
    }

    @Test
    void firstJoinCreatesTheRow() throws Exception {
        UUID playerId = UUID.randomUUID();

        LobbyProfileSnapshot created = this.repository.findOrCreate(playerId, "Crispi", 1_000L);

        assertEquals(playerId, created.playerId());
        assertEquals("Crispi", created.name());
        assertEquals(1_000L, created.firstJoin());
        assertEquals(1, created.visits());
        assertEquals(1, this.repository.count());
    }

    @Test
    void secondJoinReadsTheStoredRow() throws Exception {
        UUID playerId = UUID.randomUUID();
        this.repository.findOrCreate(playerId, "Crispi", 1_000L);

        LobbyProfileSnapshot found = this.repository.findOrCreate(playerId, "Crispi", 9_999L);

        assertEquals(1_000L, found.firstJoin(), "first join must not be overwritten on a later visit");
        assertEquals(1, this.repository.count(), "a second join must not insert a second row");
    }

    @Test
    void saveUpdatesTheStoredProfile() throws Exception {
        UUID playerId = UUID.randomUUID();
        LobbyProfileSnapshot created = this.repository.findOrCreate(playerId, "Crispi", 1_000L);

        this.repository.save(new LobbyProfileSnapshot(playerId, "CrispiRenamed", created.firstJoin(), 2_000L, 7));

        LobbyProfileSnapshot reloaded = this.repository.findOrCreate(playerId, "ignored", 3_000L);
        assertEquals("CrispiRenamed", reloaded.name());
        assertEquals(7, reloaded.visits());
        assertEquals(2_000L, reloaded.lastSeen());
    }

    @Test
    void saveAllWritesEveryProfile() throws Exception {
        List<LobbyProfileSnapshot> batch = List.of(
                LobbyProfileSnapshot.firstJoin(UUID.randomUUID(), "A", 1L),
                LobbyProfileSnapshot.firstJoin(UUID.randomUUID(), "B", 2L),
                LobbyProfileSnapshot.firstJoin(UUID.randomUUID(), "C", 3L));

        this.repository.saveAll(batch);

        assertEquals(3, this.repository.count());
    }

    @Test
    void saveAllOfOneStillWrites() throws Exception {
        this.repository.saveAll(List.of(LobbyProfileSnapshot.firstJoin(UUID.randomUUID(), "Solo", 1L)));

        assertEquals(1, this.repository.count());
    }

    @Test
    void saveAllOfNothingIsANoOp() throws Exception {
        this.repository.saveAll(List.of());

        assertEquals(0, this.repository.count());
    }

    /**
     * The ordering and the limit have to be the database's work; loading every row to sort in
     * Java would grow with the number of players who ever joined.
     */
    @Test
    void recentlySeenIsOrderedAndLimitedBySql() throws Exception {
        UUID oldest = UUID.randomUUID();
        UUID middle = UUID.randomUUID();
        UUID newest = UUID.randomUUID();

        this.repository.saveAll(List.of(
                new LobbyProfileSnapshot(oldest, "Oldest", 1L, 100L, 1),
                new LobbyProfileSnapshot(middle, "Middle", 1L, 200L, 1),
                new LobbyProfileSnapshot(newest, "Newest", 1L, 300L, 1)));

        List<LobbyProfileSnapshot> recent = this.repository.recentlySeen(2);

        assertEquals(2, recent.size());
        assertEquals(newest, recent.get(0).playerId());
        assertEquals(middle, recent.get(1).playerId());
    }

    @Test
    void queriesRunOffTheCallingThread() throws Exception {
        String worker = this.database
                .supplyAsync(() -> Thread.currentThread().getName())
                .get(10, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(worker.startsWith("landmc-lobby-test-db-"), worker);
        assertNotEquals(Thread.currentThread().getName(), worker);
    }
}
