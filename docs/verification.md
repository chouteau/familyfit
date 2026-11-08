# Validation du 8 novembre 2026

## Contrôles exécutés

| Périmètre | Résultat |
|---|---|
| Solution .NET 10 | Compilation : zéro erreur, zéro avertissement. 22 tests d’intégration réussis sur de véritables bases SQLite temporaires isolées. |
| Authentification | Invitations et codes expirés/usage unique, secret navigateur, distinction des usages, révocation mobile et web, expiration de session, refus des accès anonymes et des origines étrangères. |
| Métier et concurrence | Droits par membre, équipement incompatible, révisions de profil, immutabilité après publication, sauvegarde/publication simultanées, anciens résultats hors ligne, soumissions UUID concurrentes sans doublon, performances nulles, terminé/partiel/abandonné, dates Europe/Paris et changements d’heure. |
| Sauvegarde SQLite | API Online Backup testée en intégration. Sauvegarde du conteneur Linux ouverte en lecture seule : `PRAGMA integrity_check = ok`, membre et résultat conservés après remplacement du conteneur. |
| Android | `assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest` réussis ; 4 tests unitaires réussis. APK debug généré, identifiant historique conservé. Les tests instrumentés Room sont compilés mais pas exécutés. |
| Blazor dans Chromium | Création de membre, ajout d’un lieu, composition manuelle de sept jours, publication du programme et accès authentifié au schéma OpenAPI vérifiés dans la base de démonstration isolée. Aucune erreur JavaScript dans ces parcours. Données temporaires supprimées après vérification. |
| Interface | Accueil et planning capturés à 1440 px et 390 px. Accueil mobile : largeur du document = 390 px ; calendrier défilant jusqu’au jour sélectionné. Relecture indépendante : les trois corrections demandées sont résolues, verdict `ship` limité à ces corrections. |
| iOS | Compilation et exécution de 3 XCTest réussies avec Xcode 26.6 sur un simulateur iPhone du runner macOS GitHub, sans signature. Contrat JSON/séries, modèle SwiftData dans un nouveau contexte en mémoire et dates Europe/Paris vérifiés. Cela ne remplace pas les essais sur iPhone réel. |
| Relecture technique | Les corrections de verrouillage Android, transaction de programme, séparation des caches iOS et isolation des résultats rejetés ont été relues indépendamment. |

## Validation GitHub

La [validation du commit de code 54b2e29](https://github.com/chouteau/familyfit/actions/runs/42490733846) est réussie : 22 tests .NET sur Linux, construction de l’image, contrôle du conteneur de production et 3 XCTest iPhone. Les erreurs d’import et d’initialisation détectées par Xcode ont été corrigées avant cette exécution réussie. Le workflow a été vérifié avec actionlint 1.7.12 et ses actions sont fixées à des SHA vérifiés.

La [pull request n° 1](https://github.com/chouteau/familyfit/pull/1) reste en brouillon et n’est pas fusionnée. La publication GHCR est volontairement ignorée sur une pull request ; aucune image de cette branche n’a été publiée et aucun VPS n’a été modifié. Les sauvegardes avec `compose.registry.yaml` et le tag conservé dans `.env` ont fait l’objet d’une relecture indépendante et d’un contrôle de configuration Compose sans export interactif.

## Image et dépendances

L’image finale utilise `mcr.microsoft.com/dotnet/aspnet:10.0.14-noble-chiseled-extra`, sans shell, avec ICU et fuseaux horaires. Exécution vérifiée sous utilisateur `app`, système de fichiers en lecture seule, `/tmp` temporaire, capacités supprimées et volume SQLite persistant. État de santé Docker : `healthy`.

Analyse Trivy 0.76.2 de l’image finale le 8 novembre : **0 critique, 0 élevée, 2 moyennes, 1 faible**. Aucun correctif n’est indiqué par l’analyse pour les trois alertes restantes :

| Alerte | Paquet | Sévérité |
|---|---|---|
| CVE-2026-18374 | libc6 2.39-0ubuntu8.9 | Moyenne |
| CVE-2026-89092 | libc6 2.39-0ubuntu8.9 | Moyenne |
| CVE-2025-5222 | libicu74 74.2-1ubuntu3.1 | Faible |

Le premier runtime Ubuntu complet présentait deux alertes élevées sur apt/libapt ; il a été remplacé par la variante chiseled. Le rapport NuGet ne signale aucune vulnérabilité connue. Ces résultats reflètent la base des scanners à la date du contrôle et ne garantissent pas l’absence de vulnérabilité.

Rapports bruts locaux exclus de Git : `src/Android/output/security/trivy-final.json` et `nuget-audit.json`. Captures locales : `src/Android/output/playwright/*-final.png`.

## Vérifications restantes

- Signature Ad Hoc et essais sur iPhone 13 ou plus récent, notamment hors ligne et après fermeture forcée. La compilation et les XCTest ont été réalisés sur le Mac du runner GitHub ; l’accès SSH au Mac personnel reste à autoriser et l’équipe Apple Developer reste à configurer pour la distribution.
- Tests Android instrumentés et parcours caméra, voix, mode avion, fermeture forcée/reprise sur un appareil ou émulateur. Aucun appareil n’était connecté lors de la validation.
- Appel réel au service de génération IA : non exécuté sans clé configurée. Le fonctionnement manuel reste disponible et a été vérifié.
- Déploiement sur le VPS, DNS/certificat de `fit.chouteau.info`, passage par le reverse proxy, distribution mobile et exercice de restauration sur l’installation réelle. Aucun déploiement externe n’a été effectué.
