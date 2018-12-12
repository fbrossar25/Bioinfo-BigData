package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.parsing.Phase;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.members.HierarchyMembers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({StaticInitializer.class})
@SpringBootTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
class HierarchyServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HierarchyServiceTest.class);

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
    void getTest(){
        CommonUtils.disableHibernateLogging();
        HierarchyEntity h = new HierarchyEntity("K1", "G1", "S1", "O1");
        RepliconEntity r = new RepliconEntity("R1", h);
        hierarchyService.save(h);
        for(String tri : CommonUtils.TRINUCLEOTIDES){
            r.incrementTrinucleotideCount(tri, Phase.PHASE_1);
            r.incrementTrinucleotideCount(tri, Phase.PHASE_2);
            r.incrementTrinucleotideCount(tri, Phase.PHASE_2);
        }
        for(String di : CommonUtils.DINUCLEOTIDES){
            r.incrementDinucleotideCount(di, Phase.PHASE_1);
        }
        repliconService.save(r);
        r = repliconService.getByName("R1");
        assertNotNull(r);
        assertEquals("R1", r.getName());
        assertEquals(1, r.getVersion().intValue());
        for(String tri : CommonUtils.TRINUCLEOTIDES){
            assertEquals(0, r.getTrinucleotideCount(tri, Phase.PHASE_0).intValue());
            assertEquals(1, r.getTrinucleotideCount(tri, Phase.PHASE_1).intValue());
            assertEquals(2, r.getTrinucleotideCount(tri, Phase.PHASE_2).intValue());
        }
        for(String di : CommonUtils.DINUCLEOTIDES){
            assertEquals(0, r.getDinucleotideCount(di, Phase.PHASE_0).intValue());
            assertEquals(1, r.getDinucleotideCount(di, Phase.PHASE_1).intValue());
        }
        CommonUtils.enableHibernateLogging(true);
    }

    @Test
    void concurrencyTest(){
        CommonUtils.disableHibernateLogging();
        try{
            IntStream.range(0, 1000).parallel().forEach(i -> {
                hierarchyService.save(new HierarchyEntity("A", "B", "C", "D"+i));
            });
        }catch(Exception e){
            fail("Erreur avec le multithreading", e);
        }
        CommonUtils.enableHibernateLogging(true);
    }

    @Test
    void removeTest(){
        CommonUtils.disableHibernateLogging();
        HierarchyEntity h = new HierarchyEntity("K1", "G1", "S1", "O1");
        RepliconEntity r1 = new RepliconEntity("R1", 1, h);
        RepliconEntity r2 = new RepliconEntity("R2", 1, h);
        hierarchyService.save(h);
        repliconService.saveAll(Arrays.asList(r1, r2));
        Long r1Id = r1.getId();
        Long r2Id = r2.getId();
        Long hId = h.getId();
        hierarchyService.delete(h);

        assertNull(hierarchyService.getById(hId));
        assertFalse(hierarchyService.existsById(hId));

        assertNull(repliconService.getById(r1Id));
        assertFalse(repliconService.existsById(r1Id));

        assertNull(repliconService.getById(r2Id));
        assertFalse(repliconService.existsById(r2Id));


        assertEquals(Long.valueOf(0), repliconService.count());
    }

    @Test
    void saveTest(){
        CommonUtils.disableHibernateLogging();
        HierarchyEntity h = new HierarchyEntity("K1", "G1", "S1", "O1");
        RepliconEntity r = new RepliconEntity("R1", 1, null);
        r.setHierarchyEntity(h);
        repliconService.save(r);

        assertNotNull(r.getId());
        assertNotNull(h.getId());

        List<RepliconEntity> replicons = repliconService.getByHierarchy(h);
        assertNotNull(replicons);
        assertFalse(replicons.isEmpty());
        assertTrue(replicons.contains(r));

        assertNotNull(repliconService.getById(r.getId()));
        assertEquals(r, repliconService.getById(r.getId()));
        assertEquals(h, r.getHierarchyEntity());

        assertTrue(repliconService.existsById(r.getId()));
        assertTrue(hierarchyService.existsById(h.getId()));
    }

    @Test
    void batchSaveTest(){
        CommonUtils.disableHibernateLogging();
        int N = 10000;
        //Création de N replicons
        List<HierarchyEntity> hierarchies = Collections.synchronizedList(new ArrayList<>(N));
        IntStream.range(0, N).parallel()
                .forEach(i -> hierarchies.add(new HierarchyEntity("K"+(i%100),"G"+(i%25),"S"+(i%5),"O"+i)));

        long begin = System.currentTimeMillis();
        assertTimeout(ofSeconds(60), () ->
            hierarchyService.saveAll(hierarchies)
        );
        long end = System.currentTimeMillis();
        LOGGER.info("Save time ("+N+" entities) : "+(end - begin)+"ms");

        //Tests
        assertEquals(hierarchies.size(), hierarchyService.count().longValue());
        List<HierarchyEntity> allHierarchies = hierarchyService.getAll();
        Collections.sort(hierarchies);
        Collections.sort(allHierarchies);
        assertEquals(hierarchies.size(), allHierarchies.size());
        IntStream.range(0, N).parallel().forEach(i -> assertEquals(hierarchies.get(i), allHierarchies.get(i)));
    }

    @Test
    void paginationTest(){
        CommonUtils.disableHibernateLogging();
        int N = 100;
        for(int i=0; i<N; i++){
            hierarchyService.save(new HierarchyEntity("K1", "G1", "S1", "O"+i));
        }
        assertEquals(N, hierarchyService.count().longValue());
        Page<HierarchyEntity> page = hierarchyService.getAll(0, Sort.Direction.ASC, HierarchyMembers.ORGANISM);
        Pageable pageable = page.getPageable();
        long count = 0, total = page.getTotalElements();
        List<HierarchyEntity> entities = page.getContent();
        while(page.hasNext()){
            page = hierarchyService.getAll(pageable);
            entities = page.getContent();
            for(int i=0; i<entities.size()-1; i++){
                assertTrue(entities.get(i).compareTo(entities.get(i+1)) <= 0);
                count++;
            }
            count++;
            pageable = page.nextPageable();
        }
        assertEquals(count, hierarchyService.count().longValue());
        assertEquals(count, total);
    }
}