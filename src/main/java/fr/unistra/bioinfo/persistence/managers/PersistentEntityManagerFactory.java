package fr.unistra.bioinfo.persistence.managers;

public class PersistentEntityManagerFactory {
    private static RepliconManager repliconMgr;
    private static HierarchyManager hierarchyMgr;

    private PersistentEntityManagerFactory(){}

    public static RepliconManager getRepliconManager(){
        if(repliconMgr == null){
            repliconMgr = new RepliconManager();
        }
        return repliconMgr;
    }

    public static HierarchyManager getHierarchyManager(){
        if(hierarchyMgr == null){
            hierarchyMgr = new HierarchyManager();
        }
        return hierarchyMgr;
    }
}
