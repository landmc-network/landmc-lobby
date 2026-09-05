# LandMC Lobby

Plugin lobby sieci LandMC na Paper: spawn, profil gracza w bazie i komunikacja z proxy.

Cienka warstwa funkcjonalna nad [`landmc-platform`](https://github.com/landmc-network/landmc-platform).
Nie ma tu własnego loadera configów, puli połączeń, klienta Redisa, systemu komend ani warstwy
MiniMessage — wszystko to dostarcza platforma.

```text
Paper
    ↓
landmc-lobby
    ↓
landmc-platform
    ↓
HikariCP + ORMLite / LiteCommands / NoticeService / Messaging
```

## Status

Pierwsza wersja uruchomiona na **Paper 26.2**. To pierwszy projekt, który realnie wykonuje
`platform-paper` i `platform-database`.

```text
[landmc-lobby] LandMC Lobby starting...
[landmc-lobby] Loaded configuration.
[landmc-lobby] Database landmc-lobby connected (H2, 8 connection(s), 8 worker thread(s))
[landmc-lobby] Messaging enabled for lobby-1 (1 message type(s) subscribed)
[landmc-lobby] Database ready (H2).
[landmc-lobby] Registered 3 commands.
[landmc-lobby] Profile autosave every 300s.
[landmc-lobby] LandMC Lobby ready (524 ms).
...
[landmc-lobby] LandMC Lobby stopping...
[landmc-lobby] Saved 0 lobby profile(s) before shutdown.
```

ORMLite tworzy tabelę i indeks, Hikari podnosi i zamyka pulę, komendy wchodzą przez
LiteCommands z platformy, a przy `stop` profile zapisują się **przed** zamknięciem puli.

## Wymagania

| Element | Wersja |
|---|---|
| Paper | 26.2 |
| Java | 25 — tego wymaga bytecode Papera 26.2 i `platform-paper` |
| `landmc-platform` | 1.0.0-SNAPSHOT |
| Redis | opcjonalny, patrz *Messaging* |
| PacketEvents | opcjonalny plugin serwera |

## Build

Platforma jest pobierana z GitHub Packages jako wersja **1.0.0**, a nie ze snapshotu.
Snapshot był nadpisywany w miejscu, więc dwa buildy tego samego commita potrafiły powstać
przeciw różnym platformom — i raz build wywrócił się tylko dlatego, że trafił w moment
publikacji nowego snapshotu.

GitHub Packages wymaga uwierzytelnienia nawet dla publicznego pakietu, więc w
`~/.gradle/gradle.properties` potrzebne są:

```properties
gpr.user=twoj-login-github
gpr.token=ghp_token_z_uprawnieniem_read:packages
```

Alternatywnie, gdy i tak pracujesz nad platformą — wtedy token nie jest potrzebny, bo
`mavenLocal` ma pierwszeństwo:

```bash
cd ../landmc-platform && ./gradlew publishToMavenLocal -Pversion=1.0.0
```

```bash
./gradlew build
```

Wynik: `build/libs/landmc-lobby.jar`.

Jar zawiera wyłącznie `pl/landmc/**`. Okaeri, LiteCommands, Multification, HikariCP, ORMLite,
H2 i Jedis są zrelokowane pod `pl.landmc.lobby.libs`. Adventure, Gson i SLF4J **nie** są
pakowane — dostarcza je Paper, a Jedis ciągnie SLF4J 1.7.x, który przesłoniłby 2.x serwera.

## Profil gracza

To jest miejsce, w którym lobby korzysta z `platform-database`, i robi to według zasad
wydajnościowych LandMC:

```text
wejście  → async load → wstawienie do cache na main threadzie
gra      → odczyt i zmiany na obiekcie w pamięci, zero SQL
timer    → zebranie brudnych snapshotów → jeden batch w transakcji
wyjście  → snapshot → async zapis → usunięcie z cache
```

Trzy osobne klasy, każda po coś:

| Klasa | Rola |
|---|---|
| `LobbyProfile` | mutowalny, należy do main threada, ma flagę dirty |
| `LobbyProfileSnapshot` | niemutowalny rekord — **jedyne**, co przechodzi przez granicę wątków |
| `LobbyProfileEntity` | reprezentacja ORMLite, nie opuszcza `ProfileRepository` |

Podział nie jest ceremonią: gdyby jeden obiekt był jednocześnie encją, cache'em i obiektem
zapisywanym asynchronicznie, gra mogłaby go zmieniać w trakcie zapisu.

Autozapis zapisuje **tylko zmienione** profile, jednym zadaniem dla całego serwera — nie jedno
zadanie na gracza. `autosave-seconds: 0` go wyłącza; profile i tak zapisują się przy wyjściu
i przy wyłączaniu serwera.

Zapytania sortuje i ogranicza baza (`ORDER BY last_seen DESC LIMIT n`), a nie Java — tabela
rośnie z każdym graczem, który kiedykolwiek wszedł.

### Plik bazy H2 nie jest relokowany

Shadow relokuje biblioteki, żeby nie zderzały się z kopiami z innych pluginów, ale **H2 jest
z tego wyłączony celowo**. H2 zapisuje nazwy klas Javy do środka pliku `.mv.db`, więc plik
zbudowany z relokowanym H2 potrafi otworzyć wyłącznie ten jeden jar. Zwykłe `h2.jar` odpowiada
na taki plik `File corrupted while reading record`, a po zmianie prefiksu relokacji plugin
przestałby czytać własną bazę.

Baza profili ma przeżyć jara, który ją zapisał, więc musi być czytelna bez niego.

### Migracja bazy sprzed tej zmiany

Jeżeli masz `database.mv.db` zapisaną **starszą** wersją pluginu (z relokowanym H2), nowa
wersja jej nie otworzy — wyłączy się z `Module database:landmc-lobby failed to enable`. Baza
nie jest uszkodzona, tylko zapisana pod innymi nazwami klas. Przenosisz ją tak:

```bash
java -cp landmc-lobby-STARY.jar pl.landmc.lobby.libs.org.h2.tools.Script -url "jdbc:h2:file:./plugins/landmc-lobby/database;MODE=MySQL" -user "" -password "" -script profiles.sql
```

```bash
java -cp h2-2.4.240.jar org.h2.tools.RunScript -url "jdbc:h2:file:./plugins/landmc-lobby/database;MODE=MySQL" -user "" -password "" -script profiles.sql
```

Między jednym a drugim skasuj (albo odłóż na bok) stary `database.mv.db` — `RunScript` dopisuje
do istniejącej bazy, a nie zastępuje jej. Serwer musi być wyłączony: H2 w trybie plikowym
dopuszcza jeden proces naraz.

Sprawdzone na Paperze 26.2 z H2 2.4.240: po imporcie plugin wstaje normalnie, a profile są na
miejscu. Zrób kopię `database.mv.db`, zanim zaczniesz.

## Konfiguracja

`plugins/landmc-lobby/config.yml`:

```yaml
lobby:
  server-id: "lobby-1"
  autosave-seconds: 300

spawn:
  teleport-on-join: true
  world: ""

database:
  type: H2
  file-name: database
  pool-size: 8

messaging:
  enabled: true
  redis:
    host: "127.0.0.1"
    channel-prefix: "landmc"
```

Sekcje `database` i `redis` pochodzą z platformy, więc są identyczne w każdym pluginie LandMC.
H2 działa bez stawiania czegokolwiek — MariaDB wystarczy ustawić w `type`.

`messages.yml` zawiera komunikaty lobby oraz sekcję `platform:` ze wspólnymi komunikatami
technicznymi.

## Komendy

| Komenda | Uprawnienie | Działanie |
|---|---|---|
| `/spawn` | — | teleport na spawn lobby |
| `/setspawn` | `landmc.command.setspawn` | ustawienie spawnu w miejscu gracza |
| `/profil`, `/profile` | — | profil gracza z cache |
| `/fly` | ranga | latanie na lobby |
| `/npc` | `landmc.command.npc` | stawianie i ubieranie figurek na spawnie |

`/profil` czyta **cache, nie bazę**. Profil został wczytany przy wejściu, więc zapytanie tutaj
byłoby round tripem po dane, które serwer ma w pamięci — i to na main threadzie.

## Figurki na spawnie

Po jednej na tryb, tak jak na poprzedniej wersji sieci: ubrany i wypozowany armor stand z nazwą
trybu, nad nim liczba grających, a nad tym mrugająca zachęta. Kliknięcie albo wejście na jej blok
przenosi na ten serwer.

Trzy napisy nad figurką to **nasze** `TextDisplay`, a nie name tag standa i dwa niewidzialne
markery. Minecraft rysuje name tag tam, gdzie mu wygodnie, więc odstępy trzeba było zgadywać;
tutaj wszystkie trzy stawiamy sami i są równe.

Liczba graczy **nie jest stąd**. Backend widzi tylko tych, którzy na nim stoją, więc proxy
rozgłasza co dwie sekundy liczby ze wszystkich serwerów (`network.server-counts`), a figurka
tylko je pokazuje. Zanim przyjdzie pierwsza wiadomość, napis mówi `...` — tryb pusty i tryb, o
którym jeszcze nie słyszeliśmy, to co innego.

Figurka może też **otwierać menu** zamiast przenosić (`/npc menu <figurka> SHOP`) — na starym
serwerze stał tak NPC sklepu. W tym trybie nie ma nad nią licznika i wejście w nią niczego nie
otwiera; menu, które pojawia się dlatego, że ktoś przeszedł obok, to menu, o które nie prosił.

Stroje są w configu jako szablony, przepisane z tego, co poprzedni serwer miał wpisane w kod, i
nowa figurka dostaje ten o nazwie zgodnej z serwerem. Pozycje pisze komenda, nie człowiek —
figurka to głównie miejsce i kierunek, a nikt nie zna współrzędnych punktu, na którym stoi.

| Komenda | Działanie |
|---|---|
| `/npc utworz <nazwa> <serwer> <napis>` | stawia figurkę tam, gdzie stoisz |
| `/npc tutaj <nazwa>` | przenosi ją w to miejsce |
| `/npc usun <nazwa>` | usuwa ją |
| `/npc serwer <nazwa> <id>` | ustawia, na jaki serwer przenosi |
| `/npc menu <nazwa> <menu>` | zamiast przenosić, otwiera menu |
| `/npc szablon <nazwa> <szablon>` | ubiera ją w gotowy strój |
| `/npc nazwa\|zacheta\|skorka\|przedmiot\|kolor` | pojedyncze elementy wyglądu |
| `/npc lista` | wypisuje wszystkie z pozycjami |

## Messaging

Szyna z `platform-messaging`; w tym projekcie nie ma obsługi Redisa.

Lobby odpowiada na `test.ping` proxy, co domyka pętlę `Velocity → Redis → Paper`. Odpowiada też
na wiadomości kierowane do gracza: `PlayerPresence` sprawdza `getPlayer(uuid) != null`, czyli
indeksowany lookup, nigdy skan po wszystkich graczach. `PlayerLocator` zostaje nieustawiony —
backend nie widzi reszty sieci, a zgadywanie po cichu gubiłoby wiadomości.

`messaging.enabled: false` nie wyłącza szyny, tylko przełącza ją na transport w obrębie
procesu, więc plugin działa bez Redisa.

## Czego tu nie ma

Tablisty i czatu — to `landmc-chat`. Kosmetyk — `landmc-cosmetics`. Rysowania menu —
`landmc-menus`; lobby prosi proxy o otwarcie, bo menu buduje ta strona, która ma dane.
Dodatków do rang i hologramów — `landmc-tools`.

## Testy

```bash
./gradlew test
```

26 testów, bez potrzeby stawiania serwera:

- `ProfileDatabaseTest` — repozytorium na **prawdziwej bazie H2**: wiersz przy pierwszym
  wejściu, transakcja przy zapisie zbiorczym, sortowanie i limit po stronie SQL, zapytania poza
  wątkiem wołającego,
- `ProfileServiceTest` — pełny cykl wejście → cache → autozapis → wyjście, w tym to, że
  autozapis pisze wyłącznie brudne profile i że nieudany odczyt nie wywala gracza,
- `LobbyConfigurationTest` — ładowanie configu i wiadomości w kolejności bootstrapu,
- `LobbyPingPongTest` — odpowiedź na ping proxy i routing wiadomości do gracza.
