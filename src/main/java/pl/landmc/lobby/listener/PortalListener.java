package pl.landmc.lobby.listener;

import java.util.Locale;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.lobby.menu.MenuChannel;
import pl.landmc.menus.protocol.MenuKind;

/**
 * The portal on the spawn: run into it and it throws you back, then opens the game modes.
 *
 * <p>Straight out of the previous version, and the reason it works is that a portal reads as a
 * way through without anybody having to write "click here" over it. Nothing about it teleports,
 * so the game's own portal handling is switched off for these players rather than raced with.
 *
 * <p>The throw is backwards and a little up. The old server's was backwards and hard down,
 * which on a floor amounts to the same shove with a thump; up reads as a bounce.
 *
 * <p>The menu opens a moment later, not at once. Somebody is still moving when they touch it,
 * and a window that appears mid-stride is a window that catches a keystroke meant for walking.
 */
public final class PortalListener implements Listener {

    private final Plugin plugin;
    private final MenuChannel channel;
    private final LobbyConfig config;

    public PortalListener(Plugin plugin, MenuChannel channel, LobbyConfig config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.config = Objects.requireNonNull(config, "config");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!this.config.portal.enabled) {
            return;
        }

        // Most moves are within one block: a player walking in a straight line fires this
        // twenty times a second and changes block perhaps twice.
        if (event.getTo().getBlockX() == event.getFrom().getBlockX()
                && event.getTo().getBlockY() == event.getFrom().getBlockY()
                && event.getTo().getBlockZ() == event.getFrom().getBlockZ()) {

            return;
        }

        if (event.getTo().getBlock().getType() != Material.NETHER_PORTAL) {
            return;
        }

        Player player = event.getPlayer();
        this.bounce(player);

        MenuKind menu = menu(this.config.portal.menu);
        if (menu == null) {
            return;
        }

        // A few ticks, so the window does not appear while they are still walking into it.
        player.getScheduler().runDelayed(
                this.plugin,
                task -> this.channel.open(player, menu),
                null,
                Math.max(1L, this.config.portal.delayTicks));
    }

    /**
     * Stops the game taking them to the Nether.
     *
     * <p>A lobby portal is scenery. Without this, a player who lingers in it long enough is
     * teleported into a world that on this server does not exist as anywhere to be.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (this.config.portal.enabled) {
            event.setCancelled(true);
        }
    }

    private void bounce(Player player) {
        double strength = this.config.portal.bounce;
        if (strength <= 0.0D) {
            return;
        }

        Vector back = player.getLocation().getDirection().setY(0.0D);
        if (back.lengthSquared() < 1.0E-4D) {
            // Looking straight up or down: there is no "backwards" to speak of, so they only
            // get the lift, which is enough to break the walk into it.
            back = new Vector();
        }
        else {
            back = back.normalize().multiply(-strength);
        }

        player.setVelocity(back.setY(this.config.portal.lift));
    }

    private static MenuKind menu(String name) {
        try {
            return MenuKind.valueOf(name.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
