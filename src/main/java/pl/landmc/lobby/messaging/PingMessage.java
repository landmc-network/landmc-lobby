package pl.landmc.lobby.messaging;

import pl.landmc.platform.messaging.message.NetworkMessage;

/**
 * The diagnostic request the proxy sends to prove a node is listening.
 *
 * <p>Deliberately identical to {@code landmc-proxy}'s copy: same wire type, same fields. Both
 * ends need the class and there is no shared network API module yet, so for now it exists twice
 * and the wire format is what keeps them compatible. When a third project needs it, the pair
 * moves into a shared module - that is the point at which duplicating it stops being cheaper
 * than sharing it.
 */
public record PingMessage(String from, long sentAt) implements NetworkMessage {

    public static final String TYPE = "test.ping";

    @Override
    public String type() {
        return TYPE;
    }
}
