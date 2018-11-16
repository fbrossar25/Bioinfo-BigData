package fr.unistra.bioinfo.parsing;

import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.features.FeatureInterface;
import org.biojava.nbio.core.sequence.io.GenbankReaderHelper;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public final class GenbankParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankParser.class);

    public static void parseGenbankFile(@NonNull File repliconFile){
        if(!repliconFile.isFile() || !repliconFile.canRead()){
            LOGGER.error("Fichier '"+repliconFile.getAbsolutePath()+"' introuvable ou droits insuffisants");
            return;
        }
        boolean readingCds = false;
        boolean readingOrigin = false;
        StringBuilder accu = new StringBuilder(16);
        List<CDS> cdsList = new ArrayList<>();
        try{
            LinkedHashMap<String, DNASequence> dnaSequences = GenbankReaderHelper.readGenbankDNASequence(repliconFile);
            for(DNASequence seq : dnaSequences.values()){
                for(FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound> feature : seq.getFeaturesByType("CDS")){
                    LOGGER.info("CDS : "+feature.getSource());
                    LOGGER.info("Locations : "+feature.getLocations());
                    LOGGER.info("CDS sequence : " + feature.getLocations().getSubSequence(seq).getSequenceAsString());
                }
            }
        }catch(Exception e){
            LOGGER.error("Erreur de lecture du fichier '"+repliconFile.getPath()+"'");
        }
    }

    private static String getOriginFromFile(@NonNull File repliconFile){
        try{
            HashMap<String, DNASequence> sequences = GenbankReaderHelper.readGenbankDNASequence(repliconFile);
            StringBuilder sb = new StringBuilder(8192);
            for(DNASequence seq : sequences.values()){
                sb.append(seq.getSequenceAsString());
            }
            return sb.toString();
        }catch(Exception e){
            LOGGER.error("Erreur de récupération du ORIGIN du fichier '"+repliconFile.getAbsolutePath()+"'");
            return null;
        }
    }
}
