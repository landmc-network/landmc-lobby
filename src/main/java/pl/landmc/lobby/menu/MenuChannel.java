package pl.landmc.lobby.menu;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.landmc.menus.protocol.MenuAction;
import pl.landmc.menus.protocol.MenuKind;
import pl.landmc.menus.protocol.MenuProtocol;

/**
 * Asks the proxy to do something on behalf of a player who pressed or clicked on this server.
 *
 * <p>The commands the hotbar items lead to - {@code /serwery}, {@code /profil}, {@code /sklep} -
 * are the proxy's, not this server's. A command dispatched here never leaves the backend, which
 * is why running one from the item did nothing at all: Bukkit looked it up locally, found
 * nothing, and stopped. Typing it works only because the client sends it to the proxy first.
 *
 * <p>Sending somebody to another server is the same problem. A backend cannot move a player
 * across the network; only the proxy can, and it already knows how - it is what the servers
 * menu does when a tile is clicked. So the figure on the spawn sends exactly the message that
 * tile sends, and none of the routing is duplicated here.
 *
 * <p>What travels is the name of a menu or of a server, never a command line: the receiver has
 * a fixed list of each, so nothing sent from here can ask it to run something else.
 */
public final class MenuChannel {

    private final Plugin plugin;

    public MenuChannel(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");

        // Outgoing only. What comes back on this channel is a menu, and the plugin that draws
        // menus is the one listening for it.
        plugin.getServer().getMessenger()
                .registerOutgoingPluginChannel(plugin, MenuProtocol.CHANNEL);
    }

    public void open(Player player, MenuKind menu) {
        this.send(player, MenuAction.of(menu, "open"));
    }

    /** Asks the proxy to move this player to that server, as a clicked tile would. */
    public void connect(Player player, String serverId) {
        this.send(player, MenuAction.of(MenuKind.SERVERS, "connect", serverId));
    }

    private void send(Player player, MenuAction action) {
        player.sendPluginMessage(
                this.plugin, MenuProtocol.CHANNEL, MenuProtocol.encode(action));
    }
}
