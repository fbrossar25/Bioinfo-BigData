package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.Phase;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({StaticInitializer.class})
@SpringBootTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
class RepliconServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepliconServiceTest.class);

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
    void removeTest(){
        CommonUtils.disableHibernateLogging();
        HierarchyEntity h = new HierarchyEntity("K1", "G1", "S1", "O1");
        RepliconEntity r = new RepliconEntity("R1", 1, h);
        repliconService.save(r);
        Long rId = r.getId();
        repliconService.delete(r);

        assertNull(repliconService.getById(rId));
        assertFalse(repliconService.existsById(rId));

        assertNotNull(h.getId());
        assertTrue(hierarchyService.existsById(h.getId()));
        List<RepliconEntity> replicons = repliconService.getByHierarchy(h);
        assertNotNull(replicons);
        assertTrue(replicons.isEmpty());
    }

    @Test
    void saveTest(){
        CommonUtils.disableHibernateLogging();
        HierarchyEntity h = new HierarchyEntity("K1", "G1", "S1", "O1");
        RepliconEntity r = new RepliconEntity("R1", 1, h);
        r.setDinucleotideCount("AA", Phase.PHASE_1, 42);
        repliconService.save(r);

        assertNotNull(r.getId());
        assertNotNull(h.getId());

        assertTrue(repliconService.existsById(r.getId()));
        assertTrue(hierarchyService.existsById(h.getId()));
        assertNotNull(repliconService.getById(r.getId()));
        assertEquals(r, repliconService.getById(r.getId()));
        assertEquals(42, repliconService.getById(r.getId()).getDinucleotideCount("AA", Phase.PHASE_1).longValue());
        assertEquals(r.getHierarchyEntity(), h);

        List<RepliconEntity> replicons = repliconService.getByHierarchy(h);
        assertNotNull(replicons);
        assertFalse(replicons.isEmpty());
        assertTrue(replicons.contains(r));
    }

    @Test
    void batchSaveTest(){
        CommonUtils.disableHibernateLogging();
        HierarchyEntity h = new HierarchyEntity("K1","G1","S1","O1");
        int N = 10000;
        //Création de N replicons
        List<RepliconEntity> replicons = Collections.synchronizedList(new ArrayList<>(N));
        IntStream
                .range(0, N)
                .parallel()
                .forEach(i -> replicons.add(new RepliconEntity("R"+i,1, h)));

        long begin = System.currentTimeMillis();
        assertTimeout(ofSeconds(60), () ->
                repliconService.saveAll(replicons)
        );
        long end = System.currentTimeMillis();
        LOGGER.info("Save time ("+N+" entities) : "+(end - begin)+"ms");

        //Tests
        assertEquals(replicons.size(), repliconService.count().longValue());
        List<RepliconEntity> allReplicons = repliconService.getAll();
        Collections.sort(replicons);
        Collections.sort(allReplicons);
        assertEquals(replicons.size(), allReplicons.size());
        List<RepliconEntity> repliconsByHierarchy = new ArrayList<>(replicons.size());
        assertTimeout(ofSeconds(30), () ->
            repliconsByHierarchy.addAll(repliconService.getByHierarchy(h))
        );
        assertNotNull(repliconsByHierarchy);
        assertEquals(repliconsByHierarchy.size(), allReplicons.size());
        IntStream.range(0, N).parallel().forEach(i -> assertEquals(replicons.get(i), allReplicons.get(i)));
    }

    @Test
    void setAllComputedFalseTest(){
        HierarchyEntity h = new HierarchyEntity("k","g","s","o");
        for(int i=0; i<1000; i++){
            RepliconEntity r = new RepliconEntity("R"+i,1,h);
            r.setComputed(true);
            repliconService.save(r);
        }
        for(RepliconEntity r : repliconService.getAll()){
            assertTrue(r.isComputed(), "Au moins un replicon à isComputed à false : "+r.getName());
        }
        repliconService.setAllComputedFalse();
        for(RepliconEntity r : repliconService.getAll()){
            assertFalse(r.isComputed(), "Au moins un replicon à isComputed à true : "+r.getName());
        }
    }

    @Test
    void sumRepliconTest(){
        RepliconEntity r1 = new RepliconEntity();
        RepliconEntity r2 = new RepliconEntity();
        for(String tri : CommonUtils.TRINUCLEOTIDES.keySet()){
            r1.setTrinucleotideCount(tri, Phase.PHASE_0, 1L);
            r1.setTrinucleotideCount(tri, Phase.PHASE_1, 2L);
            r1.setTrinucleotideCount(tri, Phase.PHASE_2, 3L);
            r2.setTrinucleotideCount(tri, Phase.PHASE_0, 1L);
            r2.setTrinucleotideCount(tri, Phase.PHASE_1, 2L);
            r2.setTrinucleotideCount(tri, Phase.PHASE_2, 3L);
        }
        for(String di : CommonUtils.DINUCLEOTIDES.keySet()){
            r1.setDinucleotideCount(di, Phase.PHASE_0, 1L);
            r1.setDinucleotideCount(di, Phase.PHASE_1, 2L);
            r2.setDinucleotideCount(di, Phase.PHASE_0, 1L);
            r2.setDinucleotideCount(di, Phase.PHASE_1, 2L);
        }
        RepliconEntity sum = RepliconEntity.add(r1, r2);
        try{
            for(String tri : CommonUtils.TRINUCLEOTIDES.keySet()){
                assertEquals(Long.valueOf(2), sum.getTrinucleotideCount(tri, Phase.PHASE_0));
                assertEquals(Long.valueOf(4), sum.getTrinucleotideCount(tri, Phase.PHASE_1));
                assertEquals(Long.valueOf(6), sum.getTrinucleotideCount(tri, Phase.PHASE_2));
            }
            for(String di : CommonUtils.DINUCLEOTIDES.keySet()){
                assertEquals(Long.valueOf(2), sum.getDinucleotideCount(di, Phase.PHASE_0));
                assertEquals(Long.valueOf(4), sum.getDinucleotideCount(di, Phase.PHASE_1));
            }
            assertTrue(sum.getCounters().deepEquals(RepliconEntity.add(r2, r1).getCounters()));
        }catch(NullPointerException e){
            LOGGER.error("Erreur : un des nucleotides n'existe pas",e);
            fail(e);
        }
    }
}