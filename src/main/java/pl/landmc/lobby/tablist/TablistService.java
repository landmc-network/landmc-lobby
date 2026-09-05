package pl.landmc.lobby.tablist;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.platform.component.ComponentFormatter;

/**
 * How the tab list reads.
 *
 * <p>The name itself is what the old server did and no more: the rank's prefix followed by the
 * player's name, so the list says who is here and what they are. The header and footer above and
 * below it were not there and were added since, on request.
 *
 * <p>The list this fills is the one sent by the server the player is standing on, so it holds
 * the people on that server rather than the whole network. That was true of the original too.
 */
public final class TablistService {

    /** How a configured header or footer's lines are joined into the one string each is. */
    private static final String NEWLINE = "\n";

    private final LobbyConfig config;
    private final ComponentFormatter formatter;

    /**
     * Where a prefix comes from.
     *
     * <p>Set after the server has finished loading rather than in the constructor. This plugin
     * is enabled at STARTUP so that it can generate the default world, which puts it ahead of
     * LuckPerms - asking for LuckPerms that early answers that it is not installed.
     */
    private RankPrefixes prefixes = RankPrefixes.NONE;

    /**
     * What each player's entry was last set to.
     *
     * <p>Kept so an entry is only written when it actually changed. Setting it is a packet to
     * everybody who can see that player, and the refresh runs on a timer - writing the same
     * name every second would be a broadcast per player per second that changes nothing.
     */
    private final Map<UUID, String> written = new ConcurrentHashMap<>();

    /** The header and footer each player was last sent, held to the same rule. */
    private final Map<UUID, String> headers = new ConcurrentHashMap<>();
    private final Map<UUID, String> footers = new ConcurrentHashMap<>();

    public TablistService(LobbyConfig config, ComponentFormatter formatter) {
        this.config = Objects.requireNonNull(config, "config");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    public void prefixes(RankPrefixes prefixes) {
        this.prefixes = Objects.requireNonNull(prefixes, "prefixes");
    }

    public boolean isEnabled() {
        return this.config.tablist.enabled;
    }

    /** Writes this player's entry, if it is not already what it should be. */
    public void apply(Player player) {
        String entry = this.config.tablist.format
                // The prefix is the network's own configuration and arrives written as colour.
                .replace("{PREFIX}", this.prefixes.of(player))
                .replace("{PLAYER}", player.getName());

        String previous = this.written.put(player.getUniqueId(), entry);
        if (entry.equals(previous)) {
            return;
        }

        player.playerListName(this.formatter.format(entry));
    }

    /**
     * Sends the header and footer, if they are not already what they should be.
     *
     * <p>Held to the same rule as the entry: this is a packet, the refresh runs on a timer, and
     * a header that says the same thing does not need saying again.
     *
     * <p>Both are sent whenever either changed, because both travel in one packet and there is
     * no way to send half of it.
     */
    public void surround(Player player) {
        List<String> header = this.config.tablist.header;
        List<String> footer = this.config.tablist.footer;

        if (header.isEmpty() && footer.isEmpty()) {
            return;
        }

        String resolvedHeader = this.resolve(String.join(NEWLINE, header), player);
        String resolvedFooter = this.resolve(String.join(NEWLINE, footer), player);

        String previousHeader = this.headers.put(player.getUniqueId(), resolvedHeader);
        String previousFooter = this.footers.put(player.getUniqueId(), resolvedFooter);

        if (resolvedHeader.equals(previousHeader) && resolvedFooter.equals(previousFooter)) {
            return;
        }

        player.sendPlayerListHeaderAndFooter(
                this.formatter.format(resolvedHeader), this.formatter.format(resolvedFooter));
    }

    public void applyAll(Iterable<? extends Player> players) {
        for (Player player : players) {
            this.apply(player);
            this.surround(player);
        }
    }

    public void forget(Player player) {
        this.written.remove(player.getUniqueId());
        this.headers.remove(player.getUniqueId());
        this.footers.remove(player.getUniqueId());
    }

    private String resolve(String text, Player player) {
        return text
                .replace("{PLAYER}", player.getName())
                .replace("{SERVER}", this.config.lobby.serverId)
                .replace("{ONLINE}", Integer.toString(Bukkit.getOnlinePlayers().size()));
    }
}
