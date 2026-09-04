package pl.landmc.lobby.messaging;

import pl.landmc.platform.messaging.message.NetworkMessage;

/** The lobby's answer to a {@link PingMessage}; see that class for why it lives in both projects. */
public record PongMessage(String from, long sentAt) implements NetworkMessage {

    public static final String TYPE = "test.pong";

    @Override
    public String type() {
        return TYPE;
    }
}
