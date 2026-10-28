# 🪦 niktrick-graves — Player Death Chests & Graves System

[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur-brightgreen.svg)](#)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-blue.svg)](#)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](#)

A standalone Minecraft Paper/Purpur plugin that protects player items upon death by creating interactive graves with countdown holograms, compass tracking, auto-recovery, and a full administrative management GUI.

---

## ✨ Features

- **🪦 Safe Death Protection**:
  - Automatically spawns a protective grave chest at the player's death coordinate.
  - Keeps all items, armor, offhand, and stored XP safe from despawning, lava, and fire.
- **✨ Animated Hologram Countdown**:
  - Live rotating armor stand / text display hologram showing owner name, remaining expiration timer, and items count.
- **🧭 Compass Tracking**:
  - Gives dying players a temporary recovery compass pointing directly towards their grave location.
- **🛡️ Permission-based Access & Protection**:
  - Only the grave owner can open and claim their items (until public expiry timeout).
  - Sneak-interact to quickly loot all items directly back into the player's inventory.
- **🖥️ Full Admin GUI & Restore History**:
  - `/graves admin` opens an interactive GUI listing all active graves, historical death archives, and 1-click restore or teleportation.
- **💾 Async File Persistence**:
  - Graves are saved persistently to YAML and withstand sudden server restarts or crashes.

---

## 📜 Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/graves list` | `niktrick.graves.list` | Lists your active graves with coordinates |
| `/graves tp <id>` | `niktrick.graves.tp` | Teleports to your grave |
| `/graves admin` | `niktrick.graves.admin` | Opens admin management GUI |
| `/graves history [player]` | `niktrick.graves.admin` | Inspects death archives and restores inventories |
| `/graves reload` | `niktrick.graves.admin` | Reloads `config.yml` |

---

## 📦 Installation

1. Drop `niktrick-graves-1.0.0.jar` into your server's `plugins/` directory.
2. Restart your server.
3. Your players' death items are now fully protected!
