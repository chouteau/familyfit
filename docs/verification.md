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
| iOS | Neuf fichiers Swift analysés sans erreur de syntaxe par le parseur Swift ; structure du projet Xcode et ses deux cibles lues avec succès. Cela ne remplace pas une compilation par le SDK iOS. |
| Relecture technique | Les corrections de verrouillage Android, transaction de programme, séparation des caches iOS et isolation des résultats rejetés ont été relues indépendamment. |

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

- Compilation Xcode avec le SDK iOS, exécution des XCTest, signature Ad Hoc et essais sur iPhone 13 ou plus récent. Nécessitent le Mac et l’équipe Apple Developer ; aucune compilation iOS n’a été réalisée depuis Windows.
- Tests Android instrumentés et parcours caméra, voix, mode avion, fermeture forcée/reprise sur un appareil ou émulateur. Aucun appareil n’était connecté lors de la validation.
- Appel réel au service de génération IA : non exécuté sans clé configurée. Le fonctionnement manuel reste disponible et a été vérifié.
- Déploiement sur le VPS, DNS/certificat de `fit.chouteau.info`, passage par le reverse proxy, distribution mobile et exercice de restauration sur l’installation réelle. Aucun déploiement externe n’a été effectué.
