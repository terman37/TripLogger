# TripToExcel — notes de version

Ce que voit l'utilisateur, version par version. **Source unique** des notes
« Nouveautés » du Play Console : copier le bloc de la version concernée tel quel
(sans jargon de développement, sans numéro de commit — c'est ce que lit
l'utilisateur avant de mettre à jour).

Deux numéros, à ne pas confondre :

- **Code de version** (`versionCode` dans `app/build.gradle.kts`) : le compteur
  interne de Play. Il doit être **plus grand que tout ce qui a déjà été envoyé**,
  y compris sur une piste de test. Il change à chaque nouvel AAB.
- **Version affichée** (`versionName`) : ce que voit l'utilisateur, dans la fiche
  Play et dans « À propos de cette application ». Elle change quand la version
  change vraiment (1.0 → 1.1).

Version anglaise : [English](en/release-notes.md).

## 1.0 — 7 octobre 2026 (code de version 2)

Première version publiée.

- Enregistrement automatique des trajets dès que le Bluetooth de la voiture se
  connecte : rien à activer avant de conduire.
- Délai de reconnexion (1 à 15 minutes) pour qu'une brève coupure Bluetooth ne
  coupe pas un trajet en deux.
- Démarrage et arrêt manuels pour les trajets que la détection automatique manque.
- Export Excel (.xlsx) pour les notes de frais : dates, adresses et liens Google
  Maps, ligne de totaux.
- Application en français et en anglais.
- Données locales : ni compte, ni serveur, ni publicité, ni analyse.

<!--
  Modèle pour la prochaine version — garder le même ton, 1 à 5 puces, les
  changements visibles par l'utilisateur d'abord :

## 1.1 — (date) (code de version 3)

- …
-->
