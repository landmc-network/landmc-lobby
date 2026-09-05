package pl.landmc.lobby.npc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import pl.landmc.lobby.messaging.ServerCountsMessage;

/**
 * The last thing the proxy said about how busy each server is.
 *
 * <p>Written from a messaging worker and read from the main thread, which is why it is one
 * immutable map swapped whole rather than a map that is edited in place: a reader either sees
 * the previous broadcast or the new one, never half of each, and nothing has to be locked.
 *
 * <p>Empty until the first broadcast arrives. That is the normal state for the first couple of
 * seconds after a restart, so the signs say so rather than saying nought - a mode with nobody on
 * it and a mode we have not heard about are different things.
 */
public final class ServerCounts {

    private final AtomicReference<Map<String, ServerCountsMessage.Server>> counts =
            new AtomicReference<>(Map.of());

    /** Replaces everything known with what the proxy just said. Any thread. */
    public void accept(ServerCountsMessage message) {
        Objects.requireNonNull(message, "message");

        Map<String, ServerCountsMessage.Server> byId = new HashMap<>(message.servers().size());
        for (ServerCountsMessage.Server server : message.servers()) {
            byId.put(server.id(), server);
        }
        this.counts.set(Map.copyOf(byId));
    }

    /** What is known about that server, or null if the proxy has not mentioned it. */
    public ServerCountsMessage.Server of(String serverId) {
        return this.counts.get().get(serverId);
    }

    public boolean isEmpty() {
        return this.counts.get().isEmpty();
    }
}
