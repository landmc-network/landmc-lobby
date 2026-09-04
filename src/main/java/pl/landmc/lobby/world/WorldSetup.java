package pl.landmc.lobby.world;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.World;
import org.slf4j.Logger;
import pl.landmc.lobby.config.LobbyConfig;

/**
 * Applies the settings a lobby world needs, once, when the server has started.
 *
 * <p>Separate from the generator on purpose. The generator decides what a chunk is made of and
 * only ever runs for chunks that do not exist yet; this decides how the world behaves, and has
 * to run whether the world was generated a minute ago or a year ago.
 */
public final class WorldSetup {

    private final Server server;
    private final LobbyConfig config;
    private final Logger logger;

    public WorldSetup(Server server, LobbyConfig config, Logger logger) {
        this.server = Objects.requireNonNull(server, "server");
        this.config = Objects.requireNonNull(config, "config");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void apply() {
        for (String name : this.config.world.worlds) {
            World world = this.server.getWorld(name);
            if (world == null) {
                // Named in config.yml but not loaded: a typo, or a world this server does not
                // have. Worth saying so rather than silently doing nothing.
                this.logger.warn("World '{}' is not loaded; its lobby settings were not applied.", name);
                continue;
            }

            this.applyGameRules(world);
            this.applySpawnPoint(world);
        }
    }

    private void applyGameRules(World world) {
        Map<String, GameRule<?>> known = knownRules();

        for (Map.Entry<String, String> entry : this.config.world.gamerules.entrySet()) {
            GameRule<?> rule = known.get(simplify(entry.getKey()));
            if (rule == null) {
                this.logger.warn("Unknown game rule '{}' in config.yml; skipped.", entry.getKey());
                continue;
            }

            if (!this.set(world, rule, entry.getValue())) {
                this.logger.warn(
                        "Game rule {} does not take '{}'; skipped.", entry.getKey(), entry.getValue());
                continue;
            }

            // Logged because there is nowhere else to see it: this version does not write game
            // rules into level.dat, so without this line the only way to check what the lobby
            // is running with is to type /gamerule on a live server.
            this.logger.info(
                    "Game rule {} set to {} in '{}'.",
                    rule.getKey().getKey(), world.getGameRuleValue(rule), world.getName());
        }
    }

    /**
     * Every game rule the server has, under every spelling somebody might write.
     *
     * <p>Built from the registry rather than from {@code GameRule.getByName}, which is marked
     * for removal. The registry names a rule {@code random_tick_speed} while {@code /gamerule}
     * calls it {@code randomTickSpeed}, and the file is written by a person who has seen the
     * second - which costs nothing to support, because reducing both to their letters and digits
     * makes them the same string.
     */
    private static Map<String, GameRule<?>> knownRules() {
        Map<String, GameRule<?>> known = new HashMap<>();

        for (GameRule<?> rule : Registry.GAME_RULE) {
            known.put(simplify(rule.getKey().getKey()), rule);
        }
        return known;
    }

    /** Reduces a name to its letters and digits, so spelling and case stop mattering. */
    static String simplify(String name) {
        StringBuilder simplified = new StringBuilder(name.length());
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (Character.isLetterOrDigit(character)) {
                simplified.append(Character.toLowerCase(character));
            }
        }
        return simplified.toString();
    }

    /**
     * Sets one rule, reading the value as whatever type that rule holds.
     *
     * <p>The configuration is a map of strings because that is what a person writes, and every
     * game rule is either a boolean or an integer - so the rule itself decides how to read it
     * rather than the file having to declare a type per entry.
     */
    private boolean set(World world, GameRule<?> rule, String value) {
        if (rule.getType() == Boolean.class) {
            String normalised = value.trim().toLowerCase(Locale.ROOT);
            if (!normalised.equals("true") && !normalised.equals("false")) {
                return false;
            }

            @SuppressWarnings("unchecked")
            GameRule<Boolean> booleanRule = (GameRule<Boolean>) rule;
            world.setGameRule(booleanRule, Boolean.parseBoolean(normalised));
            return true;
        }

        if (rule.getType() == Integer.class) {
            int number;
            try {
                number = Integer.parseInt(value.trim());
            }
            catch (NumberFormatException exception) {
                return false;
            }

            @SuppressWarnings("unchecked")
            GameRule<Integer> integerRule = (GameRule<Integer>) rule;
            world.setGameRule(integerRule, number);
            return true;
        }

        return false;
    }

    /**
     * Moves the world spawn.
     *
     * <p>A void world's default spawn is wherever the server looked for ground and found none,
     * which means a player who arrives before anything has been built falls out of the world.
     * This is the setting that stops that, and it is why it defaults to being on.
     */
    private void applySpawnPoint(World world) {
        if (!this.config.world.setSpawnPoint) {
            return;
        }

        Location spawn = new Location(
                world,
                this.config.world.spawnX,
                this.config.world.spawnY,
                this.config.world.spawnZ);

        if (world.getSpawnLocation().equals(spawn)) {
            return;
        }

        world.setSpawnLocation(spawn);
        this.logger.info(
                "Spawn of '{}' set to {}, {}, {}.",
                world.getName(), spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
    }
}
