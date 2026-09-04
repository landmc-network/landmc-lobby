package pl.landmc.lobby.listener;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import pl.landmc.lobby.spawn.SpawnService;

/**
 * Puts players at the lobby spawn on join and on respawn.
 *
 * <p>Both are a config read and at most one teleport - no query, no scan.
 */
public final class SpawnListener implements Listener {

    private final SpawnService spawn;

    public SpawnListener(SpawnService spawn) {
        this.spawn = Objects.requireNonNull(spawn, "spawn");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!this.spawn.teleportOnJoin()) {
            return;
        }

        this.spawn.spawn().ifPresent(location -> event.getPlayer().teleport(location));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Optional<Location> location = this.spawn.spawn();
        location.ifPresent(event::setRespawnLocation);
    }
}
