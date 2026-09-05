package pl.landmc.lobby.tablist;

import java.util.Objects;
import java.util.Optional;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;

/**
 * The one class in this plugin that mentions a LuckPerms type.
 *
 * <p>Kept apart from {@link RankPrefixes} so nothing loads it unless LuckPerms is installed -
 * see the note there about class verification.
 *
 * <p>A prefix comes out of LuckPerms' own cache, so this is memory access rather than I/O and is
 * safe to call while building a tab list entry.
 */
final class LuckPermsPrefixes implements RankPrefixes {

    private final LuckPerms luckPerms;

    private LuckPermsPrefixes(LuckPerms luckPerms) {
        this.luckPerms = Objects.requireNonNull(luckPerms, "luckPerms");
    }

    /** @throws IllegalStateException when LuckPerms is present but has not started yet */
    static RankPrefixes bind() {
        return new LuckPermsPrefixes(LuckPermsProvider.get());
    }

    @Override
    public String of(Player player) {
        Objects.requireNonNull(player, "player");

        return Optional.ofNullable(this.luckPerms.getUserManager().getUser(player.getUniqueId()))
                .map(user -> user.getCachedData().getMetaData())
                .map(CachedMetaData::getPrefix)
                .filter(prefix -> !prefix.isBlank())
                .orElse("");
    }
}
