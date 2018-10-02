package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.CustomTestCase;
import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.entities.Hierarchy;
import fr.unistra.bioinfo.persistence.entities.Replicon;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.persistence.PersistenceException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityManagerTest extends CustomTestCase {
    @BeforeAll
    static void beforeAll(){
        DBUtils.start();
    }

    @AfterAll
    static void afterAll(){
        DBUtils.stop();
    }

    @Test
    void save() {
        Hierarchy h1 = new Hierarchy("K1", "G1", "S1","O1");
        Hierarchy h2 = new Hierarchy("K1", "G1", "S1","O0");
        Replicon r1 = new Replicon("R1",h1);
        Replicon r2 = new Replicon("R2", h2);
        HierarchyManager hMgr = PersistentEntityManagerFactory.getHierarchyManager();
        RepliconManager rMgr = PersistentEntityManagerFactory.getRepliconManager();
        try{
            hMgr.save(h1);
            assertEquals(h1.getId(), hMgr.getById(h1.getId()).getId(), "Sauvegarde des Hierarchy KO");

            rMgr.save(r1);
            assertEquals(r1.getId(), rMgr.getById(r1.getId()).getId(), "Sauvegarde des Replicon KO");

            List<Replicon> h1Replicons = hMgr.getReplicons(h1);
            assertTrue(CollectionUtils.isNotEmpty(h1Replicons));
            assertTrue(h1Replicons.contains(r1));
            rMgr.save(r2);
            assertEquals(h2.getId(), hMgr.getById(h2.getId()).getId(), "Sauvegarde des Hierarchy lors de la sauvegarde des Replicon KO");

            assertThrows(PersistenceException.class,
                    () ->  hMgr.save( new Hierarchy("K1", "G1", "S1","O1"))
            );

            assertThrows(PersistenceException.class,
                    () ->  rMgr.save(new Replicon("R1",h1))
            );
        }catch(Exception e){
            fail(e);
        }
    }

    @Test
    void delete() {
        Hierarchy h1 = new Hierarchy("K1", "G1", "S1","O1");
        Hierarchy h2 = new Hierarchy("K1", "G1", "S1","O0");
        Replicon r1 = new Replicon("R1",h1);
        Replicon r2 = new Replicon("R2", h2);
        HierarchyManager hMgr = PersistentEntityManagerFactory.getHierarchyManager();
        RepliconManager rMgr = PersistentEntityManagerFactory.getRepliconManager();
        try{
            hMgr.save(h1);
            hMgr.save(h2);
            rMgr.save(r1);
            rMgr.save(r2);

            Long r1Id = r1.getId();
            rMgr.delete(r1);
            assertNull(rMgr.getById(r1Id), "Suppression des Replicon KO");
            assertEquals(h1.getId(), hMgr.getById(h1.getId()).getId(), "Suppression de la Hierarchy lors de la suppression d'un Replicon !");

            Long h1Id = h1.getId();
            hMgr.delete(h1);
            assertNull(hMgr.getById(h1Id), "Suppression des Hierarchy KO");

            Long r2Id = r2.getId();
            hMgr.delete(h2);
            assertNull(rMgr.getById(r2Id), "Suppression des Replicon lors de la suppression des Hierarchy KO");
        }catch(Exception e){
            fail(e);
        }
    }

}