package fr.unistra.bioinfo.genbank;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.persistence.manager.HierarchyManager;
import fr.unistra.bioinfo.persistence.manager.RepliconManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GenbankUtilsTest {
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Autowired
    private HierarchyManager hierarchyService;
    @Autowired
    private RepliconManager repliconService;

    @BeforeEach
    public void beforeEach(){
        assertNotNull(hierarchyService);
        assertNotNull(repliconService);
    }

    @Test
    void getNumberOfEntries() {
        for(Reign k : Reign.values()){
            LOGGER.info("Number of "+k.getSearchTable()+" entries : "+GenbankUtils.getNumberOfEntries(k));
        }
    }

    @Test
    void updateDB(){
        try {
            GenbankUtils.updateNCDatabase();
            assertFalse(hierarchyService.getAll().isEmpty());
            assertFalse(repliconService.getAll().isEmpty());
        } catch (IOException e) {
            fail(e);
        }
    }

    @Disabled("Test long à l'éxecution")
    @Test
    void createOrganismsTreeStructure(){
        assertTrue(GenbankUtils.createAllOrganismsDirectories(Paths.get(".","Results")));
    }
}