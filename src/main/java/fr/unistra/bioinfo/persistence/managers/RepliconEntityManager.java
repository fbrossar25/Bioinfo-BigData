package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.persistence.entities.RepliconEntity;

public class RepliconEntityManager extends AbstractEntityManager<Long, RepliconEntity> {

    public RepliconEntityManager(){
        super(RepliconEntity.class);
    }

    @Override
    public void save(RepliconEntity entity) {
        PersistentEntityManagerFactory.getHierarchyManager().save(entity.getHierarchy());
        super.save(entity);
    }
}
