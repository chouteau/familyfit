---
name: FamilyFit
description: Un rendez-vous sportif familial, joyeux et encourageant.
colors:
  background: "#faf8f3"
  surface: "#ffffff"
  ink: "#18213e"
  muted: "#626779"
  primary: "#3b940b"
  primary-action: "#2d7608"
  green-soft: "#edf8df"
  orange: "#b94c03"
  orange-soft: "#fff0df"
  blue: "#126eaf"
  blue-soft: "#e8f4ff"
  lemon: "#fff5c8"
  line: "#e9e7e1"
  error: "#b83131"
typography:
  display:
    fontFamily: "Nunito Sans, sans-serif"
    fontSize: "clamp(1.8rem, 2.7vw, 2.8rem)"
    fontWeight: 900
    lineHeight: 1.16
    letterSpacing: "-0.025em"
  headline:
    fontFamily: "Nunito Sans, sans-serif"
    fontSize: "1.55rem"
    fontWeight: 850
    lineHeight: 1.25
    letterSpacing: "-0.02em"
  title:
    fontFamily: "Nunito Sans, sans-serif"
    fontSize: "1.1rem"
    fontWeight: 800
    lineHeight: 1.3
  body:
    fontFamily: "Nunito Sans, sans-serif"
    fontSize: "16px"
    lineHeight: 1.5
  label:
    fontFamily: "Nunito Sans, sans-serif"
    fontSize: "0.86rem"
    fontWeight: 750
rounded:
  card: "16px"
  button: "11px"
  control: "9px"
  nav: "13px"
spacing:
  planning-gap: "20px"
  member-gap: "14px"
  page-block: "30px"
  page-inline: "32px"
components:
  button-primary:
    backgroundColor: "{colors.primary-action}"
    textColor: "{colors.surface}"
    rounded: "{rounded.button}"
    padding: "10px 18px"
  button-secondary:
    backgroundColor: "#f0f1ed"
    textColor: "{colors.ink}"
    rounded: "{rounded.button}"
    padding: "10px 18px"
  button-orange:
    backgroundColor: "{colors.orange}"
    textColor: "{colors.surface}"
    rounded: "{rounded.button}"
    padding: "10px 18px"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "10px 12px"
  navigation-active:
    backgroundColor: "{colors.primary-action}"
    textColor: "{colors.surface}"
    rounded: "{rounded.nav}"
    padding: "14px 17px"
  status-completed:
    backgroundColor: "#e4f5d1"
    textColor: "#25690c"
    rounded: "{rounded.control}"
    padding: "5px 9px"
  member-card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.card}"
    padding: "18px 16px"
---

# Design System: FamilyFit

## Overview

**Creative North Star: "Le rendez-vous sportif en famille"**

Les compositions accueil et planning approuvées donnent le ton : fond ivoire, surfaces blanches, titres marine arrondis et accents francs. La mouette sportive apporte une présence encourageante aux bilans et conseils ; elle accompagne les informations utiles sans remplacer les commandes.

Le système privilégie la lisibilité et une progression familiale bienveillante. Les composants restent de vrais liens, boutons, formulaires et tableaux. Les données illustratives portent un repère visible, distinct des données réelles.

**Key Characteristics:**
- Ivoire chaleureux, marine lisible et accents sportifs.
- Typographie arrondie, boutons généreux et cartes souples.
- Statuts libellés et repos reconnu dans le planning.
- Navigation et consultation adaptées au téléphone.

## Colors

### Primary

Le vert feuille `primary` colore la marque, les illustrations, la progression et les contours de sélection. Le vert profond `primary-action` porte les actions principales, les liens et les éléments sélectionnés contenant du texte blanc. Le survol d'une action principale conserve ce fond profond.

**The Action Contrast Rule.** Employer le vert profond pour les commandes à texte blanc ; réserver le vert feuille à la marque et aux repères visuels.

### Secondary

L'orange `orange` porte les actions de consultation et les accents chaleureux ; `orange-soft` fournit leur surface douce. Les statuts partiels ont leur propre couple de couleurs, adapté aux petits libellés. Le bleu `blue` identifie les repères programmés et le focus clavier, accompagné de `blue-soft`. Le citron `lemon` sert au bilan du jour, aux conseils et au repère de démonstration.

### Neutral

`background` constitue le fond de page ; `surface` les cartes et menus. `ink` porte les titres et le corps, `muted` les informations secondaires, `line` les séparateurs discrets. `error` est réservé aux erreurs de validation et aux commandes de danger.

**The Labeled Status Rule.** Ne jamais transmettre un état uniquement par sa couleur : associer un libellé, éventuellement masqué visuellement pour la version compacte, et un pictogramme.

## Typography

Nunito Sans est auto-hébergée dans `/fonts/nunito-sans.woff2`, avec repli `sans-serif`, poids variables de 200 à 1000 et `font-display: swap`. Le corps est confortable ; la hiérarchie utilise des titres très gras et des espacements de lettres légèrement resserrés.

Les rôles généraux sont définis dans le frontmatter. Les titres de cartes membre montent à 1.25rem ; les titres du panneau de séance à 1.65rem. Les libellés de statut utilisent généralement 0.8rem, avec des variantes plus compactes dans les jours. Les légendes de synchronisation sont à 0.75rem, puis 0.72rem sur mobile. Ces petites tailles restent réservées aux informations complémentaires.

À 800px et moins, le titre principal passe à 1.9rem et les titres de section à 1.3rem. Les longs titres et informations peuvent revenir à la ligne, y compris dans les cartes et les pastilles d'information.

## Layout

Le cadre desktop combine une barre latérale de 220px, collante sur la hauteur de fenêtre, et une colonne principale réductible. Le contenu centré atteint 1600px, avec un retrait de 30px vertical et 32px horizontal. La barre supérieure mesure 74px. Le planning juxtapose une semaine et un panneau de détail de 330px, séparés de 20px ; les cartes membres utilisent un intervalle de 14px.

- À partir de 1650px : panneau de détail de 370px, jours plus hauts et illustrations agrandies.
- À 1200px et moins : barre latérale de 190px, retrait de contenu de 25px/22px, cartes membres sur trois colonnes ; le détail de séance passe sous la semaine et perd sa position collante.
- À 800px et moins : navigation horizontale collante, marque compacte dans la barre supérieure de 55px, contenu en 23px/16px, cartes membres sur deux colonnes et gestion sur une colonne. Le menu de compte conserve son avatar et ses accès aux appareils et à la déconnexion.
- À 450px et moins : mouette retirée du bilan pour laisser place au texte, formulaires et exercices sur une colonne, aperçus de programme sur deux colonnes.

Les semaines gardent leur géométrie à sept jours dans un conteneur à défilement horizontal local : largeur minimale de 680px sur desktop, 700px sur mobile. La semaine familiale utilise un vrai tableau. Son conteneur de défilement est positionné relativement pour contenir sa légende masquée et éviter un débordement artificiel de la page. La sélection d'un jour recentre immédiatement ce jour dans le calendrier avec un défilement local `instant`, sans déplacer verticalement la page.

## Elevation & Depth

Les cartes se détachent légèrement du fond avec une ombre diffuse (`0 3px 13px #18213e0a`). Les cartes membre montent de 3px au survol et renforcent leur ombre (`0 7px 20px #18213e12`). Les menus de compte utilisent une ombre plus présente (`0 8px 30px #18213e20`) pour rester lisibles au-dessus du contenu. Les boutons principaux possèdent une petite tranche inférieure (`0 3px 0 #286a08`) ; les contours servent surtout aux champs, séparateurs et sélections.

## Shapes

Les cartes et grands panneaux partagent des angles doux. Les boutons sont un peu plus compacts ; les champs et statuts utilisent des angles de contrôle. Les avatars sont circulaires, les sélecteurs de membre en capsule. Le calendrier distingue le jour sélectionné par un contour vert de 2px et une surface légèrement teintée. Éviter d'uniformiser toutes les formes en capsules : chaque famille a sa silhouette.

## Components

- **Actions :** bouton principal vert profond, secondaire neutre, action orange. Hauteur minimale usuelle de 44px ; variante petite de 36px. Au clic, translation verticale de 1px. Le focus clavier est bleu, de 3px, avec décalage de 4px. Les commandes désactivées perdent en opacité et changent de curseur.
- **Champs :** libellé explicite au-dessus, fond blanc, bord fin ; erreurs visibles près des champs. Ne pas remplacer les messages par la seule bordure rouge.
- **Navigation :** liens avec icônes et libellés, sélection en vert profond. Sur téléphone, conserver les libellés et l'accès au compte dans un menu natif `details`/`summary` accessible au clavier.
- **Cartes membre :** avatar, prénom, repères utiles, statut et action alignés dans une colonne. Les noms longs restent contenus. Les couleurs d'avatar servent à reconnaître les membres, sans classement de performance.
- **Statuts :** terminé vert, partiel orange, prévu/en cours bleu, abandonné/manqué rouge, repos neutre verdâtre. Les versions compactes conservent un libellé pour les lecteurs d'écran. Le repos possède son propre message, distinct d'une séance manquée.
- **Planning :** sélection de membre par lien avec `aria-current`, sélection de jour par bouton avec `aria-pressed`. Le détail expose la nouvelle sélection via `aria-live="polite"`. Les images d'exercice complètent les libellés et la liste numérotée.
- **Mouvement :** transitions brèves de 150ms sur les actions et de 180ms sur les cartes. Avec `prefers-reduced-motion`, retirer transitions et animations et conserver un défilement immédiat.

## Do's and Don'ts

### Do:
- Construire des composants réels à partir des compositions approuvées.
- Conserver les libellés d'état, le focus visible et les messages de validation.
- Garder le compte accessible sur mobile et le défilement des semaines local.
- Identifier les données de démonstration et présenter le repos comme un état prévu.

### Don't:
- Utiliser une image de maquette comme interface.
- Remplacer le vert profond des actions à texte blanc par le vert décoratif.
- Introduire un classement par poids, une inscription publique ou une exécution de séance dans le navigateur.
- Présenter une synchronisation Garmin fictive comme une réussite réelle.
