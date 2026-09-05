package pl.landmc.lobby.config;

import com.eternalcode.multification.notice.Notice;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import pl.landmc.platform.config.message.PlatformMessagesConfig;

/**
 * {@code messages.yml} - the lobby's own messages, with the platform's technical ones embedded.
 *
 * <p>Same split as every LandMC project: {@code command-no-permission} and friends are framework
 * vocabulary and live under {@code platform}, while anything about spawn or profiles is this
 * plugin's domain. Placeholders in a {@link Notice} are Multification's {@code {BRACES}}.
 */
public class LobbyMessages extends OkaeriConfig {

    @Comment("Komunikaty techniczne wspolne dla calej sieci - dostarcza je landmc-platform.")
    @Comment("Zmiana klucza 'prefix' przestawia wyglad wszystkich naraz.")
    public PlatformMessagesConfig platform = new PlatformMessagesConfig();

    @Comment("")
    @Comment("Komunikaty lobby.")
    @CustomKey("spawn-teleported")
    public Notice spawnTeleported = Notice.chat("<green><bold>LOBBY</bold> <gray>Przeniesiono na spawn.");

    @CustomKey("spawn-set")
    public Notice spawnSet = Notice.chat("<green><bold>LOBBY</bold> <gray>Ustawiono spawn w tym miejscu.");

    @CustomKey("spawn-not-set")
    public Notice spawnNotSet =
            Notice.chat("<red>Błąd> <gray>Spawn nie został jeszcze ustawiony. Użyj <white>/setspawn</white>.");

    @Comment("")
    @Comment("Komenda /fly. Slowa te same, co na starym LandMC.")
    @CustomKey("fly-enabled")
    public Notice flyEnabled = Notice.chat(
            "<green><bold>LOBBY</bold> <green>Włączono <gray>latanie dla siebie!");

    @CustomKey("fly-disabled")
    public Notice flyDisabled = Notice.chat(
            "<green><bold>LOBBY</bold> <red>Wyłączono <gray>latanie dla siebie!");

    @Comment("")
    @Comment("Gdy gracz nie ma rangi, ktora pozwala latac.")
    @CustomKey("fly-no-permission")
    public Notice flyNoPermission = Notice.chat(
            "<red>Błąd> <gray>Latanie na lobby jest dodatkiem do rangi."
                    + " Zobacz <white>/rangi</white>.");

    @Comment("")
    @Comment("Placeholdery: {NAME}, {VISITS}, {FIRST_JOIN}, {LAST_SEEN}")
    @CustomKey("profile-header")
    public Notice profileHeader = Notice.chat("<green><bold>PROFIL</bold> <gray>Profil gracza <white>{NAME}</white>:");

    @CustomKey("profile-visits")
    public Notice profileVisits = Notice.chat("<dark_gray>» <gray>Wejść: <white>{VISITS}</white>");

    @CustomKey("profile-first-join")
    public Notice profileFirstJoin = Notice.chat("<dark_gray>» <gray>Pierwsze wejście: <white>{FIRST_JOIN}</white>");

    @CustomKey("profile-not-loaded")
    public Notice profileNotLoaded =
            Notice.chat("<red>Błąd> <gray>Twój profil nie został jeszcze wczytany. Spróbuj za chwilę.");

    @Comment("")
    @Comment("Wysylane po zapisaniu profili komenda administracyjna. Placeholder: {COUNT}")
    @CustomKey("profiles-saved")
    public Notice profilesSaved = Notice.chat("<green><bold>PROFIL</bold> <gray>Zapisano profili: <white>{COUNT}</white>.");
}
