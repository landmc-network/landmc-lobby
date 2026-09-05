package pl.landmc.lobby.hotbar;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.lobby.config.LobbyConfig.HotbarItem;
import pl.landmc.menus.protocol.MenuKind;
import pl.landmc.platform.component.ComponentFormatter;
import pl.landmc.platform.paper.menu.Items;

/**
 * What a player holds on the lobby.
 *
 * <p>The items are built once, at startup, and handed out as copies. A hotbar is filled on every
 * join, and rebuilding the stacks from configuration each time - matching the material, parsing
 * MiniMessage for the name and every lore line - is work repeated per player for a result that
 * never differs.
 *
 * <p>The exception is a player head, which wears the face of whoever holds it and so has to be
 * made on the join it belongs to.
 */
public final class HotbarService {

    /** A player's inventory: nine hotbar slots, then three rows of storage. */
    private static final int LAST_SLOT = 35;

    private final LobbyConfig config;
    private final ComponentFormatter formatter;
    private final Logger logger;

    /** Slot to item, prepared. A slot with no entry is one this lobby leaves empty. */
    private final Map<Integer, ItemStack> prepared = new HashMap<>();

    /** Slot to the item that has to be built per player, because it is their own head. */
    private final Map<Integer, HotbarItem> heads = new HashMap<>();

    /** Slot to menu, so a click is a lookup rather than a walk over the list. */
    private final Map<Integer, MenuKind> menus = new HashMap<>();

    public HotbarService(LobbyConfig config, ComponentFormatter formatter, Logger logger) {
        this.config = Objects.requireNonNull(config, "config");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.logger = Objects.requireNonNull(logger, "logger");

        this.prepare();
    }

    public boolean isEnabled() {
        return this.config.hotbar.enabled && !(this.prepared.isEmpty() && this.heads.isEmpty());
    }

    /** Clears the inventory and puts the lobby items in it. */
    public void give(Player player) {
        player.getInventory().clear();

        for (Map.Entry<Integer, ItemStack> entry : this.prepared.entrySet()) {
            // A copy, so a player who somehow moves one cannot change what the next player gets.
            player.getInventory().setItem(entry.getKey(), entry.getValue().clone());
        }

        for (Map.Entry<Integer, HotbarItem> entry : this.heads.entrySet()) {
            player.getInventory().setItem(entry.getKey(), this.head(entry.getValue(), player));
        }

        player.getInventory().setHeldItemSlot(Math.clamp(this.config.hotbar.selectedSlot, 0, 8));
    }

    /** The menu a slot opens, or empty when that slot does nothing. */
    public Optional<MenuKind> menuFor(int slot) {
        return Optional.ofNullable(this.menus.get(slot));
    }

    private void prepare() {
        if (!this.config.hotbar.enabled) {
            return;
        }

        for (HotbarItem item : this.config.hotbar.items) {
            if (item.slot < 0 || item.slot > LAST_SLOT) {
                this.logger.warn(
                        "Hotbar item '{}' sits in slot {}, which is not part of a player's"
                                + " inventory; skipping it.",
                        item.name, item.slot);
                continue;
            }

            if (item.playerHead) {
                this.heads.put(item.slot, item);
                this.register(item);
                continue;
            }

            Material material = Material.matchMaterial(item.material);
            if (material == null || material.isAir()) {
                // Named, not silent: a mistyped material is a slot that quietly does nothing,
                // and the server owner should hear about it once at startup.
                this.logger.warn(
                        "Hotbar item '{}' asks for material '{}', which this server does not"
                                + " have; skipping it.",
                        item.name, item.material);
                continue;
            }

            this.prepared.put(item.slot, Items.of(material)
                    .name(this.formatter.format(item.name))
                    .lore(this.formatter.format(item.lore))
                    .plain()
                    .build());
            this.register(item);
        }
    }

    /**
     * Remembers which menu a slot opens.
     *
     * <p>Only the names this build knows. A typo names no menu and the item simply does
     * nothing, which is said once at startup rather than on every click.
     */
    private void register(HotbarItem item) {
        if (item.menu.isBlank()) {
            return;
        }

        try {
            this.menus.put(item.slot, MenuKind.valueOf(item.menu.toUpperCase(Locale.ROOT)));
        }
        catch (IllegalArgumentException unknown) {
            this.logger.warn(
                    "Hotbar item '{}' opens '{}', which is not a menu this build knows;"
                            + " it will do nothing.",
                    item.name, item.menu);
        }
    }

    private ItemStack head(HotbarItem item, Player player) {
        return Items.head(player)
                .name(this.formatter.format(item.name))
                .lore(this.formatter.format(item.lore))
                .plain()
                .build();
    }
}
