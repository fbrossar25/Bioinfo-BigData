package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({StaticInitializer.class})
@DataJpaTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
class HierarchyServiceTest {
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
        RepliconEntity r1 = new RepliconEntity("R1", 1, h);
        RepliconEntity r2 = new RepliconEntity("R2", 1, h);
        hierarchyService.save(h);
        Long r1Id = r1.getId();
        Long r2Id = r2.getId();
        Long hId = h.getId();
        hierarchyService.delete(h);

        assertNull(h.getId());
        assertNull(hierarchyService.getById(hId));
        assertFalse(hierarchyService.existsById(hId));

        assertNull(r1.getId());
        assertNull(repliconService.getById(r1Id));
        assertFalse(repliconService.existsById(r1Id));

        assertNull(r2.getId());
        assertNull(repliconService.getById(r2Id));
        assertFalse(repliconService.existsById(r2Id));

        assertTrue(h.getRepliconEntities().isEmpty());
    }

    @Test
    public void saveTest(){
        fail("Not yet implemented");
    }

    @Test
    public void batchSaveTest(){
        fail("Not yet implemented");
    }
}