<div align="center">

[![Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io/)
[![Purpur](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/purpur_vector.svg)](https://purpurmc.org/)

</div>

---

## ✨ GuardianOfNether — Plugin Minecraft

### 📌 Version 3.0.0

- **Boss :** GuardianOfNether
- **Auteur :** Zyumie (aka AyanoBrz)
- **Compatibilité :** PaperMC / Purpur 1.21+
- **Soft-depend :** StackMob (optionnel)

---

### 🧠 Concept

Le **Gardien du Nether** est un boss en 3 phases configurables qui garde l'accès au Nether.  
Il doit être vaincu pour débloquer le portail. À sa mort, il droppe la **Chestplate of Guardian** — une armure maudite aux pouvoirs uniques.

---

### ⚔️ Fonctionnalités

- **3 phases progressives** — vitesse, taille, armure, dégâts et régénération augmentent à chaque phase
- **Zone de leash** — le boss reste confiné dans un rayon configurable autour de son point de spawn
- **Sbires** — vagues de wither skeletons spawned au départ et à intervalles réguliers
- **Nether verrouillé** — le Nether est inaccessible tant que le boss n'est pas vaincu (configurable)
- **Chestplate of Guardian** — droppée à la mort du boss avec :
  - Armor trim rouge custom
  - Curse of Vanishing (disparaît si le porteur meurt)
  - Effet **Glowing** permanent quand portée
  - Impossible à retirer ou stocker dans un conteneur (coffre, ender chest, shulker box...)
- **Tout est configurable** dans le `config.yml` sans toucher au code

---

### 🔧 Installation

1. Installer un serveur compatible **PaperMC** ou **Purpur** (1.21+)
2. Télécharger `GuardianOfNether.jar` et le placer dans le dossier `plugins/`
3. Redémarrer le serveur
4. Configurer le `plugins/GuardianOfNether/config.yml` selon vos besoins
5. *(Optionnel)* Installer **StackMob** pour éviter le stacking du boss et des sbires

---

### 🎮 Commandes

| Commande | Description | Permission |
|----------|-------------|------------|
| `/guardian-of-nether spawn [x y z]` | Spawne le Gardien du Nether | `guardianofnether.spawn` |
| `/guardian-of-nether reload` | Recharge la config à chaud | `guardianofnether.reload` |
| `/guardian-of-nether nether reset` | Reverrouille le Nether pour un nouveau cycle | `guardianofnether.nether` |
| `/guardian-items [joueur]` | Donne la Chestplate of Guardian | `guardianofnether.items` |

**Alias disponibles :** `/gon`, `/guardian`

---

### 🔑 Permissions

| Permission | Description | Défaut |
|------------|-------------|--------|
| `guardianofnether.spawn` | Spawner le boss | OP |
| `guardianofnether.reload` | Recharger la config | OP |
| `guardianofnether.nether` | Gérer le verrou du Nether | OP |
| `guardianofnether.items` | Donner la Chestplate | OP |

---

### ⚙️ Configuration (`config.yml`)

```yaml
# Paramètres généraux du boss
Boss:
  name: "&4&lGardien du Nether"
  leash-radius: 20          # Zone max autour du spawn (blocs)
  minions-on-spawn: 5       # Sbires spawned au départ
  minions-wave-count: 3     # Sbires par vague
  minions-wave-interval: 60 # Intervalle entre vagues (secondes)

# 3 phases configurables (HP, vitesse, taille, dégâts, regen, armure, particules)
Phases:
  Phase-1: { hp: 300, speed: 0.22, scale: 1.0, ... }
  Phase-2: { hp-threshold-percent: 60, speed: 0.28, scale: 1.3, ... }
  Phase-3: { hp-threshold-percent: 30, speed: 0.35, scale: 1.7, ... }

# Drop à la mort du boss
Drop:
  Chestplate:
    name: "&cChestplate of Guardian"
    curse-of-vanishing: true
    trim: { material: REDSTONE, pattern: SENTRY }

# Comportement du plastron quand porté
Chestplate-Behavior:
  glowing: true               # Effet Glowing permanent
  lock-on-equip: true         # Impossible à retirer
  block-container-storage: true # Impossible à stocker dans un conteneur

# Nether
Nether:
  nether-close: true  # true = bloqué jusqu'à la mort du boss

# Effets à la mort du boss
On-Death:
  clear-inventory: false
  clear-enderchest: false
  kill-villagers: false
  give-chestplate: true

# Tous les messages sont personnalisables dans la section Messages
```

> Le fichier `config.yml` complet est généré automatiquement au premier démarrage.

---

### 📁 Structure du projet

```
fr.zyumie.GuardianOfNether
├── Main.java
├── Config/        ConfigManager, PhaseConfig
├── Boss/          GuardianBoss, PhaseManager, MinionManager
├── Manager/       RewardManager, VersionManager
├── Listener/      BossListener, ArmorListener, NetherListener, AntiStackListener
├── Commandes/     SpawnCommand, ItemCommand
└── SoftDepend/    StackMobHook
```

---

*© 2026 Zyumie. Tous droits réservés.  
Aucune utilisation, modification ou redistribution sans autorisation explicite.*
