# 🎯 ShootCraft

**ShootCraft** est un plugin Minecraft Paper 1.21+ qui transforme votre serveur en arène de combat palpitante ! Affrontez vos amis dans des duels épiques utilisant des bâtons magiques, avec un système de multi-arènes, des classements holographiques et bien plus encore.

## ✨ Fonctionnalités

### 🪄 Combat Magique
- **Bâton magique** (Blaze Rod) qui tire un rayon quasi-instantané traversant tous les joueurs alignés
- Système de **multi-kill** (double, triple, quadruple...) avec annonces dans le chat
- Réspawn intelligent sur des points aléatoires dans l'arène

### 🏟️ Multi-Arènes
- Créez et gérez autant d'arènes que vous souhaitez
- **GUI interactive** pour naviguer et sélectionner les arènes
- Configuration flexible : taille min/max de joueurs, durée des parties

### ⚡ Capacités Spéciales
- **Turbo Boost** : accélération de vitesse temporaire (5 secondes de Vitesse II)
- **Vitesse permanente** pendant les combats
- Item de **force de démarrage** pour les admins

### 📊 Statistiques & Classements
- **Leaderboards holographiques** top 10 en temps réel
- 4 catégories : Victoires, Kills, Ratio K/D, Parties jouées
- Persistance entre les redémarrages du serveur
- Scoreboard en jeu avec le classement en direct

## 🚀 Installation

1. Téléchargez la dernière release
2. Placez le fichier `.jar` dans le dossier `plugins` de votre serveur Paper 1.21+
3. Redémarrez le serveur
4. Configurez vos arènes avec la commande `/sc`

## 📋 Commandes

| Commande | Description |
|----------|-------------|
| `/sc create <nom>` | Créer une nouvelle arène |
| `/sc arenas` | Ouvrir le menu de sélection d'arène |
| `/sc setlobby <nom>` | Définir le point de spawn (lobby) |
| `/sc addspawn <nom>` | Ajouter un point de spawn |
| `/sc setgamezone <nom> <pos1|pos2>` | Définir la zone de jeu |
| `/sc leaderboard <catégorie>` | Afficher un classement holographique |
| `/sc join <nom>` | Rejoindre une arène |
| `/sc leave` | Quitter l'arène en cours |

### Permissions
- `shootcraft.admin` - Accès complet à la configuration (par défaut: OP)
- `shootcraft.play` - Jouer au minijeu (par défaut: true)

## 🛠️ Configuration Minimale

```
/sc create monarene
/sc setlobby monarene
/sc addspawn monarene (répéter au moins 2 fois)
```

## 🔧 Dépendances

- Paper 1.21 ou supérieur
- Java 21

## 📝 Compilation

```bash
mvn clean package
```

Le fichier JAR se trouvera dans `target/ShootCraft.jar`.

## 📝 License

Ce plugin est fourni tel quel.

---

⭐ N'oubliez pas de starer le projet si vous l'appréciez !
