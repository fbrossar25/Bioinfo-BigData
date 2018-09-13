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
        System.out.println("Before : "+s2.getId());
        sampleMgr.save(s2);
        System.out.println("After : "+s2.getId());
        System.out.println("Before : "+s3.getId());
        sampleMgr.save(s3);
        System.out.println("After : "+s3.getId());
        sampleMgr.delete(s2);
        List<Sample> samples = sampleMgr.getAll();
        System.out.println("Récupération de "+samples.size()+" lignes");
        for(Sample sample : samples){
            System.out.println(sample.getDescription());
        }
        DBUtils.close();
    }
}
