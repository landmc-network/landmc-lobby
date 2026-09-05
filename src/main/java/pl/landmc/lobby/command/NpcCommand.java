package pl.landmc.lobby.command;

import com.eternalcode.multification.shared.Formatter;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.join.Join;
import dev.rollczi.litecommands.annotations.permission.Permission;
import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.lobby.config.LobbyMessages;
import pl.landmc.lobby.npc.NpcService;
import pl.landmc.platform.paper.notice.PaperNoticeService;

/**
 * {@code /npc} - puts the figures up and dresses them.
 *
 * <p>A command rather than a file to edit, because a figure is mostly a position and a facing,
 * and nobody knows the coordinates of the spot they are standing on or the yaw they are looking
 * along. The previous version of the network had all of it written into the source, which is
 * why moving a figure by half a block meant a rebuild.
 *
 * <p>Everything here writes the configuration as well, so what is standing on the spawn and what
 * is on disk cannot drift apart.
 */
@Command(name = "npc", aliases = "figurka")
@Permission("landmc.command.npc")
public class NpcCommand {

    /**
     * The name of the figure argument.
     *
     * <p>Shared with the suggester registered on the command builder, which is how tab offers
     * the figures that are standing instead of the word "nazwa".
     */
    public static final String NAME_ARGUMENT = "npc";

    private final NpcService npcs;
    private final PaperNoticeService<LobbyMessages> notices;

    public NpcCommand(NpcService npcs, PaperNoticeService<LobbyMessages> notices) {
        this.npcs = Objects.requireNonNull(npcs, "npcs");
        this.notices = Objects.requireNonNull(notices, "notices");
    }

    @Execute(name = "utworz")
    void create(
            @Context Player player,
            @Arg(NAME_ARGUMENT) String id,
            @Arg("serwer") String server,
            @Join("nazwa") String name) {

        if (this.npcs.find(id) != null) {
            this.notices.viewer(player, messages -> messages.npcExists, named(id));
            return;
        }

        this.npcs.create(id, server, name, player.getLocation());
        this.notices.viewer(player, messages -> messages.npcCreated, named(id));
    }

    @Execute(name = "usun")
    void remove(@Context Player player, @Arg(NAME_ARGUMENT) String id) {
        LobbyConfig.NpcEntry entry = this.entry(player, id);
        if (entry == null) {
            return;
        }

        this.npcs.remove(entry);
        this.notices.viewer(player, messages -> messages.npcRemoved, named(entry.id));
    }

    @Execute(name = "tutaj")
    void move(@Context Player player, @Arg(NAME_ARGUMENT) String id) {
        LobbyConfig.NpcEntry entry = this.entry(player, id);
        if (entry == null) {
            return;
        }

        this.npcs.move(entry, player.getLocation());
        this.notices.viewer(player, messages -> messages.npcMoved, named(entry.id));
    }

    @Execute(name = "nazwa")
    void name(
            @Context Player player,
            @Arg(NAME_ARGUMENT) String id,
            @Join("nazwa") String name) {

        LobbyConfig.NpcEntry entry = this.entry(player, id);
        if (entry == null) {
            return;
        }

        this.npcs.setName(entry, name);
        this.notices.viewer(player, messages -> messages.npcUpdated, named(entry.id));
    }

    @Execute(name = "zacheta")
    void addon(
            @Context Player player,
            @Arg(NAME_ARGUMENT) String id,
            @Join("tekst") String text) {

        LobbyConfig.NpcEntry entry = this.entry(player, id);
        if (entry == null) {
            return;
        }

        this.npcs.setAddon(entry, text);
        this.notices.viewer(player, messages -> messages.npcUpdated, named(entry.id));
    }

    @Execute(name = "skorka")
    void skin(
            @Context Player player,
            @Arg(NAME_ARGUMENT) String id,
            @Arg("adres") String url) {

        LobbyConfig.NpcEntry entry = this.entry(player, id);
        if (entry == null) {
            return;
        }

        this.npcs.setSkin(entry, url);
        this.notices.viewer(player, messages -> messages.npcUpdated, named(entry.id));
    }

    @Execute(name = "przedmiot")
    void item(
            @Context Player player,
            @Arg(NAME_ARGUMENT) String id,
            @Arg("material") String material) {

        LobbyConfig.NpcEntry entry = this.entry(player, id);
        if (entry == null) {
            return;
        }

        this.npcs.setItem(entry, material);
        this.notices.viewer(player, messages -> messages.npcUpdated, named(entry.id));
    }

    @Execute(name = "kolor")
    void colour(
            @Context Player player,
            @Arg(NAME_ARGUMENT) String id,
            @Arg("kolor") String colour) {

        LobbyConfig.NpcEntry entry = this.entry(player, id);
        if (entry == null) {
            return;
        }

        this.npcs.setArmourColour(entry, colour);
        this.notices.viewer(player, messages -> messages.npcUpdated, named(entry.id));
    }

    @Execute(name = "lista")
    void list(@Context CommandSender sender) {
        if (this.npcs.all().isEmpty()) {
            this.notices.viewer(sender, messages -> messages.npcListEmpty);
            return;
        }

        this.notices.viewer(
                sender,
                messages -> messages.npcListHeader,
                new Formatter().register("{COUNT}", this.npcs.count()));

        for (LobbyConfig.NpcEntry entry : this.npcs.all()) {
            this.notices.viewer(
                    sender,
                    messages -> messages.npcListEntry,
                    named(entry.id)
                            .register("{SERVER}", entry.server)
                            .register("{WORLD}", entry.world)
                            .register("{X}", Math.round(entry.x))
                            .register("{Y}", Math.round(entry.y))
                            .register("{Z}", Math.round(entry.z)));
        }
    }

    /** The figure by that name, having told the sender if there is not one. */
    private LobbyConfig.NpcEntry entry(Player player, String id) {
        LobbyConfig.NpcEntry entry = this.npcs.find(id);
        if (entry == null) {
            this.notices.viewer(player, messages -> messages.npcUnknown, named(id));
        }
        return entry;
    }

    private static Formatter named(String id) {
        return new Formatter().register("{ID}", id);
    }
}
