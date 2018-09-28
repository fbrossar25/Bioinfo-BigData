package fr.unistra.bioinfo.persistence.managers;

public class PersistentEntityManagerFactory {
    private static RepliconEntityManager repliconMgr;
    private static HierarchyEntityManager hierarchyMgr;

    private PersistentEntityManagerFactory(){}

    public static RepliconEntityManager getRepliconManager(){
        if(repliconMgr == null){
            repliconMgr = new RepliconEntityManager();
        }
        return repliconMgr;
    }

    public static HierarchyEntityManager getHierarchyManager(){
        if(hierarchyMgr == null){
            hierarchyMgr = new HierarchyEntityManager();
        }
        return hierarchyMgr;
    }
}
