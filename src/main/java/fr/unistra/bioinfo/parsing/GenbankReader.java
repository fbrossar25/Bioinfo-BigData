package fr.unistra.bioinfo.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe permettant de lire les fichiers Genbank .gb.<br>
 * Attention : Ne lit que le premier replicon contenus dans le fichiers.<br>
 * les fichiers contenant plusieurs replicons ne seront pas entièrement lus.
 */
public class GenbankReader {
    private static final String END_TAG = "//";
    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\s*(.+?)(\\s+(.+))?$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(NC_\\d+?).(\\d+)$");

    enum Operator{
        JOIN, COMPLEMENT, COMPLEMENT_JOIN, NONE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankReader.class);
    /** Fichier à lire */
    private File file;
    /** Reader du fichier */
    private BufferedReader reader;
    /** Sequence complete */
    private StringBuilder origin = new StringBuilder();
    /** Liste des chaine représentant des cds */
    private List<StringBuilder> cdsList = new ArrayList<>();
    /** Sous-séquences extraites correspondantes aux CDS (operateurs join et complement déjà appliqué si présent) */
    private List<StringBuilder> processedCdsList = new ArrayList<>();
    /** Nom du replicon */
    private String name = null;
    /** Version du replicon */
    private int version = 0;

    private GenbankReader(File file){
        this.file = file;
    }

    public void process() throws IOException {
        reader = new BufferedReader(new FileReader(file));
        String line;
        while((line = reader.readLine()) != null){
            processLine(line);
        }
    }

    /** Lit une ligne et appelle processSection */
    private void processLine(String line){
        Matcher sectionMatcher = SECTION_PATTERN.matcher(line);
        String sectionName;
        String sectionLine = null;
        if(sectionMatcher.matches()){
            sectionName = sectionMatcher.group(1);
            if(sectionMatcher.groupCount() == 3){
                sectionLine = sectionMatcher.group(3);
            }
            processSection(sectionName, sectionLine);
        }
    }

    /** En fonction du nom de la section, redirige vers la méthode de parsing adéquate */
    private void processSection(@NonNull String name, String value){
        switch (name.toUpperCase()){
            case "VERSION":
                processVersion(value);
                break;
            case "CDS":
                processCDS(value);
                break;
            case "ORIGIN":
                processORIGIN();
                break;
            default:
        }
    }

    /** Parse la valeur de la section VERSION donnée en paramètre */
    private void processVersion(String versionValue){
        Matcher versionMatcher = VERSION_PATTERN.matcher(versionValue);
        if(versionMatcher.matches()){
            name = versionMatcher.group(1);
            version = Integer.parseInt(versionMatcher.group(2));
        }
    }

    /**
     * Parse la section CDS entrain d'être lue.<br>
     * Utilise reader pour avancer dans la lecture de fichier si le CDS est mutli-ligne.
     * @param cdsValue La valeur de la première ligne du CDS déjà lu par le reader à ce stade.
     */
    private void processCDS(String cdsValue){
        //TODO
        //Astuce : si le nombre de parathèse ouvrante et fermante de correspondent pas
        //où si la chaine termine par une virgule, le CDS est multi-ligne
        //Les CDS doivent trié par ordre d'index de démarrage pour que la lecture du ORIGIN
        //soit optimale (càd sans devoir sauvegarder le ORIGIN en entier)
    }

    private class CDS{
        public int debut;
        public int fin;
        public boolean complement;

        public CDS(int deb, int fin, boolean comp){
            this.debut = deb;
            this.fin = fin;
            this.complement = comp;
        }


    }

    /**
     * Parse la section ORIGIN du fichier.
     * Utilise le reader pour avancer jusqu'à la fin du fichier ou jusqu'au tag de terminaison du replicon (//).
     */
    private void processORIGIN(){
        //TODO
        //A ce stade les CDS devraient être tous lus
        //Si la liste des CDS est vide -> ne pas lire cette section car inutile
        //Sinon, attendre d'être sur la ligne ou commence le premier CDS
        //et commecner à enregistrer ce CDS jusqu'as arriver à la fin de ce CDS
        //et passer au CDS suivants.
    }

    public List<StringBuilder> getProcessedCdsList(){
        return processedCdsList;
    }

    public int getValidsCDS(){
        return -1;
    }

    public int getInvalidsCDS(){
        return -1;
    }

    public String getName(){
        return name;
    }

    public int getVersion(){
        return version;
    }

    /**
     * Instancier un lecteur de fichier genbank
     * @param file Le fichier à lire
     * @return Une instance permettant de lire le fichier en paramètre
     */
    public static GenbankReader createInstance(@NonNull File file){
        return new GenbankReader(file);
    }
}
