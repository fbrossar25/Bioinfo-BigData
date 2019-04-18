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
    private static final Pattern CDS_PATTERN = Pattern.compile("^\\s*(join\\(|complement\\((join\\()?)?((,?[<>]*\\d+[<>]*\\.\\.[<>]*\\d+[<>]*)+)\\)*\\s*$");

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
    /** Liste de tous les CDS valides*/
    private ArrayList<CDS> cdsValid = new ArrayList<>();
    /** Version du replicon */
    private int version = 0;
    /** Nombre de cds invalides */
    private int nbCdsInvalid = 0;
    /** Nombre de cds valides */
    private int nbCdsValid = 0;
    /** Taille de la séquence */
    private int sequenceLength = 17009;

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
        boolean endCds = false;
        boolean complement = false;
        boolean invalid = false;
        ArrayList<CDS> listCds = new ArrayList<CDS>();
        try {
            if (sequenceLength > 0){
                StringBuilder cdsEntier = new StringBuilder();
                while (!endCds) {
                    String cdsTrim = cdsValue.trim();
                    cdsEntier.append(cdsTrim);
                    if (cdsTrim.endsWith(",")) {
                        cdsValue = reader.readLine();
                    } else {
                        endCds = true;
                    }
                }
                Matcher cdsMatcher = CDS_PATTERN.matcher(cdsEntier);
                if (cdsMatcher.matches()) {
                    String operator = cdsMatcher.group(1);
                    String content = cdsMatcher.group(3);
                    if (operator != null) {
                        if (operator.contains("complement")) {
                            complement = true;
                        }
                    }

                    String[] intervals = content.split(",");
                    for (String interval : intervals) {
                        String[] extremity = interval.split("\\.\\.");
                        int start = Integer.parseInt(extremity[0].replaceAll("[<>]+",""));
                        int end = Integer.parseInt(extremity[1].replaceAll("[<>]+",""));
                        if ((start > end) || (end > sequenceLength)) {
                            invalid = true;
                            break;
                        }

                        listCds.add(new CDS(start, end, complement));
                    }
                }else{
                    invalid = true;
                }
            }else{
                throw new RuntimeException("Fichier invalide");
            }
            if(!invalid) {
                cdsValid.addAll(listCds);
                nbCdsValid++;
            }else{
                nbCdsInvalid++;
            }
            listCds.clear();

        }catch(Exception e){

        }
    }

    class CDS{
        public int begin;
        public int end;
        public boolean complement;

        public CDS(int begin, int end, boolean comp){
            this.begin = begin;
            this.end = end;
            this.complement = comp;
        }


    }

    /**
     * Parse la section ORIGIN du fichier.
     * Utilise le reader pour avancer jusqu'à la end du fichier ou jusqu'au tag de terminaison du replicon (//).
     */
    private void processORIGIN(){
        //TODO
        //A ce stade les CDS devraient être tous lus
        //Si la liste des CDS est vide -> ne pas lire cette section car inutile
        //Sinon, attendre d'être sur la ligne ou commence le premier CDS
        //et commecner à enregistrer ce CDS jusqu'as arriver à la end de ce CDS
        //et passer au CDS suivants.
    }

    public List<StringBuilder> getProcessedCdsList(){
        return processedCdsList;
    }

    public int getValidsCDS(){
        return this.nbCdsValid;
    }

    public List<CDS> getListCDSValid(){
        return this.cdsValid;
    }

    public int getInvalidsCDS(){
        return this.nbCdsInvalid;
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
