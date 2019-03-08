package fr.unistra.bioinfo.persistence.service.impl;

import fr.unistra.bioinfo.genbank.GenbankUtils;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.manager.HierarchyManager;
import fr.unistra.bioinfo.persistence.manager.RepliconManager;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class HierarchyServiceImpl extends AbstractServiceImpl<HierarchyEntity, Long> implements HierarchyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HierarchyServiceImpl.class);
    private final HierarchyManager hierarchyManager;
    private final RepliconManager repliconManager;

    @Autowired
    public HierarchyServiceImpl(HierarchyManager hierarchyManager, RepliconManager repliconManager) {
        super();
        this.hierarchyManager = hierarchyManager;
        this.repliconManager = repliconManager;
    }

    @Override
    @Transactional
    public void delete(@NonNull HierarchyEntity entity) {
        repliconManager.deleteAllByHierarchyEntity(entity);
        super.delete(entity);
    }

    @Override
    @Transactional
    public void deleteAll(@NonNull List<HierarchyEntity> entities) {
        repliconManager.deleteAllByHierarchyEntityIn(entities);
        super.deleteAll(entities);
    }

    public HierarchyManager getManager(){
        return hierarchyManager;
    }

    @Override
    public HierarchyEntity getByOrganism(@NonNull String organism) {
        return hierarchyManager.getByOrganism(organism);
    }

    @Override
    public HierarchyEntity getByOrganism(@NonNull String organism, boolean createIfNotExists) {
        HierarchyEntity entity = getByOrganism(organism);
        if(entity == null && createIfNotExists){
            LOGGER.info("Création du Hierarchy '"+organism+"'");
            entity = GenbankUtils.getHierarchyInfoByOrganism(organism);
            if(entity != null){
                save(entity);
            }
        }
        return entity;
    }

    public List<HierarchyEntity> getBySubgroup(String subgroup)
    {
        return hierarchyManager.getHierarchyEntitiesBySubgroup(subgroup);
    }

    public List<HierarchyEntity> getByGroup(@NonNull String group)
    {
        return hierarchyManager.getHierarchyEntitiesByGroup(group);
    }

    public List<HierarchyEntity> getByKingdom(@NonNull String kingdom)
    {
        return hierarchyManager.getHierarchyEntitiesByKingdom(kingdom);
    }

    @Override
    public void deleteAll() {
        repliconManager.deleteAll();
        hierarchyManager.deleteAll();
    }
}
