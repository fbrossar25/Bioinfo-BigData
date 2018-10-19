package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
class RepliconServiceTest {
    @Autowired
    private RepliconService repliconService;

    @Autowired
    private HierarchyService hierarchyService;

    @BeforeEach
    public void beforeEach(){
        assertNotNull(repliconService);
        assertNotNull(hierarchyService);
    }

    @Test
    public void removeTest(){
        HierarchyEntity h = new HierarchyEntity("K1", "G1", "S1", "O1");
        RepliconEntity r = new RepliconEntity("R1", 1, h);
        repliconService.save(r);
        Long rId = r.getId();
        repliconService.delete(r);

        assertNull(repliconService.getById(rId));
        assertNull(r.getId());
        assertNotNull(h.getId());
        assertTrue(hierarchyService.existsById(h.getId()));
        assertFalse(repliconService.existsById(rId));
        assertTrue(h.getRepliconEntities().isEmpty());
    }

    @Test
    public void saveTest(){
        HierarchyEntity h = new HierarchyEntity("K1", "G1", "S1", "O1");
        RepliconEntity r = new RepliconEntity("R1", 1, h);
        repliconService.save(r);

        assertNotNull(r.getId());
        assertNotNull(h.getId());

        assertNotNull(repliconService.getById(r.getId()));
        assertEquals(r, repliconService.getById(r.getId()));
        assertEquals(r.getHierarchyEntity(), h);
        assertTrue(repliconService.existsById(r.getId()));
        assertTrue(hierarchyService.existsById(h.getId()));
        assertFalse(h.getRepliconEntities().isEmpty());
        assertTrue(h.getRepliconEntities().contains(r));
    }

    @Test
    public void batchSaveTest(){
        HierarchyEntity h = new HierarchyEntity("K1","G1","S1","O1");
        List<RepliconEntity> replicons = new ArrayList<>(10000);
        for(int i=0; i<10000; i++){
            replicons.add(new RepliconEntity("",1, h));
        }
        repliconService.saveAll(replicons);
        assertEquals(10000, repliconService.count().longValue());
        List<RepliconEntity> allReplicons = repliconService.getAll();
        assertEquals(10000, allReplicons.size());
        replicons.forEach(r -> assertTrue(allReplicons.contains(r)));
    }
}