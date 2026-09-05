package pl.landmc.lobby.bossbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.lobby.profile.ProfileService;
import pl.landmc.lobby.sidebar.LobbyLevel;
import pl.landmc.lobby.sidebar.BalanceTracker;
import pl.landmc.lobby.sidebar.UiText;
import pl.landmc.platform.component.ComponentFormatter;

/**
 * The panel across the top of the lobby.
 *
 * <p>A boss bar is the one place a server can put text on a player's screen with nothing of the
 * client's own behind it: its bar is a texture, and the pack replaces that texture with an
 * empty image. The scoreboard's background is not a texture and cannot be replaced at all,
 * which is why every server with a floating interface builds one here.
 *
 * <p>A title is one line, so a panel of several rows is several bars. The client stacks them
 * itself, nineteen pixels apart, in the order they were shown.
 *
 * <p>Per player, unlike the board's panels, because what is on it is theirs - a diamond count is
 * not the same number for everybody. That costs a few objects each and a title only when the
 * text actually changes; a row that would read the same is left alone, since setting a title is
 * a packet and this runs on a timer.
 */
public final class BossBarService {

    private final LobbyConfig config;
    private final ComponentFormatter formatter;
    private final UiText ui;
    private final BalanceTracker balances;
    private final ProfileService profiles;

    /** Each player's bars, in the order they are drawn. */
    private final Map<UUID, List<BossBar>> bars = new HashMap<>();

    /** What each of those bars was last set to, so an unchanged row is not sent again. */
    private final Map<UUID, List<String>> written = new HashMap<>();

    public BossBarService(
            LobbyConfig config,
            ComponentFormatter formatter,
            UiText ui,
            BalanceTracker balances,
            ProfileService profiles) {

        this.config = Objects.requireNonNull(config, "config");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.ui = Objects.requireNonNull(ui, "ui");
        this.balances = Objects.requireNonNull(balances, "balances");
        this.profiles = Objects.requireNonNull(profiles, "profiles");
    }

    public boolean isEnabled() {
        return this.config.bossBar.enabled && !this.config.bossBar.lines.isEmpty();
    }

    /** Builds this player's bars and shows them. */
    public void show(Player player) {
        if (!this.isEnabled()) {
            return;
        }

        List<BossBar> playerBars = new ArrayList<>();
        List<String> playerLines = new ArrayList<>();

        for (String line : this.config.bossBar.lines) {
            String rendered = this.render(line, player);
            BossBar bar = BossBar.bossBar(
                    this.formatter.format(rendered),
                    // Full, because these are signs rather than a measure of anything.
                    1.0f,
                    colour(this.config.bossBar.colour),
                    overlay(this.config.bossBar.style));

            player.showBossBar(bar);
            playerBars.add(bar);
            playerLines.add(rendered);
        }

        this.bars.put(player.getUniqueId(), playerBars);
        this.written.put(player.getUniqueId(), playerLines);
    }

    /** Rewrites the rows whose text has actually changed. */
    public void refresh(Player player) {
        List<BossBar> playerBars = this.bars.get(player.getUniqueId());
        List<String> playerLines = this.written.get(player.getUniqueId());
        if (playerBars == null || playerLines == null) {
            return;
        }

        List<String> lines = this.config.bossBar.lines;
        for (int index = 0; index < playerBars.size() && index < lines.size(); index++) {
            String rendered = this.render(lines.get(index), player);
            if (rendered.equals(playerLines.get(index))) {
                continue;
            }

            playerLines.set(index, rendered);
            playerBars.get(index).name(this.formatter.format(rendered));
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.refresh(player);
        }
    }

    public void hide(Player player) {
        List<BossBar> playerBars = this.bars.remove(player.getUniqueId());
        this.written.remove(player.getUniqueId());

        if (playerBars != null) {
            for (BossBar bar : playerBars) {
                player.hideBossBar(bar);
            }
        }
    }

    /** Takes the bars off everybody, for a shutdown or a reload. */
    public void hideAll(Iterable<? extends Player> players) {
        for (Player player : players) {
            this.hide(player);
        }
    }

    private String render(String line, Player player) {
        String text = line
                .replace("{PLAYER}", player.getName())
                .replace("{SERVER}", this.config.lobby.serverId)
                .replace("{ONLINE}", Integer.toString(Bukkit.getOnlinePlayers().size()))
                .replace("{DIAMONDS}", Long.toString(this.balances.balanceOf(player.getUniqueId())))
                // Neither has a system behind it yet, and the board says the same about them.
                .replace("{COINS}", Long.toString(
                        this.balances.coinsOf(player.getUniqueId())))
                .replace("{LEVEL}", Integer.toString(this.levelOf(player)));

        return this.ui.render(text, this.config.bossBar.lineWidth);
    }

    /** The configured colour, or the old server's green when it names one that does not exist. */
    private static BossBar.Color colour(String name) {
        try {
            return BossBar.Color.valueOf(name.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException unknown) {
            return BossBar.Color.GREEN;
        }
    }

    private static BossBar.Overlay overlay(String name) {
        try {
            return BossBar.Overlay.valueOf(name.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException unknown) {
            // The original's SOLID: one unbroken bar, no notches.
            return BossBar.Overlay.PROGRESS;
        }
    }

    /**
     * Their level, from how often they have arrived.
     *
     * <p>Read from the profile the lobby already keeps rather than from a counter of its own.
     * A player whose profile has not finished loading is level nought for a moment, which is
     * what they were a second earlier anyway.
     */
    private int levelOf(Player player) {
        return this.profiles.find(player.getUniqueId())
                .map(profile -> LobbyLevel.of(
                        profile.visits(), this.config.level.visitsPerLevel))
                .orElse(0);
    }
}
