package pl.landmc.lobby.sidebar;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.slf4j.Logger;
import pl.landmc.menus.protocol.MenuProtocolException;
import pl.landmc.menus.protocol.SidebarProtocol;

/**
 * How many diamonds each player here has, as last told by the proxy.
 *
 * <p>This server owns none of it. The wallet lives on the proxy, which sends the number when a
 * player arrives and again whenever it changes, so drawing a scoreboard is reading a field
 * rather than asking a database once a second per player.
 *
 * <p>A player nobody has been told about yet shows zero. That is a first frame, not a wrong
 * answer: the message announcing them is on its way, and nothing here spends the number.
 */
public final class BalanceTracker implements PluginMessageListener {

    private final Logger logger;

    /**
     * Told when a player's number changes.
     *
     * <p>Settable rather than a constructor argument because the scoreboard needs this tracker
     * to draw itself, and this tracker needs the scoreboard to redraw it - one of the two has
     * to be wired up second.
     */
    private Consumer onChanged = player -> { };

    /** Concurrent because plugin messages and the scoreboard task are not the same thread. */
    private final Map<UUID, Long> balances = new ConcurrentHashMap<>();

    public BalanceTracker(Plugin plugin, Logger logger) {
        Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");

        plugin.getServer().getMessenger()
                .registerIncomingPluginChannel(plugin, SidebarProtocol.CHANNEL, this);
    }

    public void onChanged(Consumer onChanged) {
        this.onChanged = Objects.requireNonNull(onChanged, "onChanged");
    }

    public long balanceOf(UUID playerId) {
        return this.balances.getOrDefault(playerId, 0L);
    }

    public void forget(UUID playerId) {
        this.balances.remove(playerId);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!SidebarProtocol.CHANNEL.equals(channel)) {
            return;
        }

        long balance;
        try {
            balance = SidebarProtocol.decodeBalance(message);
        }
        catch (MenuProtocolException exception) {
            // Debug, not warn: this channel is reachable by a modified client, and a log line
            // per attempt is a way to fill a disk. Nothing is spent from this number.
            this.logger.debug(
                    "Unreadable sidebar update for {}: {}", player.getName(), exception.getMessage());
            return;
        }

        this.balances.put(player.getUniqueId(), balance);
        this.onChanged.accept(player);
    }

    /** What to do when a player's number changes. */
    @FunctionalInterface
    public interface Consumer {

        void accept(Player player);
    }
}
