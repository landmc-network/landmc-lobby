package pl.landmc.lobby.profile;

import java.util.Objects;
import java.util.UUID;

/**
 * A player's lobby profile while they are online.
 *
 * <p>Owned by the main thread. Everything that reads or changes it - commands, listeners - runs
 * there, which is why nothing here is synchronized and no field is atomic: single-threaded
 * ownership is cheaper and easier to reason about than making the whole domain concurrent.
 *
 * <p>Changes set a dirty flag rather than writing to the database. The autosave then persists
 * only what actually changed, instead of rewriting every online profile on a timer.
 */
public final class LobbyProfile {

    private final UUID playerId;
    private final long firstJoin;

    private String name;
    private long lastSeen;
    private int visits;

    private boolean dirty;

    LobbyProfile(LobbyProfileSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        this.playerId = snapshot.playerId();
        this.name = snapshot.name();
        this.firstJoin = snapshot.firstJoin();
        this.lastSeen = snapshot.lastSeen();
        this.visits = snapshot.visits();
    }

    public UUID playerId() {
        return this.playerId;
    }

    public String name() {
        return this.name;
    }

    public long firstJoin() {
        return this.firstJoin;
    }

    public long lastSeen() {
        return this.lastSeen;
    }

    public int visits() {
        return this.visits;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    /** Records a new session: a name that may have changed, and one more visit. */
    public void recordJoin(String currentName, long now) {
        Objects.requireNonNull(currentName, "currentName");

        if (!currentName.equals(this.name)) {
            this.name = currentName;
        }
        this.visits++;
        this.lastSeen = now;
        this.dirty = true;
    }

    public void touch(long now) {
        this.lastSeen = now;
        this.dirty = true;
    }

    /**
     * An immutable copy for the database worker, and the point at which the profile stops being
     * dirty - anything changed after this call marks it again and is caught by the next save.
     */
    public LobbyProfileSnapshot snapshotAndClean() {
        this.dirty = false;
        return new LobbyProfileSnapshot(
                this.playerId, this.name, this.firstJoin, this.lastSeen, this.visits);
    }
}
