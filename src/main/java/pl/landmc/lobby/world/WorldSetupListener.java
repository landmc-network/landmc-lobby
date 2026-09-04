package pl.landmc.lobby.world;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

/**
 * Applies the lobby's world settings once the worlds exist.
 *
 * <p>The plugin is enabled at {@code STARTUP} so that it can supply the default world's
 * generator - which means that during {@code onEnable} there are no worlds yet, and anything
 * that looks one up finds nothing. {@link ServerLoadEvent} is the first moment there is
 * something to configure.
 *
 * <p>It also fires again after {@code /reload}, which is the right behaviour: a reload rereads
 * the configuration, and the settings in it should follow.
 */
public final class WorldSetupListener implements Listener {

    private final WorldSetup setup;

    public WorldSetupListener(WorldSetup setup) {
        this.setup = Objects.requireNonNull(setup, "setup");
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        this.setup.apply();
    }
}
