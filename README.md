<div align="center">

[![Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io/)
[![Purpur](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/purpur_vector.svg)](https://purpurmc.org/)

</div>

---

## ✨ GuardianOfNether - Minecraft Plugin

### 📌 Version 3.5
- Boss: **Guardian of the Nether** (3 phases, 7 special attacks, improved AI)
- Author: Zyumie (aka AyanoBrz)
- Highlight: while the boss is alive, the **Nether is locked**. Once defeated, it drops the **Chestplate of Guardian**, unlocks the Nether, and can clear players' inventories and enderchests depending on the config.

---

### 🔧 Installation

1. Install a PaperMC or Purpur server (1.21+).
2. Download `GuardianOfNether.jar` and place it in the `plugins/` folder.
3. Restart the server: `Config.yml`, `Boss.yml` and `Nether.yml` are created in `plugins/GuardianOfNether/`.

> ⚠️ **Updating from a version < 3.5**: delete the old `config.yml` from the plugin folder before restarting (settings are now split across 3 files).

---

### 🎮 Commands

| Command | Description | Permission |
|---------|-------------|-----------|
| `/guardian-of-nether spawn [x y z]` | Spawns the Guardian of the Nether | `guardianofnether.spawn` |
| `/guardian-of-nether reload` | Hot-reloads the 3 config files | `guardianofnether.reload` |
| `/guardian-of-nether nether reset` | Locks the Nether again for a new cycle | `guardianofnether.nether` |
| `/guardian-items [player]` | Gives the Chestplate of Guardian | `guardianofnether.items` |

Aliases for `/guardian-of-nether`: `/gon`, `/guardian`.

Extra permission: `guardianofnether.bypass` (ignores the Nether lock, configurable in `Nether.yml`).

---

### ⚙️ Configuration

| File | Contents |
|------|----------|
| `Config.yml` | Chestplate, container protection, enchant / trim / netherite rules, on-death effects, messages |
| `Boss.yml` | Boss, phases, AI, special attacks, minions, boss messages |
| `Nether.yml` | Nether lock, bypass, messages |

#### 🛡️ Chestplate protection (`Config.yml`)

```yaml
Container-Protection:
  enabled: true
  block-all: false          # true = block ALL external inventories at once
  blocked-types:            # otherwise, only these types
    - CHEST
    - ENDER_CHEST
    - SHULKER_BOX
    # ...
  block-bundles: true
  block-item-frames: true

Chestplate-Rules:
  enchantable: false        # enchanting table + anvil
  allow-grindstone: false
  allow-trim: false
  allow-netherite-upgrade: false
```

#### ⚔️ Special attacks (`Boss.yml`)

Each attack has its own `enabled`, `phases`, `cooldown` and settings.

| Attack | Default phases | Description |
|--------|----------------|-------------|
| `magma-floor` | 2, 3 | The floor temporarily turns into magma |
| `sword-rain` | 3 | Rain of netherite swords |
| `fire-nova` | 3 | Fire wave expanding around the boss |
| `leap-slam` | 2, 3 | The boss leaps onto a distant player |
| `meteor-shower` | 3 | Meteors announced by a ring of flames |
| `vortex` | 2, 3 | Pulls players in, then explodes |
| `soul-lightning` | 2, 3 | Marked zones under players, then lightning |

#### 🌋 On boss death (`Config.yml`)

```yaml
On-Death:
  clear-inventory: false    # Clear players' inventories (the killer keeps their Guardian chestplate)
  clear-enderchest: false   # Clear players' enderchests
  kill-villagers: false     # Remove all villagers
  give-chestplate: true     # Give the Chestplate of Guardian to the killer
```

---

© 2026 Zyumie. All rights reserved.
No use, modification or redistribution without explicit permission.
