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
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.features.FeatureInterface;
import org.biojava.nbio.core.sequence.features.Qualifier;
import org.biojava.nbio.core.sequence.io.GenbankReaderHelper;
import org.biojava.nbio.core.sequence.template.AbstractSequence;
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
import java.util.*;
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
    private static final Path GENBANK_BACTH_TEST_FILE_PATH = Paths.get("src", "test", "resources", "replicons-batch-test.gb");
    private static final Path GENBANK_HUGE_FILE = Paths.get("src", "test", "resources","huge.gb");

    private static final String NC_FELIS_CATUS = "NC_001700";
    private static final Path COMPLEMENT_TEST_GB = Paths.get("src", "test", "resources", "complement-test.gb");

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
    void testHugeFileParsing(){
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_HUGE_FILE.toFile()));
    }

    @Test
    void testParseFuckedUpButRealBatchFile(){
        CommonUtils.disableHibernateLogging();
        File f = GENBANK_BATCH_REAL_FILE_PATH.toFile();
        assertTrue(GenbankParser.parseGenbankFile(f));
        List<RepliconEntity> replicons = repliconService.getAll();
        assertEquals(6, replicons.size());
        List<String> invalidsReplicons = Arrays.asList("NC_003071", "NC_003074", "NC_003075", "NC_003076");
        for(RepliconEntity r : replicons){
            assertTrue(r.isParsed());
            if(invalidsReplicons.contains(r.getName())){
                assertEquals(0, r.getValidsCDS().intValue());
                assertEquals(0, r.getInvalidsCDS().intValue());
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
                Integer pref = r.getPhasePrefDinucleotide(dinucleotide, p);
                assertTrue(pref == 0 || pref == 1);
                if(pref == 1){
                    phasePrefChceck = true;
                    break;
                }
            }
        }
        assertTrue(phasePrefChceck, "Phase pref dinucleotides KO");
        phasePrefChceck = false;
        for(Phase p : Phase.values()){
            for(String trinucleotide : CommonUtils.TRINUCLEOTIDES.keySet()){
                Integer pref = r.getPhasePrefTrinucleotide(trinucleotide, p);
                assertTrue(pref == 0 || pref == 1);
                if(pref == 1){
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
        assertEquals(7, r.getInvalidsCDS().intValue(), "Comptages CDS invalides KO");
        assertEquals(6, r.getValidsCDS().intValue(), "Comptage CDS valides KO");
        assertTrue(r.getDinucleotideCount("GG", Phase.PHASE_0) > 0, "Comptage dinucleotides KO");
        assertEquals(r.getDinucleotideCount("GG", Phase.PHASE_0), r.getDinucleotideCount("gg", Phase.PHASE_0), "Comptage dinucleotides sensible à la casse");
    }

    @Test
    void parseGenbankBatchFile() {
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_BACTH_TEST_FILE_PATH.toFile()));
        assertEquals(5, repliconService.count().intValue());
        List<RepliconEntity> replicons = repliconService.getAll();
        for(RepliconEntity r : replicons){
            checkReplicon(r);
        }
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
                    Integer count = r.getPhasePrefDinucleotide(dinucleotide, p);
                    assertNotNull(count);
                    assertTrue(count == 0 || count == 1);
                }
            }
            for (String trinucleotide : CommonUtils.TRINUCLEOTIDES.keySet()) {
                Integer count = r.getPhasePrefTrinucleotide(trinucleotide, p);
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
        CompletableFuture<List<File>> future = new CompletableFuture<>();
        GenbankUtils.downloadAllReplicons(future);
        List<File> files;
        try {
            files = future.get(20, TimeUnit.MINUTES);
            assertNotNull(files, "Pas de fichier trouvés");
            assertFalse(files.isEmpty(), "0 Fichiers trouvés");
            for(File f : files){
                assertTrue(GenbankParser.parseGenbankFile(f));
            }
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
        ArrayList<Integer> trinucleotides = new ArrayList<>(replicon.getCounters().getTrinucleotides());
        ArrayList<Integer> trinucleotides_pref = new ArrayList<>(replicon.getCounters().getTrinucleotides_pref());
        ArrayList<Integer> dinucleotides = new ArrayList<>(replicon.getCounters().getDinucleotides());
        ArrayList<Integer> dinucleotides_pref = new ArrayList<>(replicon.getCounters().getDinucleotides_pref());
        GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH.toFile());
        replicon = repliconService.getByName("NC_001700");
        assertEquals(trinucleotides, replicon.getCounters().getTrinucleotides());
        assertEquals(trinucleotides_pref, replicon.getCounters().getTrinucleotides_pref());
        assertEquals(dinucleotides, replicon.getCounters().getDinucleotides());
        assertEquals(dinucleotides_pref, replicon.getCounters().getDinucleotides_pref());
    }

    @Test
    void complementJoinParsingTest(){
        assertTrue(GenbankParser.parseGenbankFile(COMPLEMENT_TEST_GB.toFile()));
        RepliconEntity r = repliconService.getByName(NC_FELIS_CATUS);
        assertNotNull(r);
        assertNotNull(r.getCounters());
        assertEquals(2, r.getValidsCDS().intValue());
        assertEquals(0, r.getInvalidsCDS().intValue());
        assertEquals(2, r.getTrinucleotideCount("ATT", Phase.PHASE_0).intValue());
        assertEquals(0, r.getTrinucleotideCount("ATT", Phase.PHASE_2).intValue());
        assertEquals(0, r.getTrinucleotideCount("TAA", Phase.PHASE_0).intValue());
        assertEquals(2, r.getTrinucleotideCount("TAA", Phase.PHASE_2).intValue());
        assertEquals(36, r.getTrinucleotideCount("AAA", Phase.PHASE_0).intValue());
    }

    @Test
    @Disabled("Tests pour comprendre le fonctionnement de BioJava")
    void biojavaTest(){
        final File repliconFile = GENBANK_TEST_FILE_PATH.toFile();
        LinkedHashMap<String, DNASequence> dnaSequences = null;
        try {
            dnaSequences = GenbankReaderHelper.readGenbankDNASequence(repliconFile);
        } catch (Exception e) {
            fail("Erreur lecture fichier genbank",e);
        }
        for(DNASequence seq : dnaSequences.values()) {
            for (FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound> feature : seq.getFeatures()) {
                LOGGER.info(feature.getDescription());
            }
            LOGGER.info("-----------------Source features---------------");
            for (FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound> source : seq.getFeaturesByType("source")) {
                for (Map.Entry<String, List<Qualifier>> entry : source.getQualifiers().entrySet()) {
                    LOGGER.info(entry.getKey() + " -> " + entry.getValue().get(0));
                }
                for (FeatureInterface<AbstractSequence<NucleotideCompound>, NucleotideCompound> childrenFeature : source.getChildrenFeatures()) {
                    LOGGER.info(childrenFeature.getDescription());
                }
            }
        }
    }
}