# NT-graves

[![Language: English](https://img.shields.io/badge/Language-English-blue.svg)](#)
[![Deutsch](https://img.shields.io/badge/Sprache-Deutsch-orange.svg)](README_DE.md)
[![Platform: Paper / Purpur / Spigot](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Spigot-green.svg)](#)
[![Minecraft: 1.8.8 - 1.21.4+](https://img.shields.io/badge/Minecraft-1.8.8--1.21.4%2B-brightgreen.svg)](#)
[![Java: 8+](https://img.shields.io/badge/Java-8%2B-red.svg)](#)
[![Donate with PayPal](https://img.shields.io/badge/Donate-PayPal-00457C.svg?logo=paypal&logoColor=white)](https://paypal.me/niktrick)
[![Support on Ko-fi](https://img.shields.io/badge/Support-Ko--fi-FF5E5B.svg?logo=kofi&logoColor=white)](https://ko-fi.com/niktrick)

[🇩🇪 Zur deutschen Version wechseln](README_DE.md) | [🇬🇧 English Version](README.md)

---

Universal, high-reliability player death chest & grave protection plugin for Minecraft **1.8.8 to 1.21.4+** (Paper, Purpur, Spigot).

Protects player items, equipped armor, offhand, and stored XP upon death by spawning a safe grave block with animated countdown holograms, instant shift-pickup, death compass tracking, and a comprehensive admin management GUI with full historical archive recovery.

---

## Features

- **🪦 Safe Death Item Protection**:
  - Automatically spawns a protective grave at the exact death coordinate.
  - Keeps inventory, equipped armor, offhand, and experience levels safe from despawning, void/lava loss, and accidental destruction.
- **✨ Animated Hologram Countdown**:
  - Displays a clean holographic countdown above the grave showing the player's name, remaining expiration timer, and items/XP count.
- **🧭 Death Compass Tracking**:
  - Automatically equips dying players with a tracking compass pointing directly towards their active grave upon respawn.
- **⚡ Instant Shift-Pickup & Auto-Equip**:
  - Sneak-right-click to instantly loot all items directly back into the player's inventory, automatically equipping their saved armor pieces.
- **🖥️ Full Admin GUI & Historical Archive**:
  - `/graves admin` opens an interactive GUI for server admins to inspect active graves, teleport, modify timers, or restore expired death inventories from disk archives.
- **🌐 Bilingual & Live Language Switching**:
  - Configurable in `config.yml` (`language: "en"` or `"de"`).
  - Toggle live anytime via `/graves lang <de|en>`.
- **💾 Robust Async Persistence**:
  - Active and archived graves persist to disk and survive unexpected server restarts, reloads, or crashes.

---

## Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/graves list` | Lists all your active graves with coordinates | `niktrick.graves.list` |
| `/graves claim` | Collects items from your active grave | *None* |
| `/graves open` | Opens your grave inventory GUI | *None* |
| `/graves tp` | Teleports to your most recent grave | `niktrick.graves.tp` |
| `/graves admin` | Opens the administrative management GUI | `niktrick.graves.admin` |
| `/graves history [player]` | Inspects historical death archives | `niktrick.graves.admin` |
| `/graves restore <player>` | Restores a player's saved death inventory | `niktrick.graves.admin` |
| `/graves reload` | Reloads `config.yml` and `messages.yml` | `niktrick.graves.admin` |
| `/graves lang <de\|en>` | Changes plugin language live (Global) | `niktrick.graves.admin` |

---

## Configuration (`config.yml`)

```yaml
# Language setting: "en" for English, "de" for German
language: "en"

prefix: "&8[&6niktrick-graves&8] &7"

# Despawn time in seconds (1200s = 20 minutes; -1 for infinite)
despawn-time-seconds: 1200

# Drop items when grave expires
drop-items: true
dropped-item-velocity: true

# Instant sneak-pickup
enable-instant-pickup: true
instant-pickup-only-own: false

# Auto-equip saved armor when collected
auto-equip-armor: true

# Experience storage
store-xp: true
xp-keep-percentage: 1.0

# Persistence
save-graves:
  enabled: true
  auto-save-seconds: 30
```

---

## Compatibility

- **Minecraft**: 1.8.8, 1.12.2, 1.16.5, 1.17, 1.18, 1.19, 1.20, 1.21.x+
- **Server Software**: Paper, Purpur, Spigot
- **Java**: 8 to 25+ (compiled to Java 8 bytecode for maximum compatibility)
---

## 💖 Support & Donations

If you enjoy this plugin, consider supporting future development:
- [PayPal Donation](https://paypal.me/niktrick)
- [Support on Ko-fi](https://ko-fi.com/niktrick)
- Donations are voluntary and directly help maintain multi-version compatibility!
