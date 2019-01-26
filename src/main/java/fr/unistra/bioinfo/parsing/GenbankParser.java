package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.common.EventUtils;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.commons.lang3.StringUtils;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.features.FeatureInterface;
import org.biojava.nbio.core.sequence.features.Qualifier;
import org.biojava.nbio.core.sequence.io.GenbankReaderHelper;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
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

public final class GenbankParser {
    private static HierarchyService hierarchyService;
    private static RepliconService repliconService;

    /** Objet utilisé pour synchroniser le parsing */
    private static final Object synchronizedObject = new Object();

    private static Pattern organismPattern = Pattern.compile("^([\\s]+)?ORGANISM([\\s]+)(.+)([\\s]+)?$");

    public static void setHierarchyService(@NonNull HierarchyService hierarchySerice){
        GenbankParser.hierarchyService = hierarchySerice;
    }

    public static void setRepliconService(@NonNull RepliconService repliconService) {
        GenbankParser.repliconService = repliconService;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankParser.class);

    /**
     * Parse le fichier donné en paramètre et créé ou met à jour le replicon qu'il représente.</br>
     * Normalement thread-safe.
     * @param repliconFile le fichier .gb
     * @return true si le parsing à été correctement effectué, false sinon
     */
    public static boolean parseGenbankFile(@NonNull File repliconFile){
        if(!repliconFile.isFile() || !repliconFile.canRead()){
            LOGGER.error("Fichier '"+repliconFile.getAbsolutePath()+"' introuvable ou droits insuffisants");
            return false;
        }
        RepliconEntity repliconEntity;
        try{
            LinkedHashMap<String, DNASequence> dnaSequences = GenbankReaderHelper.readGenbankDNASequence(repliconFile);
            for(DNASequence seq : dnaSequences.values()){
                List<String> cdsList = new ArrayList<>();
                LOGGER.trace("DNA header : "+seq.getOriginalHeader());
                synchronized(synchronizedObject){
                    repliconEntity = repliconService.getByName(seq.getAccession().getID());
                    if(repliconEntity == null){
                        repliconEntity = new RepliconEntity();
                        repliconEntity.setName(seq.getAccession().getID());
                        HierarchyEntity h = readHierarchy(repliconFile);
                        if(h == null){
                            LOGGER.warn("Impossible de déterminer l'organisme représenté par le fichier '{}', abandon du parsing",repliconFile.getAbsolutePath());
                            return false;
                        }
                        hierarchyService.save(h);
                        repliconEntity.setHierarchyEntity(h);
                        repliconService.save(repliconEntity);
                    }
                }
                repliconEntity.setDownloaded(true);
                repliconEntity.setComputed(false);
                repliconEntity.setVersion(seq.getAccession().getVersion());
                for(FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound> feature : seq.getFeaturesByType("CDS")){
                    String cdsSeq = feature.getLocations().getSubSequence(seq).getSequenceAsString();
                    cdsList.add(cdsSeq);
                }
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
                countFrequencies(cdsList, repliconEntity);
                repliconEntity.setComputed(true);
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

    private static final class LambdaBoolean{
        private boolean value = true;

        void setFalse(){
            value = false;
        }

        void setTrue(){
            value = true;
        }

        boolean get(){
            return value;
        }
    }

    private static boolean countFrequencies(@NonNull List<String> cdsList, @NonNull final RepliconEntity repliconEntity){
        final LambdaBoolean result = new LambdaBoolean();
        cdsList.forEach(cds -> {
            if(!countFrequencies(cds, repliconEntity)){
                result.setFalse();
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
        if(StringUtils.isBlank(sequence)){
            LOGGER.trace("La séquence du replicon '{}' est vide", repliconEntity.getName());
            return false;
        }else if(sequence.length() % 3 != 0){
            LOGGER.trace("La taille de la séquence du replicon '{}' ({}) n'est pas multiple de 3", repliconEntity.getName(), sequence.length());
            return false;
        }else if(!checkStartEndCodons(sequence)){
            LOGGER.trace("La séquence ne commence et/ou ne finis pas par des codons START et END (start : {}, end : {})", sequence.substring(0,3), sequence.substring(sequence.length() - 3));
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
        for(String end : CommonUtils.TRINUCLEOTIDES_STOP){
            if(sequence.endsWith(end)){
                check = true;
                break;
            }
        }
        return check;
    }

    /**
     * Retourne le Hierarchy correspondant au fichier donné en lisant la section ORGANISM du fichier si elle existe.<br/>
     * Si elle n'existe pas, null est renvoyé, sinon le Hierarchy est soit pris dans la BDD, soit créé et sauvegardé s'il n'y est pas.
     * @param repliconFile le fichier à lire
     * @return Le hierarchy correspondant, null si non trouvé
     * @throws IOException En cas de problème de lecture du fichier
     */
    private static HierarchyEntity readHierarchy(@NonNull File repliconFile) throws IOException{
        String organism = readOrganism(repliconFile);
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
    private static String readOrganism(@NonNull File repliconFile) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(repliconFile));
        Iterator<String> it = reader.lines().iterator();
        while(it.hasNext()){
            String l = it.next();
            Matcher m = organismPattern.matcher(l);
            if(m.matches()){
                return m.group(3);
            }
        }
        return null;
    }
}
