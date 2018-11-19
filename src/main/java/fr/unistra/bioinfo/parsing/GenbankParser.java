package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.commons.lang3.StringUtils;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.features.FeatureInterface;
import org.biojava.nbio.core.sequence.io.GenbankReaderHelper;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GenbankParser {
    private static HierarchyService hierarchyService;
    private static RepliconService repliconService;

    private static Pattern organismPattern = Pattern.compile("^([\\s]+)?ORGANISM([\\s]+)(.+)([\\s]+)?$");

    public static void setHierarchyService(@NonNull HierarchyService hierarchySerice){
        GenbankParser.hierarchyService = hierarchySerice;
    }

    public static void setRepliconService(@NonNull RepliconService repliconService) {
        GenbankParser.repliconService = repliconService;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankParser.class);

    public static void parseGenbankFile(@NonNull File repliconFile){
        if(!repliconFile.isFile() || !repliconFile.canRead()){
            LOGGER.error("Fichier '"+repliconFile.getAbsolutePath()+"' introuvable ou droits insuffisants");
            return;
        }
        RepliconEntity repliconEntity;
        try{
            LinkedHashMap<String, DNASequence> dnaSequences = GenbankReaderHelper.readGenbankDNASequence(repliconFile);
            for(DNASequence seq : dnaSequences.values()){
                LOGGER.debug("DNA sequence : "+seq.getSource());
                LOGGER.debug("DNA header : "+seq.getOriginalHeader());
                repliconEntity = repliconService.getByName(seq.getAccession().getID());
                if(repliconEntity == null){
                    repliconEntity = new RepliconEntity();
                    repliconEntity.setName(seq.getAccession().getID());
                    repliconEntity.setDownloaded(true);
                    HierarchyEntity h = readHierarchy(repliconFile);
                    if(h == null){
                        LOGGER.warn("Impossible de déterminer l'organisme représenté par le fichier '"+repliconFile.getAbsolutePath()+"', abandon du parsing");
                        return;
                    }
                    hierarchyService.save(h);
                    repliconEntity.setHierarchyEntity(h);
                }
                repliconEntity.setVersion(seq.getAccession().getVersion());
                for(FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound> feature : seq.getFeaturesByType("CDS")){
                    LOGGER.debug("CDS : "+feature.getSource());
                    LOGGER.debug("Locations : "+feature.getLocations());
                    LOGGER.debug("CDS sequence : " + feature.getLocations().getSubSequence(seq).getSequenceAsString());
                }
                repliconEntity.setComputed(true);
                repliconService.save(repliconEntity);
            }
        }catch(Exception e){
            LOGGER.error("Erreur de lecture du fichier '"+repliconFile.getPath()+"'");
        }
    }

    private static HierarchyEntity readHierarchy(@NonNull File repliconFile) throws IOException{
        String organism = readOrganism(repliconFile);
        if(StringUtils.isBlank(organism)){
            LOGGER.warn("le fichier '"+repliconFile.getName()+"' ne contient pas de section ORGANISM");
            return null;
        }
        return hierarchyService.getByOrganism(organism, true);
    }

    private static String readOrganism(@NonNull File repliconFile) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(repliconFile));
        Iterator<String> it = reader.lines().iterator();
        while(it.hasNext()){
            String l = it.next();
            Matcher m = organismPattern.matcher(l);
            LOGGER.trace("Parsing : "+l);
            if(m.matches()){
                return m.group(3);
            }
        }
        return null;
    }
}
