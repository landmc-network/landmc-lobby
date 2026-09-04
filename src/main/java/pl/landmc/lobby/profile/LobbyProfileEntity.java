package pl.landmc.lobby.profile;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.UUID;

/**
 * How a profile is stored. Nothing outside {@link ProfileRepository} touches this class.
 *
 * <p>Kept separate from {@link LobbyProfile} on purpose. The domain object is mutable and owned
 * by the main thread; this one is built on a database worker thread and never leaves it. Using
 * one class for both is how a half-saved profile ends up visible to gameplay.
 *
 * <p>{@code lastSeen} is indexed because the only query that is not a lookup by id sorts by it.
 * Nothing else is indexed: an index costs write throughput on every save.
 */
@DatabaseTable(tableName = "lobby_profiles")
public class LobbyProfileEntity {

    @DatabaseField(id = true, columnName = "player_id")
    public UUID playerId;

    @DatabaseField(columnName = "name", canBeNull = false, width = 16)
    public String name;

    @DatabaseField(columnName = "first_join")
    public long firstJoin;

    @DatabaseField(columnName = "last_seen", index = true)
    public long lastSeen;

    @DatabaseField(columnName = "visits")
    public int visits;

    /** Required by ORMLite. */
    public LobbyProfileEntity() {
    }

    LobbyProfileEntity(LobbyProfileSnapshot snapshot) {
        this.playerId = snapshot.playerId();
        this.name = snapshot.name();
        this.firstJoin = snapshot.firstJoin();
        this.lastSeen = snapshot.lastSeen();
        this.visits = snapshot.visits();
    }

    LobbyProfileSnapshot toSnapshot() {
        return new LobbyProfileSnapshot(
                this.playerId, this.name, this.firstJoin, this.lastSeen, this.visits);
    }
}
