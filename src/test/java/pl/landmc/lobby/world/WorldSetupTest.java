package pl.landmc.lobby.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * How a game rule written in config.yml is matched to the one the server has.
 *
 * <p>The registry calls it {@code random_tick_speed} and {@code /gamerule} calls it
 * {@code randomTickSpeed}. Whoever edits the file has seen the second, so both have to resolve -
 * and when they do not, the rule is silently skipped and the lobby quietly keeps ticking.
 */
class WorldSetupTest {

    @Test
    @DisplayName("the two spellings of a rule name are the same name")
    void matchesBothSpellings() {
        assertEquals(
                WorldSetup.simplify("random_tick_speed"),
                WorldSetup.simplify("randomTickSpeed"));

        assertEquals(
                WorldSetup.simplify("do_daylight_cycle"),
                WorldSetup.simplify("doDaylightCycle"));
    }

    @Test
    @DisplayName("case and stray punctuation do not matter")
    void ignoresCaseAndPunctuation() {
        String expected = WorldSetup.simplify("randomTickSpeed");

        assertEquals(expected, WorldSetup.simplify("RANDOMTICKSPEED"));
        assertEquals(expected, WorldSetup.simplify("random-tick-speed"));
        assertEquals(expected, WorldSetup.simplify("  randomTickSpeed  "));
        assertEquals(expected, WorldSetup.simplify("random.tick.speed"));
    }

    @Test
    @DisplayName("different rules do not collapse into one another")
    void keepsDifferentRulesApart() {
        // The whole point of reducing a name is to be forgiving; it must not be so forgiving
        // that setting one rule sets another.
        assertNotEquals(
                WorldSetup.simplify("randomTickSpeed"),
                WorldSetup.simplify("maxEntityCramming"));

        assertNotEquals(
                WorldSetup.simplify("doMobSpawning"),
                WorldSetup.simplify("doMobLoot"));
    }

    @Test
    @DisplayName("digits are kept, because some rule names have them")
    void keepsDigits() {
        assertEquals("rule2", WorldSetup.simplify("rule_2"));
    }
}
