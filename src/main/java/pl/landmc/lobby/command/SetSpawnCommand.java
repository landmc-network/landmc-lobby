package pl.landmc.lobby.command;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import java.util.Objects;
import org.bukkit.entity.Player;
import pl.landmc.platform.paper.notice.PaperNoticeService;
import pl.landmc.lobby.config.LobbyMessages;
import pl.landmc.lobby.spawn.SpawnService;

/** {@code /setspawn} - stores the sender's position as the lobby spawn. */
@Command(name = "setspawn")
@Permission("landmc.command.setspawn")
public class SetSpawnCommand {

    private final SpawnService spawn;
    private final PaperNoticeService<LobbyMessages> notices;

    public SetSpawnCommand(SpawnService spawn, PaperNoticeService<LobbyMessages> notices) {
        this.spawn = Objects.requireNonNull(spawn, "spawn");
        this.notices = Objects.requireNonNull(notices, "notices");
    }

    @Execute
    void execute(@Context Player player) {
        this.spawn.setSpawn(player.getLocation());
        this.notices.viewer(player, messages -> messages.spawnSet);
    }
}
