package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.genbank.GenbankUtils;
import fr.unistra.bioinfo.persistence.entity.CountersEntity;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.Phase;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GenbankParser {
    private static HierarchyService hierarchyService;
    private static RepliconService repliconService;

    /** Objet utilisé pour synchroniser le parsing */
    private static final Object synchronizedObject = new Object();
    private static final Pattern ORGANISM_PATTERN = Pattern.compile("^([\\s]+)?ORGANISM([\\s]+)(.+)([\\s]+)?$");
    private static final Pattern ACCESSION_PATTERN = Pattern.compile("^([\\s]+)?ACCESSION([\\s]+)(.+)([\\s]+)?$");

    public static void setHierarchyService(@NonNull HierarchyService hierarchySerice){
        GenbankParser.hierarchyService = hierarchySerice;
    }

    public static void setRepliconService(@NonNull RepliconService repliconService) {
        GenbankParser.repliconService = repliconService;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankParser.class);

    private synchronized static RepliconEntity getOrCreateRepliconEntity(String repliconName, GenbankReader gbReader){
        RepliconEntity repliconEntity = repliconService.getByName(repliconName);
        if(repliconEntity == null) {
            try {
                repliconEntity = createReplicon(gbReader, repliconName);
            } catch (IOException e) {
                LOGGER.error("Création du replicon '{}' impossible.", repliconName, e);
            }
        }
        return repliconEntity;
    }

    public static boolean parseGenbankFile(@NonNull BufferedReader reader, RepliconEntity replicon) throws IOException{
        GenbankReader gbReader = GenbankReader.createInstance(reader);
        gbReader.process();
        String repliconName = gbReader.getName();
        RepliconEntity repliconEntity;
        if(replicon != null){
            repliconEntity = replicon;
        }else{
            repliconEntity = getOrCreateRepliconEntity(repliconName, gbReader);
        }
        if(repliconEntity == null){
            LOGGER.warn("replicons '{}' introuvable", repliconName);
            return false;
        }
        repliconEntity.setComputed(false);
        repliconEntity.setParsed(false);
        repliconEntity.setVersion(gbReader.getVersion());
        repliconEntity.setValidsCDS(gbReader.getValidsCDS());
        repliconEntity.setInvalidsCDS(gbReader.getInvalidsCDS());
        synchronized(synchronizedObject){
            repliconService.save(repliconEntity);
        }
        if(repliconEntity.getType() == null){
            repliconEntity.setType(GenbankUtils.getRepliconTypeFromRepliconName(repliconName));
        }
        countFrequencies(gbReader.getProcessedSubsequences(), repliconEntity);
        repliconEntity.setParsed(true);
        synchronized(synchronizedObject){
            repliconService.save(repliconEntity);
        }
        return true;
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
        try(BufferedReader reader = new BufferedReader(new FileReader(repliconFile))){
            return parseGenbankFile(reader, null);
        }catch (Exception e){
            LOGGER.error("Erreur lors du parsing du fichier {}", repliconFile, e);
            return false;
        }
    }

    static void countPrefPhases(RepliconEntity replicon, CountersEntity cdsCounters){
        for(String dinucleotide : CommonUtils.DINUCLEOTIDES.keySet()){
            Long p0 = cdsCounters.getDinucleotideCount(dinucleotide, Phase.PHASE_0);
            Long p1 = cdsCounters.getDinucleotideCount(dinucleotide, Phase.PHASE_1);
            if(p0 > p1){
                replicon.incrementPhasesPrefsDinucleotide(dinucleotide, Phase.PHASE_0);
            }else if(p1 > p0) {
                replicon.incrementPhasesPrefsDinucleotide(dinucleotide, Phase.PHASE_1);
            }else if(p0 != 0){
                replicon.incrementPhasesPrefsDinucleotide(dinucleotide, Phase.PHASE_0, Phase.PHASE_1);
            }else{
                replicon.incrementPhasesPrefsDinucleotide(dinucleotide);
            }
        }
        for(String trinucleotide : CommonUtils.TRINUCLEOTIDES.keySet()){
            Long p0 = cdsCounters.getTrinucleotideCount(trinucleotide, Phase.PHASE_0);
            Long p1 = cdsCounters.getTrinucleotideCount(trinucleotide, Phase.PHASE_1);
            Long p2 = cdsCounters.getTrinucleotideCount(trinucleotide, Phase.PHASE_2);
            if(p0 > p1){
                if(p0 > p2){
                    replicon.incrementPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_0);
                }else if(p2 > p0){
                    replicon.incrementPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_2);
                }else{
                    replicon.incrementPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_0, Phase.PHASE_2);
                }
            }else if(p1 > p0 || p2 > p0) {
                if (p1 > p2) {
                    replicon.incrementPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_1);
                } else if (p2 > p1) {
                    replicon.incrementPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_2);
                } else {
                    replicon.incrementPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_1, Phase.PHASE_2);
                }
            }else if(p0 != 0){
                // Toutes les phase pref
                replicon.incrementPhasesPrefsTrinucleotide(trinucleotide, Phase.PHASE_0, Phase.PHASE_1, Phase.PHASE_2);
            }else{
                // Aucun trinucleotide -> pas de phase pref
                replicon.incrementPhasesPrefsTrinucleotide(trinucleotide);
            }
        }
    }

    private static void countFrequencies(@NonNull List<StringBuilder> cdsList, @NonNull final RepliconEntity repliconEntity){
        repliconEntity.resetCounters();
        for(StringBuilder cds : cdsList){
            countPrefPhases(repliconEntity, countFrequencies(cds, repliconEntity));
        }
    }

    /**
     * Compte les fréquences et phases préférentielles des di/trinucléotides dans la séquence donnée et<br/>
     * met à jour le Replicon donné.
     * @param sequence La séquence d'ADN au format (ACGT)
     * @param repliconEntity le replicon qui seras mis à jour si le cds est correct
     * @return true si le cds est valide et pris en compte, false sinon
     */
    static CountersEntity countFrequencies(@NonNull CharSequence sequence, @NonNull final RepliconEntity repliconEntity) {
        CountersEntity counters = new CountersEntity();
        LOGGER.trace("CDS Replicon '{}' : {}", repliconEntity.getGenbankName(), sequence);
        int iMax = sequence.length() - 3;
        for(int i=0; i<iMax; i+=3){
            repliconEntity.incrementTrinucleotideCount(sequence.subSequence(i, i+3).toString(), Phase.PHASE_0);
            repliconEntity.incrementTrinucleotideCount(sequence.subSequence(i+1, i+4).toString(), Phase.PHASE_1);
            repliconEntity.incrementTrinucleotideCount(sequence.subSequence(i+2, i+5).toString(), Phase.PHASE_2);
            counters.incrementTrinucleotideCount(sequence.subSequence(i, i+3).toString(), Phase.PHASE_0);
            counters.incrementTrinucleotideCount(sequence.subSequence(i+1, i+4).toString(), Phase.PHASE_1);
            counters.incrementTrinucleotideCount(sequence.subSequence(i+2, i+5).toString(), Phase.PHASE_2);
        }
        iMax = ((sequence.length() % 2) == 0) ? sequence.length() - 4 : sequence.length() - 3;
        for(int i=0; i<iMax; i+=2){
            repliconEntity.incrementDinucleotideCount(sequence.subSequence(i, i+2).toString(), Phase.PHASE_0);
            repliconEntity.incrementDinucleotideCount(sequence.subSequence(i+1, i+3).toString(), Phase.PHASE_1);
            counters.incrementDinucleotideCount(sequence.subSequence(i, i+2).toString(), Phase.PHASE_0);
            counters.incrementDinucleotideCount(sequence.subSequence(i+1, i+3).toString(), Phase.PHASE_1);
        }
        return counters;
    }

    /**
     * Créé et sauvegarde le replicon à partir du fichier fournis et de son nom. Créer également le HierarchyEntity associé s'il n'existe pas.
     * @param repliconName Le nom du replicon
     * @return Le counters créé où null
     * @throws IOException Si une erreur arrive lors de la lecture du fichier
     */
    static RepliconEntity createReplicon(@NonNull GenbankReader gbReader, @NonNull String repliconName) throws IOException{
        RepliconEntity repliconEntity = new RepliconEntity();
        repliconEntity.setName(repliconName);
        HierarchyEntity h = readHierarchy(gbReader, repliconEntity.getName());
        if(h == null){
            LOGGER.warn("Impossible de déterminer l'organisme du replicon '{}', abandon du parsing",repliconName);
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
     * @return Le hierarchy correspondant, null si non trouvé
     * @throws IOException En cas de problème de lecture du fichier
     */
    private static HierarchyEntity readHierarchy(@NonNull GenbankReader gbReader, @NonNull String repliconName) throws IOException{
        String organism = gbReader.getOrganism();
        if(StringUtils.isBlank(organism)){
            LOGGER.warn("Le replicon '{}' ne contient pas de section ORGANISM", repliconName);
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
