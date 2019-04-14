package fr.unistra.bioinfo.configuration;

import fr.unistra.bioinfo.genbank.GenbankUtils;
import fr.unistra.bioinfo.parsing.AbstractCustomReader;
import fr.unistra.bioinfo.parsing.GenbankParser;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class StaticInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticInitializer.class);

    private final RepliconService repliconService;
    private final HierarchyService hierarchyService;

    @Autowired
    public StaticInitializer(RepliconService repliconService, HierarchyService hierarchyService) {
        this.repliconService = repliconService;
        this.hierarchyService = hierarchyService;
    }

    @PostConstruct
    public void postConstruct(){
        if(hierarchyService == null){
            throw new IllegalStateException("Le HierarchyManager n'a pas été initialisé");
        }
        GenbankUtils.setHierarchyService(hierarchyService);
        GenbankParser.setHierarchyService(hierarchyService);
        if(repliconService == null){
            throw new IllegalStateException("Le RepliconManager n'a pas été initialisé");
        }
        GenbankUtils.setRepliconService(repliconService);
        GenbankParser.setRepliconService(repliconService);
        AbstractCustomReader.setRepliconService(repliconService);
        LOGGER.debug("Initialisation statique OK");
    }
}
