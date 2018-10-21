package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.configuration.StaticInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

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
        fail("Not yet implemented");
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