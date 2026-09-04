package pl.landmc.lobby.listener;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.landmc.lobby.profile.ProfileService;

/**
 * Loads a profile when a player joins and saves it when they leave.
 *
 * <p>Both handlers do the minimum on the main thread - read the player's id and name, hand the
 * rest to {@link ProfileService} - and neither touches the database directly. Nothing here
 * iterates over the online players.
 */
public final class ProfileListener implements Listener {

    private final ProfileService profiles;

    public ProfileListener(ProfileService profiles) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        this.profiles.loadOnJoin(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName(),
                System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.profiles.unload(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }
}
