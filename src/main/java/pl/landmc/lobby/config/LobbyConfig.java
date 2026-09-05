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
    public HotbarSection hotbar = new HotbarSection();

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

    /** What a player is holding on the lobby, and what each item opens. */
    public static class HotbarSection extends OkaeriConfig {

        @Comment("Czy gracz po wejsciu dostaje ekwipunek lobby.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Ktory slot jest zaznaczony po wejsciu.")
        @CustomKey("selected-slot")
        public int selectedSlot = 0;

        @Comment("")
        @Comment("Przedmioty. Sloty 0-8 to hotbar, tak jak w oryginale: kompas na 0,")
        @Comment("sklep na 2, glowa gracza na 4. Slot 8 mial /podserwery, ktorych ta siec")
        @Comment("jeszcze nie ma - dopisz go tutaj, kiedy beda.")
        public List<HotbarItem> items = defaultItems();

        private static List<HotbarItem> defaultItems() {
            List<HotbarItem> items = new ArrayList<>();
            items.add(new HotbarItem(
                    0, "COMPASS", false,
                    "<green>Wybierz serwer <dark_gray>(PPM/LPM)",
                    List.of("<gray>Wybierz jeden z serwerów, aby ...",
                            "<gray>... się na niego przenieść."),
                    "SERVERS"));
            items.add(new HotbarItem(
                    2, "EMERALD", false,
                    "<green>Sklep premium <dark_gray>(PPM/LPM)",
                    List.of("<gray>Zakup rangę lub inne rzeczy za diamenty!"),
                    "SHOP"));
            items.add(new HotbarItem(
                    4, "PLAYER_HEAD", true,
                    "<green>Twój profil <dark_gray>(PPM/LPM)",
                    List.of("<gray>Sprawdź swój profil klikając."),
                    "PROFILE"));
            return items;
        }
    }

    /** One item on the lobby hotbar. */
    public static class HotbarItem extends OkaeriConfig {

        @Comment("Slot ekwipunku, liczony od zera. Hotbar to 0-8.")
        public int slot = 0;

        @Comment("Material przedmiotu.")
        public String material = "PAPER";

        @Comment("Czy to ma byc glowa gracza, ktory go trzyma.")
        @CustomKey("player-head")
        public boolean playerHead = false;

        @Comment("Nazwa przedmiotu. MiniMessage.")
        public String name = "";

        public List<String> lore = new ArrayList<>();

        @Comment("Menu otwierane po kliknieciu: SERVERS, PROFILE albo SHOP.")
        @Comment("Puste = przedmiot nic nie robi.")
        @Comment("To nazwa menu, a nie komenda: lobby prosi proxy o otwarcie jednego ze")
        @Comment("znanych menu, zamiast wysylac mu komende do wykonania.")
        public String menu = "";

        /** Required by Okaeri. */
        public HotbarItem() {
        }

        public HotbarItem(
                int slot,
                String material,
                boolean playerHead,
                String name,
                List<String> lore,
                String menu) {

            this.slot = slot;
            this.material = material;
            this.playerHead = playerHead;
            this.name = name;
            this.lore = new ArrayList<>(lore);
            this.menu = menu;
        }
    }
}
