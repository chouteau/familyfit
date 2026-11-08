# Contrat mobile FamilyFit v1

Base URL de production : `https://fit.chouteau.info`. HTTPS obligatoire. Pas de login/mot de passe ; tous les identifiants membre/jour/appareil sont des GUID sous forme de chaîne. Dates de planning : `yyyy-MM-dd`, horodatages ISO 8601 avec décalage UTC. Les clients ignorent les champs JSON inconnus.

## Activation

- `POST /api/v1/auth/invitations/preview` : `{ "code": "<10 caractères>" }` → `memberId`, `nickname`, `expiresAtUtc`. N’active pas le téléphone.
- `POST /api/v1/auth/enroll` : `{ "code": "…", "deviceName": "iPhone" }` → `token`, `deviceId`, `member`. Consomme l’invitation une seule fois.
- L’autorisation doit être conservée dans Keychain/Android Keystore, jamais dans les logs, liens ou préférences non chiffrées.
- En-tête des requêtes authentifiées : `Authorization: Bearer <autorisation de l’appareil>`.

## Connexion navigateur

- Le navigateur appelle `POST /auth/browser/challenges` avec `X-FamilyFit-Request: 1`. Il reçoit `id`, `code`, `expiresAtUtc`, `qrSvg` ; son secret reste dans un cookie HttpOnly.
- Le mobile scanne le QR ou saisit le code puis demande confirmation explicite au membre.
- `POST /auth/browser/approve` avec l’autorisation mobile et `{ "code": "…" }` approuve la demande.
- Le navigateur appelle `POST /auth/browser/consume` avec son cookie et le même en-tête dédié : `pending`, `approved` ou `expired`. Le serveur pose le cookie de session uniquement dans ce navigateur.
- Les liens QR sont distincts : `familyfit://enroll?code=…&server=…`, `familyfit://login?code=…&server=…`, `familyfit://workout?day=…&server=…`. Refuser un serveur différent de celui configuré ; ne jamais changer automatiquement l’adresse d’envoi de l’autorisation.

## Synchronisation

`GET /api/v1/sync` renvoie un instantané cohérent complet : `serverTimeUtc`, `member`, `members`, `locations`, `exercises`, `days`, `results`, `weights`. Remplacer atomiquement les données téléchargées ; conserver séparément la séance active et la file d’envoi. Ce choix simplifie les suppressions et les changements de version à l’échelle d’une famille.

Chaque journée contient `id`, `programId`, `date`, `title`, `locationId`, `locationName`, `durationMinutes`, `isRest`, `exercises`. Chaque exercice inclut `id`, `name`, `equipmentId`, `muscle`, `targetType` (`time` ou `repetitions`), `instructions`, `postureTip`, `imageUrl`, `videoUrl`, `sets`, `target`, `restSeconds`. Les médias relatifs `/media/…` se téléchargent avec l’autorisation ; ils sont mis en cache avant une séance hors ligne.

`POST /api/v1/workouts` attend :

```json
{
  "id": "<GUID créé une seule fois sur le mobile>",
  "programDayId": "<GUID du jour téléchargé>",
  "startedAtUtc": "2026-11-09T07:00:00Z",
  "finishedAtUtc": "2026-11-09T07:30:00Z",
  "exercises": [
    { "exerciseId": "<ID du catalogue>", "completedSets": 3, "repetitions": 12, "durationSeconds": null, "weightKg": null, "skipped": false }
  ]
}
```

Les tentatives répétées doivent réutiliser exactement le même résultat enregistré. Un même GUID avec un contenu différent renvoie 409. Le serveur calcule `completed`, `partial` ou `abandoned` ; un résultat final est immuable. Les résultats d’anciennes versions publiées restent acceptés après archivage pour permettre le retour du réseau.

Ne retirer un résultat de la file qu’après une réponse 2xx. Sur 401, demander une nouvelle invitation sans supprimer la file. Sur 409/400, conserver le résultat et son erreur pour correction d’exploitation, sans boucle d’envoi rapide. Les résultats d’un autre membre ne doivent jamais être réattribués lors d’une réactivation.

## Organisation serveur

Blazor et les endpoints appellent les extensions métier `IMediator`. Les contrats `IRequest<Outcome<T>>` sont dans `FamilyFit.Contracts/Models/<Entité>`, les handlers dans `FamilyFit.Core/Handlers/<Entité>` et les extensions dans `FamilyFit.Core/ModelExtensions`. Les handlers vérifient l’autorisation actuelle puis valident les données, persistent et publient la notification familiale après succès. Le navigateur ne peut pas fournir librement l’acteur via JSON.
