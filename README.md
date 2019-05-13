# Projet Bioinformatique
Projet Bioinformatique Master ILC 2018-2019.

## Lancer le projet

Lancer le script bin/bioinfo.bat (Windows) où bin/bioinfo (Linux).  
Il est possible de modifier les options de démarrage dans ces scripts afin par exemple
d'allouer plus de mémoire (argument -Xmx8G pour 8Go de RAM max)

## Particularités du projet

* Nous détectons 6 types de replicons : Chromosome, mitochondrion, plasmid, linkage, plast et dna.
* Le type plast regroupe tous les type données par Genbank ayant plast dans leurs nom (plasti, chloroplast, etc...).
* Dans le cas ou le type n'est pas renseigné ou n'as pas pu être déterminé, le type par défaut DNA est attribué
 * Ceci entraine par exemple que la quasi totalité des virus (étant des RNA) sont classé comme DNA.

## Importer le projet dans Eclipse

Sous Eclipse Oxygen (4.7), après avoir récupérer le code source du projet :

1. Menu 'File' > 'Import' > 'Existing Gradle Project' > 'Next'
2. Indiquer le dossier du projet
3. 'Finish'


En cas d'erreur d'accès aux classes de JavaFX :

1. S'assurer d'avoir installer le JDK 8 de JAVA
1. Clic droit sur le projet
2. 'Build Path' > 'Configure Build Path...'
3. Onglet 'Libraries' > déployer 'JRE System Library'
4. Double clic sur 'Access Rules' > 'Add...'
5. Choisir 'Resolution' : 'Accessible'
6. 'Rule Pattern' : '**'
7. 'Ok' > 'Ok' > 'Apply and Close'

--------------------------------------------

## Gradle

### Modifications des dépendances

En cas de modification d'une ou plusieurs dépendances,il peut être
nécessaire de rafraichir le projet Gradle pour qu'eclipse prenne en compte les changements :

1. Clic droit sur le projet
2. 'Gradle' > 'Refresh Gadle Project'

### Tâches Gradle

Différentes tâches sont mises en place par défaut, et sont accessible depuis le panneau 'Gradle Task'. 
Les plus utiles sont :
* 'Build' : Compile le projet et les librairie
* 'Distribution' > 'bootDistZip' : Créer un zip contenant les libraires et deux script (Unix et Windows) permettant de distribuer le projet (pas de jar auto-éxécutable)

D'autres tâches ne sont pas affichées sur ce panneau, il faut créer une 'Launch Configuration' pour les utiliser :
1. Cliquer sur la petite flèche à côté du bouton 'Run'
2. 'Run Configurations...'
3. Sélectionner 'Gradle Project' et cliquer sur le bouton 'New'
4. Nommer la configuration comme souhaité
5. Écrire les tâches souhaitées dans le champ 'Gradle Tasks'
6. Dans la section 'Working Directory', cliquer sur 'Workspaces' et sélectionner le projet
7. 'Apply' sauvegarde la configuration.

Penser à augmenter le niveau de log à WARN où ERROR dans le fichier logback.xml si les logs sont trop verbeux.

-----------------------------

## Développeurs

- **Thomas JANOWSKYJ** - @janowskyj
- **Alexandre COMBEAU** - @acombeau
- **François HALLER** - @francois.haller
- **Matthieu FUCHS** - @matthieufuchs
- **Florian BROSSARD** - @brossard