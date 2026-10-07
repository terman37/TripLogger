# TripToExcel — Politique de confidentialité

*Dernière mise à jour : 2 octobre 2026*

> **TripToExcel est une application purement locale.** Elle ne collecte, ne
> transmet et ne vend aucune donnée personnelle. Il n'y a ni compte, ni serveur,
> ni outil d'analyse. Vos trajets restent sur votre téléphone, sauf si vous
> choisissez vous-même de les partager.

## 1. Champ d'application

Cette politique couvre l'application Android **TripToExcel**, publiée sur Google
Play sous le nom *TripToExcel - Km Logger* (package `com.terman37.triplogger`),
développée par **Anthony Jourdan**. Elle explique ce que l'application utilise sur
votre appareil, où vont ces données et comment vous pouvez les supprimer.

## 2. Ce que l'application utilise sur votre appareil

TripToExcel accède aux données suivantes, *uniquement sur votre appareil* :

- **L'état de connexion Bluetooth** de l'appareil que vous avez enregistré dans
  l'application. Il sert uniquement à déterminer quand un trajet commence et quand
  il se termine. L'application ne lit ni ne transfère d'audio, de contacts ni de
  fichiers via Bluetooth.
- **La position (GPS)**, pendant l'enregistrement d'un trajet, pour calculer la
  distance parcourue et retrouver les adresses de départ et d'arrivée.
- **Les enregistrements de trajets** qui en découlent : heures de début et de fin,
  coordonnées de départ et d'arrivée, nom de rue et ville, et distance.
- **Les notifications**, pour afficher la notification permanente « Suivi actif »
  qui indique que l'application enregistre.

## 3. Aucune collecte, aucune transmission

TripToExcel n'envoie vos données nulle part. L'application n'a ni serveur, ni
compte utilisateur, ni régie publicitaire, ni outil d'analyse ou de rapport de
plantage.

La seule manière dont vos trajets quittent votre téléphone est lorsque **vous**
exportez un tableau et le partagez via le partage Android (par e-mail, vers un
espace de stockage en ligne, etc.). Cette action est entièrement sous votre
contrôle.

## 4. Où les données sont stockées

Les enregistrements de trajets sont stockés dans l'espace privé de l'application,
sur votre téléphone (une base de données locale, plus un dossier temporaire
d'export dans le cache privé de l'application). Les autres applications de votre
téléphone ne peuvent pas lire cet espace. Les tableaux exportés ne sont partagés
que via le partage Android, et uniquement lorsque vous lancez l'export.

La sauvegarde automatique d'Android est désactivée (`allowBackup="false"`) : les
données de trajets ne sont donc ni copiées vers votre Google Drive, ni transférées
vers un autre téléphone par le système.

## 5. Supprimer vos données

- Supprimez un trajet depuis la liste des trajets ou depuis le rapport.
- Supprimez tous les trajets d'une période depuis l'onglet Rapport.
- Désinstallez l'application pour effacer toutes les données de trajets stockées
  sur votre téléphone.

Comme le développeur ne reçoit jamais vos données, il n'y a rien à supprimer sur
un serveur : la suppression est entièrement locale et immédiate.

## 6. Tiers

- **Le géocodeur intégré d'Android** sert à transformer des coordonnées en
  adresse. C'est un service du système : le traitement des données relève de votre
  appareil et des conditions de Google, pas de TripToExcel.
- **Google Maps** ne s'ouvre que lorsque vous touchez un lien de carte.
  L'application transmet simplement une URL à l'application Maps ou au navigateur ;
  aucune donnée de trajet n'est envoyée par TripToExcel.

Aucun autre tiers ne reçoit de données de l'application.

## 7. Autorisations et raisons

- `BLUETOOTH_CONNECT` — détecter la connexion et la déconnexion de votre voiture.
- `ACCESS_FINE_LOCATION` — relevés GPS pour la distance et les adresses pendant
  l'enregistrement.
- `ACCESS_BACKGROUND_LOCATION` (« Autoriser en permanence ») — pour que le suivi
  puisse reprendre tout seul après un redémarrage du téléphone. Sans cette
  autorisation, Android interdit à un service de localisation de démarrer en
  arrière-plan.
- `POST_NOTIFICATIONS` — afficher la notification permanente « Suivi actif ».
- `RECEIVE_BOOT_COMPLETED` — redémarrer le suivi après un redémarrage du téléphone.

Le suivi n'est jamais silencieux : tant qu'il est actif, la notification est
visible, et balayer cette notification arrête le suivi.

## 8. Enfants

TripToExcel ne s'adresse pas aux enfants et ne collecte sciemment aucune
information les concernant.

## 9. Modifications de cette politique

Si le comportement de l'application change d'une manière qui affecte cette
politique (par exemple l'ajout d'une synchronisation), cette page sera mise à jour
avant la publication de cette version, et la date de « Dernière mise à jour »
ci-dessus changera.

## 10. Contact

Pour toute question sur cette politique ou sur le traitement des données :
[anthony.jourdan@gmail.com](mailto:anthony.jourdan@gmail.com).

---

TripToExcel — politique de confidentialité. Cette page est un fichier statique :
elle n'utilise ni cookie ni script. Code source :
[github.com/terman37/TripLogger](https://github.com/terman37/TripLogger).
Version anglaise : [English](en/privacy-policy.md).
