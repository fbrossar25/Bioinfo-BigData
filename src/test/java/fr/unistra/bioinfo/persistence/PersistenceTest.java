package fr.unistra.bioinfo.persistence;

import fr.unistra.bioinfo.persistence.manager.HierarchyManager;
import fr.unistra.bioinfo.persistence.manager.RepliconManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertNotNull;

@SpringBootTest
public class PersistenceTest {
    @Autowired
    private HierarchyManager hierarchyManager;
    @Autowired
    private RepliconManager repliconManager;

    @BeforeEach
    public void beforeEach(){
        assertNotNull(hierarchyManager);
        assertNotNull(repliconManager);
    }

    @Test
    public void removeHierarchyEntityTest(){
        fail("Not yet implemented");
    }

    @Test
    public void saveHierarchyEntityTest(){
        fail("Not yet implemented");
    }

    @Test
    public void updateHierarchyEntityTest(){
        fail("Not yet implemented");
    }

    @Test
    public void getHierarchyEntityTest(){
        fail("Not yet implemented");
    }

    @Test
    public void removeRepliconEntityTest(){
        fail("Not yet implemented");
    }

    @Test
    public void saveRepliconEntityTest(){
        fail("Not yet implemented");
    }

    @Test
    public void updateRepliconEntityTest(){
        fail("Not yet implemented");
    }

    @Test
    public void getRepliconEntityTest(){
        fail("Not yet implemented");
    }
}
