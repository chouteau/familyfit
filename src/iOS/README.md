# FamilyFit iPhone

Application native SwiftUI, SwiftData, AVFoundation et Speech. iOS 18 minimum ; cible de validation : iPhone 13 et plus récents. Aucune dépendance tierce ni clé de service à mettre dans l’application.

## Compilation sur Mac

Ouvrir `FamilyFit.xcodeproj` dans Xcode 16 ou plus récent. Sélectionner le scheme partagé `FamilyFit`. Dans Signing & Capabilities, choisir ton équipe Apple Developer pour un appareil physique. Le bundle ID est `info.chouteau.familyfit`.

Compilation sans signature :

```sh
xcodebuild -project FamilyFit.xcodeproj -scheme FamilyFit -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

Tests : choisir un simulateur iPhone installé dans Xcode, puis `Product > Test` ; le scheme inclut `FamilyFitTests`. Pour le terminal, utiliser sa destination exacte listée par `xcodebuild -showdestinations -scheme FamilyFit`.

L’adresse du serveur est dans `FamilyFit/Info.plist` (`FamilyFitServerURL`). Utiliser une adresse HTTPS accessible au mobile. Les liens QR sont refusés si leur serveur ne correspond pas à cette adresse.

## Distribution privée

Enregistrer les iPhone dans le compte Apple Developer, créer une archive Release puis distribuer aux appareils enregistrés (Ad Hoc). Ne jamais committer certificats, profils, clés ou identifiants de signature. Prévoir le renouvellement avant expiration du profil.

## Parcours à vérifier sur iPhone réel

Invitation personnelle par caméra et par code, confirmation navigateur, révocation, refus de QR d’un autre serveur ; téléchargement puis séance en mode avion ; pause, série de répétitions, exercices sautés et validation de fin ; fermeture forcée/reprise ; retour du réseau et absence de doublons ; autorisations caméra/micro refusées, Bluetooth et coexistence de la voix avec la reconnaissance. Les commandes et le minuteur sont mis en pause quand l’application quitte le premier plan.

Les résultats non acquittés restent dans SwiftData. Les autorisations sont conservées dans le trousseau avec `WhenUnlockedThisDeviceOnly`. Une réactivation conserve les résultats en attente et ne les transfère pas à un autre membre. Ne pas désinstaller pour corriger un problème de synchronisation : cela supprimerait les résultats encore uniquement sur l’iPhone.
