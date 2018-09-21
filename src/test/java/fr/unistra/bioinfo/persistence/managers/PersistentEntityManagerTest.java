package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.CustomTestCase;
import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.entities.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entities.RepliconEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.persistence.Table;
import java.util.List;

class PersistentEntityManagerTest extends CustomTestCase {
    @BeforeAll
    static void beforeAll(){
        DBUtils.start();
    }

    @AfterAll
    static void afterAll(){
        DBUtils.stop();
    }

    @Test
    void testPersistence(){
        PersistentEntityManager<HierarchyEntity> mgr = PersistentEntityManager.create(HierarchyEntity.class);
        HierarchyEntity h = new HierarchyEntity("TEST", "TEST", "TEST", "TEST");
        PersistentEntityManager<RepliconEntity> mgr2 = PersistentEntityManager.create(RepliconEntity.class);
        RepliconEntity r = new RepliconEntity("Test", h);
        r.setComputed(false);
        r.setDownloaded(true);
        mgr2.save(r);
        List<RepliconEntity> list = mgr2.getAll();
        LOGGER.info("Récupération de "+list.size()+" lignes dans "+RepliconEntity.class.getAnnotation(Table.class).name());
        for(RepliconEntity replicon : list){
            LOGGER.info("ID du replicons : " + replicon.getId());
        }
        mgr2.delete(r);
        mgr.delete(h);
    }
}