package fr.unistra.bioinfo;

import java.util.List;

public class Main {

    public static void main(String [] args){
        Sample s1 = new Sample();
        s1.setDescription("test1");
        Sample s2 = new Sample();
        s2.setDescription("test2");
        Sample s3 = new Sample();
        s3.setDescription("test3");
        PersistentEntityManager<Sample> sampleMgr = PersistentEntityManager.create(Sample.class);
        int n = sampleMgr.deleteAll();
        System.out.print("Suppression de "+n+" lignes");
        sampleMgr.save(s1);
        sampleMgr.save(s2);
        sampleMgr.save(s3);
        sampleMgr.delete(s2);
        List<Sample> samples = sampleMgr.getAll();
        System.out.println("Récupération de "+samples.size()+" lignes");
        for(Sample sample : samples){
            System.out.println(sample.getDescription());
        }
        DBUtils.close();
    }
}
