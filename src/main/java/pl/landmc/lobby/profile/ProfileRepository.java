package pl.landmc.lobby.profile;

import com.j256.ormlite.dao.Dao;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pl.landmc.platform.database.DatabaseService;

/**
 * Reads and writes profiles. The only class in the lobby that knows ORMLite exists.
 *
 * <p>Every method here runs on a database worker thread - it is called from
 * {@code DatabaseService#supplyAsync}, never directly from gameplay. The methods are
 * deliberately blocking and synchronous: making them return futures would hide which thread
 * they run on, which is exactly what the platform's executor is there to make explicit.
 *
 * <p>Lives in the lobby, not in {@code platform-database}. The platform provides the connection
 * and the DAO; what a profile is and how it is stored belongs to the plugin that owns the data.
 */
public final class ProfileRepository {

    private final DatabaseService database;

    public ProfileRepository(DatabaseService database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /** Creates the table on first start. Startup work, blocking. */
    public void createTables() {
        this.database.createTables(LobbyProfileEntity.class);
    }

    /**
     * The stored profile, or a fresh one written on first join.
     *
     * @throws SQLException so the caller's {@code supplyAsync} wraps it with context
     */
    public LobbyProfileSnapshot findOrCreate(UUID playerId, String name, long now) throws SQLException {
        Dao<LobbyProfileEntity, UUID> dao = this.dao();

        LobbyProfileEntity stored = dao.queryForId(playerId);
        if (stored != null) {
            return stored.toSnapshot();
        }

        LobbyProfileSnapshot created = LobbyProfileSnapshot.firstJoin(playerId, name, now);
        dao.create(new LobbyProfileEntity(created));
        return created;
    }

    public void save(LobbyProfileSnapshot snapshot) throws SQLException {
        this.dao().createOrUpdate(new LobbyProfileEntity(snapshot));
    }

    /**
     * Writes several profiles as one transaction.
     *
     * <p>Used by the autosave and by shutdown. One transaction rather than a statement per
     * profile: a hundred independent writes is a hundred round trips, and a partial autosave is
     * harder to reason about than one that either happened or did not.
     */
    public void saveAll(Collection<LobbyProfileSnapshot> snapshots) throws SQLException {
        if (snapshots.isEmpty()) {
            return;
        }
        if (snapshots.size() == 1) {
            this.save(snapshots.iterator().next());
            return;
        }

        Dao<LobbyProfileEntity, UUID> dao = this.dao();
        this.database.daos().inTransaction(() -> {
            for (LobbyProfileSnapshot snapshot : snapshots) {
                dao.createOrUpdate(new LobbyProfileEntity(snapshot));
            }
            return null;
        });
    }

    /**
     * The most recently seen players.
     *
     * <p>Ordered and limited by the database rather than by loading every row and sorting in
     * Java - the table grows with every player who ever joined.
     */
    public List<LobbyProfileSnapshot> recentlySeen(int limit) throws SQLException {
        return this.dao().queryBuilder()
                .orderBy("last_seen", false)
                .limit((long) Math.max(1, limit))
                .query()
                .stream()
                .map(LobbyProfileEntity::toSnapshot)
                .toList();
    }

    public long count() throws SQLException {
        return this.dao().countOf();
    }

    private Dao<LobbyProfileEntity, UUID> dao() {
        return this.database.dao(LobbyProfileEntity.class);
    }
}
