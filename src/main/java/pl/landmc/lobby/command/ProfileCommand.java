package pl.landmc.lobby.command;

import com.eternalcode.multification.shared.Formatter;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.entity.Player;
import pl.landmc.platform.paper.notice.PaperNoticeService;
import pl.landmc.lobby.config.LobbyMessages;
import pl.landmc.lobby.profile.LobbyProfile;
import pl.landmc.lobby.profile.ProfileService;

/**
 * {@code /profil} - shows the sender's lobby profile.
 *
 * <p>Reads the cache, never the database. The profile was loaded when the player joined, so
 * running a query here would be a round trip for data the server already has in memory - and it
 * would run on the main thread.
 */
@Command(name = "profil", aliases = "profile")
public class ProfileCommand {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault());

    private final ProfileService profiles;
    private final PaperNoticeService<LobbyMessages> notices;

    public ProfileCommand(ProfileService profiles, PaperNoticeService<LobbyMessages> notices) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.notices = Objects.requireNonNull(notices, "notices");
    }

    @Execute
    void execute(@Context Player player) {
        Optional<LobbyProfile> found = this.profiles.find(player.getUniqueId());
        if (found.isEmpty()) {
            // The load has not finished yet, or it failed and was logged.
            this.notices.viewer(player, messages -> messages.profileNotLoaded);
            return;
        }

        LobbyProfile profile = found.get();
        this.notices.viewer(
                player,
                messages -> messages.profileHeader,
                new Formatter().register("{NAME}", profile.name()));
        this.notices.viewer(
                player,
                messages -> messages.profileVisits,
                new Formatter().register("{VISITS}", profile.visits()));
        this.notices.viewer(
                player,
                messages -> messages.profileFirstJoin,
                new Formatter().register("{FIRST_JOIN}", DATE.format(Instant.ofEpochMilli(profile.firstJoin()))));
    }
}
