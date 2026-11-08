# Déploiement FamilyFit sur VPS

## Livraison par GitHub

Le workflow `.github/workflows/ci.yml` vérifie les tests .NET/SQLite et l’image de production sur chaque pull request. Il contrôle `/health`, le refus des données familiales anonymes et l’absence de `/demo`. Un job macOS indépendant compile l’application iPhone et exécute ses XCTest sur un simulateur disponible, sans certificat de signature.

Après les contrôles web, les mises à jour de `main` publient `ghcr.io/chouteau/familyfit:latest` et un tag `sha-<commit complet>`. Un tag Git `v2.0.0`, par exemple, produit le tag Docker `2.0.0`. Le déclenchement manuel publie uniquement sur `main` ou un tag de version. Une pull request ne publie rien. Les actions sont fixées à leurs SHA vérifiés ; la connexion GHCR utilise le jeton temporaire GitHub avec `packages:write` limité au job de publication, sans secret personnel supplémentaire.

Pour une installation utilisant l’image GitHub, conserver `compose.registry.yaml` sur le VPS et choisir un tag explicite publié :

```sh
export FAMILYFIT_IMAGE_TAG=sha-<commit-complet-publie>
docker compose -f compose.registry.yaml config --quiet
docker compose -f compose.registry.yaml pull
# Sur une installation existante : sauvegarder avant tout remplacement.
docker compose -f compose.registry.yaml up -d --no-build
docker compose -f compose.registry.yaml ps
```

Ce fichier conserve le nom de projet `familyfit` et le volume `familyfit_familyfit-data`. Il ne construit pas sur le VPS et refuse de démarrer sans tag configuré. Ne pas changer le nom de projet ni supprimer le volume lors d’une mise à jour. Garder l’ancien tag pour le retour arrière et tenir compte des migrations lors d’une restauration.

La visibilité d’un nouveau package GHCR doit être vérifiée dans les paramètres GitHub : un dépôt public ne garantit pas que le package est public. Pour une image privée, le compte Docker du VPS doit disposer d’un accès de lecture au package ; injecter son autorisation par le gestionnaire de secrets de l’installation et ne pas l’inscrire dans Compose.

La publication GHCR prépare l’image ; elle ne remplace aucun conteneur du VPS. Le raccordement à l’outil de déploiement choisi par l’utilisateur reste à configurer. Le DNS, HTTPS, les sauvegardes et le premier administrateur restent nécessaires.

Références : [publication d’images avec GitHub Actions](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images), [permissions et actions fixées à un commit](https://docs.github.com/en/actions/reference/security/secure-use).

## Préparation

Utiliser un VPS Linux avec Docker/Compose, le dépôt dans un dossier dédié et un reverse proxy HTTPS. Le domaine `fit.chouteau.info` doit pointer vers ce VPS et son certificat doit être configuré. Ne pas remplacer une configuration de proxy existante sans l’avoir examinée.

Le conteneur expose uniquement `127.0.0.1:5088`, utilise la base .NET `noble-chiseled-extra` (ICU et fuseaux horaires inclus), exécute l’application sans privilèges, avec système de fichiers en lecture seule et volume persistant `familyfit_familyfit-data`. Cette base n’inclut pas de shell : les commandes d’exploitation utilisent directement `dotnet`. Vérifier l’absence de conflit de port et de réseau : le réseau dédié par défaut est `172.30.84.0/24`, passerelle `172.30.84.1`.

Variables d’exploitation :

| Variable | Usage |
|---|---|
| `FAMILYFIT_PUBLIC_URL` | Défaut `https://fit.chouteau.info`, doit correspondre au serveur configuré dans les mobiles |
| `FAMILYFIT_PORT` | Port local du reverse proxy, défaut 5088 |
| `FAMILYFIT_IMAGE_TAG` | Version d’image à conserver pour permettre un retour arrière |
| `FAMILYFIT_PROXY_IP` | Adresse source exacte du proxy vue par le conteneur, défaut passerelle 172.30.84.1 |
| `FAMILYFIT_DOCKER_SUBNET` / `FAMILYFIT_DOCKER_GATEWAY` | Réseau à adapter en cas de conflit, en cohérence avec l’IP du proxy |
| `OPENAI_API_KEY` | Secret optionnel, injecté uniquement dans l’environnement du serveur |
| `OPENAI_MODEL` | Modèle de génération, défaut gpt-4.1-mini |

Protéger tout fichier d’environnement local avec des droits 600 et ne pas l’ajouter à Git. Éviter `docker compose config` sans `--quiet` lorsqu’un secret est configuré : cette commande peut afficher l’environnement résolu.

## Première mise en service

```sh
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps
curl --fail http://127.0.0.1:5088/health
```

Adapter le modèle `deploy/nginx.conf.example` au proxy déjà en place. Tester sa configuration avant rechargement. Les en-têtes Host, X-Forwarded-Proto et WebSocket sont nécessaires. L’adresse du proxy doit être explicitement reconnue par l’application ; ne pas faire confiance à toutes les adresses entrantes.

Après mise en place de HTTPS :

```sh
docker compose exec -T web dotnet FamilyFit.Web.dll --bootstrap --nickname Marc --age 58 --weight 68
docker compose cp web:/data/private ./private-invitation
```

Adapter âge/poids à la réalité. Récupérer l’invitation localement par SSH, activer le mobile administrateur et supprimer la copie locale après usage. Ce répertoire n’est jamais servi par HTTP. Ajouter ensuite les membres et leurs lieux depuis le site, puis publier leurs programmes.

## Sauvegardes et mises à jour

`deploy/backup.sh` crée une sauvegarde SQLite cohérente et exporte les clés de session et médias persistants. Adapter `FAMILYFIT_ROOT` et `FAMILYFIT_BACKUP_ROOT`. Répliquer les sauvegardes dans le système de sauvegarde externe existant ; une copie sur le même VPS ne protège pas de sa perte.

Les unités systemd fournies permettent une exécution quotidienne à 03:15 Europe/Paris. Adapter les chemins, installer les deux unités, puis activer le timer :

```sh
sudo systemctl daemon-reload
sudo systemctl enable --now familyfit-backup.timer
```

Avant une nouvelle version : exécuter le script de sauvegarde avec l’ancienne image encore en service, conserver son tag, construire la nouvelle image puis remplacer le conteneur. Les migrations EF Core s’appliquent au démarrage d’une seule instance. Une sauvegarde `--backup` ne déclenche pas de migration ni de modification du catalogue.

Ne jamais utiliser `docker compose down -v` sur l’installation réelle : cette option supprime le volume de données.

## Restauration

Arrêter le service web, conserver une copie de l’état en place puis restaurer `familyfit.db`, `keys` et `media` dans le volume avec les droits de l’utilisateur du conteneur. Restaurer à froid et écarter les éventuels fichiers `familyfit.db-wal`/`familyfit.db-shm` de l’ancienne base avant remplacement ; ne pas mélanger des fichiers de deux sauvegardes. Repartir de l’image correspondant à la sauvegarde, vérifier `/health`, la connexion mobile, les programmes et un résultat connu avant de rouvrir l’accès.

## Validation réelle

Vérifier une activation Android et iPhone, une connexion navigateur par QR et code, une séance hors ligne puis une seule réception sur le site, la révocation d’un appareil, les WebSockets après passage par le proxy et la restauration d’une sauvegarde. La distribution iPhone Ad Hoc nécessite le compte Apple Developer et l’enregistrement des appareils.
