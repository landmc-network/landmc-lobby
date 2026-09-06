package pl.landmc.lobby.sidebar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.lobby.profile.ProfileService;
import pl.landmc.platform.component.ComponentFormatter;

/**
 * The sidebar every player on the lobby sees.
 *
 * <p>Each line is a team whose entry never changes and whose prefix does. That is the difference
 * between a scoreboard that updates and one that flickers: removing and re-adding a score makes
 * the client blank the line for a frame, while changing a team's prefix redraws it in place.
 * The old server drew its board the same way, for the same reason.
 *
 * <p>Lines that contain no placeholder are written once, when the board is built, and never
 * touched again. Only the ones that can change are rewritten on a refresh, so a full board costs
 * two or three string substitutions per player rather than a dozen.
 */
public final class ScoreboardService {

    /**
     * The invisible text each line is scored under.
     *
     * <p>A scoreboard identifies a line by its entry, and two lines with the same entry are one
     * line. Colour codes are used because they are unique, ordered, and render as nothing.
     */
    private static final String[] ENTRIES = {
        "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
        "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f",
    };

    private final LobbyConfig config;
    private final BalanceTracker balances;
    private final ProfileService profiles;
    private final ComponentFormatter formatter;

    /** Which configured lines can change, so a refresh rewrites only those. */
    private final List<Integer> dynamic;

    /**
     * Keyed by id rather than by the player object, so a board left behind by a
     * disconnect that went wrong holds nothing alive.
     */
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardService(
            LobbyConfig config,
            BalanceTracker balances,
            ProfileService profiles,
            ComponentFormatter formatter) {

        this.config = Objects.requireNonNull(config, "config");
        this.balances = Objects.requireNonNull(balances, "balances");
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.dynamic = dynamicLines(config.scoreboard.lines);
    }

    public boolean isEnabled() {
        return this.config.scoreboard.enabled && !this.lines().isEmpty();
    }

    /** Builds this player's board and shows it. */
    public void show(Player player) {
        List<String> lines = this.lines();
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective = board.registerNewObjective(
                "landmc",
                Criteria.DUMMY,
                this.formatter.format(this.config.scoreboard.title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        if (this.config.scoreboard.hideNumbers) {
            // The score is drawn after the line and hard against the right edge, so it is the
            // one thing a panel cannot cover. Blanking it is the only way it goes away.
            objective.numberFormat(NumberFormat.blank());
        }

        for (int index = 0; index < lines.size() && index < ENTRIES.length; index++) {
            Team team = board.registerNewTeam("line" + index);
            team.addEntry(ENTRIES[index]);
            team.prefix(this.render(lines.get(index), player));

            // Counted down so the first configured line is drawn at the top.
            objective.getScore(ENTRIES[index]).setScore(lines.size() - index);
        }

        player.setScoreboard(board);
        this.boards.put(player.getUniqueId(), board);
    }

    /** Rewrites the lines that can change. Does nothing for a player with no board. */
    public void refresh(Player player) {
        Scoreboard board = this.boards.get(player.getUniqueId());
        if (board == null) {
            return;
        }

        List<String> lines = this.lines();
        for (int index : this.dynamic) {
            Team team = board.getTeam("line" + index);
            if (team != null) {
                team.prefix(this.render(lines.get(index), player));
            }
        }
    }

    /**
     * Rewrites every board.
     *
     * <p>One task for the whole server rather than one per player: a lobby with two hundred
     * people would otherwise be two hundred repeating tasks doing the same three substitutions.
     */
    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.refresh(player);
        }
    }

    public void hide(Player player) {
        this.boards.remove(player.getUniqueId());
    }

    private Component render(String line, Player player) {
        String text = line
                .replace("{PLAYER}", player.getName())
                .replace("{SERVER}", this.config.lobby.serverId)
                .replace("{ONLINE}", Integer.toString(Bukkit.getOnlinePlayers().size()))
                .replace("{DIAMONDS}", Long.toString(this.balances.balanceOf(player.getUniqueId())))
                // Neither of these has a system behind it yet. They are written as zero rather
                // than left as raw placeholders, and the config says what they are waiting for.
                .replace("{COINS}", Long.toString(
                        this.balances.coinsOf(player.getUniqueId())))
                .replace("{LEVEL}", Integer.toString(this.levelOf(player)));

        return this.formatter.format(text);
    }

    private List<String> lines() {
        return this.config.scoreboard.lines;
    }

    private static List<Integer> dynamicLines(List<String> lines) {
        List<Integer> dynamic = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).contains("{")) {
                dynamic.add(index);
            }
        }
        return List.copyOf(dynamic);
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
