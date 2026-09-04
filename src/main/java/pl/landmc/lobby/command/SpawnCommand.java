package pl.landmc.lobby.command;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.landmc.platform.paper.notice.PaperNoticeService;
import pl.landmc.lobby.config.LobbyMessages;
import pl.landmc.lobby.spawn.SpawnService;

/** {@code /spawn} - moves the player to the lobby spawn. */
@Command(name = "spawn")
public class SpawnCommand {

    private final SpawnService spawn;
    private final PaperNoticeService<LobbyMessages> notices;

    public SpawnCommand(SpawnService spawn, PaperNoticeService<LobbyMessages> notices) {
        this.spawn = Objects.requireNonNull(spawn, "spawn");
        this.notices = Objects.requireNonNull(notices, "notices");
    }

    @Execute
    void execute(@Context Player player) {
        Optional<Location> target = this.spawn.spawn();
        if (target.isEmpty()) {
            this.notices.viewer(player, messages -> messages.spawnNotSet);
            return;
        }

        player.teleport(target.get());
        this.notices.viewer(player, messages -> messages.spawnTeleported);
    }
}
