package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.genbank.GenbankUtils;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.Phase;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconType;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GenbankParser {
    private static HierarchyService hierarchyService;
    private static RepliconService repliconService;

    /** Objet utilisé pour synchroniser le parsing */
    private static final Object synchronizedObject = new Object();

    private static final Pattern ORGANISM_PATTERN = Pattern.compile("^([\\s]+)?ORGANISM([\\s]+)(.+)([\\s]+)?$");

    private static final Pattern ACCESSION_PATTERN = Pattern.compile("^([\\s]+)?ACCESSION([\\s]+)(.+)([\\s]+)?$");
    private static final Pattern ACGT_PATTERN = Pattern.compile("^[ACGT]+$");

    public static void setHierarchyService(@NonNull HierarchyService hierarchySerice){
        GenbankParser.hierarchyService = hierarchySerice;
    }

    public static void setRepliconService(@NonNull RepliconService repliconService) {
        GenbankParser.repliconService = repliconService;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankParser.class);

    private synchronized static RepliconEntity getOrCreateRepliconEntity(String repliconName, File repliconFile){
        RepliconEntity repliconEntity = repliconService.getByName(repliconName);
        if(repliconEntity == null) {
            try {
                repliconEntity = createReplicon(repliconFile, repliconName);
            } catch (IOException e) {
                LOGGER.error("Création du replicon '{}' à partir du fichier '{}' impossible.", repliconName, repliconFile.getName(), e);
            }
        }
        return repliconEntity;
    }

    /**
     * Parse le fichier donné en paramètre et créé ou met à jour le replicon qu'il représente.</br>
     * Normalement thread-safe.
     * @param repliconFile le fichier .gb
     * @return true si le parsing à été correctement effectué, false sinon, et false si 0 replicon étaient présents
     */
    public static boolean parseGenbankFile(@NonNull File repliconFile){
        if(!repliconFile.isFile() || !repliconFile.canRead()){
            LOGGER.error("Fichier '"+repliconFile.getAbsolutePath()+"' introuvable ou droits insuffisants");
            return false;
        }
        try{
            GenbankReader gbReader = GenbankReader.createInstance(repliconFile);
            gbReader.process();
            RepliconEntity repliconEntity;
            String repliconName = gbReader.getName();
            repliconEntity = getOrCreateRepliconEntity(repliconName, repliconFile);
            if(repliconEntity == null){
                LOGGER.warn("replicons '{}' introuvable", repliconName);
                return false;
            }
            repliconEntity.setFileName(repliconFile.getName());
            repliconEntity.setComputed(false);
            repliconEntity.setParsed(false);
            repliconEntity.setVersion(gbReader.getVersion());
            repliconEntity.setValidsCDS(gbReader.getValidsCDS());
            repliconEntity.setInvalidsCDS(gbReader.getInvalidsCDS());
            synchronized(synchronizedObject){
                repliconService.save(repliconEntity);
            }
            if(RepliconType.DNA.equals(repliconEntity.getType())){
                repliconEntity.setType(GenbankUtils.getRepliconTypeFromRepliconName(repliconName));
            }
            countFrequencies(gbReader.getProcessedSubsequences(), repliconEntity);
            countPrefPhases(repliconEntity);
            repliconEntity.setParsed(true);
            synchronized(synchronizedObject){
                repliconService.save(repliconEntity);
            }
        }catch(Exception e){
            LOGGER.error("Erreur de lecture du fichier '{}'", repliconFile.getPath(), e);
            return false;
        }
        return true;
    }

    private static void countPrefPhases(RepliconEntity replicon){
        for(String dinucleotide : CommonUtils.DINUCLEOTIDES.keySet()){
            int p0 = replicon.getDinucleotideCount(dinucleotide, Phase.PHASE_0);
            int p1 = replicon.getDinucleotideCount(dinucleotide, Phase.PHASE_1);
            if(p0 > p1){
                replicon.setPhasesPrefsDinucleotide(dinucleotide, Phase.PHASE_0);
            }else if(p1 > p0) {
                replicon.setPhasesPrefsDinucleotide(dinucleotide, Phase.PHASE_1);
            }else if(p0 != 0){
                replicon.setPhasesPrefsDinucleotide(dinucleotide, Phase.PHASE_0, Phase.PHASE_1);
            }else{
                replicon.setPhasesPrefsDinucleotide(dinucleotide);
            }
        }
        for(String trinucleotide : CommonUtils.TRINUCLEOTIDES.keySet()){
            int p0 = replicon.getTrinucleotideCount(trinucleotide, Phase.PHASE_0);
            int p1 = replicon.getTrinucleotideCount(trinucleotide, Phase.PHASE_1);
            int p2 = replicon.getTrinucleotideCount(trinucleotide, Phase.PHASE_2);
            if(p0 > p1){
                if(p0 > p2){
                    replicon.setPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_0);
                }else if(p2 > p0){
                    replicon.setPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_2);
                }else{
                    replicon.setPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_0, Phase.PHASE_2);
                }
            }else if(p1 > p0 || p2 > p0) {
                if (p1 > p2) {
                    replicon.setPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_1);
                } else if (p2 > p1) {
                    replicon.setPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_2);
                } else {
                    replicon.setPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_1, Phase.PHASE_2);
                }
            }else if(p0 != 0){
                // Toutes les phase pref
                replicon.setPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_0, Phase.PHASE_1, Phase.PHASE_2);
            }else{
                // Aucun trinucleotide -> pas de phase pref
                replicon.setPhasesPrefsTrinucleotide(trinucleotide);
            }
        }
    }

    private static boolean countFrequencies(@NonNull List<StringBuilder> cdsList, @NonNull final RepliconEntity repliconEntity){
        final AtomicBoolean result = new AtomicBoolean();
        repliconEntity.resetCounters();
        cdsList.forEach(cds -> {
            if(countFrequencies(cds, repliconEntity)){
                repliconEntity.incrementValidsCDS();
            }else{
                result.set(false);
                repliconEntity.incrementInvalidsCDS();
            }
        });
        return result.get();
    }

    /**
     * Compte les fréquences des di/trinucléotides dans la séquence donnée et<br/>
     * met à jour le Replicon donné.
     * @param sequence La séquence d'ADN au format (ACGT)
     * @param repliconEntity le replicon qui seras mis à jour si le cds est correct
     * @return true si le cds est valide et pris en compte, false sinon
     */
    private static boolean countFrequencies(@NonNull CharSequence sequence, @NonNull final RepliconEntity repliconEntity) {
        if(StringUtils.isBlank(sequence)){
            LOGGER.trace("Le CDS du replicon '{}' est vide", repliconEntity.getName());
            return false;
        }else if(!ACGT_PATTERN.matcher(sequence).matches()){
            LOGGER.trace("Le CDS du replicon '{}' n'est pas uniquement constitué des lettres ACGT", repliconEntity.getName());
            return false;
        }else if(sequence.length() % 3 != 0){
            LOGGER.trace("La taille du CDS du replicon '{}' ({}) n'est pas multiple de 3", repliconEntity.getName(), sequence.length());
            return false;
        }else if(!checkStartEndCodons(sequence)){
            LOGGER.trace("Le CDS du replicon '{}' ne commence et/ou ne finis pas par des codons START et END (start : {}, end : {})", repliconEntity.getName(), sequence.subSequence(0,3), sequence.subSequence(sequence.length() - 3, sequence.length()));
            return false;
        }
        int iMax = sequence.length() - 3;
        for(int i=0; i<iMax; i+=3){
            repliconEntity.incrementTrinucleotideCount(sequence.subSequence(i, i+3).toString(), Phase.PHASE_0);
            repliconEntity.incrementTrinucleotideCount(sequence.subSequence(i+1, i+4).toString(), Phase.PHASE_1);
            repliconEntity.incrementTrinucleotideCount(sequence.subSequence(i+2, i+5).toString(), Phase.PHASE_2);
        }
        iMax = ((sequence.length() % 2) == 0) ? sequence.length() - 4 : sequence.length() - 3;
        for(int i=0; i<iMax; i+=3){
            repliconEntity.incrementDinucleotideCount(sequence.subSequence(i, i+2).toString(), Phase.PHASE_0);
            repliconEntity.incrementDinucleotideCount(sequence.subSequence(i+1, i+3).toString(), Phase.PHASE_1);
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

    /**
     * Créé et sauvegarde le replicon à partir du fichier fournis et de son nom. Créer également le HierarchyEntity associé s'il n'existe pas.
     * @param repliconFile Le fichier genbank
     * @param repliconName Le nom du replicon
     * @return Le repliconEntity créé où null
     * @throws IOException Si une erreur arrive lors de la lecture du fichier
     */
    static RepliconEntity createReplicon(@NonNull File repliconFile, @NonNull String repliconName) throws IOException{
        RepliconEntity repliconEntity = new RepliconEntity();
        repliconEntity.setName(repliconName);
        repliconEntity.setFileName(repliconFile.getName());
        HierarchyEntity h = readHierarchy(repliconFile, repliconEntity.getName());
        if(h == null){
            LOGGER.warn("Impossible de déterminer l'organisme représenté par le fichier '{}', abandon du parsing",repliconFile.getAbsolutePath());
            return null;
        }
        synchronized(synchronizedObject){
            hierarchyService.save(h);
            repliconEntity.setHierarchyEntity(h);
            repliconEntity.setType(GenbankUtils.getRepliconTypeFromRepliconName(repliconEntity.getName()));
            repliconService.save(repliconEntity);
        }
        return repliconEntity;
    }

    /**
     * Retourne le Hierarchy correspondant au fichier donné en lisant la section ORGANISM du fichier si elle existe.<br/>
     * Si elle n'existe pas, null est renvoyé, sinon le Hierarchy est soit pris dans la BDD, soit créé et sauvegardé s'il n'y est pas.
     * @param repliconFile le fichier à lire
     * @return Le hierarchy correspondant, null si non trouvé
     * @throws IOException En cas de problème de lecture du fichier
     */
    private static HierarchyEntity readHierarchy(@NonNull File repliconFile, @NonNull String repliconName) throws IOException{
        String organism = readOrganism(repliconFile, repliconName);
        if(StringUtils.isBlank(organism)){
            LOGGER.warn("le fichier '{}' ne contient pas de section ORGANISM", repliconFile.getName());
            return null;
        }
        return hierarchyService.getByOrganism(organism, true);
    }

    /**
     * Retourne la valeur de la section ORGANISM du fichier donné
     * @param repliconFile le fichier à lire
     * @return Le nom de l'organisme, null si la section n'existe pas ou est vide
     * @throws IOException En cas d'erreur de lecture du fichier
     */
    private static String readOrganism(@NonNull File repliconFile, @NonNull String repliconName) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(repliconFile));
        boolean isCorrectAccession = false;
        Iterator<String> it = reader.lines().iterator();
        Matcher m;
        while(it.hasNext()){
            String l = it.next();
            m = ACCESSION_PATTERN.matcher(l);
            if(m.matches()){
                isCorrectAccession = repliconName.equalsIgnoreCase(m.group(3));
            }
            if(isCorrectAccession){
                m = ORGANISM_PATTERN.matcher(l);
                if(m.matches()){
                    return m.group(3);
                }
            }
        }
        return null;
    }
}
