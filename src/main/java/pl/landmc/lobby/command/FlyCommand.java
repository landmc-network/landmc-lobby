package pl.landmc.lobby.command;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import java.util.Objects;
import org.bukkit.entity.Player;
import pl.landmc.lobby.config.LobbyMessages;
import pl.landmc.lobby.fly.FlyService;
import pl.landmc.platform.paper.notice.PaperNoticeService;

/**
 * {@code /fly} - flight for whoever is allowed it, as the old lobby had.
 *
 * <p>Registered only when flight is switched on in the configuration, so a lobby that does not
 * want it does not answer the command at all rather than refusing it.
 *
 * <p>The permission is read from the configuration rather than declared here: which rank flies
 * is a decision for whoever runs the network, and it is the same node the join grant checks.
 */
@Command(name = "fly")
public class FlyCommand {

    private final FlyService fly;
    private final PaperNoticeService<LobbyMessages> notices;

    public FlyCommand(FlyService fly, PaperNoticeService<LobbyMessages> notices) {
        this.fly = Objects.requireNonNull(fly, "fly");
        this.notices = Objects.requireNonNull(notices, "notices");
    }

    @Execute
    void execute(@Context Player player) {
        if (!this.fly.mayFly(player)) {
            this.notices.viewer(player, messages -> messages.flyNoPermission);
            return;
        }

        boolean flying = this.fly.toggle(player);
        this.notices.viewer(
                player, messages -> flying ? messages.flyEnabled : messages.flyDisabled);
    }
}
