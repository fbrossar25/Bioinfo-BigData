package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.CustomTestCase;
import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.QueryUtils;
import fr.unistra.bioinfo.persistence.entities.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entities.RepliconEntity;
import fr.unistra.bioinfo.persistence.properties.HierarchyProperties;
import fr.unistra.bioinfo.persistence.properties.RepliconProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.persistence.Table;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    void testCascadedActions(){
        HierarchyEntityManager hMgr = PersistentEntityManagerFactory.getHierarchyManager();
        RepliconEntityManager rMgr = PersistentEntityManagerFactory.getRepliconManager();

        HierarchyEntity h = new HierarchyEntity("TEST", "TEST", "TEST", "TEST");
        RepliconEntity r = new RepliconEntity("Test", h);

        //Si les précédents tests laissent des traces
        hMgr.delete(QueryUtils.equals(HierarchyProperties.ORGANISM, h.getOrganism()));
        rMgr.delete(QueryUtils.equals(RepliconProperties.REPLICON, r.getReplicon()));

        //Sauvegarde de Hierarchy quand sauvegarde de Replicon
        rMgr.save(r);
        assertEquals(h, hMgr.getById(h.getId()));

        //Pas de suppression de Hierarchy quand suppression de Replicon
        rMgr.delete(r);
        assertNull(rMgr.getById(r.getId()));
        assertEquals(h, hMgr.getById(h.getId()));

        //Suppression de Replicon quand suppression de Hierarchy
        rMgr.save(r);
        hMgr.delete(h);
        assertNull(rMgr.getById(r.getId()));
    }

    @Test
    void testPersistence(){
        HierarchyEntityManager hMgr = PersistentEntityManagerFactory.getHierarchyManager();
        RepliconEntityManager rMgr = PersistentEntityManagerFactory.getRepliconManager();

        HierarchyEntity h = new HierarchyEntity("TEST", "TEST", "TEST", "TEST");
        RepliconEntity r = new RepliconEntity("Test", h);

        hMgr.delete(QueryUtils.equals(HierarchyProperties.ORGANISM,h.getOrganism()));
        rMgr.delete(QueryUtils.equals(RepliconProperties.REPLICON,r.getReplicon()));

        r.setComputed(false);
        r.setDownloaded(true);
        hMgr.save(h);
        rMgr.save(r);
        assertNotNull(r.getId());

        List<RepliconEntity> list = rMgr.getAll();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        LOGGER.info("Récupération de "+list.size()+" lignes dans "+RepliconEntity.class.getAnnotation(Table.class).name());
        for(RepliconEntity replicon : list){
            LOGGER.info("ID du replicons : " + replicon.getId());
        }

        assertEquals(h, hMgr.getById(h.getId()));
        assertEquals(r, rMgr.getById(r.getId()));
        rMgr.deleteAll();
        hMgr.deleteAll();
        assertTrue(rMgr.getAll().isEmpty());
        assertTrue(hMgr.getAll().isEmpty());
        assertEquals(Long.valueOf(0), hMgr.count());
        assertEquals(Long.valueOf(0), rMgr.count());
    }
}