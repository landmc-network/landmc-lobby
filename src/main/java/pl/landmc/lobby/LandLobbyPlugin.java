package pl.landmc.lobby;

import org.bukkit.plugin.java.JavaPlugin;
import pl.landmc.lobby.bootstrap.LobbyBootstrap;

/**
 * The Paper entry point for the LandMC lobby.
 *
 * <p>Holds no logic: spawn, profiles and messaging live in their own services, and
 * {@link LobbyBootstrap} assembles them. This class exists to receive Paper's lifecycle calls.
 *
 * <p>A failed startup is allowed to escape {@code onEnable}. The lobby stores player data, and a
 * plugin that "started" without its database would hand every player an empty profile and write
 * nothing - Paper disabling it is the correct outcome.
 */
public final class LandLobbyPlugin extends JavaPlugin {

    private LobbyBootstrap bootstrap;

    @Override
    public void onEnable() {
        this.bootstrap = new LobbyBootstrap(this, this.getSLF4JLogger(), this.getDataPath());
        this.bootstrap.start();
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stop();
            this.bootstrap = null;
        }
    }
}
