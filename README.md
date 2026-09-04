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

```bash
cd ../landmc-platform && ./gradlew publishToMavenLocal
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

`/profil` czyta **cache, nie bazę**. Profil został wczytany przy wejściu, więc zapytanie tutaj
byłoby round tripem po dane, które serwer ma w pamięci — i to na main threadzie.

## Messaging

Szyna z `platform-messaging`; w tym projekcie nie ma obsługi Redisa.

Lobby odpowiada na `test.ping` proxy, co domyka pętlę `Velocity → Redis → Paper`. Odpowiada też
na wiadomości kierowane do gracza: `PlayerPresence` sprawdza `getPlayer(uuid) != null`, czyli
indeksowany lookup, nigdy skan po wszystkich graczach. `PlayerLocator` zostaje nieustawiony —
backend nie widzi reszty sieci, a zgadywanie po cichu gubiłoby wiadomości.

`messaging.enabled: false` nie wyłącza szyny, tylko przełącza ją na transport w obrębie
procesu, więc plugin działa bez Redisa.

## Czego tu nie ma

Zgodnie z zakresem: NPC, scoreboardu, tablisty, kosmetyk, menu wyboru trybu. Pierwsza wersja
ma udowodnić, że fundament działa pod realnym obciążeniem, a nie dowieźć całe lobby.

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
