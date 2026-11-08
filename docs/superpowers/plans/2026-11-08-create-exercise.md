# Création d’exercices MCP
Objectif : ajouter create_exercise, puis préparer deux semaines en brouillon, sans publication.
Architecture : contrat ExerciseInput/CreateExerciseRequest, handler administrateur MCP, extension mediator, outil MCP. Table existante, aucune migration. Création seule pour préserver les prescriptions historiques.
Validation : longueurs, identifiant stable, type time/repetitions, matériel existant, doublons et lecture seule. Tests HTTP MCP puis suite .NET.
Déploiement : vérifier la cible et l’accès avant mise à jour. Après déploiement, relire le catalogue, créer les mouvements manquants, enregistrer les semaines 2026-11-09 et 2026-11-16 et vérifier draft, repos mercredi, sans renforcement samedi.
