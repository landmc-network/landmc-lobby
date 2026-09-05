package pl.landmc.lobby.listener;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import pl.landmc.lobby.spawn.SpawnService;

/**
 * Puts players at the lobby spawn on join and on respawn, and marks the arrival.
 *
 * <p>All of it is a config read and at most one teleport - no query, no scan.
 */
public final class SpawnListener implements Listener {

    /**
     * How long to wait before playing the arrival effect.
     *
     * <p>Not a flourish - a client that is still loading the world drops entity effects sent to
     * it, so the animation played during the join event is one nobody ever sees. Half a second
     * is past that and still reads as part of arriving.
     */
    private static final long EFFECT_DELAY_TICKS = 10L;

    private final SpawnService spawn;
    private final Plugin plugin;

    public SpawnListener(SpawnService spawn, Plugin plugin) {
        this.spawn = Objects.requireNonNull(spawn, "spawn");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (this.spawn.teleportOnJoin()) {
            this.spawn.spawn().ifPresent(player::teleport);
        }

        if (this.spawn.totemOnJoin()) {
            // The old server played this when somebody logged in, and arriving here is the same
            // moment: a player who has not logged in is held on the limbo and never reaches the
            // lobby at all.
            //
            // Scheduled on the player rather than on the server, so it follows them if they are
            // moved and quietly does nothing if they leave first.
            player.getScheduler().runDelayed(
                    this.plugin,
                    task -> player.playEffect(EntityEffect.TOTEM_RESURRECT),
                    null,
                    EFFECT_DELAY_TICKS);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Optional<Location> location = this.spawn.spawn();
        location.ifPresent(event::setRespawnLocation);
    }
}
