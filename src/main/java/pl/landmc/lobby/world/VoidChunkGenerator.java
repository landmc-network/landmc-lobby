package pl.landmc.lobby.world;

import java.util.List;
import java.util.Random;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

/**
 * A world made of nothing.
 *
 * <p>The lobby is a build that is pasted in, not terrain that is generated, so every chunk the
 * server makes has to be empty. Every generation stage is overridden to do nothing: leaving one
 * out is how a void world ends up with an ore vein or a village floating next to the build,
 * because Paper runs the default stage for anything a generator does not claim.
 *
 * <p>Structures and decorations are switched off for the same reason, and mob generation because
 * a lobby with no floor is a lobby where anything spawned falls out of the world for ever.
 *
 * <p>There is no bedrock stage here: it is deprecated in this API version, and with the noise and
 * surface stages already producing nothing there is no floor for it to sit under.
 */
public final class VoidChunkGenerator extends ChunkGenerator {

    private final BiomeProvider biomes;

    public VoidChunkGenerator(BiomeProvider biomes) {
        this.biomes = biomes;
    }

    @Override
    public void generateNoise(
            @NotNull WorldInfo world, @NotNull Random random, int chunkX, int chunkZ,
            @NotNull ChunkData chunk) {
        // Deliberately empty: this is the stage that would otherwise carve terrain.
    }

    @Override
    public void generateSurface(
            @NotNull WorldInfo world, @NotNull Random random, int chunkX, int chunkZ,
            @NotNull ChunkData chunk) {
    }

    @Override
    public void generateCaves(
            @NotNull WorldInfo world, @NotNull Random random, int chunkX, int chunkZ,
            @NotNull ChunkData chunk) {
    }

    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo world) {
        return this.biomes;
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    /**
     * No populators.
     *
     * <p>A populator runs after generation and would place trees and ores into the empty chunks
     * this generator just produced.
     */
    @Override
    public @NotNull List<org.bukkit.generator.BlockPopulator> getDefaultPopulators(
            @NotNull org.bukkit.World world) {
        return List.of();
    }
}
