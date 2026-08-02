# 🎯 ShootCraft

**ShootCraft** est un mini-jeu PvP palpitant pour serveur Minecraft Paper 1.21+ dans lequel les joueurs s'affrontent dans des arènes en utilisant des bâtons magiques ! Affûtez vos réflexes et devenez le champion des duels.

![Version](https://img.shields.io/badge/Version-1.0.1-blue)
![Paper](https://img.shields.io/badge/Paper-1.21.1-green)
![Java](https://img.shields.io/badge/Java-21-orange)

---

## ⚔️ Comment Jouer

Chaque joueur reçoit un **Bâton Magique** (Blaze Rod) et entre dans l'arène !

### 🎮 Contrôles

| Action | Contrôle | Description |
|--------|----------|-------------|
| **Tir Magique** | Clic droit | Tirez un rayon quasi-instantané qui traverse tous les joueurs alignés sur sa trajectoire |
| **Boost de Vitesse** | Clic gauche | Activez un boost éphémère pour semer vos adversaires |

### 🏆 Système de Combat

- **Tir Précis** : Le rayon traverse les joueurs alignés - touchez plusieurs adversaires d'un seul tir !
- **Kills Multiples** : Double Kill, Triple Kill, Quadruple Kill... et au-delà ! Chaque tir multi-kill est annoncé dans le chat
- **Réapparition Instantanée** : Les victimes réapparaissent immédiatement sur un point de spawn aléatoire
- **Effet de Vitesse Permanent** : Tous les joueurs bénéficient de Vitesse II pendant la partie

### 📊 Statistiques et Classements

Le plugin sauvegarde automatiquement vos statistiques :
- 🏅 **Victoires**
- 💀 **Kills**
- 📈 **Ratio K/D**
- 🎮 **Parties Jouées**

Affichez des **leaderboards holographiques** (Top 10) n'importe où sur la carte avec la commande `/sc leaderboard`.

---

## 🚀 Installation

1. Téléchargez la dernière version depuis la [page des releases](https://github.com/herocraftlol/ShootCraft/releases)
2. Placez le fichier `ShootCraft.jar` dans le dossier `plugins` de votre serveur
3. Redémarrez votre serveur
4. Configurez vos arènes avec les commandes ci-dessous

---

## ⚙️ Configuration des Arènes

### Commandes d'Administration

```
/sc create <nom>                - Créer une nouvelle arène
/sc delete <nom>                - Supprimer une arène
/sc list                        - Lister toutes les arènes
/sc setlobby <nom>              - Définir le point de lobby
/sc addspawn <nom>              - Ajouter un point de spawn
/sc setgamezone <nom>           - Définir la zone de jeu (anti-chute)
/sc setminplayers <nom> <n>    - Définir le nombre minimum de joueurs
/sc setmaxplayers <nom> <n>     - Définir le nombre maximum de joueurs
/sc settime <nom> <secondes>   - Définir la durée de la partie
```

### Configuration Minimale

```
/sc create arene1
/sc setlobby arene1
/sc addspawn arene1  (répéter au moins 2 fois)
```

---

## 🎯 Commandes de Jeu

| Commande | Description |
|----------|-------------|
| `/sc arenas` | Ouvrir le menu de sélection d'arène |
| `/sc join <nom>` | Rejoindre une arène spécifique |
| `/sc joinrandom` | Rejoindre une arène aléatoire |
| `/sc leave` | Quitter la partie en cours |
| `/sc info <nom>` | Afficher les informations d'une arène |
| `/sc leaderboard <catégorie>` | Afficher un leaderboard holographique |

### Catégories de Leaderboard

- `victoires` - Classement par nombre de victoires
- `kills` - Classement par nombre de kills
- `kd` - Classement par ratio kills/décès
- `parties` - Classement par parties jouées

---

## 🔧 Permissions

| Permission | Description | Défaut |
|------------|-------------|--------|
| `shootcraft.admin` | Administration des arènes | OP |
| `shootcraft.play` | Jouer à ShootCraft | Tous |

---

## 🛠️ Compilation

Pour compiler vous-même le plugin :

```bash
mvn clean package
```

Le fichier JAR sera généré dans `target/ShootCraft.jar`.

---

## 📋 Prérequis

- Serveur Minecraft Paper ou Paper-compatible (Purpur, Airplane, etc.)
- Java 21 ou supérieur
- Version Minecraft : 1.21.1+

---

## 🌟 Fonctionnalités

- ✅ Système de combat au bâton magique avec tir穿透
- ✅ Multi-kill system avec annonces dans le chat
- ✅ Plusieurs arènes supportées simultanément
- ✅ Lobby avec compte à rebours adaptatif
- ✅ Stats permanentes sauvegardées (stats.yml)
- ✅ Leaderboards holographiques (Armor Stands)
- ✅ Scoreboard dynamique pendant les parties
- ✅ Interface GUI pour la sélection des arènes
- ✅ Protection anti-chute configurable par arène
- ✅ Configuration complète via config.yml

---

## 📝 Changelog

### v1.0.1
- Corrections de bugs et optimisations
- Compatible Paper 1.21.1

### v1.0.0
- Version initiale
- Système de tir magique avec détection par hitbox
- Boost de vitesse éphémère
- Multi-arènes
- Statistiques et leaderboards holographiques
- Interface GUI de sélection d'arène

---

**Amusez-vous bien sur ShootCraft ! 🎉**
