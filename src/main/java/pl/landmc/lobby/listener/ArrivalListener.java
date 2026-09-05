package pl.landmc.lobby.listener;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import pl.landmc.lobby.bossbar.BossBarService;
import pl.landmc.lobby.fly.FlyService;

/**
 * The two things the old lobby did to a player the moment they were in: put the rank bar on
 * their screen, and give flight to whoever had paid for it.
 *
 * <p>Both were in one place there too, and they belong together: they are what arriving on the
 * hub looks like, and neither is worth a listener of its own.
 *
 * <p>The flight is delayed a tick past the join for the same reason the arrival effect is - a
 * client still loading the world is not yet in a state to be told it may fly, and the original
 * waited a second for it.
 */
public final class ArrivalListener implements Listener {

    private final BossBarService bossBar;
    private final FlyService fly;
    private final Plugin plugin;

    public ArrivalListener(BossBarService bossBar, FlyService fly, Plugin plugin) {
        this.bossBar = Objects.requireNonNull(bossBar, "bossBar");
        this.fly = Objects.requireNonNull(fly, "fly");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        this.bossBar.show(player);

        if (!this.fly.grantsOnJoin()) {
            return;
        }

        // Scheduled on the player rather than on the server, so it follows them if they are
        // moved and quietly does nothing if they leave first.
        player.getScheduler().runDelayed(
                this.plugin,
                task -> this.fly.grantOnJoin(player),
                null,
                this.fly.joinDelayTicks());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Adventure drops a bar with no viewers on its own, but saying so here means the bar
        // does not depend on that and a reconnecting player is not shown it twice.
        this.bossBar.hide(event.getPlayer());
    }
}
