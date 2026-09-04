package pl.landmc.lobby.messaging;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Server;
import org.slf4j.Logger;
import pl.landmc.platform.messaging.MessageBus;
import pl.landmc.platform.messaging.PlayerLocator;
import pl.landmc.platform.messaging.PlayerPresence;
import pl.landmc.platform.messaging.redis.RedisMessageTransport;
import pl.landmc.platform.messaging.serialization.MessageRegistry;
import pl.landmc.platform.messaging.serialization.MessageSerializer;
import pl.landmc.platform.messaging.transport.LocalMessageTransport;
import pl.landmc.platform.messaging.transport.MessageTransport;
import pl.landmc.lobby.config.LobbyConfig;

/**
 * Assembles the platform's message bus for this lobby instance.
 *
 * <p>The Paper side of the network answers player-targeted messages, which is the opposite of
 * the proxy: {@code PlayerPresence} here really does know whether a player is on this server, so
 * a message aimed at a player is delivered when they are, and ignored when they are not. That is
 * what lets the proxy address a player without knowing which backend they are on.
 *
 * <p>{@code PlayerLocator} stays unset - a backend has no view of the rest of the network, and
 * guessing would silently drop messages.
 */
public final class LobbyMessaging {

    private LobbyMessaging() {
    }

    public static MessageBus create(LobbyConfig config, Server server, Logger logger) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(logger, "logger");

        String serverId = config.lobby.serverId;

        MessageRegistry registry = new MessageRegistry()
                .register(PingMessage.TYPE, PingMessage.class)
                .register(PongMessage.TYPE, PongMessage.class);

        MessageSerializer serializer = new MessageSerializer(registry);
        MessageTransport transport = transport(config, serverId, serializer, logger);

        return MessageBus.builder(serverId, transport, serializer, logger)
                .playerPresence(presence(server))
                .playerLocator(PlayerLocator.UNKNOWN)
                .build();
    }

    /**
     * Answers "is this player here?".
     *
     * <p>Runs on the transport thread for every player-targeted message, so it is a direct
     * lookup by UUID - {@code getPlayer(UUID)} is indexed - and never a scan over the online
     * players. It reads no world state, which is what makes it safe off the main thread.
     */
    private static PlayerPresence presence(Server server) {
        return (UUID playerId) -> server.getPlayer(playerId) != null;
    }

    private static MessageTransport transport(
            LobbyConfig config, String serverId, MessageSerializer serializer, Logger logger) {

        if (!config.messaging.enabled) {
            logger.warn("Messaging is disabled in config.yml - {} will not see other instances", serverId);
            return new LocalMessageTransport(serverId);
        }

        return new RedisMessageTransport(config.messaging.redis, serverId, serializer, logger);
    }
}
