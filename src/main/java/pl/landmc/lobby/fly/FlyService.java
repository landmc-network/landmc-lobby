package pl.landmc.lobby.fly;

import java.util.Objects;
import org.bukkit.entity.Player;
import pl.landmc.lobby.config.LobbyConfig;

/**
 * Flight on the lobby, which is a rank's privilege rather than a game rule.
 *
 * <p>Two halves, both from the old server: it turned flight on by itself when somebody with a
 * rank logged in, and it gave them {@code /fly} to turn it off again. The first is what people
 * actually paid for - nobody wants to type a command every time they connect - and the second
 * exists because a hub is also a place you walk around in.
 *
 * <p>Only {@code setAllowFlight}: the player still double-taps to leave the ground, exactly as
 * before. Switching it off drops whoever was in the air, which is what the original did and
 * what anybody who types the command is asking for.
 */
public final class FlyService {

    private final LobbyConfig config;

    public FlyService(LobbyConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public boolean isEnabled() {
        return this.config.fly.enabled;
    }

    public boolean grantsOnJoin() {
        return this.config.fly.enabled && this.config.fly.onJoin;
    }

    public long joinDelayTicks() {
        return Math.max(1L, this.config.fly.onJoinDelayTicks);
    }

    public boolean mayFly(Player player) {
        return this.config.fly.enabled && player.hasPermission(this.config.fly.permission);
    }

    /**
     * Turns flight on or off for one player.
     *
     * @return the state it is now in
     */
    public boolean toggle(Player player) {
        boolean allowed = !player.getAllowFlight();
        player.setAllowFlight(allowed);

        if (!allowed) {
            // Otherwise a player who was flying keeps hovering until something else moves them.
            player.setFlying(false);
        }
        return allowed;
    }

    /** Gives flight to somebody who has just arrived and is allowed it. */
    public void grantOnJoin(Player player) {
        if (this.grantsOnJoin() && this.mayFly(player)) {
            player.setAllowFlight(true);
        }
    }
}
