package pl.landmc.lobby.listener;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.landmc.lobby.sidebar.ScoreboardService;

/** Shows the sidebar on arrival and forgets it on the way out. */
public final class ScoreboardListener implements Listener {

    private final ScoreboardService scoreboards;

    public ScoreboardListener(ScoreboardService scoreboards) {
        this.scoreboards = Objects.requireNonNull(scoreboards, "scoreboards");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.scoreboards.show(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.scoreboards.hide(event.getPlayer());
    }
}
