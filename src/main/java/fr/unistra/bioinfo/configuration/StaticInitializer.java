package fr.unistra.bioinfo.configuration;

import fr.unistra.bioinfo.genbank.GenbankUtils;
import fr.unistra.bioinfo.persistence.manager.HierarchyManager;
import fr.unistra.bioinfo.persistence.manager.RepliconManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class StaticInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticInitializer.class);

    private final RepliconManager repliconManager;
    private final HierarchyManager hierarchyManager;

    @Autowired
    public StaticInitializer(RepliconManager repliconManager, HierarchyManager hierarchyManager) {
        this.repliconManager = repliconManager;
        this.hierarchyManager = hierarchyManager;
    }

    @PostConstruct
    public void postConstruct(){
        if(hierarchyManager == null){
            throw new IllegalStateException("Le HierarchyManager n'a pas été initialisé");
        }
        GenbankUtils.setHierarchyManager(hierarchyManager);
        if(repliconManager == null){
            throw new IllegalStateException("Le RepliconManager n'a pas été initialisé");
        }
        GenbankUtils.setRepliconManager(repliconManager);
        LOGGER.debug("Initialisation statique OK");
    }
}
