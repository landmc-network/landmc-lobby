package pl.landmc.lobby.listener;

import java.util.Locale;
import java.util.Objects;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.lobby.menu.MenuChannel;
import pl.landmc.lobby.npc.NpcService;
import pl.landmc.menus.protocol.MenuKind;

/**
 * Sending somebody to a game mode by clicking its figure, or by walking into it.
 *
 * <p>Both were how the previous version of the network worked, and the second one is the reason
 * the figures stand on the floor rather than on a plinth: you walked at SkyBlock and you were on
 * SkyBlock.
 *
 * <p>Also the guard around them. An armour stand is furniture to this plugin and a container of
 * four armour pieces to everybody else - without cancelling the manipulate event, the first
 * player to right-click one walks away wearing the SkyBlock man's head.
 *
 * <p>The move handler is the hottest path a lobby has, so it does as little as possible: it
 * returns immediately unless the player crossed into a different block, and then asks a map,
 * not a list.
 */
public final class NpcListener implements Listener {

    private final NpcService npcs;
    private final MenuChannel channel;
    private final LobbyConfig config;

    public NpcListener(NpcService npcs, MenuChannel channel, LobbyConfig config) {
        this.npcs = Objects.requireNonNull(npcs, "npcs");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.config = Objects.requireNonNull(config, "config");
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(PlayerInteractAtEntityEvent event) {
        LobbyConfig.NpcEntry entry = this.npcs.owning(event.getRightClicked());
        if (entry == null) {
            return;
        }

        event.setCancelled(true);
        this.send(event.getPlayer(), entry);
    }

    /**
     * Stops the figure being undressed.
     *
     * <p>Cancelled for every armour stand of ours whether or not the player took anything: the
     * event fires for putting an item on as well as taking one off.
     */
    @EventHandler(ignoreCancelled = true)
    public void onManipulate(PlayerArmorStandManipulateEvent event) {
        if (this.npcs.owning(event.getRightClicked()) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Stops the figure being broken.
     *
     * <p>They are spawned invulnerable, which covers most of it, but a plugin that damages
     * entities directly does not go through that flag - and an armour stand destroyed in
     * adventure mode is one the next refresh silently puts back, which reads as a flicker
     * rather than as a fixed problem.
     */
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (this.npcs.owning(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!this.config.npcs.walkIn) {
            return;
        }

        // Most moves are within one block: a player walking in a straight line fires this
        // twenty times a second and changes block perhaps twice.
        if (event.getTo().getBlockX() == event.getFrom().getBlockX()
                && event.getTo().getBlockY() == event.getFrom().getBlockY()
                && event.getTo().getBlockZ() == event.getFrom().getBlockZ()) {

            return;
        }

        LobbyConfig.NpcEntry entry = this.npcs.standingIn(event.getTo());
        // Walking through a figure that opens a menu does not open it: a menu that appears
        // because somebody walked past is a menu they did not ask for.
        if (entry != null && NpcService.sendsToAServer(entry)) {
            this.send(event.getPlayer(), entry);
        }
    }

    /**
     * Does whatever the figure is for.
     *
     * <p>Both halves go through the proxy. Moving somebody to another server is obviously its
     * business, and so is opening a menu: the menus are drawn from a payload the proxy builds,
     * and a backend has no list of them to open.
     */
    private void send(Player player, LobbyConfig.NpcEntry entry) {
        if (NpcService.sendsToAServer(entry)) {
            if (entry.server.isBlank()) {
                return;
            }
            this.channel.connect(player, entry.server);
            this.click(player);
            return;
        }

        MenuKind menu = menu(entry.menu);
        if (menu == null) {
            return;
        }
        this.channel.open(player, menu);
        this.click(player);
    }

    /** The note the old server played when somebody clicked one of these. */
    private void click(Player player) {
        String name = this.config.npcs.clickSound;
        if (name.isBlank()) {
            return;
        }

        try {
            player.playSound(
                    player.getLocation(), Sound.valueOf(name.toUpperCase(Locale.ROOT)), 1.0F, 1.0F);
        }
        catch (IllegalArgumentException unknown) {
            // A name this build does not have. Silent: a missing note is not worth a line in
            // the log every time somebody clicks.
        }
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
