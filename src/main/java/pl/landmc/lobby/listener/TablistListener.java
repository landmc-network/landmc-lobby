package pl.landmc.lobby.listener;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.landmc.lobby.tablist.TablistService;

/** Writes a player's tab list entry when they arrive, and forgets it when they leave. */
public final class TablistListener implements Listener {

    private final TablistService tablist;

    public TablistListener(TablistService tablist) {
        this.tablist = Objects.requireNonNull(tablist, "tablist");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.tablist.apply(event.getPlayer());
        this.tablist.surround(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.tablist.forget(event.getPlayer());
    }
}
