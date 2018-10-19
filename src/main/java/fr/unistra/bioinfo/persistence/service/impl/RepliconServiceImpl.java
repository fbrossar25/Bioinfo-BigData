package fr.unistra.bioinfo.persistence.service.impl;

import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.manager.RepliconManager;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManagerFactory;
import java.util.List;

@Service
public class RepliconServiceImpl extends AbstractServiceImpl<RepliconEntity, Long> implements RepliconService {
    private final RepliconManager repliconManager;

    @Autowired
    public RepliconServiceImpl(RepliconManager repliconManager, EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory, RepliconEntity.class);
        this.repliconManager = repliconManager;
    }

    @Override
    public void delete(RepliconEntity entity) {
        if(entity.getHierarchyEntity() != null){
            entity.getHierarchyEntity().removeRepliconEntity(entity);
        }
        super.delete(entity);
    }

    @Override
    public void deleteAll(List<RepliconEntity> entities) {
        if(CollectionUtils.isNotEmpty(entities)){
            entities.forEach(e -> {
                if(e.getHierarchyEntity() != null){
                    e.getHierarchyEntity().removeRepliconEntity(e);
                }
            });
        }
        super.deleteAll(entities);
    }

    public RepliconManager getManager(){
        return repliconManager;
    }
}
