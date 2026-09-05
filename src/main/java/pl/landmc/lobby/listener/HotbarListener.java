package pl.landmc.lobby.listener;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import pl.landmc.lobby.menu.MenuChannel;
import pl.landmc.lobby.hotbar.HotbarService;

/**
 * Gives out the lobby hotbar and answers clicks on it.
 *
 * <p>Also stops the items being dropped or moved. That is not tidiness: the hotbar is how a
 * player reaches the servers, the shop and their profile, and one that can be thrown away is one
 * a player loses by accident and then cannot get back without reconnecting.
 */
public final class HotbarListener implements Listener {

    private final HotbarService hotbar;
    private final MenuChannel channel;

    public HotbarListener(HotbarService hotbar, MenuChannel channel) {
        this.hotbar = Objects.requireNonNull(hotbar, "hotbar");
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.hotbar.give(event.getPlayer());
    }

    /**
     * Opens the menu behind whatever the player is holding.
     *
     * <p>Only the main hand: an interaction fires once per hand, and answering both would open
     * the menu twice.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isClick(event.getAction())) {
            return;
        }

        Player player = event.getPlayer();
        this.hotbar.menuFor(player.getInventory().getHeldItemSlot()).ifPresent(menu -> {
            // Cancelled so the click does not also place a block or hit what is in front of it.
            event.setCancelled(true);
            this.channel.open(player, menu);
        });
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    /**
     * Stops the hotbar being rearranged.
     *
     * <p>Only the player's own inventory. A menu is an inventory too, and cancelling everything
     * here would be the lobby quietly breaking every menu on the network.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {

            event.setCancelled(true);
        }
    }

    private static boolean isClick(Action action) {
        return action == Action.LEFT_CLICK_AIR
                || action == Action.LEFT_CLICK_BLOCK
                || action == Action.RIGHT_CLICK_AIR
                || action == Action.RIGHT_CLICK_BLOCK;
    }
}
