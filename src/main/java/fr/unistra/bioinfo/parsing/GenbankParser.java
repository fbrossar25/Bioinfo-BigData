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
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.Strand;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.features.FeatureInterface;
import org.biojava.nbio.core.sequence.features.Qualifier;
import org.biojava.nbio.core.sequence.location.template.Location;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
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
    private static final Pattern REPLICON_ACCESSION_PATTERN = Pattern.compile("((NC_\\d+?)\\.(\\d))");
    private static final Pattern REPLICON_NAME_PATTERN_ONLY = Pattern.compile("^(NC_\\d+?)$");

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
            Map<String, DNASequence> dnaSequences = bioJAVAReadDNASequences(repliconFile);
            LOGGER.debug("{} séquences dans '{}'", dnaSequences.size(), repliconFile.getAbsolutePath());
            if(dnaSequences.size() == 0){
                return false;
            }
            for(DNASequence seq : dnaSequences.values()){
                RepliconEntity repliconEntity;
                String repliconName;
                LOGGER.trace("DNA header : "+seq.getOriginalHeader());
                repliconName = seq.getAccession().getID();
                repliconEntity = getOrCreateRepliconEntity(repliconName, repliconFile);
                if(repliconEntity == null){
                    LOGGER.warn("replicons '{}' introuvable", repliconName);
                    continue;
                }
                repliconEntity.setFileName(repliconFile.getName());
                repliconEntity.setComputed(false);
                repliconEntity.setParsed(false);
                repliconEntity.setVersion(seq.getAccession().getVersion());
                synchronized(synchronizedObject){
                    repliconService.save(repliconEntity);
                }
                List<String> cdsList = extractCdsFromSequence(seq, repliconName);
                if(seq.getFeaturesByType("source").size() > 0){
                    FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound> source = seq.getFeaturesByType("source").get(0);
                    for (Map.Entry<String, List<Qualifier>> entry : source.getQualifiers().entrySet()) {
                        switch(entry.getKey()){
                            case "organelle":
                                List<Qualifier> qualifiers = entry.getValue();
                                if(!qualifiers.isEmpty() && "plastid".equals(entry.getValue().get(0).getValue())){
                                    repliconEntity.setType(RepliconType.PLAST);
                                } else {
                                    repliconEntity.setType(RepliconType.MITOCHONDRION);
                                }
                                break;
                            case "chromosome": repliconEntity.setType(RepliconType.CHROMOSOME); break;
                            case "linkage_group": repliconEntity.setType(RepliconType.LINKAGE); break;
                            case "plasmid": repliconEntity.setType(RepliconType.PLASMID); break;
                            default:
                        }
                    }
                }
                countFrequencies(cdsList, repliconEntity);
                countPrefPhases(repliconEntity);
                repliconEntity.setParsed(true);
                synchronized(synchronizedObject){
                    repliconService.save(repliconEntity);
                }
            }
        }catch(Exception e){
            LOGGER.error("Erreur de lecture du fichier '{}'", repliconFile.getPath(), e);
            return false;
        }
        return true;
    }

    private static String extractSubSequence(DNASequence seq, Location location){
        if(Strand.NEGATIVE.equals(location.getStrand())){
            //Par défaut BioJAVA renvoie le reverse complement à la place du complement simple
            List<NucleotideCompound> compounds = seq.getSubSequence(location.getStart().getPosition(), location.getEnd().getPosition()).getAsList();
            StringBuilder builder = new StringBuilder(compounds.size());
            for(NucleotideCompound c : compounds){
                builder.append(c.getComplement().toString());
            }
            return builder.toString();
        }
        return location.getSubSequence(seq).getSequenceAsString();
    }

    private static List<String> extractCdsFromSequence(DNASequence seq, String repliconName) {
        List<String> cdsList = new ArrayList<>();
        for(FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound> feature : seq.getFeaturesByType("CDS")){
            try{
                if(feature.getLocations().getSubLocations().isEmpty()){
                    cdsList.add(extractSubSequence(seq, feature.getLocations()));
                }else{
                    StringBuilder cdsBuilder = new StringBuilder();
                    for(Location loc : feature.getLocations().getSubLocations()){
                        cdsBuilder.append(extractSubSequence(seq, loc));
                    }
                    cdsList.add(cdsBuilder.toString());
                }
            }catch(NullPointerException e){
                LOGGER.error("Erreur lors de la lecture du cds '{}' du replicon '{}'", feature.getLocations(), repliconName, e);
            }
        }
        return cdsList;
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

    private static boolean countFrequencies(@NonNull List<String> cdsList, @NonNull final RepliconEntity repliconEntity){
        final AtomicBoolean result = new AtomicBoolean();
        repliconEntity.resetCounters();
        cdsList.forEach(cds -> {
            if(!countFrequencies(cds, repliconEntity)){
                result.set(false);
                repliconEntity.incrementInvalidsCDS();
            }else{
                repliconEntity.incrementValidsCDS();
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
    private static boolean countFrequencies(@NonNull String sequence, @NonNull final RepliconEntity repliconEntity) {
        LOGGER.trace("CDS du replicon '{}' : {}", repliconEntity.getName(), sequence);
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
            LOGGER.trace("Le CDS du replicon '{}' ne commence et/ou ne finis pas par des codons START et END (start : {}, end : {})", repliconEntity.getName(), sequence.substring(0,3), sequence.substring(sequence.length() - 3));
            return false;
        }
        int iMax = sequence.length() - 3;
        for(int i=0; i<iMax; i+=3){
            repliconEntity.incrementTrinucleotideCount(sequence.substring(i, i+3), Phase.PHASE_0);
            repliconEntity.incrementTrinucleotideCount(sequence.substring(i+1, i+4), Phase.PHASE_1);
            repliconEntity.incrementTrinucleotideCount(sequence.substring(i+2, i+5), Phase.PHASE_2);
        }
        iMax = ((sequence.length() % 2) == 0) ? sequence.length() - 4 : sequence.length() - 3;
        for(int i=0; i<iMax; i+=3){
            repliconEntity.incrementDinucleotideCount(sequence.substring(i, i+2), Phase.PHASE_0);
            repliconEntity.incrementDinucleotideCount(sequence.substring(i+1, i+3), Phase.PHASE_1);
        }
        return true;
    }

    private static boolean checkStartEndCodons(String sequence) {
        boolean check = false;
        for(String start : CommonUtils.TRINUCLEOTIDES_INIT){
            if(sequence.startsWith(start)){
                check = true;
                break;
            }
        }
        if(!check){
            return false;
        }
        check = false;
        for(String end : CommonUtils.TRINUCLEOTIDES_STOP){
            if(sequence.endsWith(end)){
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

    private static Map<String, DNASequence> bioJAVAReadDNASequences(@NonNull File file){
        try{
            return new CustomGenbankReader(file).process();
        }catch(IOException e) {
            LOGGER.error("Erreur lors du parsing du fichier '{}'", file.getAbsolutePath(), e);
            return new HashMap<>();
        }
    }
}
