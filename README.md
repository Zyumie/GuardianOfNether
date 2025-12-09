<div align="center">

[![Paper](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/paper_vector.svg)](https://papermc.io/)
[![Purpur](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/purpur_vector.svg)](https://purpurmc.org/)

</div>

---

## ✨ GuardianOfNether - Plugin Minecraft

### 📌 Version 2.0.0
- Boss: **GuardianOfNether**
- Auteur: Zyumie (aka AyanoBrz)
- Spécialité: Une fois vaincu, le boss droppe le **Pack 5** (P5) et peut clear les stuffs et enderchests selon la config.

---

### 🔧 Installation

1. Installer un serveur compatible PaperMC ou Purpur (1.21+).  
2. Télécharger le plugin `GuardianOfNether.jar` et placer dans le dossier `plugins/`.  
3. Redémarrer le serveur.  

---

### 🎮 Commandes

| Commande | Description | Permission | Exemple |
|----------|------------|-----------|---------|
| `/guardian-of-nether spawn [x y z]` | Fait apparaître le Gardien du Nether | `guardianofnether.spawn.gardianofnether` | `/guardian-of-nether spawn 100 64 -200` |
| `/guardian-items [player]` | Donne les items du Gardien du Nether | `guardianofnether.give.guardianitems` | `/guardian-items Zyumie` |

---

### ⚙️ Configuration (`config.yml`)

```yaml
Clear-Stuff:
  clear-inventory: true   # Clear l'inventaire des joueurs (sauf killer si false)
  clear-enderchest: true  # Clear l'enderchest des joueurs (sauf killer si false)
