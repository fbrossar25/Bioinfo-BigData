package fr.unistra.bioinfo.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final Pattern INTERVAL_PATTERN = Pattern.compile("^(\\d+)\\.\\.(\\d+)$");
    private static final Pattern ORIGIN_LINE_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*(.+?)\\s*$");
    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankReader.class);

    class CDS{
        public int begin;
        public int end;
        public boolean complement;
        List<CDS> linkedCDS = new ArrayList<>();

        CDS(int begin, int end, boolean comp){
            this.begin = begin;
            this.end = end;
            this.complement = comp;
        }
    }

    /** Fichier à lire */
    private File file;
    /** Reader du fichier */
    private BufferedReader reader;
    /** Sous-séquences extraites correspondantes aux CDS (operateurs join et complement déjà appliqué si présent) */
    private StringBuilder processedSequence = new StringBuilder();
    /** Nom du replicon */
    private String name = null;
    /** Liste de tous les CDS valides*/
    private List<CDS> cdsValid = new ArrayList<>();
    /** Version du replicon */
    private int version = 0;
    /** Nombre de cds invalides */
    private int nbCdsInvalid = 0;
    /** Nombre de cds valides */
    private int nbCdsValid = 0;
    /** Taille du origin */
    private int sequenceLength = -1;

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
    private void processLine(String line) throws IOException{
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
    private void processSection(@NonNull String name, String value) throws IOException{
        switch (name){
            case "VERSION":
                processVersion(value);
                break;
            case "source":
                if(sequenceLength < 1){
                    processSource(value);
                }
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

    private void processSource(@NonNull String value){
        Matcher m = INTERVAL_PATTERN.matcher(value);
        if(m.matches()){
            sequenceLength = Integer.parseInt(m.group(2));
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
    private void processCDS(String cdsValue) throws IOException{
        //TODO
        //Astuce : si le nombre de parathèse ouvrante et fermante de correspondent pas
        //où si la chaine termine par une virgule, le CDS est multi-ligne
        //Les CDS doivent trié par ordre d'index de démarrage pour que la lecture du ORIGIN
        //soit optimale (càd sans devoir sauvegarder le ORIGIN en entier)
        boolean endCds = false;
        boolean complement = false;
        boolean invalid = false;
        List<CDS> listCds = new ArrayList<>();
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
            for(CDS cds : listCds){
                cds.linkedCDS.addAll(listCds);
            }
            nbCdsValid++;
        }else{
            nbCdsInvalid++;
        }
        listCds.clear();
    }

    /**
     * Parse la section ORIGIN du fichier.
     * Utilise le reader pour avancer jusqu'à la end du fichier ou jusqu'au tag de terminaison du replicon (//).
     */
    private void processORIGIN() throws IOException{
        //TODO
        //A ce stade les CDS devraient être tous lus
        //Si la liste des CDS est vide -> ne pas lire cette section car inutile
        //Sinon, attendre d'être sur la ligne ou commence le premier CDS
        //et commecner à enregistrer ce CDS jusqu'as arriver à la end de ce CDS
        //et passer au CDS suivants.
        if(cdsValid.isEmpty()){
            return;
        }
        sortAndcheckCDSIntervalValidity();
        CDS currentCDS = cdsValid.get(0);
        int cdsIdx = 0;
        String originLine = reader.readLine();
        //Index de la premiere lettre de la ligne courante
        int currentLineBeginIdx = 0;
        //Index de la derniere lettre de la ligne courante
        int currentLineEndIdx = 0;
        int currentReadIdx = 0;
        StringBuilder localSubSequence = new StringBuilder();

        while(originLine != null && !END_TAG.equals(originLine.trim())){
            //Lecture des index de la ligne actuelle
            Matcher m = ORIGIN_LINE_PATTERN.matcher(originLine);
            if(!m.matches()){
                break;
            }
            currentLineBeginIdx = currentLineEndIdx + 1;
            originLine = m.group(2).trim().replaceAll("\\s+", "");
            currentLineEndIdx = currentLineBeginIdx + originLine.length() - 1;

            //Le CDS commence dans la ligne
            while(currentLineBeginIdx <= currentCDS.begin && currentCDS.begin <= currentLineEndIdx){
                currentReadIdx = currentCDS.begin;
                int realReadIdx = currentReadIdx - currentLineBeginIdx;
                while(currentReadIdx <= currentLineEndIdx){

                    char c = getChar(currentCDS.complement, originLine.charAt(realReadIdx));
                    if(c == '?'){
                        //CDS suivant si le CDS courant est invalide
                        localSubSequence.setLength(0);
                        nbCdsInvalid++;
                        nbCdsValid--;
                        cdsValid.removeAll(currentCDS.linkedCDS);
                        cdsValid.sort(Comparator.comparingInt(cds -> cds.begin));
                        //On avance pas l'index car, la liste étant trié, un autre cds viens à la place automatiquement
                        if(cdsIdx >= cdsValid.size()){
                            return;
                        }
                        currentCDS = cdsValid.get(cdsIdx);
                        break;
                    }
                    localSubSequence.append(c);
                    currentReadIdx++;
                    realReadIdx++;

                    if(currentReadIdx > currentCDS.end){
                        processedSequence.append(localSubSequence);
                        localSubSequence.setLength(0);
                        //CDS suivant si l'on à terminé
                        cdsIdx++;
                        if(cdsIdx >= cdsValid.size()){
                            return;
                        }
                        currentCDS = cdsValid.get(cdsIdx);
                    }
                }
                if(realReadIdx >= originLine.length()){
                    //Le CDS continue à la ligne suivante
                    break;
                }
            }

            //Ligne suivante
            originLine = reader.readLine();
        }
    }

    /**
     * Trie la liste de cds par ordre croissant des débuts des CDS et supprime les CDS qui démarre ou termine en même qu'un autre
     */
    private void sortAndcheckCDSIntervalValidity(){
        cdsValid.sort(Comparator.comparingInt(cds -> cds.begin));
        if(cdsValid.size() > 1){
            List<CDS> checkedCDSList = new ArrayList<>(cdsValid);
            CDS cds;
            for(int i=1; i<cdsValid.size(); i++){
                cds = cdsValid.get(i);
                if(!checkedCDSList.contains(cds)){
                    continue;
                }
                if(i + 1 < cdsValid.size() && cds.end >= cdsValid.get(i+1).begin){
                    //Si ce CDS termine après que le cds suivant aient démarré
                    nbCdsInvalid++;
                    nbCdsValid--;
                    checkedCDSList.removeAll(cds.linkedCDS);
                }else if(cds.begin <= cdsValid.get(i - 1).end){
                    //Si le  CDS commence avant que le cds précédent n'ais terminé
                    nbCdsInvalid++;
                    nbCdsValid--;
                    checkedCDSList.removeAll(cds.linkedCDS);
                }
            }
            cdsValid = checkedCDSList;
        }
    }

    private char getChar(boolean complement, char c) {
        if(complement){
            switch (c){
                case 'a':
                case 'A': return 'T';
                case 't':
                case 'T': return 'A';
                case 'c':
                case 'C': return 'G';
                case 'g':
                case 'G': return 'C';
                default : return '?';
            }
        }
        switch (c){
            case 'a':
            case 'A': return 'A';
            case 't':
            case 'T': return 'T';
            case 'c':
            case 'C': return 'C';
            case 'g':
            case 'G': return 'G';
            default : return '?';
        }
    }

    public StringBuilder getProcessedSubsequence(){
        return processedSequence;
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

    public int getSequenceLength(){
        return sequenceLength;
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
