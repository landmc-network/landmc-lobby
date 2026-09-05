package pl.landmc.lobby.bossbar;

import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.platform.component.ComponentFormatter;

/**
 * The bar across the top of the lobby, advertising the ranks.
 *
 * <p>The old server put one there on login and left it: a red "Rangi", the three purchasable
 * ranks in their own colours, and an arrow pointing at {@code /rangi}. It is the only thing on
 * a hub that tells a new player the shop exists without them having to open anything.
 *
 * <p>One bar shown to everybody rather than one built per player. Nothing in it depends on who
 * is looking, and a bar per player would be an object and a packet each for the same line of
 * text - built once here, and each arrival is shown the one that already exists.
 */
public final class BossBarService {

    private final LobbyConfig config;
    private final BossBar bar;

    public BossBarService(LobbyConfig config, ComponentFormatter formatter) {
        this.config = Objects.requireNonNull(config, "config");
        Objects.requireNonNull(formatter, "formatter");

        this.bar = BossBar.bossBar(
                formatter.format(config.bossBar.text),
                // Full, because it is a sign rather than a measure of anything.
                1.0f,
                colour(config.bossBar.colour),
                overlay(config.bossBar.style));
    }

    public boolean isEnabled() {
        return this.config.bossBar.enabled && !this.config.bossBar.text.isBlank();
    }

    public void show(Player player) {
        if (this.isEnabled()) {
            player.showBossBar(this.bar);
        }
    }

    public void hide(Player player) {
        player.hideBossBar(this.bar);
    }

    /** Takes the bar off everybody, for a shutdown or a reload. */
    public void hideAll(Iterable<? extends Player> players) {
        for (Player player : players) {
            player.hideBossBar(this.bar);
        }
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
