# FamilyFit

Fitness familial : site central Blazor .NET 10, API ChannelMediator, SQLite et applications natives Android/iPhone. Production prévue sur `https://fit.chouteau.info`.

## Organisation

- `src/FamilyFit.Contracts` : modèles et requêtes ChannelMediator.
- `src/FamilyFit.Core` : handlers par domaine, validation, SQLite/EF Core, migrations et préparation IA.
- `src/Web` : interface Blazor, connexion par QR/code et endpoints mobiles.
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

Sans invitation et mobile activé, le site affiche uniquement la connexion. La base est créée/migrée au premier démarrage dans `src/Web/data` ; ce dossier est exclu de Git.

Pour explorer l’interface avec des données fictives, utiliser uniquement l’environnement Development :

```powershell
$env:ASPNETCORE_ENVIRONMENT='Development'
$env:FamilyFit__Demo='true'
$env:FamilyFit__PublicUrl='http://localhost:5088'
dotnet run --project src/Web --no-launch-profile --urls http://localhost:5088
```

Ouvrir `/demo`. Ce mode emploie `familyfit-demo.db`, distinct de la base réelle ; son activation est refusée en production. Retirer ces variables pour revenir au fonctionnement normal. Aucun historique réel n’est créé par défaut.

## Connexion sans mot de passe

L’administrateur crée les profils puis leurs invitations personnelles (QR ou code, dix minutes, usage unique). Chaque téléphone confirme le profil puis conserve son autorisation dans son stockage sécurisé. Pour ouvrir le site, scanner son QR ou saisir son code dans l’application et approuver le navigateur. Les sessions web expirent après douze heures ; révoquer le téléphone invalide aussi ses sessions web.

Créer le premier administrateur sur la machine du serveur, avec tes valeurs réelles :

```powershell
dotnet run --project src/Web --no-launch-profile -- --bootstrap --nickname Marc --age 58 --weight 68
```

La commande indique seulement le chemin d’un fichier HTML privé. Récupérer ce fichier par SSH si nécessaire et ouvrir localement son QR/code. Il ne doit jamais être publié par le serveur web ni ajouté à Git. L’original est supprimé après activation ; supprimer également sa copie locale. La commande `--recover` révoque les anciens accès administrateurs et prépare une nouvelle invitation si le téléphone est perdu. Aucun mot de passe de secours n’est créé.

## Programmes et fonctionnement hors ligne

Ajouter les lieux et leur matériel confirmé, compléter les objectifs/niveau/disponibilités puis composer un programme. La proposition IA est optionnelle et reste un brouillon à relire ; seul l’administrateur publie. Les programmes publiés sont immuables et les anciennes versions restent disponibles pour recevoir leurs résultats hors ligne.

Configurer le service IA uniquement côté serveur avec `OpenAI__ApiKey` et, si nécessaire, `OpenAI__Model` (défaut : `gpt-4.1-mini`). Ne pas mettre la clé dans les sources, les mobiles ou un fichier versionné. Sans clé, la composition manuelle et les entraînements restent disponibles.

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
cp deploy/traefik.variables.example .env
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

Le modèle laisse les trois premières valeurs vides : Compose demande de les renseigner avant de démarrer. Pour identifier le réseau et l’adresse du conteneur Traefik, utiliser `docker inspect NOM_DU_CONTENEUR_TRAEFIK --format '{{json .NetworkSettings.Networks}}'`. `TRAEFIK_PROXY_IP` permet à ASP.NET de reconnaître les en-têtes HTTPS du bon proxy ; si cette IP change, adapter la variable et recréer le conteneur FamilyFit.

```sh
docker compose -f compose.traefik.yaml config --quiet
docker compose -f compose.traefik.yaml pull
docker compose -f compose.traefik.yaml up -d --no-build
docker compose -f compose.traefik.yaml ps
```

Ouvrir ensuite `https://fit.chouteau.info`. Le volume `familyfit_familyfit-data` conserve SQLite, les clés de session et les sauvegardes ; ne pas le supprimer lors des mises à jour et garder le nom de projet `familyfit`. La configuration est prévue pour une seule instance web.

Pour le premier administrateur, adapter les valeurs à la réalité :

```sh
docker compose -f compose.traefik.yaml exec -T web dotnet FamilyFit.Web.dll --bootstrap --nickname Marc --age 58 --weight 68
docker compose -f compose.traefik.yaml cp web:/data/private ./private-invitation
```

Récupérer le fichier privé localement, scanner son QR/code avec l’application mobile puis supprimer la copie après activation. Le site s’ouvre ensuite par approbation depuis le téléphone, sans mot de passe.

Pour utiliser `deploy/backup.sh` avec cette variante : définir `FAMILYFIT_COMPOSE_FILE=compose.traefik.yaml`. Pour le timer fourni, adapter aussi son entrée `Environment=FAMILYFIT_COMPOSE_FILE=compose.traefik.yaml` dans le service systemd. Le `.env` et le tag d’image doivent rester dans le dossier du projet. Sauvegarder avant chaque changement de version.

Les labels suivent la [configuration Docker officielle de Traefik](https://doc.traefik.io/traefik/reference/routing-configuration/other-providers/docker/). Le résolveur TLS et le réseau restent ceux de votre proxy existant.

Le workflow GitHub valide le web et son conteneur, puis publie l’image `ghcr.io/chouteau/familyfit` depuis `main` ou un tag de version. `compose.registry.yaml` permet de consommer une version publiée sans compilation sur le VPS. Le job iPhone compile et teste séparément sur un simulateur macOS, sans signature. Voir `docs/deployment.md` pour les tags, droits GHCR et étapes de mise en service ; la publication d’une image ne déploie pas automatiquement le VPS.

Voir `docs/deployment.md`. Les données SQLite, journaux WAL et clés de session sont dans un volume persistant. Les médias du catalogue d’origine sont inclus dans l’image ; le volume réserve aussi un répertoire aux futurs médias. Une sauvegarde utilise l’API SQLite Online Backup, jamais une simple copie à chaud du fichier principal.

Le contrat partagé est décrit dans `docs/api.md`. Les images générées et la licence de police sont décrites dans `docs/assets.md`.

Les validations réalisées et celles restant à effectuer sont consignées dans `docs/verification.md`.
