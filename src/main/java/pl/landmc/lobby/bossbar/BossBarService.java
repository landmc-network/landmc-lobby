package pl.landmc.lobby.bossbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.lobby.sidebar.UiText;
import pl.landmc.platform.component.ComponentFormatter;

/**
 * The panel across the top of the lobby.
 *
 * <p>A boss bar is the one place a server can put text on a player's screen with nothing of the
 * client's own behind it - its bar is a texture, and the pack replaces that texture with an
 * empty image. What is left is the title, and a title can carry the same drawn panels the
 * sidebar uses. That is why every server with a floating interface builds it here rather than
 * on the scoreboard, whose background is not a texture and cannot be replaced at all.
 *
 * <p>A title is one line, so a panel of several rows is several bars. The client stacks them
 * itself, in order, a fixed distance apart; the panel is drawn on the first and reaches down
 * over the rest.
 *
 * <p>One set of bars for the whole server rather than a set per player. Nothing in them depends
 * on who is looking - a count of everybody online is the same number for everybody - so a bar
 * per player would be an object and a packet each for identical text. It also means a refresh
 * is one update rather than one per player.
 */
public final class BossBarService {

    private final LobbyConfig config;
    private final ComponentFormatter formatter;
    private final UiText ui;

    /** One bar per configured row, in the order they are drawn. */
    private final List<BossBar> bars = new ArrayList<>();

    /** What each bar's title was last set to, so an unchanged row is not sent again. */
    private final List<String> written = new ArrayList<>();

    public BossBarService(LobbyConfig config, ComponentFormatter formatter, UiText ui) {
        this.config = Objects.requireNonNull(config, "config");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.ui = Objects.requireNonNull(ui, "ui");

        for (String line : config.bossBar.lines) {
            String rendered = this.render(line);
            this.bars.add(BossBar.bossBar(
                    this.formatter.format(rendered),
                    // Full, because these are signs rather than a measure of anything.
                    1.0f,
                    colour(config.bossBar.colour),
                    overlay(config.bossBar.style)));
            this.written.add(rendered);
        }
    }

    public boolean isEnabled() {
        return this.config.bossBar.enabled && !this.bars.isEmpty();
    }

    public void show(Player player) {
        if (!this.isEnabled()) {
            return;
        }
        for (BossBar bar : this.bars) {
            player.showBossBar(bar);
        }
    }

    public void hide(Player player) {
        for (BossBar bar : this.bars) {
            player.hideBossBar(bar);
        }
    }

    /** Takes the bars off everybody, for a shutdown or a reload. */
    public void hideAll(Iterable<? extends Player> players) {
        for (Player player : players) {
            this.hide(player);
        }
    }

    /**
     * Rewrites the rows that can change.
     *
     * <p>Changing a bar's title reaches everybody who can see it, so a row that says the same
     * thing is left alone: the refresh runs on a timer, and re-sending an unchanged line would
     * be a packet per player per tick for nothing.
     */
    public void refresh() {
        List<String> lines = this.config.bossBar.lines;

        for (int index = 0; index < this.bars.size() && index < lines.size(); index++) {
            String rendered = this.render(lines.get(index));
            if (rendered.equals(this.written.get(index))) {
                continue;
            }

            this.written.set(index, rendered);
            this.bars.get(index).name(this.formatter.format(rendered));
        }
    }

    /**
     * Fills in what the whole server shares.
     *
     * <p>Only that: a placeholder about one player would need a bar per player, and the point of
     * one shared set is that there is nothing per player in it.
     */
    private String render(String line) {
        String text = line
                .replace("{SERVER}", this.config.lobby.serverId)
                .replace("{ONLINE}", Integer.toString(Bukkit.getOnlinePlayers().size()));

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
}
