# TripToExcel — documentation

Application Android qui **enregistre automatiquement vos trajets en voiture** dès
que le Bluetooth de la voiture se connecte, et qui vous permet de **les exporter
dans un tableur Excel** pour votre note de frais. Tout reste sur le téléphone : ni
compte, ni serveur, ni envoi de données.

## Documents

- **[Guide d'utilisation](user-guide.md)** — installation, première configuration,
  usage quotidien, rapports et tableurs, version et licence.
- **[Détails techniques](DETAILS.md)** — architecture, déroulement d'un trajet,
  modèle de données, autorisations, tests. *(en anglais)*
- **[Spécification de l'interface](UI.md)** — chaque écran, chaque état, chaque cas
  vide. *(en anglais)*
- **[Compiler et installer](BUILD.md)** — compilation de débogage, paquet de
  publication signé. *(en anglais)*
- **[Politique de confidentialité](privacy-policy.md)** — ce que l'application fait
  de vos données (réponse courte : rien ne quitte le téléphone).

English version of this page: [English](en/index.md).

## L'application en un paragraphe

Appairez votre voiture dans les réglages Bluetooth d'Android et enregistrez-la dans
l'application. Quand la voiture se connecte, un trajet démarre ; quand elle se
déconnecte, le trajet se termine après un délai configurable, pour qu'une brève
coupure Bluetooth ne coupe pas un trajet en deux. La distance, les adresses et les
heures de départ et d'arrivée sont enregistrées avec le téléphone dans votre poche.
Ensuite, vous choisissez une période et exportez un tableur dont les lignes
contiennent de véritables dates Excel, des liens Google Maps et une ligne de
totaux.
