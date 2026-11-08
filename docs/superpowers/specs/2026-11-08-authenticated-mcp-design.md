# FamilyFit — MCP authentifié et connexion web personnelle

## Décisions utilisateur

Retirer la génération IA intégrée au site et permettre le suivi depuis un agent externe. La clé dédiée est fournie en production dans le `.env` associé au Docker Compose sous le nom exact `MCP_API_KEY`. Aucun compte ou mot de passe supplémentaire n’est créé. Aucune clé OpenAI n’est nécessaire au serveur ; les réglages vocaux Android historiques sont conservés.

Sans session ni lien personnel valide, le site public reste vide. L’administrateur génère un lien web pour un membre depuis « Membres ». La page conserve le style existant, affiche le pseudo attendu et exige l’approbation du téléphone activé sur le même profil. Le suivi familial reste partagé après connexion.

## Architecture

- SDK officiel `ModelContextProtocol.AspNetCore` 2.4.0, endpoint `/mcp`, Streamable HTTP stateless, origine validée et limites de débit.
- Bearer MCP indépendant des autorisations mobiles et des cookies. Clé de 32 à 512 caractères, comparaison constante SHA-256, sans stockage de clé en SQLite. Valeur absente : accès désactivé. Rotation/suppression et recréation du conteneur : ancien accès révoqué.
- La clé représente l’unique administrateur existant, vérifié à chaque appel. `MCP_READ_ONLY=true` bloque les modifications ; défaut false. Le principal agent est séparé des acteurs mobile et navigateur. Aucun outil ne crée d’invitation, de lien de connexion, de clé ou de session.
- Les 18 outils couvrent guide/identité, tableau familial, profils/poids, lieux/matériel, exercices, planning/programmes et résultats. Ils appellent les extensions ChannelMediator et les mêmes validations métier. Les lectures MCP utilisent un handler borné séparé, sans modifier les requêtes SQL web/mobile historiques.
- Listes paginées (1–100), période de 366 jours maximum, tableau de 1000 journées maximum. Les programmes publiés sont immuables ; les résultats gardent leur propriété et leur idempotence UUID. Aucune séance ne doit être inventée ni publiée implicitement.
- Liens web : secret aléatoire 256 bits dans le fragment `#secret=…`, hash seul en SQLite, expiration cinq minutes après création. Fragment effacé dès lecture, politique no-referrer/no-store. Liaison au premier navigateur, code renouvelable sans prolongation, consommation à la création de session. Validation obligatoire de l’identité du téléphone par GUID membre, pas par simple texte de pseudo.
- Migration `PrivateMemberLoginLinks` limitée aux liens et demandes de connexion. Sessions et téléphones existants restent valides. Commande opérateur `--login-link --nickname …` ou `--member-id …` pour le premier accès ou la récupération web ; fichier HTML privé, chemin seul dans les logs.

## Validation

Tests SQLite/HTTP réels : client MCP officiel avec protocoles 2025-11-25 et 2026-07-28, schémas sans identité fournie par client, authentification invalide et rotation, lecture seule, refus des opérations d’accès, publication/résultats réels, idempotence et filtres. Liens privés : page anonyme vide, cinq minutes exactes, navigateur lié, usage unique, code renouvelé sans prolongation, mauvais membre refusé et création réservée au navigateur administrateur. Vérification visuelle desktop/mobile sur données fictives isolées, contrôle Docker et revue technique indépendante. Aucune clé ou donnée de production n’est créée par les validations.

## Première connexion administrateur (décision complémentaire)

L’utilisateur demande un code initial dans le .env, saisi directement sur le site. FAMILYFIT_BOOTSTRAP_CODE (16 à 128 caractères, aléatoire, distinct de MCP_API_KEY) permet une seule première session administrateur de douze heures. Aucun téléphone préactivé n’est nécessaire. Pseudo/âge/poids créent le premier profil ; un unique admin déjà créé mais sans session antérieure est réutilisé. Un état SQLite ID1 et une transaction sérialisable empêchent le réemploi et les courses. Toute installation ayant déjà eu une session web refuse l’initialisation. Retirer ensuite le code du .env. Les invitations mobiles et les liens personnels reprennent le fonctionnement prévu. La session initiale sans Device reste distincte des mobiles, se déconnecte et se révoque avec la récupération opérateur. Le formulaire n’existe que tant que l’initialisation est disponible ; sinon le public reste vide. Endpoint via ChannelMediator, comparaison constante, cinq tentatives/minute/IP, contrôle d’origine et validation du profil. Migration InitialAdminCode séparée, aucun ancien téléphone/session modifié.
