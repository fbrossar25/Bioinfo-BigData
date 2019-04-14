package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.biojava.nbio.core.sequence.compound.DNACompoundSet;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.features.AbstractFeature;
import org.biojava.nbio.core.sequence.io.DNASequenceCreator;
import org.biojava.nbio.core.sequence.io.template.SequenceCreatorInterface;
import org.biojava.nbio.core.sequence.location.InsdcLocations;
import org.biojava.nbio.core.sequence.location.template.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCustomReader {
    protected static RepliconService repliconService;
    protected final File file;
    protected final Object synchronizedObject = new Object();
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCustomReader.class);
    private boolean isClosed = false;
    protected SequenceCreatorInterface<NucleotideCompound> sequenceCreator = new DNASequenceCreator(DNACompoundSet.getDNACompoundSet());

    AbstractCustomReader(File f){
        this.file = f;
    }

    protected boolean checkCDSFeature(AbstractFeature f, int segLength, String repliconName){
        //Élimination des CDS invalides, conservation des CDS valides
        List<Location> checkedLocations = new ArrayList<>();
        if(!f.getLocations().getSubLocations().isEmpty()){
            //Si le CDS est composé
            for(Location subLocation : f.getLocations().getSubLocations()){
                if(subLocation.getStart().getPosition() >= 0 && subLocation.getEnd().getPosition() < segLength){
                    checkedLocations.add(subLocation);
                }else{
                    LOGGER.debug("CDS invalides : replicon '{}' dans le fichier '{}' -> '{}' (taille sequence : {})", repliconName, file.getName(), subLocation, segLength);
                }
            }
        }else if(f.getLocations().getStart().getPosition() >= 0 && f.getLocations().getEnd().getPosition() < segLength){
            //Si le CDS n'est pas composé
            checkedLocations.add(f.getLocations());
        }else{
            //Si le CDS non composé est invalide
            LOGGER.debug("CDS invalide : replicon '{}' dans le fichier '{}' -> '{}' (taille sequence : {})", repliconName, file.getName(), f.getLocations(), segLength);
        }

        if(checkedLocations.isEmpty()){
            return false;
        }else{
            //On garde les CDS valides
            f.setLocation(new InsdcLocations.GroupLocation(checkedLocations));
            return true;
        }
    }

    protected abstract void closeImpl();

    public void close(){
        if(!isClosed){
            isClosed = true;
            closeImpl();
        }
    }

    /**
     * Retourne le replicon du nom donné. Le créé s'il n'existe pas ainsi que le hierarchy associé.
     * @param replicon Le nom du replicon
     * @return Le replicon, où null s'il n'as pas pu être créé
     * @see GenbankParser#createReplicon(File, String)
     */
    RepliconEntity getOrCreateReplicon(String replicon){
        RepliconEntity entity;
        synchronized(synchronizedObject){
            entity = repliconService.getByName(replicon);
        }
        if(entity == null){
            try {
                return GenbankParser.createReplicon(file, replicon);
            } catch (IOException e) {
                LOGGER.error("Erreur lors de la lecture du fichier", e);
                return null;
            }
        }else{
            return entity;
        }
    }

    public static void setRepliconService(RepliconService repliconService){
        CustomGenbankReader.repliconService = repliconService;
    }
}
