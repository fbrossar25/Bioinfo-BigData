package fr.unistra.bioinfo.genbank;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({StaticInitializer.class})
@SpringBootTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
class GenbankUtilsTest {
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Autowired
    private HierarchyService hierarchyService;
    @Autowired
    private RepliconService repliconService;

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
            assertTrue(hierarchyService.count() > 0);
            assertTrue(repliconService.count() > 0);
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