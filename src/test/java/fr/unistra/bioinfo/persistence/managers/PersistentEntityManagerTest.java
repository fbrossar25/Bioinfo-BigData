package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.CustomTestCase;
import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.entities.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entities.RepliconEntity;
import fr.unistra.bioinfo.persistence.entities.Sample;
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
        Sample s1 = new Sample();
        s1.setDescription("test1");
        Sample s2 = new Sample();
        s2.setDescription("test2");
        Sample s3 = new Sample();
        s3.setDescription("test3");
        PersistentEntityManager<Sample> sampleMgr = PersistentEntityManager.create(Sample.class);
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
        int n = sampleMgr.deleteAll();
        LOGGER.info("Suppression de "+n+" lignes");
    }

    @Test
    void testGenbankEntities(){
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
            LOGGER.info(replicon.getId());
        }
        mgr2.delete(r);
        mgr.delete(h);
    }
}