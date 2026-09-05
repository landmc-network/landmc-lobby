package pl.landmc.lobby.listener;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

/**
 * Runs something once, after every plugin on this server has been enabled.
 *
 * <p>This plugin is enabled at STARTUP so that it can supply the default world's generator,
 * which puts it ahead of everything else. Anything that needs another plugin - LuckPerms, most
 * of all - has to wait for this rather than ask during enable and be told it is not installed.
 */
public final class ServerLoadedListener implements Listener {

    private final Runnable work;

    public ServerLoadedListener(Runnable work) {
        this.work = Objects.requireNonNull(work, "work");
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        this.work.run();
    }
}
