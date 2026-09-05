package pl.landmc.lobby.sidebar;

/**
 * A player's level, worked out from how often they have come back.
 *
 * <p>There is no experience counter behind this and deliberately so. The network already knows
 * how many times somebody has arrived, and a level that is read from something already true
 * cannot drift from it, cannot be lost when a table is rebuilt, and needs nothing to be granted
 * anywhere. The previous version of the network had a level on its sidebar that was the
 * character nought written into the source; this is the smallest honest thing that is not that.
 *
 * <p>The curve is triangular: each level costs more visits than the one before it, so the
 * numbers stay small and keep moving. With the default step of five, level one arrives on the
 * fifth visit, level two on the fifteenth, level three on the thirtieth.
 */
public final class LobbyLevel {

    private LobbyLevel() {
    }

    /**
     * The level a number of visits earns.
     *
     * <p>Solved rather than counted up, so this is arithmetic rather than a loop that gets
     * slower for the players who have been here longest.
     */
    public static int of(int visits, int step) {
        if (visits <= 0 || step <= 0) {
            return 0;
        }

        // visits >= step * L * (L + 1) / 2, solved for L.
        double solved = (Math.sqrt(1.0D + 8.0D * visits / step) - 1.0D) / 2.0D;
        return (int) Math.floor(solved);
    }

    /** How many visits the next level needs, for a sidebar that wants to show progress. */
    public static int visitsFor(int level, int step) {
        if (level <= 0 || step <= 0) {
            return 0;
        }
        return step * level * (level + 1) / 2;
    }
}
