package pl.landmc.lobby.world;

import java.util.List;
import java.util.Objects;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

/**
 * One biome everywhere.
 *
 * <p>A world still has a biome even where it has no blocks, and the biome is what decides the sky
 * colour, the fog, the water tint and the ambient sound. Left to the default the lobby would take
 * its mood from whatever biome the noise generator would have put there, which changes as a
 * player walks across the build.
 *
 * <p>The void biome is the honest choice for a world with no terrain: no weather effects, no
 * ambient noise, a plain sky.
 */
public final class VoidBiomeProvider extends BiomeProvider {

    private final Biome biome;

    public VoidBiomeProvider(Biome biome) {
        this.biome = Objects.requireNonNull(biome, "biome");
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo world, int x, int y, int z) {
        return this.biome;
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo world) {
        return List.of(this.biome);
    }
}
