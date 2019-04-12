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
        assertEquals(42, repliconService.getById(r.getId()).getDinucleotideCount("AA", Phase.PHASE_1).intValue());
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
}