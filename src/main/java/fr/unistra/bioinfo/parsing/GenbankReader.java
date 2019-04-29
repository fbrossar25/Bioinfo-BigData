package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.common.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
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

    class Location{
        int begin;
        int end;
        private int length;
        Location(int begin, int end){
            this.begin = begin;
            this.end = end;
            length = end - begin + 1;
            if(length < 1){
                length = 0;
            }
        }

        int length(){
            return length;
        }
    }

    class CDS{
        List<Location> locations = new ArrayList<>();
        boolean complement = false;
        void addLocation(int begin, int end){
            locations.add(new Location(begin, end));
        }

        int length(){
            if(locations.isEmpty()){
                return 0;
            }
            return locations.stream().mapToInt(Location::length).sum();
        }
    }

    /** Fichier à lire */
    private File file;
    /** Reader du fichier */
    private BufferedReader reader = null;
    /** Sous-séquence extraite correspondante aux CDS du fichier (operateurs join et complement déjà appliqués si présent) */
    private StringBuilder processedSequence;
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
    /** Organisme du replicon */
    private String organism;
    /** Taille du origin */
    private int sequenceLength = -1;
    private boolean isTest = false;
    /** Indique que le mot clé FEATURES à été lu */
    private boolean features = false;

    private GenbankReader(File file){
        this.file = file;
    }
    private GenbankReader(BufferedReader reader){
        this.reader = reader;
    }

    public void process() throws IOException {
        if(reader == null){
            if(file == null){
                throw new IOException("Le fichier n'est pas définis");
            }
            reader = new BufferedReader(new FileReader(file));
        }
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
                if(sequenceLength < 1 && value != null){
                    processSource(value);
                }
                break;
            case "FEATURES":
                features = true;
                break;
            case "CDS":
                if(features){
                    processCDS(value);
                }
                break;
            case "ORIGIN":
                if(features){
                    processORIGIN();
                }
                break;
            case "ORGANISM":
                if(StringUtils.isNotBlank(value)){
                    organism = value.trim();
                }
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
        boolean endCds = false;
        boolean invalid = false;
        CDS cds = new CDS();
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
                        cds.complement = true;
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

                    cds.addLocation(start, end);
                }
            }else{
                invalid = true;
            }
        }else{
            throw new RuntimeException("Fichier invalide : La taille de la séquence n'as pas pu être déterminé");
        }
        if(!invalid && !cds.locations.isEmpty()) {
            cdsValid.add(cds);
            nbCdsValid++;
        }else{
            nbCdsInvalid++;
        }
    }

    /**
     * Parse la section ORIGIN du fichier.
     * Utilise le reader pour avancer jusqu'à la end du fichier ou jusqu'au tag de terminaison du replicon (//).
     */
    private void processORIGIN() throws IOException{
        if(cdsValid.isEmpty()){
            return;
        }
        String originLine = reader.readLine();
        //Sous-séquence d'un seul CDS à la fois, ajouté à la liste processedSequences si valide
        StringBuilder fullOrigin = new StringBuilder(sequenceLength > 1 ? sequenceLength : 1000);
        while(originLine != null && !END_TAG.equals(originLine.trim())){
            //Lecture des index de la ligne actuelle
            Matcher m = ORIGIN_LINE_PATTERN.matcher(originLine);
            if(!m.matches()){
                break;
            }
            originLine = m.group(2).trim().replaceAll("\\s+", "");
            fullOrigin.append(originLine);
            originLine = reader.readLine();
        }

        StringBuilder localSubsequence = new StringBuilder();

        List<CDS> invalidsCDS = new ArrayList<>();

        for(CDS cds : cdsValid){
            boolean invalid = false;
            StringBuilder cdsSubsequence = new StringBuilder();
            for(Location l : cds.locations){
                String s = fullOrigin.substring(l.begin-1, l.end);
                for(int i=0; i<s.length(); i++){
                    char c = getChar(cds.complement, s.charAt(i));
                    if(c == '?'){
                        LOGGER.debug("Caractère invalide pour le replicon '{}' dans le CDS '{}' : '{}'", name, cds, s.charAt(i));
                        invalidsCDS.add(cds);
                        invalid = true;
                        break;
                    }else{
                        cdsSubsequence.append(c);
                    }
                }
                if(invalid){
                    break;
                }
            }
            if(!invalid){
                if(cds.complement){
                    cdsSubsequence.reverse();
                }
                localSubsequence.append(cdsSubsequence);
            }
        }
        nbCdsValid -= invalidsCDS.size();
        nbCdsInvalid += invalidsCDS.size();
        cdsValid.removeAll(invalidsCDS);
        invalidsCDS.clear();



        if(checkProcessedSequence(localSubsequence)){
            processedSequence = localSubsequence;
        }else{
            LOGGER.debug("La sous-séquences extraites du replicons '{}' est invalide", name);
            nbCdsInvalid += nbCdsValid;
            nbCdsValid = 0;
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

    private boolean checkProcessedSequence(StringBuilder subSeq){
        if(StringUtils.isBlank(subSeq)){
            LOGGER.trace("Le CDS du replicon '{}' est vide", name);
            return false;
        }if(subSeq.length() % 3 != 0){
            LOGGER.trace("La taille du CDS du replicon '{}' ({}) n'est pas multiple de 3", name, subSeq.length());
            return false;
        }else if(!checkStartEndCodons(subSeq)){
            LOGGER.trace("Le CDS du replicon '{}' ne commence et/ou ne finis pas par des codons START et END (start : {}, end : {})", name, subSeq.subSequence(0,3), subSeq.subSequence(subSeq.length() - 3, subSeq.length()));
            return false;
        }
        return true;
    }

    private static boolean checkStartEndCodons(@NonNull CharSequence sequence) {
        boolean check = false;
        for(String start : CommonUtils.TRINUCLEOTIDES_INIT){
            if(start.contentEquals(sequence.subSequence(0, 3))){
                check = true;
                break;
            }
        }
        if(!check){
            return false;
        }
        check = false;
        int end_start = sequence.length() - 3;
        int end_stop = sequence.length();
        for(String end : CommonUtils.TRINUCLEOTIDES_STOP){
            if(end.contentEquals(sequence.subSequence(end_start, end_stop))){
                check = true;
                break;
            }
        }
        return check;
    }

    public boolean processedSequenceIsValid(){
        return processedSequence != null;
    }

    public StringBuilder getProcessedSequence(){
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

    public String getOrganism(){
        return organism;
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

    /**
     * Instancier un lecteur de fichier genbank
     * @param reader Le buefer à lire
     * @return Une instance permettant de lire le fichier en paramètre
     */
    public static GenbankReader createInstance(@NonNull BufferedReader reader){
        return new GenbankReader(reader);
    }

    static GenbankReader createInstance(@NonNull File file, boolean isTest){
        GenbankReader gbReader =  new GenbankReader(file);
        gbReader.isTest = true;
        return  gbReader;
    }
}
