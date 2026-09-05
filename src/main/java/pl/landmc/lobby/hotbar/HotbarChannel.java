package pl.landmc.lobby.hotbar;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.landmc.menus.protocol.MenuAction;
import pl.landmc.menus.protocol.MenuKind;
import pl.landmc.menus.protocol.MenuProtocol;

/**
 * Asks the proxy to open a menu, on behalf of a player who pressed an item.
 *
 * <p>The commands these items lead to - {@code /serwery}, {@code /profil}, {@code /sklep} - are
 * the proxy's, not this server's. A command dispatched here never leaves the backend, which is
 * why running one from the item did nothing at all: Bukkit looked it up locally, found nothing,
 * and stopped. Typing it works only because the client sends it to the proxy first.
 *
 * <p>So the click travels the same way a menu click does, and the proxy decides what to do with
 * it. What is sent is the name of a menu, never a command line: the receiver has a fixed list of
 * menus it will open, so nothing here can ask it to run something else.
 */
public final class HotbarChannel {

    private final Plugin plugin;

    public HotbarChannel(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");

        // Outgoing only. What comes back on this channel is a menu, and the plugin that draws
        // menus is the one listening for it.
        plugin.getServer().getMessenger()
                .registerOutgoingPluginChannel(plugin, MenuProtocol.CHANNEL);
    }

    public void open(Player player, MenuKind menu) {
        player.sendPluginMessage(
                this.plugin,
                MenuProtocol.CHANNEL,
                MenuProtocol.encode(MenuAction.of(menu, "open")));
    }
}
