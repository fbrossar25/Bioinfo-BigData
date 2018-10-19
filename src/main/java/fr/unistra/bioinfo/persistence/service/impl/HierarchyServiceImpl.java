package fr.unistra.bioinfo.persistence.service.impl;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.manager.HierarchyManager;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManagerFactory;

@Service
public class HierarchyServiceImpl extends AbstractServiceImpl<HierarchyEntity, Long> implements HierarchyService {
    private final HierarchyManager hierarchyManager;

    @Autowired
    public HierarchyServiceImpl(HierarchyManager hierarchyManager, EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory, HierarchyEntity.class);
        this.hierarchyManager = hierarchyManager;
    }

    public HierarchyManager getManager(){
        return hierarchyManager;
    }
}
