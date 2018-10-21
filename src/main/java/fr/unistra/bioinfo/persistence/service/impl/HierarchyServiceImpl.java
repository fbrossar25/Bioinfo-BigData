package fr.unistra.bioinfo.persistence.service.impl;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.manager.HierarchyManager;
import fr.unistra.bioinfo.persistence.manager.RepliconManager;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManagerFactory;
import java.util.List;

@Service
public class HierarchyServiceImpl extends AbstractServiceImpl<HierarchyEntity, Long> implements HierarchyService {
    private final HierarchyManager hierarchyManager;
    private final RepliconManager repliconManager;

    @Autowired
    public HierarchyServiceImpl(HierarchyManager hierarchyManager, RepliconManager repliconManager, EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory, HierarchyEntity.class);
        this.hierarchyManager = hierarchyManager;
        this.repliconManager = repliconManager;
    }

    @Override
    public HierarchyEntity save(HierarchyEntity entity) {
        if(CollectionUtils.isNotEmpty(entity.getRepliconEntities())){
            repliconManager.saveAll(entity.getRepliconEntities());
        }
        return super.save(entity);
    }

    @Override
    public List<HierarchyEntity> saveAll(List<HierarchyEntity> entities) {
        for(HierarchyEntity entity : entities){
            if(CollectionUtils.isNotEmpty(entity.getRepliconEntities())){
                repliconManager.saveAll(entity.getRepliconEntities());
            }
        }
        return super.saveAll(entities);
    }

    public HierarchyManager getManager(){
        return hierarchyManager;
    }
}
