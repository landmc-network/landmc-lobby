package pl.landmc.lobby.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.landmc.platform.messaging.MessageBus;
import pl.landmc.platform.messaging.PlayerPresence;
import pl.landmc.platform.messaging.message.MessageTarget;
import pl.landmc.platform.messaging.serialization.MessageRegistry;
import pl.landmc.platform.messaging.serialization.MessageSerializer;
import pl.landmc.platform.messaging.transport.LocalMessageTransport;

/**
 * The lobby's side of the network loop: it answers the proxy's ping, and it accepts messages
 * aimed at a player who is on this server.
 *
 * <p>Runs over the in-process transport, so it needs no Redis. Swapping in the Redis transport
 * changes only how the bytes travel, and that layer has its own test in the platform.
 */
class LobbyPingPongTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LobbyPingPongTest.class);

    private final List<MessageBus> buses = new ArrayList<>();

    @AfterEach
    void closeBuses() {
        this.buses.forEach(MessageBus::close);
        this.buses.clear();
    }

    @Test
    void answersThePingSentByTheProxy() throws Exception {
        LocalMessageTransport proxyWire = new LocalMessageTransport("proxy-1");
        LocalMessageTransport lobbyWire = LocalMessageTransport.joining(proxyWire, "lobby-1");

        MessageBus proxy = this.bus("proxy-1", proxyWire, PlayerPresence.NONE);
        MessageBus lobby = this.bus("lobby-1", lobbyWire, PlayerPresence.NONE);

        // Exactly what LobbyBootstrap registers.
        lobby.subscribe(PingMessage.class, (message, context) ->
                context.reply(new PongMessage(lobby.serverId(), message.sentAt())));

        proxy.enable();
        lobby.enable();

        long sentAt = Instant.now().toEpochMilli();
        PongMessage pong = proxy
                .request(
                        MessageTarget.server("lobby-1"),
                        new PingMessage("proxy-1", sentAt),
                        PongMessage.class)
                .get(5, TimeUnit.SECONDS);

        assertEquals("lobby-1", pong.from());
        assertEquals(sentAt, pong.sentAt());
    }

    /**
     * A player-targeted message must reach the server the player is on, and only that one.
     * This is the mechanism a future {@code player.open_gui} will ride on.
     */
    @Test
    void acceptsAMessageAimedAtAPlayerWhoIsHere() {
        UUID playerId = UUID.randomUUID();

        LocalMessageTransport proxyWire = new LocalMessageTransport("proxy-1");
        LocalMessageTransport hostingWire = LocalMessageTransport.joining(proxyWire, "lobby-1");
        LocalMessageTransport otherWire = LocalMessageTransport.joining(proxyWire, "lobby-2");

        MessageBus proxy = this.bus("proxy-1", proxyWire, PlayerPresence.NONE);
        MessageBus hosting = this.bus("lobby-1", hostingWire, playerId::equals);
        MessageBus other = this.bus("lobby-2", otherWire, PlayerPresence.NONE);

        List<String> hostingSaw = new ArrayList<>();
        List<String> otherSaw = new ArrayList<>();
        hosting.subscribe(PingMessage.class, (message, context) -> hostingSaw.add(message.from()));
        other.subscribe(PingMessage.class, (message, context) -> otherSaw.add(message.from()));

        proxy.enable();
        hosting.enable();
        other.enable();

        proxy.publish(
                MessageTarget.player(playerId),
                new PingMessage("proxy-1", Instant.now().toEpochMilli()));

        assertEquals(List.of("proxy-1"), hostingSaw);
        assertTrue(otherSaw.isEmpty(), "a server the player is not on must not be woken");
    }

    private MessageBus bus(String serverId, LocalMessageTransport transport, PlayerPresence presence) {
        MessageRegistry registry = new MessageRegistry()
                .register(PingMessage.TYPE, PingMessage.class)
                .register(PongMessage.TYPE, PongMessage.class);

        MessageBus bus = MessageBus
                .builder(serverId, transport, new MessageSerializer(registry), LOGGER)
                .playerPresence(presence)
                .build();
        this.buses.add(bus);
        return bus;
    }
}
