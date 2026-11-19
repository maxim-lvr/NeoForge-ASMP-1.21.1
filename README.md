
# ASMP Things

Mod Minecraft **1.21.1**, NeoForge **21.1.226**, Java **21**.
Identifiant : `asmpthingsmod`. Les versions sont définies dans `gradle.properties`.

## Compiler et lancer

Configurer Java 21 dans `JAVA_HOME`, puis exécuter depuis la racine du projet :

```bash
./gradlew build
./gradlew runClient
```

Le JAR est produit dans `build/libs/`. Le client de développement utilise le
dossier `run/` pour les configurations, journaux et sauvegardes.

Le mod **Camera** est nécessaire pour les photos du CrazyPhone. Gradle récupère
la version de développement depuis Modrinth. Sophisticated Core et Sophisticated
Backpacks ne sont plus requis : la commande d'inspection `/asmpbackpack` et son
intégration ont été retirées.

## Dimension et structures

La dimension `asmpthingsmod:asmp_dimension` est déclarée dans
`src/main/resources/data/asmpthingsmod/dimension/asmp_dimension.json`.
Elle utilise le générateur plat de Minecraft : 1001 couches de métal renforcé,
de Y = 0 à Y = 1000 inclus, sans couche de bedrock. Minecraft impose des hauteurs
multiples de 16 : son type, dans `dimension_type/`, réserve donc 1008 couches
(jusqu’à Y = 1007), avec 7 couches d’air au sommet et une hauteur logique de 1001.
La nuit est fixe et il n’y a pas de lumière du ciel.
`AsmpThingsModClient` ajoute un brouillard sombre dans cette dimension.

Le bloc solide `asmpthingsmod:reinforced_metal_block` utilise la texture importée
`localimport/renforced_metal_block.jpg`. Il se casse en environ 60 secondes en
survie à 20 TPS, indépendamment de l’outil, des enchantements et des effets.
Il est disponible dans l’onglet ASMP ; le mode créatif conserve la casse instantanée.
Ce remplissage concerne les nouveaux chunks : les chunks déjà sauvegardés ne
sont pas convertis automatiquement. Le plan JSON creuse les espaces et place
ses structures dans ce terrain selon les coordonnées du fichier. Le changement
de hauteur de la dimension ne redimensionne pas automatiquement le plan JSON.

Le bloc `asmpthingsmod:deepslate_portal` envoie le joueur en `(0.5, 100, 0.5)`
dans cette dimension et dégage les deux blocs occupés par le joueur.
Depuis la dimension ASMP, il renvoie vers l'Overworld.

La carte construite est placée séparément, sur commande, par
`src/main/java/net/maximlvr/asmpthings/command/MapBuildPlacementCommand.java`.
Le placement lit le plan
`src/main/resources/data/asmpthingsmod/msmp/map_generation_final_seed42_1000x1000x1000.json`.
Ce plan de test commence en `(0, 0, 0)` et couvre un cube de 1000 blocs par axe,
soit des coordonnées de 0 à 999 inclus (borne de fin 1000 exclusive).
L'export actuel décrit directement les remplissages de blocs : il n'a pas besoin
de modèles NBT. Les anciens exports avec `name` restent pris en charge et utilisent
les modèles de `src/main/resources/data/asmpthingsmod/structure/`.

L’ancien plan `map_generation_final_seed42_10000x10000.json` est exclu de Git.
Le plan de test doit être présent à l’emplacement ci-dessus
avant la compilation, ou fourni via un datapack sous
`data/asmpthingsmod/msmp/map_generation_final_seed42_1000x1000x1000.json`.
Le client et la dimension peuvent se charger sans ce fichier, mais les commandes
de construction signaleront une ressource introuvable.

Les commandes nécessitent les permissions opérateur de niveau 2 :

| Commande | Comportement |
| --- | --- |
| `/asmpbuildzone start [budgetParTick]` | Prépare puis place tout le plan par chunks, avec une cadence cible de 10 chunks/s. |
| `/asmpbuildzone lazy start [rayonChunks] [budgetParTick]` | Place les zones autour des joueurs présents dans la dimension ASMP, rayon par défaut de 5 chunks. |
| `/asmpbuildzone chunks start <chunksParSeconde>` | Parcourt toute la carte par chunks, du nord au sud puis d'ouest en est. |
| `/asmpbuildzone status` | Affiche la progression de lecture/indexation ou de placement. |
| `/asmpbuildzone stop` | Annule aussi la préparation en arrière-plan ; conserve les blocs déjà placés. |

Le JSON est lu en flux sur un thread dédié, puis indexé sur disque dans
`<sauvegarde>/asmp_map_cache/plan-…/`. Ce cache temporaire est supprimé après
la fin, l'arrêt ou l'échec de la tâche. L'index n'expanse pas les remplissages
sur toute leur hauteur : seuls les fragments du chunk en cours sont parcourus.
Le fichier d'origine et ses métadonnées restent inchangés.

Chaque chunk reçoit les opérations dans l'ordre exact du JSON, y compris les
remplissages d'air et les états comme `minecraft:ladder[facing=north]`. Les
opérations chevauchant plusieurs chunks sont découpées aux frontières de chacun.
Un ancien placement NBT reste ancré une seule fois à sa position, avec sa rotation.

Le budget par défaut est de 16384 unités de travail par tick, avec une limite
de temps de pose de 8 ms et au plus un nouveau chunk par tick. Le chargement
Minecraft d'un chunk ou la pose d'un NBT reste une opération indivisible pouvant
dépasser cette durée. Les cadences demandées sont des cibles ; le plafond de
chargement est donc de 20 chunks/s à 20 TPS. Les sections uniformes sont remplies
par palette, en préservant leurs biomes. Les hauteurs, l'éclairage, la sauvegarde
et l'affichage sont finalisés par chunk.

La progression reste en mémoire et est réinitialisée à l'arrêt de la tâche ou du
serveur. En mode `lazy`, seuls les joueurs présents dans la dimension ASMP
déclenchent les placements. Pour un premier essai limité à proximité :

```mcfunction
/asmpbuildzone lazy start 2 2048
```

Vérifications :

```bash
./gradlew verifyMapPlan
./gradlew verifyMapPlanLarge
python3 tools/map/gametest-template.py
./gradlew runGameTestServer --init-script tools/map/gametest.gradle
```

`verifyMapPlan` est aussi exécuté par `build`. `verifyMapPlanLarge` indexe le
JSON réel avec un heap limité à 256 Mio et compare des chunks échantillons à une
seconde lecture du fichier. Les GameTests utilisent un monde temporaire et
vérifient les commandes non bloquantes, l'ordre des remplissages, les limites
de chunks, les échelles, l'eau, les biomes, les hauteurs et les entités de blocs.

## Organisation du code

Le code Java se trouve dans `src/main/java/net/maximlvr/asmpthings/` :

- `AsmpThingsMod` : enregistrement des blocs, objets, entités, réseau et configuration.
- `world/`, `block/custom/DeepslatePortalBlock`, `command/MapBuildPlacementCommand` : dimension, portail et construction de la carte.
- `entity/` et `client/` : Undertaker, golem, drone, boss Armi et Cube Boss, animations et rendus.
- `bank/`, `block/` et `network/` : comptes bancaires persistants, distributeurs, cartes et lecteurs avec code PIN.
- `item/`, `integration/camera/` et `client/screen/` : armes, bâton météo, CrazyPhone, messages, photos et tickets à gratter.
- `ai/` : dialogues et mémoires des PNJ via DeepSeek, désactivés par défaut dans `Config`.
- `ai/voice/`, `client/voice/` et `network/PnjVoiceNetworking` : POC de conversation vocale GPT Realtime.
- `command/AsmpTestCommand` et `mixin/` : prototype de caméra/contrôle entre joueurs et adaptations du comportement Minecraft.
- `command/ItemLocationDebugCommand` : recherche administrative d'objets dans les fichiers de sauvegarde, sans dépendance Java à Sophisticated Backpacks.

Les textures, modèles et traductions sont dans `src/main/resources/assets/asmpthingsmod/`.
`localimport/` contient les sources de certains modèles et textures ; `tools/`
contient des scripts d'import et un dispositif de GameTests pour l'Undertaker.

## Fenêtre sur le ciel

Le bloc décoratif `asmpthingsmod:void_window`, nommé « Fenêtre sur le ciel »,
affiche le ciel du monde sur les six faces d'un cube solide : couleur du ciel,
soleil, lune et étoiles selon l'heure et la météo. Le rendu du ciel est capturé
avant le terrain, puis projeté sur le bloc ; les nuages ne sont pas inclus.
Dans l'Overworld, il suit donc son cycle jour/nuit ; dans la dimension ASMP,
il suit sa nuit fixe. L'identifiant existant est conservé pour les blocs déjà posés.
Plusieurs blocs juxtaposés permettent de faire une grande fenêtre. Il n'a ni
téléportation ni animation côté serveur. Il est disponible dans l'onglet créatif ASMP :

```mcfunction
/give @s asmpthingsmod:void_window
```

Le second bloc `asmpthingsmod:mountain_window`, « Fenêtre sur les montagnes »,
ajoute trois plans de décor à ce ciel : sommets enneigés lointains, reliefs
intermédiaires et collines vertes proches. Le style cartoon utilise une palette
vive, des ombres en aplats et des contours foncés, lisibles même de nuit.
Un quatrième plan de terre marron forme le sol au premier plan, au niveau des
pieds du joueur, quelle que soit l'altitude du cube. La parallaxe reste horizontale ;
le décor vertical ne dépend pas de la première altitude visitée. Le sol reste
visuel et n'ajoute pas de terrain physique.
Leur parallaxe dépend du déplacement
de la caméra ; les blocs juxtaposés partagent le même panorama. Le paysage est
calculé par shader, sans terrain supplémentaire ni textures à charger.

```mcfunction
/give @s asmpthingsmod:mountain_window
```

## POC vocal GPT Realtime

La commande `/pnj on` sélectionne le PNJ renommé que le joueur regarde à moins de
12 blocs, ou le plus proche à moins de 6 blocs s'il n'en vise aucun. Les joueurs
et les entités derrière un obstacle sont exclus. Une nouvelle personnalité est
créée automatiquement pour un nom inconnu ; un PNJ explicitement désactivé dans
le fichier de personnalités reste désactivé.

Le micro système s'ouvre après connexion à OpenAI. Un indicateur « Micro actif »
et une jauge de niveau apparaissent en haut à gauche. Le joueur parle sans `@`
et entend la réponse en continu. La détection de fin de parole est automatique.
Reparler interrompt la réponse et retire de la conversation sa partie non entendue.
Le POC diffuse le son uniquement au joueur qui parle avec le PNJ.

`/pnj off` ferme le micro, la lecture audio et la connexion. La conversation se
termine aussi en cas de déconnexion, de mort, de changement de dimension, de
renommage du PNJ ou d'éloignement à plus de 16 blocs. Par défaut, elle dure au
maximum 10 minutes, avec deux sessions simultanées et une session par PNJ.
La commande d'activation est réservée aux opérateurs par défaut.

### Configuration

Le fichier `config/asmpthingsmod-voice.toml` est créé au lancement, dans `run/config/`
en développement. La clé est lue côté serveur depuis `OPENAI_API_KEY` ou, à défaut,
depuis `realtime.api_key`. Elle n'est pas envoyée aux clients. Le modèle par défaut
est `gpt-realtime`, la voix `marin`. Les appels nécessitent un accès et des crédits API OpenAI.

```toml
[realtime]
enabled = true
api_key = "VOTRE_CLE_OPENAI"
model = "gpt-realtime"
voice = "marin"
operator_only = true
```

Le fichier client `config/asmpthingsmod-voice-client.toml` permet de choisir un
`microphone`, une sortie `speaker` et un `volume`. Un nom vide utilise le
périphérique système par défaut. Les noms des micros OpenAL sont écrits dans le
journal lors de l'activation. Utiliser un casque évite que le micro reprenne la
voix du PNJ ; le POC n'applique pas d'annulation d'écho locale.

Le client utilise OpenAL, déjà fourni par Minecraft, pour capturer et lire du
PCM16 mono à 24 kHz. Un contexte audio propre au thread vocal préserve les sons
du jeu. Le serveur relaie l'audio vers OpenAI en WebSocket, sans bloquer ses ticks.
Les paquets, files audio et débits sont bornés. Le micro ne s'ouvre jamais au
démarrage du jeu, seulement après `/pnj on`.

### Souvenirs et test rapide

Le chat DeepSeek avec `@nom` reste disponible si son ancienne configuration est
activée. Les modes texte et vocal partagent les mêmes caches et fichiers :

- `asmpthings_ai_personality_persistent.json` : personnalité et connaissances fixes par nom de PNJ ;
- `asmpthings_ai_player_memory.json` : profil, souvenirs et relation par nom de PNJ et UUID de joueur ;
- `asmpthings_ai_general_memory.json` : informations générales et alias par nom de PNJ.

Après la réponse orale, une analyse textuelle Realtime distincte met à jour les
souvenirs en arrière-plan, puis les rend disponibles aux tours suivants et aux
sessions futures. Attendre la fin de la réponse avant de quitter ; un arrêt
pendant l'analyse peut interrompre la dernière mise à jour. Deux PNJ portant
exactement le même nom partagent leur personnalité et leurs souvenirs persistants.

1. Relancer le client pour charger les modifications Java et rejoindre un monde avec les commandes activées.
2. Créer un villageois nommé :

   ```mcfunction
   /summon minecraft:villager ~ ~ ~ {CustomName:'{"text":"Marcel"}',PersistenceRequired:1b,NoAI:1b}
   ```

3. Regarder Marcel, exécuter `/pnj on`, attendre « Micro actif », puis parler.
4. Lui donner une information, laisser terminer sa réponse, puis faire `/pnj off`.
5. Refaire `/pnj on` et demander cette information pour vérifier sa mémoire.

Vérifications automatiques, sans réseau ni micro : `./gradlew verifyPnjVoice`
(également exécutées par `build`). Elles couvrent le PCM, les interruptions,
les fragments WebSocket, la fermeture, la limite de file et la persistance des
profils après rechargement, avec isolation entre joueurs et PNJ. Les réponses
mémoire JSON et XML sont couvertes, ainsi que les tours sans information nouvelle.

Le test API est séparé et payant :
`./gradlew verifyPnjVoiceLive -PasmpVoiceKeyFile=/chemin/vers/fichier-cle`.
Il utilise de la parole synthétique, vérifie la réponse audio et la mémoire après
reconnexion ; il n'ouvre pas le micro. Le fichier-clé doit contenir uniquement la clé.

Références : [conversations Realtime](https://developers.openai.com/api/docs/guides/realtime-conversations),
[connexion WebSocket](https://developers.openai.com/api/docs/guides/voice-websockets).

## Pets volants

Deux objets sont disponibles dans l’onglet ASMP : `asmpthingsmod:pet_1` et
`asmpthingsmod:pet_2`. Équiper un seul pet dans le slot dédié du menu du pet
fait apparaître son compagnon. Il vole près du joueur, légèrement
derrière et au-dessus, à 60 % de la taille du modèle importé. Il est invulnérable
et traverse les obstacles pour suivre le joueur. Le pet équipé est sauvegardé
et conservé après la mort ; le compagnon est recréé à la connexion et au changement
de dimension.

La touche **P**, modifiable dans Options → Contrôles → ASMP, ouvre le menu
d’équipement et affiche les bonus en points entiers (+2 en vie = un cœur).
Le slot du pet apparaît uniquement dans ce menu, avec l’inventaire du joueur
et sans slots d’armure. Reposer l’objet tenu au curseur avant d’ouvrir le menu.

Pour le moment, les deux variantes utilisent `localimport/pet_msmp.java` et
`texture_pet_1.png`, avec une teinte bleutée pour la seconde. Les noms sont
provisoires : Pet 1 donne +1 cœur maximum, Pet 2 donne +2 points d’armure.
Les bonus sont réglables côté serveur dans
`<monde>/serverconfig/asmpthingsmod-pets.toml` et se retirent quand le pet est
déséquipé. Les variantes sont définies dans `pet/PetKind.java`, leurs objets dans
`item/ModItems.java` et leurs réglages dans `pet/PetConfig.java`.

## Ressources NeoForge

Le projet utilise les noms officiels Mojang, soumis à leur
[licence de mappings](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md).
Documentation : https://docs.neoforged.net/
