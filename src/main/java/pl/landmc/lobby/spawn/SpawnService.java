package pl.landmc.lobby.spawn;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import pl.landmc.platform.config.ConfigService;
import pl.landmc.lobby.config.LobbyConfig;

/**
 * Where the lobby's spawn is, and how it gets there.
 *
 * <p>The location lives in {@code config.yml} rather than in the database: it is set once by an
 * operator and read on every join, so a row nobody ever updates would be a query for nothing.
 * Writing it back goes through the platform's {@code ConfigService}, which replaces the file
 * atomically.
 *
 * <p>Every method here touches the Bukkit API and must be called from the main thread.
 */
public final class SpawnService {

    private final LobbyConfig config;
    private final ConfigService configs;
    private final Server server;

    public SpawnService(LobbyConfig config, ConfigService configs, Server server) {
        this.config = Objects.requireNonNull(config, "config");
        this.configs = Objects.requireNonNull(configs, "configs");
        this.server = Objects.requireNonNull(server, "server");
    }

    /**
     * The configured spawn, or empty when it was never set or its world is not loaded.
     *
     * <p>The unloaded-world case is deliberately not an exception: renaming a world should mean
     * "spawn is not set" and a message saying so, not a plugin that fails to start.
     */
    public Optional<Location> spawn() {
        String worldName = this.config.spawn.world;
        if (worldName == null || worldName.isBlank()) {
            return Optional.empty();
        }

        World world = this.server.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }

        return Optional.of(new Location(
                world,
                this.config.spawn.x,
                this.config.spawn.y,
                this.config.spawn.z,
                this.config.spawn.yaw,
                this.config.spawn.pitch));
    }

    public boolean isSet() {
        return this.spawn().isPresent();
    }

    public boolean teleportOnJoin() {
        return this.config.spawn.teleportOnJoin;
    }

    /** Stores a new spawn and writes it to disk. */
    public void setSpawn(Location location) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(location.getWorld(), "location world");

        this.config.spawn.world = location.getWorld().getName();
        this.config.spawn.x = location.getX();
        this.config.spawn.y = location.getY();
        this.config.spawn.z = location.getZ();
        this.config.spawn.yaw = location.getYaw();
        this.config.spawn.pitch = location.getPitch();

        this.configs.save(this.config);
    }
}
