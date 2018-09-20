package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.CustomTestCase;
import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.entities.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entities.RepliconEntity;
import fr.unistra.bioinfo.persistence.entities.Sample;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        Sample s1 = new Sample();
        s1.setDescription("test1");
        Sample s2 = new Sample();
        s2.setDescription("test2");
        Sample s3 = new Sample();
        s3.setDescription("test3");
        PersistentEntityManager<Sample> sampleMgr = PersistentEntityManager.create(Sample.class);
        int n = sampleMgr.deleteAll();
        LOGGER.info("Suppression de "+n+" lignes");
        sampleMgr.save(s1);
        LOGGER.info("Before : "+s2.getId());
        sampleMgr.save(s2);
        LOGGER.info("After : "+s2.getId());
        LOGGER.info("Before : "+s3.getId());
        sampleMgr.save(s3);
        LOGGER.info("After : "+s3.getId());
        sampleMgr.delete(s2);
        List<Sample> samples = sampleMgr.getAll();
        LOGGER.info("Récupération de "+samples.size()+" lignes");
        for(Sample sample : samples){
            LOGGER.info(sample.getDescription());
        }
    }

    @Test
    void testGenbankEntities(){
        PersistentEntityManager<HierarchyEntity> mgr = PersistentEntityManager.create(HierarchyEntity.class);
        mgr.save(new HierarchyEntity());
        mgr.deleteAll();
        PersistentEntityManager<RepliconEntity> mgr2 = PersistentEntityManager.create(RepliconEntity.class);
        mgr2.save(new RepliconEntity());
        mgr2.deleteAll();
    }
}