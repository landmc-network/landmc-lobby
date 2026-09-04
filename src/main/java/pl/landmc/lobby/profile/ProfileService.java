package pl.landmc.lobby.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import pl.landmc.platform.database.DatabaseService;

/**
 * Profiles of the players currently online, and the lifecycle that keeps them in step with the
 * database.
 *
 * <pre>{@code
 * join   -> async load -> insert into the cache on the main thread
 * play   -> read and change the cached profile, no SQL at all
 * timer  -> collect dirty snapshots on the main thread -> one async batch write
 * quit   -> snapshot -> async save -> drop from the cache
 * }</pre>
 *
 * <p>The cache is a plain map. A player's profile is read on nearly every interaction, so it
 * must not be a query; it is not a cache framework because a map keyed by UUID is the whole
 * requirement.
 *
 * <p>The map is written from the main thread only - a load completes on a database worker and
 * hops back before inserting. It is concurrent because {@link #dirtySnapshots()} and shutdown
 * can read it from elsewhere.
 */
public final class ProfileService {

    private final ProfileRepository repository;
    private final DatabaseService database;
    private final Executor mainThread;
    private final Logger logger;

    private final Map<UUID, LobbyProfile> profiles = new java.util.concurrent.ConcurrentHashMap<>();

    public ProfileService(
            ProfileRepository repository, DatabaseService database, Executor mainThread, Logger logger) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.database = Objects.requireNonNull(database, "database");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Loads a player's profile and puts it in the cache.
     *
     * <p>The query runs on a database worker; the cache insert hops back to the main thread, so
     * everything that reads the map stays single-threaded. Only identifiers cross the boundary -
     * no {@code Player} is captured, because by the time the query returns the player may be
     * gone.
     */
    public CompletableFuture<Void> loadOnJoin(UUID playerId, String name, long now) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(name, "name");

        return this.database
                .supplyAsync(() -> this.repository.findOrCreate(playerId, name, now))
                .thenAccept(snapshot -> this.mainThread.execute(() -> {
                    LobbyProfile profile = new LobbyProfile(snapshot);
                    profile.recordJoin(name, now);
                    this.profiles.put(playerId, profile);
                }))
                .exceptionally(error -> {
                    // The player stays connected without a profile rather than being kicked; the
                    // lobby is still usable and the failure is visible in the log with context.
                    this.logger.error("Could not load the lobby profile of {}", playerId, error);
                    return null;
                });
    }

    /** The cached profile, if the player is online and their load has finished. */
    public Optional<LobbyProfile> find(UUID playerId) {
        return Optional.ofNullable(this.profiles.get(Objects.requireNonNull(playerId, "playerId")));
    }

    /**
     * Saves and forgets a player's profile.
     *
     * <p>The snapshot is taken here, on the main thread, before the write is handed off - the
     * profile object itself must not travel to the worker.
     */
    public CompletableFuture<Void> unload(UUID playerId, long now) {
        LobbyProfile profile = this.profiles.remove(Objects.requireNonNull(playerId, "playerId"));
        if (profile == null) {
            return CompletableFuture.completedFuture(null);
        }

        profile.touch(now);
        LobbyProfileSnapshot snapshot = profile.snapshotAndClean();

        return this.database
                .runAsync(() -> this.repository.save(snapshot))
                .exceptionally(error -> {
                    this.logger.error("Could not save the lobby profile of {}", playerId, error);
                    return null;
                });
    }

    /**
     * Persists every profile that changed since the last run.
     *
     * <p>Called on a timer. Collects on the calling thread - the main thread - and hands one
     * batch to the database, rather than firing a task per profile.
     */
    public CompletableFuture<Integer> saveDirty() {
        List<LobbyProfileSnapshot> pending = this.dirtySnapshots();
        if (pending.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }

        return this.database
                .runAsync(() -> this.repository.saveAll(pending))
                .handle((ignored, error) -> {
                    if (error != null) {
                        this.logger.error("Autosave of {} lobby profile(s) failed", pending.size(), error);
                        return 0;
                    }
                    return pending.size();
                });
    }

    /** Number of profiles currently cached. */
    public int cached() {
        return this.profiles.size();
    }

    /** Drops every cached profile without saving; shutdown persists first, then calls this. */
    public void clear() {
        this.profiles.clear();
    }

    private List<LobbyProfileSnapshot> dirtySnapshots() {
        List<LobbyProfileSnapshot> pending = new ArrayList<>();
        for (LobbyProfile profile : this.profiles.values()) {
            if (profile.isDirty()) {
                pending.add(profile.snapshotAndClean());
            }
        }
        return pending;
    }
}
