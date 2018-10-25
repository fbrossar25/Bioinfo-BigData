package fr.unistra.bioinfo.persistence.service.impl;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.manager.HierarchyManager;
import fr.unistra.bioinfo.persistence.manager.RepliconManager;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManagerFactory;
import javax.transaction.Transactional;
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
    @Transactional
    public void delete(HierarchyEntity entity) {
        repliconManager.deleteAllByHierarchyEntity(entity);
        super.delete(entity);
    }

    @Override
    @Transactional
    public void deleteAll(List<HierarchyEntity> entities) {
        repliconManager.deleteAllByHierarchyEntityIn(entities);
        super.deleteAll(entities);
    }

    public HierarchyManager getManager(){
        return hierarchyManager;
    }

    @Override
    public HierarchyEntity getByOrganism(String organism) {
        return hierarchyManager.getByOrganism(organism);
    }
}
