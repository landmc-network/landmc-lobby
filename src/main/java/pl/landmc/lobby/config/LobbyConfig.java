package pl.landmc.lobby.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pl.landmc.platform.database.DatabaseConfig;
import pl.landmc.platform.messaging.redis.RedisConfig;

/**
 * {@code config.yml}. The database and Redis sections come from the platform rather than being
 * restated here, so their options stay identical across every LandMC plugin.
 */
public class LobbyConfig extends OkaeriConfig {

    public LobbySection lobby = new LobbySection();

    public SpawnSection spawn = new SpawnSection();

    @Comment("")
    public WorldSection world = new WorldSection();

    @Comment("Baza danych profili lobby. H2 dziala bez zadnej konfiguracji.")
    public DatabaseConfig database = new DatabaseConfig();

    public MessagingSection messaging = new MessagingSection();

    public static class LobbySection extends OkaeriConfig {

        @Comment("Identyfikator tej instancji w sieci - musi byc unikalny i zgodny z nazwa")
        @Comment("serwera w konfiguracji proxy, bo pod nim przychodza wiadomosci.")
        @CustomKey("server-id")
        public String serverId = "lobby-1";

        @Comment("Co ile sekund zapisywane sa zmienione profile. 0 wylacza autozapis;")
        @Comment("profile i tak zapisuja sie przy wyjsciu gracza i przy wylaczaniu serwera.")
        @CustomKey("autosave-seconds")
        public int autosaveSeconds = 300;
    }

    public static class SpawnSection extends OkaeriConfig {

        @Comment("Czy gracz po wejsciu ma byc teleportowany na spawn lobby.")
        @CustomKey("teleport-on-join")
        public boolean teleportOnJoin = true;

        @Comment("")
        @Comment("Animacja totemu po wejsciu na lobby - czyli zaraz po zalogowaniu,")
        @Comment("bo niezalogowany gracz nigdy tu nie trafia. Tak bylo na starym LandMC.")
        @CustomKey("totem-on-join")
        public boolean totemOnJoin = true;

        @Comment("Ustawiany komenda /setspawn. Pusty world = spawn nie jest jeszcze ustawiony.")
        public String world = "";

        public double x = 0.0;

        public double y = 0.0;

        public double z = 0.0;

        public float yaw = 0.0f;

        public float pitch = 0.0f;
    }

    /**
     * Ustawienia swiata lobby. Sam generator pustych chunkow podpina sie w bukkit.yml:
     *
     * <pre>
     * worlds:
     *   world:
     *     generator: landmc-lobby
     * </pre>
     */
    public static class WorldSection extends OkaeriConfig {

        @Comment("Swiaty traktowane jako lobby: dostaja ponizsze gamerule i punkt spawnu.")
        @Comment("Same w sobie nie staja sie przez to puste - o generatorze decyduje bukkit.yml.")
        public List<String> worlds = new ArrayList<>(List.of("world"));

        @Comment("")
        @Comment("Punkt spawnu swiata. W pustym swiecie domyslny spawn wypada tam, gdzie nie ma")
        @Comment("bloku, wiec gracz wchodzacy przed wklejeniem budowli spada w void.")
        @CustomKey("set-spawn-point")
        public boolean setSpawnPoint = true;

        @CustomKey("spawn-x")
        public double spawnX = 0.0;

        @CustomKey("spawn-y")
        public double spawnY = 100.0;

        @CustomKey("spawn-z")
        public double spawnZ = 0.0;

        @Comment("")
        @Comment("Gamerule ustawiane przy starcie. Nazwa dokladnie jak w /gamerule.")
        @Comment("randomTickSpeed 0 zatrzymuje losowe ticki blokow - w lobby nie ma czemu rosnac,")
        @Comment("a kazdy taki tick to praca serwera nad budowla, ktora ma stac nieruchomo.")
        public Map<String, String> gamerules = new LinkedHashMap<>(Map.of("randomTickSpeed", "0"));
    }

    public static class MessagingSection extends OkaeriConfig {

        @Comment("Komunikacja z proxy i pozostalymi instancjami przez Redis.")
        @Comment("Wylaczenie nie wylacza szyny - uzywany jest wtedy transport w obrebie procesu,")
        @Comment("wiec plugin dziala bez Redisa, tylko nie widzi innych instancji.")
        public boolean enabled = true;

        public RedisConfig redis = new RedisConfig();
    }
}
