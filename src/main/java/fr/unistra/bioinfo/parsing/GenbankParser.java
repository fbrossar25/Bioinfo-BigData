package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.File;

public final class GenbankParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankParser.class);

    public RepliconEntity parseGenbankFile(@NonNull File f){
        throw new NotImplementedException("TODO");
    }
}
