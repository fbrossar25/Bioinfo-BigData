package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.genbank.GenbankUtils;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.Phase;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconType;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.AfterEach;
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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({StaticInitializer.class})
@SpringBootTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
class GenbankParserTest {
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Path GENBANK_BATCH_REAL_FILE_PATH = Paths.get("src", "test", "resources", "replicons-0.gb");
    private static final Path GENBANK_TEST_NOT_ONLY_ACGT_FILE_PATH = Paths.get("src", "test", "resources", "not-only-acgt.gb");
    private static final Path GENBANK_BATCH_VOID_FILE_PATH = Paths.get("src", "test", "resources", "void.gb");
    private static final Path GENBANK_BATCH_ONLY_END_TAGS_FILE_PATH = Paths.get("src", "test", "resources", "only-end-tags.gb");
    private static final Path GENBANK_TEST_FILE_PATH = Paths.get("src", "test", "resources", "NC_001700.1.gb");

    @Autowired
    private RepliconService repliconService;
    @Autowired
    private HierarchyService hierarchyService;

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
    void testShouldCountDinucleotidesCorrectly(){
        RepliconEntity r = new RepliconEntity("test", null);
        GenbankParser.countFrequencies("ACGTCCTAA", r);
        GenbankParser.countFrequencies("ACCATATAA", r);
        assertEquals(2, r.getDinucleotideCount("AC", Phase.PHASE_0).longValue());
        assertEquals(1, r.getDinucleotideCount("GT", Phase.PHASE_0).longValue());
        assertEquals(1, r.getDinucleotideCount("CC", Phase.PHASE_0).longValue());
        assertEquals(1, r.getDinucleotideCount("TA", Phase.PHASE_0).longValue());
        assertEquals(1, r.getDinucleotideCount("CA", Phase.PHASE_0).longValue());


        assertEquals(1, r.getDinucleotideCount("CG", Phase.PHASE_1).longValue());
        assertEquals(1, r.getDinucleotideCount("TC", Phase.PHASE_1).longValue());
        assertEquals(1, r.getDinucleotideCount("CT", Phase.PHASE_1).longValue());
        assertEquals(0, r.getDinucleotideCount("AA", Phase.PHASE_1).longValue());
        assertEquals(1, r.getDinucleotideCount("CC", Phase.PHASE_1).longValue());
        assertEquals(2, r.getDinucleotideCount("AT", Phase.PHASE_1).longValue());

        assertEquals(6, r.getTotalDinucleotides(Phase.PHASE_0).longValue());
        assertEquals(6, r.getTotalDinucleotides(Phase.PHASE_1).longValue());
    }

    @Test
    void testShouldCountTrinucleotidesCorrectly(){
        RepliconEntity r = new RepliconEntity("test", null);
        GenbankParser.countFrequencies("ACGTCCTAA", r);
        GenbankParser.countFrequencies("ACCATATAA", r);
        assertEquals(1, r.getTrinucleotideCount("ACG", Phase.PHASE_0).longValue());
        assertEquals(1, r.getTrinucleotideCount("TCC", Phase.PHASE_0).longValue());
        assertEquals(1, r.getTrinucleotideCount("ACC", Phase.PHASE_0).longValue());
        assertEquals(1, r.getTrinucleotideCount("ATA", Phase.PHASE_0).longValue());
        assertEquals(0, r.getTrinucleotideCount("TAA", Phase.PHASE_0).longValue(), "Les trinucleotides STOP ne doivent pas être comptés");

        assertEquals(1, r.getTrinucleotideCount("CGT", Phase.PHASE_1).longValue());
        assertEquals(1, r.getTrinucleotideCount("CCT", Phase.PHASE_1).longValue());
        assertEquals(1, r.getTrinucleotideCount("CCA", Phase.PHASE_1).longValue());
        assertEquals(1, r.getTrinucleotideCount("TAT", Phase.PHASE_1).longValue());


        assertEquals(1, r.getTrinucleotideCount("GTC", Phase.PHASE_2).longValue());
        assertEquals(1, r.getTrinucleotideCount("CTA", Phase.PHASE_2).longValue());
        assertEquals(1, r.getTrinucleotideCount("CAT", Phase.PHASE_2).longValue());
        assertEquals(1, r.getTrinucleotideCount("ATA", Phase.PHASE_2).longValue());

        assertEquals(4, r.getTotalTrinucleotides(Phase.PHASE_0).longValue());
        assertEquals(4, r.getTotalTrinucleotides(Phase.PHASE_1).longValue());
        assertEquals(4, r.getTotalTrinucleotides(Phase.PHASE_2).longValue());
    }

    @Test
    void testFileWithCDSKeywordInComment(){
        CommonUtils.disableHibernateLogging();
        GenbankUtils.updateNCDatabase();
        CompletableFuture<List<RepliconEntity>> future = new CompletableFuture<>();
        GenbankUtils.downloadReplicons(Collections.singletonList(repliconService.getByName("NC_001700")),future);
        try {
            assertNotNull(future.get());
            assertFalse(future.get().isEmpty());
            assertTrue(future.get().get(0).isParsed());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            fail(e);
        }
        CommonUtils.enableHibernateLogging(true);
    }

    @Test
    void testParseFuckedUpButRealBatchFile(){
        CommonUtils.disableHibernateLogging();
        File f = GENBANK_BATCH_REAL_FILE_PATH.toFile();
        assertTrue(GenbankParser.parseGenbankFile(f));
        List<RepliconEntity> replicons = repliconService.getAll();
        assertEquals(1, replicons.size());
        List<String> invalidsReplicons = Collections.singletonList("NC_037304");
        for(RepliconEntity r : replicons){
            assertTrue(r.isParsed());
            if(invalidsReplicons.contains(r.getName())){
                assertEquals(29, r.getValidsCDS().intValue());
                assertEquals(4, r.getInvalidsCDS().intValue());
            }
        }
        CommonUtils.enableHibernateLogging(true);
    }

    @Test
    void testParseVoidFile(){
        CommonUtils.disableHibernateLogging();
        File f = GENBANK_BATCH_VOID_FILE_PATH.toFile();
        assertFalse(GenbankParser.parseGenbankFile(f));
        CommonUtils.enableHibernateLogging(true);
    }

    @Test
    void testParseOnlyEndTagsFile(){
        CommonUtils.disableHibernateLogging();
        File f = GENBANK_BATCH_ONLY_END_TAGS_FILE_PATH.toFile();
        assertFalse(GenbankParser.parseGenbankFile(f));
        CommonUtils.enableHibernateLogging(true);
    }

    void checkReplicon(RepliconEntity r){
        assertNotNull(r, "Replicon null");
        assertTrue(r.isParsed(), "Replicon non parsé");
        assertFalse(r.isComputed(), "Stats Replicon générées");
        boolean phasePrefChceck = false;
        for(Phase p : Phase.values()){
            if(p == Phase.PHASE_2)
                continue;
            for(String dinucleotide : CommonUtils.DINUCLEOTIDES.keySet()){
                Long pref = r.getPhasePrefDinucleotide(dinucleotide, p);
                assertTrue(pref >= 0 || pref <= r.getValidsCDS());
                if(pref > 0){
                    phasePrefChceck = true;
                    break;
                }
            }
        }
        assertTrue(phasePrefChceck, "Phase pref dinucleotides KO");
        phasePrefChceck = false;
        for(Phase p : Phase.values()){
            for(String trinucleotide : CommonUtils.TRINUCLEOTIDES.keySet()){
                Long pref = r.getPhasePrefTrinucleotide(trinucleotide, p);
                assertTrue(pref >= 0 || pref <= r.getValidsCDS());
                if(pref > 0){
                    phasePrefChceck = true;
                    break;
                }
            }
        }
        assertTrue(phasePrefChceck, "Phase pref trinucleotides KO");
        assertEquals(1, r.getVersion().intValue());
        HierarchyEntity h = r.getHierarchyEntity();
        assertNotNull(h, "Hierarchy null");
        assertEquals("Eukaryota", h.getKingdom());
        assertEquals("Animals", h.getGroup());
        assertEquals("Mammals", h.getSubgroup());
        assertEquals("Felis catus", h.getOrganism());
        assertEquals(RepliconType.MITOCHONDRION, r.getType());
        assertEquals(6, r.getInvalidsCDS().intValue(), "Comptages CDS invalides KO");
        assertEquals(7, r.getValidsCDS().intValue(), "Comptage CDS valides KO");
        assertTrue(r.getDinucleotideCount("GG", Phase.PHASE_0) > 0, "Comptage dinucleotides KO");
        assertEquals(r.getDinucleotideCount("GG", Phase.PHASE_0), r.getDinucleotideCount("gg", Phase.PHASE_0), "Comptage dinucleotides sensible à la casse");
    }

    @Test
    void parseGenbankFile() {
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH.toFile()));
        RepliconEntity r = repliconService.getByName("NC_001700");
        checkReplicon(r);
        LOGGER.info(ToStringBuilder.reflectionToString(r, ToStringStyle.MULTI_LINE_STYLE));
    }

    @Test
    void parseNotOnlyAcgtGenbankFile() {
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_TEST_NOT_ONLY_ACGT_FILE_PATH.toFile()));
        RepliconEntity r = repliconService.getByName("NC_001700");
        checkReplicon(r);
        LOGGER.info(ToStringBuilder.reflectionToString(r, ToStringStyle.MULTI_LINE_STYLE));
    }

    private void checkRepliconBenchmark(){
        assertEquals(1, hierarchyService.count().longValue());
        HierarchyEntity h = hierarchyService.getByOrganism("Felis catus");
        assertNotNull(h);
        List<RepliconEntity> replicons = repliconService.getByHierarchy(h);
        CommonUtils.enableHibernateLogging(true);
        assertNotNull(replicons);
        assertEquals(1, replicons.size());
        RepliconEntity r = replicons.get(0);
        for(Phase p : Phase.values()){
            if(p != Phase.PHASE_2){
                for (String dinucleotide : CommonUtils.DINUCLEOTIDES.keySet()) {
                    Long count = r.getPhasePrefDinucleotide(dinucleotide, p);
                    assertNotNull(count);
                    assertTrue(count == 0 || count == 1);
                }
            }
            for (String trinucleotide : CommonUtils.TRINUCLEOTIDES.keySet()) {
                Long count = r.getPhasePrefTrinucleotide(trinucleotide, p);
                assertNotNull(count);
                assertTrue(count == 0 || count == 1);
            }
        }
        assertTrue(r.isParsed());
        assertFalse(r.isComputed());
        assertEquals(RepliconType.MITOCHONDRION, r.getType());
    }

    //Entre 1min 30 et 4min, core i5, 2 coeurs physiques, 4 logiques, 2.8Ghz
    @Test
    @Disabled("Ce test est un benchmark à lancer manuellement.")
    void parse10kGenbankFileBenchmark(){
        CommonUtils.disableHibernateLogging();
        Iterator<Integer> it = IntStream.range(0, 10000).iterator();
        File f = GENBANK_TEST_FILE_PATH.toFile();
        long begin = System.currentTimeMillis();
        while(it.hasNext()){
            assertTrue(GenbankParser.parseGenbankFile(f));
            it.next();
        }
        long end = System.currentTimeMillis();
        LOGGER.info("Le parsing de 10000 fichier à pris "+(end - begin)+"ms");
        checkRepliconBenchmark();
    }

    //Entre 50 secondes et 1min 30, core i5, 2 coeurs physiques, 4 logiques, 2.8Ghz
    @Test
    @Disabled("Ce test est un benchmark à lancer manuellement.")
    void parse10kGenbankFileBenchmarkParallel(){
        CommonUtils.disableHibernateLogging();
        final File f = GENBANK_TEST_FILE_PATH.toFile();
        long begin = System.currentTimeMillis();
        IntStream.range(0, 10000).parallel().forEach(i -> {
            try{
                assertTrue(GenbankParser.parseGenbankFile(f));
            }catch(Exception e){
                LOGGER.error("Erreur de parsing", e);
                fail(e);
            }
        });
        long end = System.currentTimeMillis();
        LOGGER.info("Le parsing de 10000 fichier à pris "+(end - begin)+"ms");
        checkRepliconBenchmark();
    }

    @Test
    @Disabled("Tests à des fins de debuggage")
    void parseDatas(){
        GenbankUtils.updateNCDatabase(50);
        CompletableFuture<List<RepliconEntity>> future = new CompletableFuture<>();
        GenbankUtils.downloadAllReplicons(future);
        List<RepliconEntity> replicons;
        try {
            replicons = future.get(20, TimeUnit.MINUTES);
            assertNotNull(replicons, "Pas de replicons trouvés");
            assertFalse(replicons.isEmpty(), "0 replicons trouvés");
            assertTrue(repliconService.count() > 0);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            fail("Erreur lors du téléchargement", e);
        }
        File dataDir = CommonUtils.DATAS_PATH.toFile();
        assertNotNull(dataDir);
        assertTrue(dataDir.exists() && dataDir.isDirectory());
    }

    @Test
    void testParseSameReplicon(){
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH.toFile()));
        RepliconEntity replicon = repliconService.getByName("NC_001700");
        assertNotNull(replicon);
        assertNotNull(replicon.getCounters());
        ArrayList<Long> trinucleotides = new ArrayList<>(replicon.getCounters().getTrinucleotides());
        ArrayList<Long> trinucleotides_pref = new ArrayList<>(replicon.getCounters().getTrinucleotides_pref());
        ArrayList<Long> dinucleotides = new ArrayList<>(replicon.getCounters().getDinucleotides());
        ArrayList<Long> dinucleotides_pref = new ArrayList<>(replicon.getCounters().getDinucleotides_pref());
        GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH.toFile());
        replicon = repliconService.getByName("NC_001700");
        assertEquals(trinucleotides, replicon.getCounters().getTrinucleotides());
        assertEquals(trinucleotides_pref, replicon.getCounters().getTrinucleotides_pref());
        assertEquals(dinucleotides, replicon.getCounters().getDinucleotides());
        assertEquals(dinucleotides_pref, replicon.getCounters().getDinucleotides_pref());
    }
}