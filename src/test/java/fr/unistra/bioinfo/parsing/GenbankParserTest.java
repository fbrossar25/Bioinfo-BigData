package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.configuration.StaticInitializer;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({StaticInitializer.class})
@SpringBootTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
class GenbankParserTest {
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final Path GENBANK_TEST_FILE_PATH = Paths.get(".","src", "test", "resources", "NC_001700.1.gb");
    private static final Path GENBANK_BACTH_TEST_FILE_PATH = Paths.get(".","src", "test", "resources", "replicons-batch-test.gb");
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

    void checkReplicon(RepliconEntity r){
        assertNotNull(r);
        assertTrue(r.isParsed());
        assertFalse(r.isComputed());
        boolean checkAtLeastOnePref = false;
        for (String dinucleotide : CommonUtils.DINUCLEOTIDES) {
            assertNotNull(r.getPhasesPrefsDinucleotide(dinucleotide));
            if(r.getPhasesPrefsDinucleotide(dinucleotide).isEmpty()){
                assertEquals(0, r.getDinucleotideCount(dinucleotide, Phase.PHASE_0).intValue());
                assertEquals(0, r.getDinucleotideCount(dinucleotide, Phase.PHASE_1).intValue());
            }else{
                assertTrue(r.getDinucleotideCount(dinucleotide, Phase.PHASE_0) > 0 ||
                        r.getDinucleotideCount(dinucleotide, Phase.PHASE_1) > 0);
                checkAtLeastOnePref = true;
            }
        }
        assertTrue(checkAtLeastOnePref, "Phases préferentielles dinucléotides KO");
        checkAtLeastOnePref = false;
        for (String trinucleotide : CommonUtils.TRINUCLEOTIDES) {
            assertNotNull(r.getPhasesPrefsTrinucleotide(trinucleotide));
            if(r.getPhasesPrefsTrinucleotide(trinucleotide).isEmpty()){
                assertEquals(0, r.getTrinucleotideCount(trinucleotide, Phase.PHASE_0).intValue());
                assertEquals(0, r.getTrinucleotideCount(trinucleotide, Phase.PHASE_1).intValue());
                assertEquals(0, r.getTrinucleotideCount(trinucleotide, Phase.PHASE_2).intValue());
            }else{
                assertTrue(r.getTrinucleotideCount(trinucleotide, Phase.PHASE_0) > 0 ||
                        r.getTrinucleotideCount(trinucleotide, Phase.PHASE_1) > 0 ||
                        r.getTrinucleotideCount(trinucleotide, Phase.PHASE_2) > 0);
                checkAtLeastOnePref = true;
            }
        }
        assertTrue(checkAtLeastOnePref, "Phases préferentielles trinucléotides KO");
        assertEquals(1, r.getVersion().intValue());
        HierarchyEntity h = r.getHierarchyEntity();
        assertNotNull(h);
        assertEquals("Eukaryota", h.getKingdom());
        assertEquals("Animals", h.getGroup());
        assertEquals("Mammals", h.getSubgroup());
        assertEquals("Felis catus", h.getOrganism());
        assertEquals(RepliconType.MITOCHONDRION, r.getType());
        assertTrue(r.getDinucleotideCount("GG", Phase.PHASE_0) > 0);
        assertEquals(r.getDinucleotideCount("GG", Phase.PHASE_0), r.getDinucleotideCount("gg", Phase.PHASE_0));
    }

    @Test
    void parseGenbankBatchFile() {
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_BACTH_TEST_FILE_PATH.toFile()));
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH.toFile()));
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

    //Entre 1min 30 et 4min, core i5, 2 coeurs physiques, 4 logiques, 2.8Ghz
    @Test
    @Disabled("Ce test est un benchmark à lancer manuellement.")
    void parse10kGenbankFileBenchmark(){
        CommonUtils.disableHibernateLogging();
        Iterator<Integer> it = IntStream.range(0, 10000).iterator();
        File f = GENBANK_TEST_FILE_PATH.toFile();
        long begin = System.currentTimeMillis();
        while(it.hasNext()){
            GenbankParser.parseGenbankFile(f);
            it.next();
        }
        long end = System.currentTimeMillis();
        LOGGER.info("Le parsing de 10000 fichier à pris "+(end - begin)+"ms");
        assertEquals(1, hierarchyService.count().longValue());
        HierarchyEntity h = hierarchyService.getByOrganism("Felis catus");
        assertNotNull(h);
        List<RepliconEntity> replicons = repliconService.getByHierarchy(h);
        CommonUtils.enableHibernateLogging(true);
        assertNotNull(replicons);
        assertEquals(1, replicons.size());
        RepliconEntity r = replicons.get(0);
        for (String dinucleotide : CommonUtils.DINUCLEOTIDES) {
            assertNotNull(r.getPhasesPrefsDinucleotide(dinucleotide));
            assertFalse(r.getPhasesPrefsDinucleotide(dinucleotide).isEmpty());
        }
        for (String trinucleotide : CommonUtils.TRINUCLEOTIDES) {
            assertNotNull(r.getPhasesPrefsTrinucleotide(trinucleotide));
            assertFalse(r.getPhasesPrefsTrinucleotide(trinucleotide).isEmpty());
        }
        assertTrue(r.isParsed());
        assertFalse(r.isComputed());
        assertEquals(RepliconType.MITOCHONDRION, r.getType());
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
                GenbankParser.parseGenbankFile(f);
            }catch(Exception e){
                LOGGER.error("Erreur de parsing", e);
                fail(e);
            }
        });
        long end = System.currentTimeMillis();
        LOGGER.info("Le parsing de 10000 fichier à pris "+(end - begin)+"ms");
        assertEquals(1, hierarchyService.count().longValue());
        HierarchyEntity h = hierarchyService.getByOrganism("Felis catus");
        assertNotNull(h);
        List<RepliconEntity> replicons = repliconService.getByHierarchy(h);
        CommonUtils.enableHibernateLogging(true);
        assertNotNull(replicons);
        assertEquals(1, replicons.size());
        RepliconEntity r = replicons.get(0);
        for (String dinucleotide : CommonUtils.DINUCLEOTIDES) {
            assertNotNull(r.getPhasesPrefsDinucleotide(dinucleotide));
            assertFalse(r.getPhasesPrefsDinucleotide(dinucleotide).isEmpty());
        }
        for (String trinucleotide : CommonUtils.TRINUCLEOTIDES) {
            assertNotNull(r.getPhasesPrefsTrinucleotide(trinucleotide));
            assertFalse(r.getPhasesPrefsTrinucleotide(trinucleotide).isEmpty());
        }
        assertTrue(r.isParsed());
        assertFalse(r.isComputed());
        assertEquals(RepliconType.MITOCHONDRION, r.getType());
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