# FamilyFit

Fitness familial : site central Blazor .NET 10, API ChannelMediator, SQLite et applications natives Android/iPhone. Production prévue sur `https://fit.chouteau.info`.

## Organisation

- `src/FamilyFit.Contracts` : modèles et requêtes ChannelMediator.
- `src/FamilyFit.Core` : handlers par domaine, validation, SQLite/EF Core, migrations et authentification.
- `src/Web` : interface Blazor, connexion personnelle par QR/code, endpoints mobiles et serveur MCP.
- `src/Android` : application Kotlin/Compose existante, Room, scan QR et synchronisation.
- `src/iOS` : application SwiftUI/SwiftData et projet Xcode partagé.
- `tests/FamilyFit.Tests` : tests d’intégration avec de vraies bases SQLite isolées.

## Démarrer le site localement

Prérequis : SDK .NET 10.0.403 ou correctif compatible. La solution fixe cette version majeure avec `global.json`.

```powershell
dotnet restore FamilyFit.sln
dotnet test FamilyFit.sln
dotnet run --project src/Web --no-launch-profile --urls http://localhost:5088
```

Sans session ni lien personnel valide, le site reste vide, sauf pendant l’initialisation administrateur configurée ci-dessous. La base est créée/migrée au premier démarrage dans `src/Web/data` ; ce dossier est exclu de Git.

Pour explorer l’interface avec des données fictives, utiliser uniquement l’environnement Development :

```powershell
$env:ASPNETCORE_ENVIRONMENT='Development'
$env:FamilyFit__Demo='true'
$env:FamilyFit__PublicUrl='http://localhost:5088'
dotnet run --project src/Web --no-launch-profile --urls http://localhost:5088
```

Ouvrir `/demo`. Ce mode emploie `familyfit-demo.db`, distinct de la base réelle ; son activation est refusée en production. Retirer ces variables pour revenir au fonctionnement normal. Aucun historique réel n’est créé par défaut.

## Connexion sans mot de passe

L’administrateur crée les profils puis leurs invitations personnelles (QR ou code, dix minutes, usage unique). Chaque téléphone confirme le profil puis conserve son autorisation dans son stockage sécurisé. Dans « Membres », l’administrateur choisit « Créer un lien web » pour le membre concerné et lui transmet ce lien privé. Il expire cinq minutes après sa création et affiche uniquement la connexion liée à son pseudo. Seul un téléphone activé sur ce profil peut confirmer le QR/code. Après connexion, le suivi familial reste accessible.

Le secret est dans le fragment `#secret=…` de l’URL, retiré de la barre d’adresse dès sa lecture ; il n’est pas envoyé dans les journaux HTTP du proxy. Le lien est lié au premier navigateur qui demande son code, puis consommé à la connexion. « Nouveau code » conserve la date d’expiration initiale. Les sessions web expirent après douze heures ; révoquer le téléphone invalide aussi ses sessions web.

### Première connexion administrateur avec le `.env`

Avant le premier démarrage en production, ajouter au `.env` situé à côté du Docker Compose un code aléatoire dédié (16 à 128 caractères), distinct de la clé MCP :

```dotenv
FAMILYFIT_BOOTSTRAP_CODE=<code-aléatoire-d-initialisation>
MCP_API_KEY=<clé-dédiée-à-l-agent>
MCP_READ_ONLY=false
```

Générer le code avec `openssl rand -hex 16`. Protéger le `.env` avec des droits 600 et ne pas le versionner. Les trois Compose transmettent `FAMILYFIT_BOOTSTRAP_CODE` au conteneur.

1. Démarrer le service, puis ouvrir `https://fit.chouteau.info`.
2. Saisir le code d’initialisation sur le site et renseigner le pseudo, l’âge et le poids du premier administrateur. Si son profil a déjà été créé par l’opérateur, seul le code est demandé.
3. La session administrateur s’ouvre immédiatement, sans téléphone préalablement activé. Dans « Membres », utiliser « Inviter son mobile » pour activer son téléphone et préparer les liens web de la famille.
4. Retirer `FAMILYFIT_BOOTSTRAP_CODE` du `.env`, puis recréer le service avec `docker compose -f compose.traefik.yaml up -d --no-build`.

Le code est consommé une seule fois, avec un état persistant en SQLite. Changer le code ou redémarrer le conteneur ne permet pas de refaire l’initialisation. Une installation ayant déjà créé une session web ne peut pas utiliser ce mécanisme. Sans code valide configuré, aucun formulaire d’initialisation n’est affiché. La première session dure douze heures ; les connexions suivantes utilisent un lien personnel et le téléphone activé.

### Commandes de récupération

Pour une installation existante sans session web active, générer un lien de connexion depuis le serveur :

```powershell
dotnet run --project src/Web --no-launch-profile -- --login-link --nickname Marc
```

La commande indique le chemin d’un fichier HTML privé, à récupérer localement et ouvrir dans les cinq minutes, puis confirmer depuis le téléphone du même membre. `--member-id <GUID>` permet de sélectionner précisément un membre si plusieurs pseudos sont identiques. Ne pas publier ce fichier ni le versionner ; supprimer sa copie après usage. Les connexions courantes se préparent depuis « Membres ».

Si le téléphone administrateur est perdu, `--recover` révoque ses anciens accès et prépare une nouvelle invitation mobile. L’ancienne commande opérateur `--bootstrap` reste disponible pour créer un administrateur hors du parcours web :

```powershell
dotnet run --project src/Web --no-launch-profile -- --bootstrap --nickname Marc --age 58 --weight 68
```

Adapter les valeurs à la réalité et récupérer l’invitation privée indiquée pour activer le mobile. Aucun mot de passe de secours permanent n’est créé.

## Programmes et fonctionnement hors ligne

Ajouter les lieux et leur matériel confirmé, compléter les objectifs/niveau/disponibilités puis composer un programme manuellement ou avec un agent externe via MCP. Seul l’administrateur publie. Les programmes publiés sont immuables et les anciennes versions restent disponibles pour recevoir leurs résultats hors ligne. La génération IA intégrée au site a été retirée ; aucune clé OpenAI n’est nécessaire côté serveur. Les réglages vocaux Android existants sont conservés.

## Connecter un agent par MCP

Le serveur Streamable HTTP est disponible sur `https://fit.chouteau.info/mcp`. Placer la clé dédiée dans le `.env` situé à côté du Docker Compose :

```dotenv
MCP_API_KEY=<clé-aléatoire-d-au-moins-32-caractères>
MCP_READ_ONLY=false
```

Générer une valeur avec `openssl rand -hex 32`, protéger le `.env` avec des droits 600 et ne pas le versionner. Les trois variantes Compose transmettent ces variables au conteneur. Sans clé, l’accès MCP est désactivé. `MCP_READ_ONLY=true` interdit toutes les modifications. La clé correspond aux droits métier de l’unique administrateur existant ; elle ne peut ni inviter un téléphone ni approuver une connexion web. Après un changement de clé ou de mode, recréer le conteneur. La rotation de la clé révoque l’ancien accès.

Sur la machine de l’agent, rendre la même valeur disponible dans la variable d’environnement `MCP_API_KEY`, puis configurer Codex :

```toml
[mcp_servers.familyfit]
url = "https://fit.chouteau.info/mcp"
bearer_token_env_var = "MCP_API_KEY"
```

Ou utiliser `codex mcp add familyfit --url https://fit.chouteau.info/mcp --bearer-token-env-var MCP_API_KEY`. Relancer Codex après modification de son environnement. La valeur du secret reste hors du fichier TOML. Voir la [configuration MCP officielle de Codex](https://learn.chatgpt.com/docs/extend/mcp?surface=cli).

Les 18 outils couvrent identité, guide, tableau familial, profils, poids, lieux/matériel, exercices, planning, programmes et résultats. Les écritures passent par ChannelMediator et les mêmes validations que le site. La préparation d’un brouillon ne publie pas automatiquement de programme ; l’agent doit recevoir une demande explicite de publication et ne doit jamais inventer une séance réalisée. Catalogue et limites : [docs/api.md](docs/api.md).

Les mobiles téléchargent programmes et médias, enregistrent la progression localement et conservent les résultats jusqu’à acquittement du serveur. Les synchronisations se font à l’ouverture, après une séance, manuellement et au retour du réseau lorsque l’application est active. Aucune exécution en arrière-plan app fermée n’est garantie. Le suivi distingue terminé, partiel, abandonné et repos ; les jours de repos sont exclus de l’objectif du jour. Les dates de planning suivent Europe/Paris.

Un résultat refusé définitivement reste conservé avec son erreur dans « Résultats à vérifier », sans bloquer les autres résultats ni le téléchargement du planning. Le bouton de réessai renvoie ce résultat sans le modifier. Les erreurs d’autorisation nécessitent une réactivation ; une erreur d’horaires ou de programme doit être examinée avec l’administrateur. Les données locales ne sont jamais effacées automatiquement pour résoudre un rejet.

## Android

```powershell
cd src/Android
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

APK : `app/build/outputs/apk/debug/app-debug.apk`. Identifiant conservé : `com.example.familiyfit`, version 2.0/code 2. Utiliser la même signature que l’APK déjà installé pour une mise à jour sans suppression des données. Le coach local reste accessible, mais seules les séances d’un programme familial publié alimentent le site. Les anciens compteurs ne sont pas transformés en faux résultats.

Les tests de persistance Room dans `androidTest` nécessitent un émulateur ou un appareil Android. Le projet iPhone et ses commandes Xcode sont documentés dans `src/iOS/README.md`.

## Docker et exploitation

### Installer derrière Traefik

Utiliser [compose.traefik.yaml](compose.traefik.yaml) avec le Traefik existant. Le fichier récupère l’image `ghcr.io/chouteau/familyfit`, route `fit.chouteau.info` en HTTPS vers le port interne 8080 et redirige HTTP vers HTTPS. Aucun port du service n’est publié sur l’hôte. Traefik doit utiliser son fournisseur Docker et partager un réseau avec FamilyFit ; le DNS du domaine doit pointer vers le serveur.

Placer le Compose et [deploy/traefik.variables.example](deploy/traefik.variables.example) sur le serveur, dans un dossier dédié, puis préparer les variables :

```sh
umask 077
# Première installation : conserver un éventuel .env existant.
test -e .env || cp deploy/traefik.variables.example .env
chmod 600 .env
# Compléter .env avec les valeurs de votre installation Traefik.
```

| Variable | Valeur à renseigner |
|---|---|
| `TRAEFIK_NETWORK` | Nom exact du réseau Docker existant partagé avec Traefik |
| `TRAEFIK_CERTRESOLVER` | Nom du résolveur TLS déjà configuré dans Traefik |
| `TRAEFIK_PROXY_IP` | Adresse IP de Traefik sur ce réseau partagé, de préférence fixe |
| `TRAEFIK_HTTP_ENTRYPOINT` | Point d’entrée HTTP ; défaut `web` |
| `TRAEFIK_HTTPS_ENTRYPOINT` | Point d’entrée HTTPS ; défaut `websecure` |
| `FAMILYFIT_IMAGE_TAG` | Défaut `latest` ; utiliser un tag `sha-<commit complet>` pour figer une version |
| `MCP_API_KEY` | Clé dédiée à l’agent ; vide désactive MCP |
| `MCP_READ_ONLY` | `true` pour limiter le MCP à la lecture ; défaut `false` |
| `FAMILYFIT_BOOTSTRAP_CODE` | Code à usage unique pour la première connexion administrateur ; retirer après initialisation |

Le modèle laisse les trois premières valeurs vides : Compose demande de les renseigner avant de démarrer. Pour identifier le réseau et l’adresse du conteneur Traefik, utiliser `docker inspect NOM_DU_CONTENEUR_TRAEFIK --format '{{json .NetworkSettings.Networks}}'`. `TRAEFIK_PROXY_IP` permet à ASP.NET de reconnaître les en-têtes HTTPS du bon proxy ; si cette IP change, adapter la variable et recréer le conteneur FamilyFit.

```sh
docker compose -f compose.traefik.yaml config --quiet
docker compose -f compose.traefik.yaml pull
docker compose -f compose.traefik.yaml up -d --no-build
docker compose -f compose.traefik.yaml ps
```

Ouvrir ensuite `https://fit.chouteau.info`. Le volume `familyfit_familyfit-data` conserve SQLite, les clés de session et les sauvegardes ; ne pas le supprimer lors des mises à jour et garder le nom de projet `familyfit`. La configuration est prévue pour une seule instance web.

Pour la première installation, renseigner `FAMILYFIT_BOOTSTRAP_CODE` dans `.env` **avant** le démarrage, puis suivre la section « Première connexion administrateur » : saisir le code directement sur le site. Retirer ensuite la variable et recréer le conteneur. Aucun transfert de fichier par SSH n’est nécessaire pour cette initialisation.

Sur une installation déjà utilisée, si aucune session web n’est encore active, récupérer un lien personnel par la commande opérateur :

```sh
docker compose -f compose.traefik.yaml exec -T web dotnet FamilyFit.Web.dll --login-link --nickname Marc
docker compose -f compose.traefik.yaml cp web:/data/private ./private-invitation
```

Ouvrir ce lien dans les cinq minutes et confirmer depuis le téléphone du même membre. Supprimer les copies privées après usage. Le site public reste vide sans lien ni session.

Pour utiliser `deploy/backup.sh` avec cette variante : définir `FAMILYFIT_COMPOSE_FILE=compose.traefik.yaml`. Pour le timer fourni, adapter aussi son entrée `Environment=FAMILYFIT_COMPOSE_FILE=compose.traefik.yaml` dans le service systemd. Le `.env` et le tag d’image doivent rester dans le dossier du projet. Sauvegarder avant chaque changement de version.

Les labels suivent la [configuration Docker officielle de Traefik](https://doc.traefik.io/traefik/reference/routing-configuration/other-providers/docker/). Le résolveur TLS et le réseau restent ceux de votre proxy existant.

Le workflow GitHub valide le web et son conteneur, puis publie l’image `ghcr.io/chouteau/familyfit` depuis `main` ou un tag de version. `compose.registry.yaml` permet de consommer une version publiée sans compilation sur le VPS. Le job iPhone compile et teste séparément sur un simulateur macOS, sans signature. Voir `docs/deployment.md` pour les tags, droits GHCR et étapes de mise en service ; la publication d’une image ne déploie pas automatiquement le VPS.

Voir `docs/deployment.md`. Les données SQLite, journaux WAL et clés de session sont dans un volume persistant. Les médias du catalogue d’origine sont inclus dans l’image ; le volume réserve aussi un répertoire aux futurs médias. Une sauvegarde utilise l’API SQLite Online Backup, jamais une simple copie à chaud du fichier principal.

Le contrat partagé est décrit dans `docs/api.md`. Les images générées et la licence de police sont décrites dans `docs/assets.md`.

Les validations réalisées et celles restant à effectuer sont consignées dans `docs/verification.md`.
