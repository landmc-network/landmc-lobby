package pl.landmc.lobby;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.landmc.lobby.bootstrap.LobbyBootstrap;
import pl.landmc.lobby.world.VoidBiomeProvider;
import pl.landmc.lobby.world.VoidChunkGenerator;

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

    /**
     * The empty-world generator, selected per world in {@code bukkit.yml}:
     *
     * <pre>
     * worlds:
     *   world:
     *     generator: landmc-lobby
     * </pre>
     *
     * <p>Paper asks for this while the world is being created, which is before {@code onEnable}
     * runs - so it must not depend on anything the bootstrap builds.
     */
    @Override
    public @NotNull ChunkGenerator getDefaultWorldGenerator(
            @NotNull String worldName, @Nullable String id) {
        return new VoidChunkGenerator(new VoidBiomeProvider(Biome.THE_VOID));
    }

    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(
            @NotNull String worldName, @Nullable String id) {
        return new VoidBiomeProvider(Biome.THE_VOID);
    }

    @Override
    public void onDisable() {
        if (this.bootstrap != null) {
            this.bootstrap.stop();
            this.bootstrap = null;
        }
    }
}
