package pl.landmc.lobby.messaging;

import java.util.List;
import java.util.Objects;
import pl.landmc.platform.messaging.message.NetworkMessage;

/**
 * How many players are on each server, as the proxy last said.
 *
 * <p>This server can see the people standing on it and nobody else. A sign on the spawn saying
 * how busy SkyBlock is therefore has to be told, and the proxy is the only process that can
 * tell it.
 *
 * <p>Deliberately identical to {@code landmc-proxy}'s copy: same wire type, same fields. Both
 * ends need the class and there is no shared network API module, so for now it exists twice and
 * the wire format is what keeps them compatible. When a third project needs it, the pair moves
 * into a shared module - that is the point at which duplicating stops being cheaper than
 * sharing.
 */
public record ServerCountsMessage(List<Server> servers, long sentAt) implements NetworkMessage {

    public static final String TYPE = "network.server-counts";

    public ServerCountsMessage {
        servers = List.copyOf(Objects.requireNonNull(servers, "servers"));
    }

    @Override
    public String type() {
        return TYPE;
    }

    /** One server: what it is called, how many are on it, and whether it answered. */
    public record Server(String id, int online, boolean reachable) {

        public Server {
            Objects.requireNonNull(id, "id");
        }
    }
}
