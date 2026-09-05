package pl.landmc.lobby.tablist;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

/**
 * Where a player's rank prefix comes from.
 *
 * <p>The seam that keeps LuckPerms optional, in the same shape as the one on the proxy. No class
 * naming a LuckPerms type appears here: {@link #NONE} answers when it is absent, and
 * {@link LuckPermsPrefixes} is only loaded once it is known to be installed. That distinction is
 * not academic - a class is verified when it is first loaded, and one referring to a missing
 * library throws {@code NoClassDefFoundError} before any {@code try} inside its methods can
 * catch it.
 */
public interface RankPrefixes {

    /** Answers as though nobody had a rank; installed when LuckPerms is absent. */
    RankPrefixes NONE = player -> "";

    /** Binds to LuckPerms when it is installed, otherwise returns {@link #NONE}. */
    static RankPrefixes create(Logger logger) {
        Objects.requireNonNull(logger, "logger");

        try {
            RankPrefixes prefixes = LuckPermsPrefixes.bind();
            logger.info("LuckPerms found; the tab list shows rank prefixes.");
            return prefixes;
        }
        catch (IllegalStateException | NoClassDefFoundError exception) {
            logger.info("LuckPerms is not installed; the tab list shows plain names.");
            return NONE;
        }
    }

    /** The player's prefix, or an empty string when they have none. */
    String of(Player player);
}
