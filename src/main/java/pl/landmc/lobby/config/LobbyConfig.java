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
    public ScoreboardSection scoreboard = new ScoreboardSection();

    @Comment("")
    public UiSection ui = new UiSection();

    @Comment("")
    public FlySection fly = new FlySection();

    @Comment("")
    @CustomKey("boss-bar")
    public BossBarSection bossBar = new BossBarSection();

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
    /**
     * The drawn interface: which characters the resource pack turns into panels.
     *
     * <p>Only the names live here. Where a panel goes and what sits on it is written in the
     * scoreboard's own lines, because that is the layout and it belongs next to the text it
     * arranges.
     */
    public static class UiSection extends OkaeriConfig {

        @Comment("Czy scoreboard rysuje panele z resourcepacka.")
        @Comment("Wylaczone = tokeny {PANEL:...} i {SPACE:...} sa po prostu usuwane,")
        @Comment("wiec plansza bez paczki wyglada zwyczajnie zamiast pokazywac puste kwadraty.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Fonty z paczki. Pierwszy rysuje panele, drugi tylko przesuwa kursor.")
        public String font = "landmc:ui";

        @CustomKey("space-font")
        public String spaceFont = "landmc:space";

        @Comment("")
        @Comment("Szerokosc, do ktorej dopychana jest kazda linia planszy, w pikselach.")
        @Comment("Bez tego kazda linia jest wyrownywana do prawej osobno i plansza rozjezdza")
        @Comment("sie przy kazdej zmianie liczby. 0 wylacza dopychanie.")
        @CustomKey("line-width")
        public int lineWidth = 124;

        @Comment("")
        @Comment("Panele i ich znaki. Te same, ktore wypisuje scripts/build-ui-font.py")
        @Comment("w landmc-deploy - przy dodaniu nowego panelu przepisz stamtad kolejny.")
        public Map<String, String> panels = new LinkedHashMap<>(new LinkedHashMap<>(Map.ofEntries(
                Map.entry("sidebar_head", "U+E000"),
                Map.entry("sidebar_body", "U+E001"),
                Map.entry("sidebar_foot", "U+E002"),
                Map.entry("chip", "U+E003"),
                Map.entry("chip_wide", "U+E004"),
                Map.entry("icon_time", "U+E100"),
                Map.entry("icon_gems", "U+E101"),
                Map.entry("icon_coins", "U+E102"),
                Map.entry("icon_star", "U+E103"),
                Map.entry("icon_generators", "U+E104"),
                Map.entry("icon_boost", "U+E105"),
                Map.entry("icon_plus", "U+E106"),
                Map.entry("icon_minus", "U+E107"))));

        @Comment("")
        @Comment("Szerokosci tych paneli. Panel przesuwa kursor o swoja szerokosc, wiec")
        @Comment("bez tego linia z panelem liczylaby sie o te piksele za krotko.")
        @CustomKey("panel-widths")
        @Comment("Ikony maja 12 pikseli obrazka plus piksel odstepu, tak jak litera.")
        public Map<String, Integer> panelWidths = new LinkedHashMap<>(new LinkedHashMap<>(Map.ofEntries(
                Map.entry("sidebar_head", 132),
                Map.entry("sidebar_body", 132),
                Map.entry("sidebar_foot", 132),
                Map.entry("chip", 64),
                Map.entry("chip_wide", 220),
                Map.entry("icon_time", 13),
                Map.entry("icon_gems", 13),
                Map.entry("icon_coins", 13),
                Map.entry("icon_star", 13),
                Map.entry("icon_generators", 13),
                Map.entry("icon_boost", 13),
                Map.entry("icon_plus", 13),
                Map.entry("icon_minus", 13))));
    }

    public static class FlySection extends OkaeriConfig {

        @Comment("Komenda /fly i latanie z rangi. Wylaczone = komendy w ogole nie ma.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Kto moze latac. Ten sam wezel sprawdza komenda i nadanie po wejsciu.")
        public String permission = "landmc.lobby.fly";

        @Comment("")
        @Comment("Czy uprawniony gracz dostaje latanie sam po wejsciu.")
        @Comment("Tak bylo na starym LandMC i to jest wlasnie to, za co gracz placi -")
        @Comment("nikt nie chce wpisywac komendy przy kazdym wejsciu.")
        @CustomKey("on-join")
        public boolean onJoin = true;

        @Comment("Po ilu tickach od wejscia. Klient, ktory jeszcze wczytuje swiat, to gubi.")
        @CustomKey("on-join-delay-ticks")
        public int onJoinDelayTicks = 20;
    }

    public static class BossBarSection extends OkaeriConfig {

        @Comment("Pasek u gory ekranu, reklamujacy rangi - tak jak na starym LandMC.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Wiersze paska, od gory. Kazdy to osobny bossbar - tytul bossbara to jedna")
        @Comment("linia, wiec panel na kilka wierszy to kilka paskow, a klient sam ustawia je")
        @Comment("jeden pod drugim. Panel rysowany jest na pierwszym i siega w dol po resztę.")
        @Comment("Placeholdery: {SERVER}, {ONLINE} - tylko takie, ktore sa wspolne dla")
        @Comment("wszystkich, bo paski sa jedne dla calego serwera.")
        public List<String> lines = new ArrayList<>(List.of(
                // An empty row, and that is how the panel is moved down off the top edge of the
                // screen. A boss bar cannot be placed - the client stacks them from the top,
                // nineteen pixels apart - so the way to start lower is to give it a row above
                // with nothing in it. Its own bar is invisible, so nothing shows.
                " ",
                // A row of tiles. Each one is drawn, the cursor is put back inside it for its
                // contents, then moved to where the next one starts - which is what {AT:...}
                // is for. Three of 56 with four pixels between them comes to 176, the width of
                // the row below.
                "{PANEL:chip}{AT:6}{PANEL:icon_gems}{AT:22}<aqua>{DIAMONDS}"
                        + "{AT:78}{PANEL:chip}{AT:84}{PANEL:icon_coins}{AT:100}<gold>{COINS}"
                        + "{AT:156}{PANEL:chip}{AT:162}{PANEL:icon_star}{AT:178}<yellow>{LEVEL}",
                "{PANEL:chip_wide}{AT:8}<red>Rangi <dark_gray>("
                        + "<yellow><bold>VIP</bold><gray>, <light_purple><bold>SVIP</bold><gray>,"
                        + " <b><#FF5555>S<#FFAA00>Z<#FFFF55>E<#55FF55>F<#55FFFF>U<#00AAAA>N"
                        + "<#FF55FF>C<#FF5555>I<#FFAA00>O</b><dark_gray>)"
                        + " <red>➤ <white><underlined>/rangi"));

        @Comment("")
        @Comment("Szerokosc, do ktorej dopychany jest kazdy wiersz paska.")
        @Comment("Tytul bossbara jest wysrodkowany, wiec rowna szerokosc to jedyne, co ustawia")
        @Comment("wiersze w pionie wzgledem siebie - inaczej kazdy centruje sie osobno.")
        @Comment("Rowna sie szerokosci rzedu kafelkow: trzy po 64 z czternastoma przerwami")
        @Comment("miedzy nimi, czyli tyle samo, co szeroki kafelek pod spodem.")
        @CustomKey("line-width")
        public int lineWidth = 220;

        @Comment("")
        @Comment("Kolor paska: PINK, BLUE, RED, GREEN, YELLOW, PURPLE lub WHITE.")
        @Comment("Musi byc ten, ktorego tekstury zeruje paczka - inaczej pod napisami zostana")
        @Comment("widoczne paski. Zerowany jest jeden kolor, zeby zwykly boss dalej mial swoj.")
        public String colour = "BLUE";

        @Comment("Styl: PROGRESS (jednolity, jak w oryginale) albo NOTCHED_6/10/12/20.")
        public String style = "PROGRESS";
    }

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
        @Comment("sklep na 2, glowa gracza na 4, podserwery na 8.")
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
                    List.of("<gray>Sprawdź swoje statystyki klikając."),
                    "PROFILE"));
            items.add(new HotbarItem(
                    8, "PAPER", false,
                    "<green>Wybierz podserwer <dark_gray>(PPM/LPM)",
                    List.of("<gray>Wybierz odpowiedni dla siebie podserwer."),
                    "LOBBIES"));
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

    /** The sidebar, in the shape the old lobby drew it. */
    public static class ScoreboardSection extends OkaeriConfig {

        @Comment("Czy gracze na lobby widza scoreboard.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Co ile tickow odswiezane sa linie ze zmiennymi. 20 tickow to sekunda.")
        @Comment("Zmieniaja sie tylko te linie, ktore zawieraja placeholder.")
        @CustomKey("refresh-ticks")
        public int refreshTicks = 20;

        @Comment("")
        @Comment("Tytul planszy. Rysowany nad pierwsza linia, wiec panel naglowka jest tutaj.")
        @Comment("Pasek z nazwa serwera. Tekst wysrodkowany na panelu: -90 to jego szerokosc")
        @Comment("minus polowa tego, co zostaje po odjeciu napisu.")
        public String title =
                "{SPACE:-6}{PANEL:sidebar_head}{SPACE:-90}<yellow><bold>LandMC.PL";

        @Comment("")
        @Comment("Czy chowac liczby po prawej stronie linii.")
        @Comment("Przy rysowanym interfejsie musza zniknac - to jedyna czesc planszy,")
        @Comment("ktorej pakiet nie przykryje, bo klient rysuje ja po tekscie.")
        @CustomKey("hide-numbers")
        public boolean hideNumbers = true;

        @Comment("")
        @Comment("Linie od gory. Maksymalnie 16.")
        @Comment("{PANEL:nazwa} rysuje panel z paczki, {SPACE:n} przesuwa o n pikseli.")
        @Comment("Panel nie zajmuje miejsca: rysuje sie, a nastepny {SPACE:-...} cofa kursor")
        @Comment("na jego poczatek, wiec tekst laduje na nim, a nie za nim.")
        @Comment("Placeholdery: {PLAYER}, {SERVER}, {ONLINE}, {DIAMONDS}, {COINS}, {LEVEL}")
        @Comment("{COINS} i {LEVEL} pokazuja na razie zero - sieć nie ma jeszcze drugiej")
        @Comment("waluty ani poziomow. Zapala sie same, kiedy te systemy powstana.")
        public List<String> lines = new ArrayList<>(List.of(
                // Empty, and that is the gap between the name bar and the body.
                " ",
                "{SPACE:-6}{PANEL:sidebar_body}{SPACE:-118}<green><bold>Podserwer",
                "{SPACE:8}<white>{SERVER}",
                "{SPACE:8} ",
                "{SPACE:8}<light_purple><bold>Statystyki <white>gracza",
                "{SPACE:8}<light_purple>• <white>Monety: <gold>{COINS}",
                "{SPACE:8}<light_purple>• <white>Diamenty: <aqua>{DIAMONDS}❖",
                "{SPACE:8}<light_purple>• <white>Poziom: <gold>{LEVEL}✰",
                // And this one is the gap between the body and the address.
                " ",
                "{SPACE:-6}{PANEL:sidebar_foot}{SPACE:-82}<yellow>landmc.pl"));
    }
}
