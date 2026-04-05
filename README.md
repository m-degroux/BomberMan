# Bomberman

## Description

Bomberman est un jeu multijoueur Bomberman avec intelligence artificielle, développé en Java. Le projet utilise une architecture client-serveur avec JavaFX pour l'interface client. Il comprend des fonctionnalités telles que la création de lobbies, le chat en jeu, et des IA pour les joueurs.

Le projet est organisé en modules Maven :
- **common** : Classes partagées (modèles, protocoles, logging)
- **serverSide** : Logique serveur et gestion du jeu
- **clientSide** : Interface utilisateur et logique client
- **launcher** : Lanceur pour démarrer les composants
- **coverage** : Configuration pour la couverture de code

## Fonctionnalités

- Jeu multijoueur Bomberman
- Intelligence artificielle pour les joueurs
- Création et gestion de lobbies
- Chat en temps réel
- Interface graphique avec JavaFX
- Architecture client-serveur
- Tests unitaires avec JUnit 5
- Couverture de code avec JaCoCo

## Architecture

Le système suit une architecture modulaire avec séparation claire entre client et serveur. Les diagrammes PlantUML suivants décrivent l'architecture :

- [bomberman-architecture.puml](bomberman-architecture.puml) : Vue d'ensemble de l'architecture
- [bomberman-class-hierarchy.puml](bomberman-class-hierarchy.puml) : Hiérarchie des classes
- [bomberman-entities.puml](bomberman-entities.puml) : Entités du jeu
- [bomberman-network.puml](bomberman-network.puml) : Architecture réseau
- [bomberman-sequence.puml](bomberman-sequence.puml) : Diagrammes de séquence
- [bomberman-ai-system.puml](bomberman-ai-system.puml) : Système d'IA

## Prérequis

- Java 21 ou supérieur
- Maven 3.6 ou supérieur

## Installation et Compilation

1. Clonez le dépôt :
   ```bash
   git clone https://git.unicaen.fr/picot244/sae401.git
   cd sae401
   ```

2. Compilez le projet :
   ```bash
   mvn clean install
   ```

## Exécution

Utilisez les scripts fournis pour démarrer les composants :

- Démarrer un serveur :
  ```bash
  ./start-launcher.sh server
  ```

- Démarrer un client :
  ```bash
  ./start-launcher.sh client
  ```

- Démarrer un serveur et un client :
  ```bash
  ./start-launcher.sh
  ```

## Tests

Exécutez les tests avec Maven :
```bash
mvn test
```

Pour générer le rapport de couverture :
```bash
mvn jacoco:report
```

## Structure du Projet

```
sae401/
├── common/          # Classes communes
├── serverSide/      # Côté serveur
├── clientSide/      # Côté client (JavaFX)
├── launcher/        # Lanceur
├── coverage/        # Configuration couverture
├── pom.xml          # Configuration Maven parent
└── README.md        # Ce fichier
```

## Technologies Utilisées

- **Java 21** : Langage principal
- **JavaFX 21** : Interface utilisateur
- **Maven** : Gestion des dépendances et build
- **JUnit 5** : Tests unitaires
- **JaCoCo** : Couverture de code
- **PlantUML** : Diagrammes d'architecture

## Contribution

1. Forkez le projet
2. Créez une branche pour votre fonctionnalité (`git checkout -b feature/AmazingFeature`)
3. Commitez vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Pushez vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

## Auteurs

- ANTOINE Charly - Développement initial
- DEGROUX Mathieu - Développement initial
- LE MESLE Martin - Développement initial
- MULLOIS Matthéo - Développement initial
- PARIS LEMPERRIERE Victor - Développement initial
- PICOT Solal - Développement initial
- POIRIER--DUBOIS Arthur - Développement initial
- SANTIER Thomas - Développement initial

## Remerciements

- Université de Caen Normandie
- Projet SAE 401
