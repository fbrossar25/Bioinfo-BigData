package fr.unistra.bioinfo.genbank;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconType;
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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

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
    @Disabled("Test pour la vitesse de dl")
    void testDownloadNumerousFile(){
        CommonUtils.disableHibernateLogging();
        GenbankUtils.updateNCDatabase(0);
        CompletableFuture<List<RepliconEntity>> future = new CompletableFuture<>();
        long begin = System.currentTimeMillis();
        try {
            GenbankUtils.downloadAllReplicons(future);
            future.get();
            long end = System.currentTimeMillis();
            LOGGER.info("Le téléchargement pris {}.", LocalTime.MIN.plus(end-begin, ChronoUnit.MILLIS).toString());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Une erreur est survenue.", e);
            fail(e);
        }
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
        assertNotNull(replicons);
        assertEquals(n, replicons.size());
        final List<File> files = new ArrayList<>(n);
        replicons.parallelStream().forEach((replicon) -> {
            if(!oneFailed.get()){
                URI testDlUri = GenbankUtils.getGBDownloadURL(replicon);
                GenbankUtils.GENBANK_REQUEST_LIMITER.acquire();
                try(InputStream in = testDlUri.toURL().openStream()){
                    File f = TEST_DL_PATH
                            .resolve(replicon.getGenbankName()+".gb")
                            .toFile();
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
            GenbankUtils.updateNCDatabase(0);
            assertTrue(hierarchyService.count() > 0);
            assertTrue(repliconService.count() > 0);
            LOGGER.info("Méta-données de {} replicons chargées", repliconService.count());
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
            CompletableFuture<List<RepliconEntity>> future = new CompletableFuture<>();
            GenbankUtils.downloadReplicons(replicons, future);
            assertFalse(future.get().isEmpty());
        } catch (InterruptedException | ExecutionException e) {
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
    void getRepliconTypeFromRepliconNameTest(){
        assertEquals(RepliconType.MITOCHONDRION, GenbankUtils.getRepliconTypeFromRepliconName("NC_001700"));
        assertEquals(RepliconType.CHROMOSOME, GenbankUtils.getRepliconTypeFromRepliconName("NC_007112"));
        assertEquals(RepliconType.PLASMID, GenbankUtils.getRepliconTypeFromRepliconName("NC_001889"));
        assertEquals(RepliconType.LINKAGE, GenbankUtils.getRepliconTypeFromRepliconName("NC_037638"));
        assertEquals(RepliconType.PLAST, GenbankUtils.getRepliconTypeFromRepliconName("NC_007758"));
    }

    @Test
    @Disabled("A des fin de benchmarks uniquement")
    void testMultiThreadsDownloadSpeed(){
        GenbankUtils.updateNCDatabase(0);
        RepliconEntity r = repliconService.getByName("NC_016088");
        assertNotNull(r);
        try {
            CompletableFuture<List<RepliconEntity>> future = new CompletableFuture<>();
            long begin = System.currentTimeMillis();
            GenbankUtils.downloadReplicons(Collections.singletonList(r), future);
            List<RepliconEntity> replicons = future.get();
            long end = System.currentTimeMillis();
            assertEquals(1, replicons.size());
            long duration = end - begin;
            LOGGER.info("Temps de téléchargement pour 1 fichier : {} secondes", duration/1000);

            future = new CompletableFuture<>();
            begin = System.currentTimeMillis();
            GenbankUtils.downloadReplicons(Arrays.asList(r,r,r,r,r,r,r,r,r,r), future);
            replicons = future.get();
            end = System.currentTimeMillis();
            assertEquals(3, replicons.size());
            duration = end - begin;
            LOGGER.info("Temps de téléchargement pour {} fichier : {} secondes",replicons.size(), duration/1000);
        } catch (InterruptedException |ExecutionException e) {
            fail(e);
        }
    }

    @Test
    void testDownloadThenUpdateReplicons(){
        try {
            GenbankUtils.updateNCDatabase(1);
            RepliconEntity r = repliconService.getAll().get(0);
            assertNotNull(r);
            List<RepliconEntity> entities = new ArrayList<>(1);
            entities.add(r);
            CompletableFuture<List<RepliconEntity>> future = new CompletableFuture<>();
            GenbankUtils.downloadReplicons(entities, future);
            future.get();
            assertTrue(repliconService.count() > 0);
            RepliconEntity replicon = repliconService.getAll().get(0);
            assertNotNull(replicon);
            replicon.setVersion(0);
            repliconService.save(replicon);
            GenbankUtils.updateNCDatabase(1);
            replicon = repliconService.getByName(replicon.getName());
            assertTrue(replicon.getVersion() > 0);
            assertFalse(replicon.isParsed());
        } catch (GenbankException | InterruptedException | ExecutionException e) {
            fail(e);
        }
    }

    @Test
    void testDownloadThenDeleteRepliconsFile(){
        try {
            GenbankUtils.updateNCDatabase(1);
            CompletableFuture<List<RepliconEntity>> future = new CompletableFuture<>();
            GenbankUtils.downloadAllReplicons(future);
            future.get();
            assertTrue(repliconService.count() > 0);
            RepliconEntity replicon = repliconService.getAll().get(0);
            assertNotNull(replicon);
        } catch (GenbankException | InterruptedException | ExecutionException e) {
            fail(e);
        }
    }

    @Test
    void testRepliconsIdsString(){
        RepliconEntity testReplicon = new RepliconEntity();
        testReplicon.setName("NC_0123");
        testReplicon.setVersion(0);
        List<RepliconEntity> replicons = new ArrayList<>(5);
        IntStream.range(0,5).forEach(i -> replicons.add(testReplicon));
        assertEquals("NC_0123.0,NC_0123.0,NC_0123.0,NC_0123.0,NC_0123.0", GenbankUtils.getRepliconsIdsString(replicons));
    }
}