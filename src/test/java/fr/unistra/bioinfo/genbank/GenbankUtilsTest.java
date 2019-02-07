package fr.unistra.bioinfo.genbank;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.*;
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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({StaticInitializer.class})
@SpringBootTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
class GenbankUtilsTest {
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static Path TEST_DL_PATH = Paths.get("Temp");

    @Autowired
    private HierarchyService hierarchyService;
    @Autowired
    private RepliconService repliconService;

    @BeforeAll
    static void beforeAll(){
        FileUtils.deleteQuietly(TEST_DL_PATH.toFile());
    }

    @AfterAll
    static void afterAll(){
        FileUtils.deleteQuietly(TEST_DL_PATH.toFile());
    }

    @BeforeEach
    void beforeEach(){
        assertNotNull(hierarchyService);
        assertNotNull(repliconService);
        CommonUtils.disableHibernateLogging();
        assertEquals(0, hierarchyService.count().longValue());
        assertEquals(0, repliconService.count().longValue());
        CommonUtils.enableHibernateLogging(true);
    }

    @AfterEach
    void afterEach(){
        CommonUtils.disableHibernateLogging();
        hierarchyService.deleteAll();
        repliconService.deleteAll();
        CommonUtils.enableHibernateLogging(true);
    }

    @Test
    void testRateLimiter(){
        int n = 45;
        try{
            GenbankUtils.updateNCDatabase(45);
        }catch(GenbankException e){
            fail(e);
        }

        final AtomicBoolean oneFailed = new AtomicBoolean(false);
        List<RepliconEntity> replicons = repliconService.getAll(PageRequest.of(0, n)).getContent();
        assertEquals(n, replicons.size());
        final List<File> files = new ArrayList<>(n);
        replicons.parallelStream().forEach((replicon) -> {
            if(!oneFailed.get()){
                URI testDlUri = GenbankUtils.getGBDownloadURL(replicon);
                GenbankUtils.GENBANK_REQUEST_LIMITER.acquire();
                try(InputStream in = testDlUri.toURL().openStream()){
                    File f = TEST_DL_PATH.resolve(replicon.getFileName()).toFile();
                    FileUtils.copyToFile(in, f);
                    files.add(f);
                }catch(IOException e){
                    oneFailed.set(true);
                    LOGGER.error("Erreur lors du téléchargement", e);
                }
            }
        });
        assertFalse(oneFailed.get(), "Au moins un téléchargement à échoué");

        for(File f : files){
            assertTrue(f.isFile());
            assertTrue(f.exists());
            assertTrue(f.canRead());
            try {
                assertTrue(CollectionUtils.isNotEmpty(FileUtils.readLines(f, StandardCharsets.UTF_8)));
            } catch (IOException e) {
                fail(e);
            }
            assertTrue(f.delete());
        }
    }

    @Test
    void getNumberOfEntries() {
        for(Reign k : Reign.values()){
            int n = GenbankUtils.getNumberOfEntries(k);
            assertTrue(n > 0);
            LOGGER.info("Number of "+k.getSearchTable()+" entries : "+n);
        }
    }

    @Test
    void updateDB(){
        try {
            GenbankUtils.updateNCDatabase();
            assertTrue(hierarchyService.count() > 0);
            assertTrue(repliconService.count() > 0);
        } catch (GenbankException e) {
            fail(e);
        }
    }

    @Test
    void downloadReplicons(){
        int PAGE_SIZE = 16;
        try {
            GenbankUtils.updateNCDatabase(PAGE_SIZE);
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


    @Test
    void createOrganismsTreeStructure(){
        try {
            GenbankUtils.updateNCDatabase(10);
        } catch (GenbankException e) {
            fail("Erreur lors de l'update", e);
        }
        assertTrue(GenbankUtils.createAllOrganismsDirectories(Paths.get(".","Results")));
        File dir;
        for(RepliconEntity r : repliconService.getAll()){
            dir = GenbankUtils.getPathOfOrganism(r.getHierarchyEntity()).toFile();
            assertTrue(dir.exists(), dir.getPath()+" n'existe pas");
            assertTrue(dir.isDirectory(), dir.getPath()+" n'est pas un dossier");
        }
    }

    @Test
    void buildNgramString(){
        try{
            URIBuilder uriBuilder = new URIBuilder("https://www.ncbi.nlm.nih.gov/Structure/ngram");
            String queryString = "[display(organism,kingdom,group,subgroup)].from(GenomeAssemblies).matching(tab==[\"Eukaryotes\",\"Viruses\",\"Prokaryotes\"] and organism == \"Felis catus\")";
            assertEquals(queryString, GenbankUtils.buildNgramQueryString(Reign.ALL, "organism == \"Felis catus\"","organism","kingdom","group","subgroup"));
            queryString = "[display()].from(GenomeAssemblies).matching(tab==[\"Eukaryotes\"] and replicons like \"NC_\")";
            assertEquals(queryString, GenbankUtils.buildNgramQueryString(Reign.EUKARYOTES, "replicons like \"NC_\""));
            queryString = "[display()].from(GenomeAssemblies).matching(tab==[\"Prokaryotes\"])";
            uriBuilder.setParameter("q", queryString);
            uriBuilder.setParameter("limit", "1");
            assertEquals(uriBuilder.build().toString(), GenbankUtils.getReignTotalEntriesNumberURL(Reign.PROKARYOTES));
            queryString = "[display(group,subgroup,kingdom)].from(GenomeAssemblies).matching(tab==[\"Viruses\"])";
            uriBuilder.setParameter("q", queryString);
            uriBuilder.setParameter("limit", "0");
            assertEquals(uriBuilder.build().toString(), GenbankUtils.getKingdomCountersURL(Reign.VIRUSES));
            queryString = "[display(organism,kingdom,group,subgroup,replicons)].from(GenomeAssemblies).matching(tab==[\"Eukaryotes\",\"Viruses\",\"Prokaryotes\"])";
            assertEquals(queryString, GenbankUtils.buildNgramQueryString(Reign.ALL, "", "organism", "kingdom", "group", "subgroup", "replicons"));
            queryString = "[display(organism,kingdom,group,subgroup,replicons)].from(GenomeAssemblies).matching(tab==[\"Eukaryotes\",\"Viruses\",\"Prokaryotes\"] and replicons like \"NC_\")";
            assertEquals(queryString, GenbankUtils.buildNgramQueryString(Reign.ALL, "replicons like \"NC_\"", "organism", "kingdom", "group", "subgroup", "replicons"));
        }catch(URISyntaxException e){
            fail(e);
        }
    }

    @Test
    void testDownloadThenUpdateReplicons(){
        try {
            GenbankUtils.updateNCDatabase(10);
            assertTrue(repliconService.count() > 0);
            RepliconEntity replicon = repliconService.getAll().get(0);
            assertNotNull(replicon);
            replicon.setVersion(0);
            replicon.setParsed(true);
            replicon.setComputed(true);
            replicon.setDownloaded(true);
            repliconService.save(replicon);
            GenbankUtils.updateNCDatabase(10);
            replicon = repliconService.getByName(replicon.getName());
            assertTrue(replicon.getVersion() > 0);
            assertFalse(replicon.isParsed());
            assertFalse(replicon.isComputed());
            assertFalse(replicon.isDownloaded());
        } catch (GenbankException e) {
            fail(e);
        }
    }
}