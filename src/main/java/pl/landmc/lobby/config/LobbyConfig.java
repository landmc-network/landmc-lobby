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
    public FlySection fly = new FlySection();

    @Comment("")
    @CustomKey("boss-bar")
    public BossBarSection bossBar = new BossBarSection();

    @Comment("")
    public NpcSection npcs = new NpcSection();

    @Comment("")
    public LevelSection level = new LevelSection();

    @Comment("")
    public ProtectionSection protection = new ProtectionSection();

    @Comment("")
    public PortalSection portal = new PortalSection();

    @Comment("")
    @CustomKey("launch-pads")
    public LaunchPadSection launchPads = new LaunchPadSection();

    @Comment("")
    public WorldSection world = new WorldSection();

    @Comment("Baza danych profili lobby. H2 dziala bez zadnej konfiguracji.")
    public DatabaseConfig database = new DatabaseConfig();

    public MessagingSection messaging = new MessagingSection();

    /**
     * The figures on the spawn, one per game mode.
     *
     * <p>The list starts empty. A figure is mostly a position and a facing, and no default can
     * be right on a map nobody has built yet - so they are placed with {@code /npc utworz},
     * standing where the figure should stand and looking the way it should look, and this file
     * is written from there.
     */
    public static class NpcSection extends OkaeriConfig {

        @Comment("Czy figurki serwerow stoja na tym lobby.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Co ile tickow odswiezany jest licznik i mruganie. Czterdziesci tickow to dwie")
        @Comment("sekundy - tyle samo, co na starym LandMC.")
        @CustomKey("refresh-ticks")
        public long refreshTicks = 40L;

        @Comment("")
        @Comment("Ile nad figurka wisza kolejno: nazwa, licznik graczy i tekst zachety.")
        @Comment("Wszystkie trzy sa naszymi napisami, wiec odstepy sa rowne i przewidywalne;")
        @Comment("stary serwer zostawial nazwe Minecraftowi i dlatego siedziala gdzie chciala.")
        @CustomKey("name-offset")
        public double nameOffset = 2.15D;

        @CustomKey("count-offset")
        public double countOffset = 2.45D;

        @CustomKey("addon-offset")
        public double addonOffset = 2.75D;

        @Comment("")
        @Comment("Napis z liczba graczy. {ONLINE} to liczba z TAMTEGO serwera, nie z lobby -")
        @Comment("przysyla ja proxy, bo backend nie widzi nikogo poza swoimi graczami.")
        @CustomKey("count-format")
        public String countFormat = "<yellow>{ONLINE} graczy";

        @Comment("Zanim przyjdzie pierwsza wiadomosc od proxy. Nie zero: tryb, na ktorym")
        @Comment("nikogo nie ma, i tryb, o ktorym jeszcze nie slyszelismy, to co innego.")
        @CustomKey("count-unknown")
        public String countUnknown = "<gray>...";

        @Comment("Gdy proxy mowi, ze serwer nie odpowiada.")
        @CustomKey("count-offline")
        public String countOffline = "<dark_red>Niedostępny";

        @Comment("")
        @Comment("Tekst zachety mruga - stary serwer przelaczal go miedzy czerwonym a bialym")
        @Comment("co dwie sekundy. Kolory sa tutaj, sam tekst przy figurce.")
        @CustomKey("addon-colours")
        public List<String> addonColours = new ArrayList<>(List.of("<red>", "<white>"));

        @Comment("")
        @Comment("Tekst zachety dla nowo postawionej figurki.")
        @Comment("Osobno dla figurki przenoszacej i otwierajacej menu: zaproszenie do")
        @Comment("dolaczenia nad sklepem jest zaproszeniem donikad.")
        @CustomKey("default-addon")
        public String defaultAddon = "Kliknij, aby dołączyć";

        @CustomKey("default-addon-menu")
        public String defaultAddonMenu = "Kliknij, aby otworzyć";

        @Comment("")
        @Comment("Czy wejscie na blok figurki tez przenosi na serwer, tak jak na starym LandMC.")
        @Comment("Klikniecie dziala zawsze. Dotyczy tylko figurek przenoszacych na serwer -")
        @Comment("w figurke otwierajaca menu mozna wejsc bez otwierania czegokolwiek.")
        @CustomKey("walk-in")
        public boolean walkIn = true;

        @Comment("")
        @Comment("Dzwiek klikniecia w figurke. Stary serwer gral tu BLOCK_NOTE_BASS.")
        @Comment("Puste = bez dzwieku.")
        @CustomKey("click-sound")
        public String clickSound = "BLOCK_NOTE_BLOCK_BASS";

        @Comment("")
        @Comment("Przezroczystosc tla pod napisami, 0-255. Zero to sam tekst, jak nad glowa")
        @Comment("gracza - i tak wygladalo to na starym serwerze.")
        @CustomKey("background-opacity")
        public int backgroundOpacity = 0;

        @Comment("")
        @Comment("Z jakiej odleglosci widac napisy, jako mnoznik zasiegu domyslnego.")
        @CustomKey("view-range")
        public double viewRange = 1.0D;

        @Comment("")
        @Comment("Stroje. Nowa figurka dostaje ten, ktorego nazwa zgadza sie z serwerem -")
        @Comment("wiec /npc utworz na serwer skyblock ubiera ja od razu. Pozniej zmienia to")
        @Comment("/npc szablon. Wartosci sa przepisane ze starego LandMC, gdzie kazdy tryb")
        @Comment("mial swoj stroj wpisany w kod i nie dalo sie go ruszyc bez kompilacji.")
        public List<NpcPreset> presets = new ArrayList<>(List.of(
                preset(
                        "skyblock",
                        "https://textures.minecraft.net/texture/"
                                + "a96d16691ff549e8d1897a2190fbc4e0f642c910fa3da1ae19b45fa9c4f3bc2",
                        "#556B2F",
                        "GRASS_BLOCK",
                        List.of(),
                        List.of(294.08D, 0.0D, 147.04D),
                        List.of(0.0D, 116.62D, 344.79D),
                        List.of()),
                preset(
                        "budowlany",
                        "https://textures.minecraft.net/texture/"
                                + "637ec2c01b3f55b9ed09cac1c5378732abf26f383ba0a0df3eac2e29e280c3",
                        "#FFFF00",
                        "CRAFTING_TABLE",
                        List.of(50.0D, 97.0D, 27.0D),
                        List.of(357.0D, 40.0D, 14.0D),
                        List.of(9.0D, 17.0D, 12.0D),
                        List.of(0.0D, 29.0D, 72.0D)),
                preset(
                        "sklep",
                        "",
                        "#FFAA00",
                        "EMERALD",
                        List.of(),
                        List.of(340.0D, 0.0D, 15.0D),
                        List.of(),
                        List.of(),
                        "EMERALD_BLOCK"),
                preset(
                        "nagrody",
                        "",
                        "#55FF55",
                        "GOLD_INGOT",
                        List.of(),
                        List.of(340.0D, 0.0D, 15.0D),
                        List.of(),
                        List.of(),
                        "CHEST"),
                preset(
                        "partygames",
                        "https://textures.minecraft.net/texture/"
                                + "fa4c0a8c200c4f1fedf8bbbe31daa81504dcb2f920b00e71f85e0eb1904a1929",
                        "#A06540",
                        "NETHER_STAR",
                        List.of(56.0D, 65.0D, 282.0D),
                        List.of(0.0D, 0.0D, 134.0D),
                        List.of(27.0D, 27.0D, 354.0D),
                        List.of(0.0D, 67.0D, 39.0D))));

        @Comment("")
        @Comment("Postawione figurki. Pisze to /npc, recznie zwykle nie trzeba tu zagladac.")
        public List<NpcEntry> list = new ArrayList<>();

        private static NpcPreset preset(
                String id,
                String skin,
                String armourColour,
                String item,
                List<Double> leftArm,
                List<Double> rightArm,
                List<Double> leftLeg,
                List<Double> rightLeg) {

            return preset(id, skin, armourColour, item, leftArm, rightArm, leftLeg, rightLeg,
                    "PLAYER_HEAD");
        }

        private static NpcPreset preset(
                String id,
                String skin,
                String armourColour,
                String item,
                List<Double> leftArm,
                List<Double> rightArm,
                List<Double> leftLeg,
                List<Double> rightLeg,
                String head) {

            NpcPreset preset = new NpcPreset();
            preset.head = head;
            preset.id = id;
            preset.skin = skin;
            preset.armourColour = armourColour;
            preset.item = item;
            preset.leftArm = new ArrayList<>(leftArm);
            preset.rightArm = new ArrayList<>(rightArm);
            preset.leftLeg = new ArrayList<>(leftLeg);
            preset.rightLeg = new ArrayList<>(rightLeg);
            return preset;
        }
    }

    /**
     * How one kind of figure is dressed and posed.
     *
     * <p>Separate from the figure itself because the two answer different questions. Where a
     * figure stands is about this map and nothing else; what it wears is about the mode it
     * advertises, and is the same on every lobby the network ever has.
     */
    public static class NpcPreset extends OkaeriConfig {

        @Comment("Nazwa szablonu. Zgodna z nazwa serwera = zakladany automatycznie.")
        public String id = "";

        @Comment("Adres tekstury glowy. Puste = uzyty zostanie material ponizej.")
        public String skin = "";

        @Comment("Co na glowie, kiedy nie ma tekstury.")
        public String head = "PLAYER_HEAD";

        @Comment("Kolor skorzanej zbroi, szesnastkowo.")
        @CustomKey("armour-colour")
        public String armourColour = "";

        @Comment("Przedmiot w rece.")
        public String item = "";

        @Comment("")
        @Comment("Pozy, po trzy stopnie: obrot wokol X, Y i Z. Pusta lista = poza domyslna.")
        @CustomKey("left-arm")
        public List<Double> leftArm = new ArrayList<>();

        @CustomKey("right-arm")
        public List<Double> rightArm = new ArrayList<>();

        @CustomKey("left-leg")
        public List<Double> leftLeg = new ArrayList<>();

        @CustomKey("right-leg")
        public List<Double> rightLeg = new ArrayList<>();
    }

    /** One figure: where it stands, what it is called and where it sends you. */
    public static class NpcEntry extends OkaeriConfig {

        @Comment("Nazwa uzywana w komendach.")
        public String id = "";

        @Comment("Co robi klikniecie: SERWER przenosi na serwer z pola nizej,")
        @Comment("MENU otwiera menu. Na starym LandMC byly obie: figurki trybow przenosily,")
        @Comment("a figurka sklepu odpalala /sklep.")
        public String action = "SERWER";

        @Comment("Serwer, na ktory przenosi. Ta sama nazwa, co w konfiguracji proxy.")
        public String server = "";

        @Comment("Menu otwierane przy action: MENU. SHOP, RANKS, VISUAL_RANKS, COSMETICS,")
        @Comment("PROFILE, STATISTICS, SERVERS, LOBBIES, FRIENDS, PUNISHMENTS, REPORT.")
        public String menu = "";

        @Comment("Napis nad figurka i tekst zachety pod nim. MiniMessage;")
        @Comment("kolor zachety nadaja addon-colours, wiec tutaj sam tekst.")
        public String name = "";

        public String addon = "";

        @Comment("")
        public String world = "world";

        public double x = 0.0D;
        public double y = 0.0D;
        public double z = 0.0D;

        @Comment("W ktora strone patrzy.")
        public float yaw = 0.0F;

        @Comment("")
        @Comment("Adres tekstury glowy. Puste = uzyty zostanie material ponizej.")
        public String skin = "";

        @Comment("Co figurka ma na glowie, kiedy nie ma tekstury. Armor stand nie ma wlasnej")
        @Comment("glowy - glowa to zawartosc slotu na helm, wiec bez tego stoi bez niej.")
        public String head = "PLAYER_HEAD";

        @Comment("Kolor skorzanej zbroi: nazwa (green, gold, brown...) albo #rrggbb.")
        @Comment("Puste = zbroja bez barwienia.")
        @CustomKey("armour-colour")
        public String armourColour = "";

        @Comment("Przedmiot w rece. Puste = pusta reka.")
        public String item = "";

        @Comment("")
        @Comment("Pozy, po trzy stopnie: obrot wokol X, Y i Z. Pusta lista = poza domyslna.")
        @Comment("Latwiej ustawic je patrzac na figurke niz licząc - stary serwer mial je")
        @Comment("wpisane w kod i dochodzil do nich tak samo.")
        @CustomKey("left-arm")
        public List<Double> leftArm = new ArrayList<>();

        @CustomKey("right-arm")
        public List<Double> rightArm = new ArrayList<>();

        @CustomKey("left-leg")
        public List<Double> leftLeg = new ArrayList<>();

        @CustomKey("right-leg")
        public List<Double> rightLeg = new ArrayList<>();
    }

    /** Keeping people alive in a place where nothing is meant to hurt them. */
    public static class ProtectionSection extends OkaeriConfig {

        @Comment("Czy gracz na lobby moze dostac obrazenia. Tryb adventure i peaceful zdejmuja")
        @Comment("moby i glod, ale nie upadek - a lobby to mapa z krawedziami.")
        @CustomKey("block-damage")
        public boolean blockDamage = true;

        @Comment("")
        @Comment("Czy blokowac glod. Peaceful i tak go zdejmuje; to jest po to, zeby podniesienie")
        @Comment("poziomu trudnosci nie zaczelo po cichu glodzic lobby.")
        @CustomKey("keep-fed")
        public boolean keepFed = true;
    }

    /**
     * Portal na spawnie: wbiegniecie odrzuca i otwiera wybor trybu.
     *
     * <p>Prosto ze starej wersji sieci. Portal czyta sie jako przejscie dalej bez pisania nad
     * nim "kliknij tutaj", a nic w nim nie teleportuje.
     */
    public static class PortalSection extends OkaeriConfig {

        @Comment("Czy portale na lobby odrzucaja i otwieraja menu.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Sila odrzutu do tylu i podbicia w gore.")
        public double bounce = 0.8D;

        public double lift = 0.45D;

        @Comment("")
        @Comment("Po ilu tickach otwiera sie menu. Okno, ktore pojawia sie w pol kroku,")
        @Comment("lapie klawisz przeznaczony na chodzenie.")
        @CustomKey("delay-ticks")
        public long delayTicks = 7L;

        @Comment("")
        @Comment("Ktore menu. SERVERS to wybor trybu, tak jak /serwery.")
        public String menu = "SERVERS";
    }

    /**
     * Plytki, ktore wyrzucaja gracza w powietrze.
     *
     * <p>Stary serwer mial jedna, kamienna, wpisana w kod. Lista materialow zamiast jednego,
     * bo to dekoracja mapy - budowniczy chce miec do wyboru cos, co pasuje do podlogi.
     */
    public static class LaunchPadSection extends OkaeriConfig {

        @Comment("Czy plytki naciskowe wyrzucaja gracza.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Materialy dzialajace jak wyrzutnia.")
        public List<String> materials =
                new ArrayList<>(List.of("STONE_PRESSURE_PLATE"));

        @Comment("")
        @Comment("Sila do przodu i w gore. Stary serwer mial poltora w obu.")
        public double power = 1.5D;

        public double lift = 1.5D;

        @Comment("")
        @Comment("Dzwiek wyrzutu. Pusty = bez dzwieku.")
        public String sound = "ENTITY_ENDER_DRAGON_SHOOT";
    }

    /**
     * Poziom gracza, liczony z liczby wejsc.
     *
     * <p>Bez osobnego licznika expa: siec i tak wie, ile razy ktos wrocil, a poziom czytany
     * z czegos, co juz jest prawda, nie ma jak sie z tym rozjechac. Stary serwer mial na
     * scoreboardzie zero wpisane na sztywno.
     */
    public static class LevelSection extends OkaeriConfig {

        @Comment("Ile wejsc kosztuje pierwszy poziom. Kazdy nastepny kosztuje o tyle wiecej:")
        @Comment("przy piatce poziom 1 wypada na piatym wejsciu, 2 na pietnastym, 3 na trzydziestym.")
        @CustomKey("visits-per-level")
        public int visitsPerLevel = 5;
    }

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
        @Comment("Wiersze paska, od gory. Kazdy to osobny bossbar - klient sam ustawia je")
        @Comment("jeden pod drugim.")
        @Comment("Placeholdery: {SERVER}, {ONLINE}, {DIAMONDS}, {COINS}, {LEVEL}")
        public List<String> lines = new ArrayList<>(List.of(
                "<aqua>Diamenty: <white>{DIAMONDS}"
                        + " <dark_gray>| <gold>Monety: <white>{COINS}"
                        + " <dark_gray>| <yellow>Poziom: <white>{LEVEL}",
                "<yellow><bold>VIP</bold><gray>, <light_purple><bold>SVIP</bold><gray>,"
                        + " <b><#FF5555>S<#FFAA00>Z<#FFFF55>E<#55FF55>F<#55FFFF>U<#00AAAA>N"
                        + "<#FF55FF>C<#FF5555>I<#FFAA00>O</b>"
                        + " <red>➤ <white><underlined>/rangi"));

        @Comment("")
        @Comment("Kolor paska: PINK, BLUE, RED, GREEN, YELLOW, PURPLE lub WHITE.")
        public String colour = "GREEN";

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
        @Comment("sklep na 2, glowa gracza na 4, podserwery na 8. Slot 6 stal pusty i")
        @Comment("w oryginale, i jest teraz nasz - dodatki nie istnialy na starym LandMC.")
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
                    6, "FIREWORK_ROCKET", false,
                    "<green>Dodatki <dark_gray>(PPM/LPM)",
                    List.of("<gray>Cząsteczki i poświata za diamenty."),
                    "COSMETICS"));
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
        public String title = "<yellow><bold>LandMC.PL";

        @Comment("")
        @Comment("Czy chowac liczby po prawej stronie linii.")
        @Comment("Przy rysowanym interfejsie musza zniknac - to jedyna czesc planszy,")
        @Comment("ktorej pakiet nie przykryje, bo klient rysuje ja po tekscie.")
        @CustomKey("hide-numbers")
        public boolean hideNumbers = true;

        @Comment("")
        @Comment("Linie od gory. Maksymalnie 16.")
        @Comment("Placeholdery: {PLAYER}, {SERVER}, {ONLINE}, {DIAMONDS}, {COINS}, {LEVEL}")
        public List<String> lines = new ArrayList<>(List.of(
                " ",
                "<green><bold>Podserwer",
                "<gray>» <white>{SERVER}",
                "  ",
                "<light_purple><bold>Statystyki",
                "<gray>» <white>Monety: <gold>{COINS}",
                "<gray>» <white>Diamenty: <aqua>{DIAMONDS}",
                "<gray>» <white>Poziom: <yellow>{LEVEL}",
                "   ",
                "<yellow>landmc.pl"));
    }
}
