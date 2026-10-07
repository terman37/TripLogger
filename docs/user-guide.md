# TripToExcel — guide d'utilisation

Une application Android qui **enregistre vos trajets en voiture automatiquement**
et vous permet de les exporter dans un tableur pour votre note de frais.

Vous n'avez rien à faire avant de conduire : quand le Bluetooth de votre voiture se
connecte, le trajet démarre ; quand il se déconnecte, le trajet se termine. La
distance, les adresses et les heures de départ et d'arrivée sont enregistrées.
Ensuite, vous choisissez une période et exportez un tableur que vous pouvez envoyer
à votre employeur ou à votre comptable.

Tout reste **sur votre téléphone**. Il n'y a ni compte, ni serveur, et
l'application n'envoie rien.

## Installer l'application

Les étapes de compilation et d'installation sont dans [BUILD.md](BUILD.md) ; si
quelqu'un vous a déjà donné un fichier `.apk`, ouvrez-le simplement sur votre
téléphone et autorisez l'installation depuis cette source.

Configuration requise : Android 14 ou version ultérieure.

## Première configuration (2 minutes)

1. **Appairez votre voiture dans les réglages Android** (l'application ne fait
   jamais l'appairage elle-même) : Réglages → Bluetooth → appairez votre voiture,
   votre autoradio ou votre adaptateur habituel.
2. Ouvrez **TripToExcel**. Lors d'une première installation, activez simplement
   l'interrupteur **Suivi des trajets** : aucun appareil n'étant encore
   enregistré, l'application vous amène dans l'onglet **Réglages** avec la liste
   *Disponibles* déjà ouverte.
3. Touchez **Autoriser l'accès Bluetooth** (le bouton ambre), puis **+** à côté de
   votre voiture dans la liste *Disponibles*. Votre voiture apparaît maintenant
   sous *Enregistrés*.
4. Revenez à l'onglet **Accueil** (premier onglet) et activez l'interrupteur
   **Suivi des trajets**. L'application affiche d'abord un message d'information
   qui explique l'usage de la position (y compris en arrière-plan, écran éteint) :
   touchez **Continuer**. Android demande alors les autorisations (Bluetooth,
   localisation, notifications) : acceptez les trois, elles sont nécessaires.
5. Une petite notification « Suivi actif » apparaît. La configuration est
   terminée.

Vous pouvez enregistrer plusieurs voitures/appareils : ajoutez-les avec **+**, et
supprimez-les avec le **X** à côté de leur nom.

### Le délai de reconnexion (onglet Réglages)

Le Bluetooth se coupe parfois quelques secondes (téléphone dans la poche, tunnel…).
Le **Délai de reconnexion** (1 à 15 minutes, 3 par défaut) fait qu'une déconnexion
ne termine le trajet qu'après ce délai : si la voiture se reconnecte à temps, le
trajet reste un seul trajet.

## Utilisation quotidienne

**En conduisant**

Conduisez, tout simplement. Quand la voiture se connecte, l'écran Accueil affiche
une carte colorée « Enregistrement » avec la distance et la durée en direct. Quand
la voiture se déconnecte, le trajet est enregistré après le délai de reconnexion.

**Si la détection automatique manque un trajet** (rare), touchez
**Démarrer manuellement** sur l'accueil, puis **Arrêter** à l'arrivée. Les trajets
manuels ignorent complètement le Bluetooth.

**Liste des trajets (Accueil)** — affiche les trajets d'aujourd'hui et d'hier, du
plus récent au plus ancien.

- Touchez un trajet pour voir les détails : heures, durée, adresses, distance.
- La petite icône de carte sur la ligne ouvre tout l'itinéraire dans Google Maps ;
  dans les détails, chaque adresse a sa propre icône d'épingle qui ouvre ce lieu
  précis.
- L'icône corbeille rouge supprime un trajet (une confirmation est toujours
  demandée).

Un trajet de moins de 50 mètres est ignoré — par exemple quand le moteur tourne
alors que la voiture reste stationnée et que le Bluetooth se connecte.

## Rapports et tableurs

Ouvrez l'onglet **Rapport** :

1. Choisissez une date **Du** et **Au** (côte à côte). Par défaut, les 7 derniers
   jours sont affichés ; la liste se met à jour dès que vous changez une date.
2. Une ligne de résumé indique le nombre de trajets et le total de kilomètres de la
   période.
3. Touchez le **bouton d'export** (en bas à droite) pour créer le tableur : le
   partage Android s'ouvre, ce qui vous permet de l'envoyer par e-mail, de
   l'enregistrer sur Drive ou de l'ouvrir dans Google Sheets/Excel.
4. L'**icône corbeille** (en bas à gauche) supprime **tous** les trajets de la
   période affichée — une confirmation est toujours demandée avant.

Le tableur contient une ligne par trajet : les dates et heures de début et de fin
en véritables dates Excel (Excel les affiche dans votre langue ; utilisez Format de
cellule pour les modifier), l'adresse de départ et d'arrivée (chacune ouvre le lieu
dans Google Maps), les kilomètres et un lien « Trajet » ouvrant l'itinéraire
complet. La ligne d'en-tête est figée et gris clair, et la ligne des totaux est
mise en évidence. Chaque ligne de trajet peut aussi être supprimée depuis le
rapport avec son icône corbeille rouge.

> Si un trajet s'est terminé alors que vous n'aviez pas de réseau, ses adresses
> apparaissent comme « Adresse en attente ». Elles se complètent toutes seules la
> prochaine fois que vous ouvrez l'onglet Rapport ou que vous exportez, dès que
> vous avez du réseau.

## À propos de cette application (version et licence)

L'onglet Réglages se termine par une ligne **À propos de cette application** : elle
indique la version installée et renvoie vers la documentation, la politique de
confidentialité et l'adresse de contact.

## Bon à savoir

- **La notification, c'est le suivi.** Si vous balayez la notification
  « TripToExcel », l'application arrête le suivi (c'est volontaire : elle
  n'enregistre jamais en silence). Réactivez l'interrupteur sur l'accueil si c'était
  une erreur.
- **Fermer l'application ne pose aucun problème.** Le suivi continue en
  arrière-plan tant que l'interrupteur est activé.
- **Forcer l'arrêt de l'application** ou **redémarrer le téléphone** pendant un
  trajet ne le fait pas perdre : au prochain démarrage de l'application, ce trajet
  est enregistré et se termine à la dernière position relevée. (Un trajet de moins
  de 50 m reste écarté comme du bruit.)
- **Redémarrer le téléphone** relance aussi le suivi automatiquement une fois le
  démarrage terminé (la notification revient), tant que l'interrupteur est activé
  et qu'un appareil est enregistré. C'est pourquoi l'application demande la
  localisation « Autoriser en permanence » lorsque vous activez le suivi : sinon,
  Android ne permet pas à une application en arrière-plan de relancer le suivi.
  Certaines marques bloquent le démarrage automatique des applications : si
  l'interrupteur est activé mais qu'il n'y a pas de notification, désactivez puis
  réactivez le suivi.
- **Batterie** : l'application ne demande le GPS que pendant l'enregistrement d'un
  trajet (un relevé toutes les 30 secondes), elle consomme donc peu. Certaines
  marques restreignent en plus les applications en arrière-plan ; si
  l'enregistrement s'arrête sans raison, autorisez TripToExcel à fonctionner en
  arrière-plan / désactivez l'optimisation de la batterie pour elle.
- **Autorisations** : le Bluetooth sert uniquement à détecter la connexion de votre
  voiture ; la localisation sert à la distance et aux adresses, et « Autoriser en
  permanence » à reprendre le suivi après un redémarrage. Rien ne quitte le
  téléphone.

## Pour aller plus loin

- [Détails techniques](DETAILS.md) — comment un trajet est enregistré, architecture,
  tests.
- [Spécification de l'interface](UI.md) — chaque écran et chaque état.
- [Compiler et installer](BUILD.md) — compiler vous-même, versions signées.
- [Politique de confidentialité](privacy-policy.md) — ce que l'application fait de
  vos données.

Version anglaise de ce guide : [English](en/user-guide.md).
