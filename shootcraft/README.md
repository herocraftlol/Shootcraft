# ShootCraft 🎯

**ShootCraft** est un plugin Minecraft Paper 1.21 offrant un mini-jeu PvP palpitant où les joueurs s'affrontent avec des bâtons magiques dans des arènes personnalisables.

## ✨ Fonctionnalités

### 🎮 Gameplay
- **Combat tactique** : Chaque joueur dispose d'un **bâton magique** (Blaze Rod) qui tire un rayon quasi-instantané avec effet visuel de particules
- **Système de kills** : Un tir qui touche un adversaire le tue instantanément, le tireur marque un point
- **Turbo** : Un objet de type plume offrant un boost de vitesse de 5 secondes, rechargable toutes les 10 secondes
- **Respawn intelligent** : Les joueurs réapparaissent sur un point aléatoire de l'arène, en évitant la proximité avec d'autres joueurs

### 🏟️ Multi-Arena
- Création et gestion de **plusieurs arènes** simultanément
- **GUI interactive** pour la sélection des arènes
- Système de **zones de jeu** optionnelles (garde-fou anti-vide)
- Configurations individuelles par arène : joueurs min/max, durée de partie

### 📊 Système de jeu
- **Lobby** avec attente et compte à rebours adaptatif
- **Sidebar dynamique** affichant le temps restant et le classement en temps réel
- **Join en cours de partie** possible jusqu'à la fin du timer
- **Classement final** avec options de rejouer/quitter

## 📋 Commandes

Toute la gestion s'effectue via `/sc` (alias `/shootcraft`) :

| Commande | Description |
|----------|-------------|
| `/sc create <nom>` | Crée une nouvelle arène |
| `/sc delete <nom>` | Supprime une arène |
| `/sc list` | Liste toutes les arènes |
| `/sc arenas` | Ouvre le menu GUI de sélection |
| `/sc setlobby <nom>` | Définit le point de lobby |
| `/sc addspawn <nom>` | Ajoute un point de spawn |
| `/sc delspawn <nom> <index>` | Supprime un spawn |
| `/sc setgamezone <nom> <pos1|pos2>` | Définit la zone de jeu |
| `/sc setminplayers <nom> <n>` | Nombre minimum de joueurs |
| `/sc setmaxplayers <nom> <n>` | Nombre maximum de joueurs |
| `/sc settime <nom> <secondes>` | Durée de la partie |
| `/sc join <nom>` | Rejoindre une arène |
| `/sc joinrandom` | Rejoindre une arène aléatoire |
| `/sc leave` | Quitter la partie en cours |
| `/sc start <nom>` | Forcer le démarrage |
| `/sc stop <nom>` | Forcer l'arrêt |
| `/sc info <nom>` | Afficher les infos d'une arène |

### 🔑 Permissions

- `shootcraft.admin` - Configuration du plugin (défaut: op)
- `shootcraft.play` - Jouer au mini-jeu (défaut: true)

## 🚀 Installation rapide

1. Placez `ShootCraft.jar` dans le dossier `plugins` de votre serveur Paper 1.21+
2. Redémarrez le serveur
3. Créez votre première arène :

```
/sc create arene1
/sc setlobby arene1
/sc addspawn arene1
/sc addspawn arene1
```

Votre arène est prête ! Utilisez `/sc arenas` pour la sélectionner et commencer à jouer.

## 🔧 Configuration minimale requise

Pour qu'une arène soit jouable :
- ✅ Un lobby défini (`/sc setlobby`)
- ✅ Au moins 2 points de spawn (`/sc addspawn`)

## 📁 Structure du projet

```
src/main/java/com/shootcraft/plugin/
├── ShootCraftPlugin.java      # Classe principale du plugin
├── commands/                  # Gestion des commandes
├── game/                      # Logique de jeu (GameManager, Arena, etc.)
├── gui/                       # Interface graphique
├── listeners/                 # Événements Bukkit
├── scoreboard/                # Gestion du scoreboard
└── util/                      # Utilitaires
```

## 🛠️ Compilation

```bash
mvn clean package
```

Le fichier JAR final se trouve dans `target/ShootCraft.jar`.

## 📌 Prérequis

- **Serveur** : Paper ou Purpur 1.21.1+
- **Java** : JDK 21
- **Maven** : 3.6+

---

⭐ N'hésitez pas à laisser une étoile si ce plugin vous plaît !

**Version** : 1.0.0
**License** : MIT
