package fr.unistra.bioinfo.genbank;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

    @Test
    void downloadReplicons(){
        int PAGE_SIZE = 64;
        try {
            GenbankUtils.updateNCDatabase();
            List<RepliconEntity> replicons = repliconService.getAll(PageRequest.of(0, PAGE_SIZE)).getContent();
            assertEquals(PAGE_SIZE, replicons.size());
            CompletableFuture<List<File>> future = new CompletableFuture<>();
            GenbankUtils.downloadReplicons(replicons, future);
            assertFalse(future.get().isEmpty());
            for(File f : future.get()){
                assertTrue(f.exists());
                assertTrue(f.canRead());
                assertTrue(CollectionUtils.isNotEmpty(FileUtils.readLines(f, StandardCharsets.UTF_8)));
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            fail(e);
        }
    }


    @Disabled("Test long à l'éxecution")
    @Test
    void createOrganismsTreeStructure(){
        assertTrue(GenbankUtils.createAllOrganismsDirectories(Paths.get(".","Results")));
    }
}