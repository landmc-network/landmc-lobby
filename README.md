# LandMC Lobby

Plugin lobby LandMC dla serwera Paper.

Lobby jest miejscem startowym dla gracza: spawn, wybór trybu, informacje o sieci i podstawowe interakcje.

## Odpowiedzialność

- spawn lobby,
- wybór trybu gry,
- menu serwerów,
- NPC,
- scoreboard,
- tablist,
- kosmetyki,
- podstawowe komendy gracza,
- przekierowanie do SkyBlocka przez proxy.

## Zależności

Projekt powinien korzystać z bibliotek:

- `platform-api`,
- `platform-common`,
- `platform-config`,
- `platform-messaging`,
- `platform-paper`.

## Proponowane moduły

```text
landmc-lobby/
  lobby-plugin/
  lobby-menu/
  lobby-scoreboard/
  lobby-npc/
  lobby-cosmetics/
```

## Zasady

- Lobby nie zawiera logiki SkyBlocka.
- Lobby powinno być lekkie i odporne na restarty innych instancji.
- Menu i wiadomości powinny być konfigurowalne.

## Status

Projekt w przygotowaniu.