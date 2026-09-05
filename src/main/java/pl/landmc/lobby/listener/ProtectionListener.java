package pl.landmc.lobby.listener;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import pl.landmc.lobby.config.LobbyConfig;

/**
 * Keeping people alive in a place where nothing is meant to hurt them.
 *
 * <p>Adventure mode and a peaceful difficulty between them stop mobs, hunger loss and most
 * else, but not falling. A lobby is a built map with drops in it, and a player who walks off a
 * ledge and dies on the way to picking a game mode is a player who has just met the network at
 * its worst.
 *
 * <p>All damage rather than only falling, which is what the previous version did. A lobby with
 * one kind of damage switched off and another left on is a lobby somebody eventually finds a
 * way to kill people in.
 */
public final class ProtectionListener implements Listener {

    private final LobbyConfig config;

    public ProtectionListener(LobbyConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (this.config.protection.blockDamage && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    /**
     * Hunger, which peaceful already handles and easy does not.
     *
     * <p>Here so that turning the difficulty up - which is what somebody does the day they want
     * a pet that is technically a monster - does not quietly start starving the lobby.
     */
    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (this.config.protection.keepFed && event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }
}
