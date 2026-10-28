# NT-graves

[![Sprache: Deutsch](https://img.shields.io/badge/Sprache-Deutsch-orange.svg)](#)
[![English](https://img.shields.io/badge/Language-English-blue.svg)](README.md)
[![Plattform: Paper / Purpur / Spigot](https://img.shields.io/badge/Plattform-Paper%20%7C%20Purpur%20%7C%20Spigot-green.svg)](#)
[![Minecraft: 1.8.8 - 1.21.4+](https://img.shields.io/badge/Minecraft-1.8.8--1.21.4%2B-brightgreen.svg)](#)
[![Java: 8+](https://img.shields.io/badge/Java-8%2B-red.svg)](#)
[![Donate with PayPal](https://img.shields.io/badge/Donate-PayPal-00457C.svg?logo=paypal&logoColor=white)](https://paypal.me/niktrick)

[🇬🇧 Switch to English Version](README.md) | [🇩🇪 Deutsche Version](README_DE.md)

---

Universelles, hochzuverlässiges Todes-Kisten- und Grab-Schutz-Plugin für Minecraft **1.8.8 bis 1.21.4+** (Paper, Purpur, Spigot).

Schützt Spieler-Inventare, Rüstungen, Zweithand und Erfahrungspunkte beim Tod durch die Erstellung eines geschützten Grabblocks mit animiertem Countdown-Hologramm, Sofort-Einsammeln per Sneaken, Kompass-Tracking und einem vollwertigen Admin-Verwaltungsmenü samt historischem Wiederherstellungsarchiv.

---

## Features

- **🪦 Zuverlässiger Todesschutz**:
  - Erstellt beim Tod automatisch ein sicheres Grab an den exakten Todeskoordinaten.
  - Schützt alle Gegenstände, Rüstung, Zweithand und XP vor Despawnen, Lava, Feuer und fremdem Plündern.
- **✨ Animiertes Countdown-Hologramm**:
  - Zeigt ein schwebendes Hologramm mit Spielernamen, verbleibender Ablaufzeit und Gegenstandsanzahl.
- **🧭 Todeskompass**:
  - Spieler erhalten beim Respawn einen Wiederherstellungs-Kompass, der direkt zu ihrem aktiven Grab weist.
- **⚡ Schnelleinsammeln & Auto-Equip**:
  - Durch Sneaken + Rechtsklick wird das gesamte Grab-Inventar auf einmal eingesammelt und Rüstungsteile werden automatisch angelegt.
- **🖥️ Admin-GUI & Historisches Archiv**:
  - `/graves admin` öffnet ein übersichtliches GUI für Admins, um aktive Gräber einzusehen, sich zu teleportieren oder abgelaufene Inventare aus dem Archiv wiederherzustellen.
- **🌐 Zweisprachig & Live im Spiel umschaltbar**:
  - Einstellbar in der `config.yml` (`language: "de"` oder `"en"`).
  - Live per In-Game-Befehl `/graves lang <de|en>` umschaltbar.
- **💾 Asynchrone Persistenz**:
  - Alle aktiven und archivierten Gräber werden persistent auf Festplatte gespeichert und überstehen Server-Neustarts und Abstürze problemlos.

---

## Befehle & Permissions

| Befehl | Beschreibung | Permission |
| :--- | :--- | :--- |
| `/graves list` | Listet deine aktiven Gräber mit Koordinaten auf | `niktrick.graves.list` |
| `/graves claim` | Sammelt dein aktives Grab-Inventar ein | *Keine* |
| `/graves open` | Öffnet das Grab-Inventar per GUI | *Keine* |
| `/graves tp` | Teleportiert dich zu deinem letzten Grab | `niktrick.graves.tp` |
| `/graves admin` | Öffnet das Admin-Verwaltungsmenü | `niktrick.graves.admin` |
| `/graves history [spieler]` | Durchsucht archivierte Todesfälle | `niktrick.graves.admin` |
| `/graves restore <spieler>` | Stellt das Grab-Inventar eines Spielers wieder her | `niktrick.graves.admin` |
| `/graves reload` | Lädt Konfiguration und Nachrichten neu | `niktrick.graves.admin` |
| `/graves lang <de\|en>` | Ändert die Sprache des Plugins live | `niktrick.graves.admin` |

---

## Konfiguration (`config.yml`)

```yaml
# Spracheinstellung: "en" für Englisch, "de" für Deutsch
language: "en"

prefix: "&8[&6niktrick-graves&8] &7"

# Ablaufzeit in Sekunden (1200s = 20 Minuten; -1 für unbegrenzt)
despawn-time-seconds: 1200

# Items nach Ablauf auf den Boden werfen
drop-items: true
dropped-item-velocity: true

# Sofortiges Einsammeln per Sneak-Rechtsklick
enable-instant-pickup: true
instant-pickup-only-own: false

# Rüstung beim Einsammeln automatisch ausrüsten
auto-equip-armor: true

# Erfahrungs-Speicherung
store-xp: true
xp-keep-percentage: 1.0

# Speicherung
save-graves:
  enabled: true
  auto-save-seconds: 30
```

---

## Kompatibilität

- **Minecraft**: 1.8.8, 1.12.2, 1.16.5, 1.17, 1.18, 1.19, 1.20, 1.21.x+
- **Server-Software**: Paper, Purpur, Spigot
- **Java**: 8 bis 25+ (kompiliert auf Java 8 Bytecode für maximale Kompatibilität)
---

## 💖 Unterstützung & Spenden

Wenn dir dieses Plugin gefällt, freuen wir uns über deine freiwillige Unterstützung:
- [PayPal Spende](https://paypal.me/niktrick)
- Alle Spenden sind freiwillig und fließen direkt in die Wartung und Weiterentwicklung!
