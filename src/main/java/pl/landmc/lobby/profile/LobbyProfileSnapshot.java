package pl.landmc.lobby.profile;

import java.util.UUID;

/**
 * An immutable copy of a profile, taken on the main thread and handed to a database worker.
 *
 * <p>This is what crosses the thread boundary. Passing the live {@link LobbyProfile} instead
 * would mean gameplay can mutate a row while it is being written - the class of bug that shows
 * up as a value that is neither the old one nor the new one, weeks later, and cannot be
 * reproduced.
 */
public record LobbyProfileSnapshot(
        UUID playerId, String name, long firstJoin, long lastSeen, int visits) {

    public static LobbyProfileSnapshot firstJoin(UUID playerId, String name, long now) {
        return new LobbyProfileSnapshot(playerId, name, now, now, 1);
    }
}
