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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
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
    public void delete(@NonNull HierarchyEntity entity) {
        repliconManager.deleteAllByHierarchyEntity(entity);
        super.delete(entity);
    }

    @Override
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

    public List<HierarchyEntity> getBySubgroup(@NonNull String subgroup)
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

    @Override
    public List<HierarchyEntity> getOrganismToUpdateExcel() {
        List<Long> ids = repliconManager
                        .getAllByComputed(false)
                        .stream()
                        .map(replicon -> replicon.getHierarchyEntity().getId())
                        .distinct()
                        .collect(Collectors.toList());
        LOGGER.trace("Liste des ids des organismes à mettre à jour : {}", ids);
        return getByIds(ids);
    }

    @Override
    public List<HierarchyEntity> getByIds(List<Long> ids) {
        return hierarchyManager.getByIdIn(ids);
    }

    @Override
    public void deleteHierarchyWithoutReplicons() {
        List<Long> ids = repliconManager
                .getAll()
                .stream()
                .map(replicon -> replicon.getHierarchyEntity().getId())
                .distinct()
                .collect(Collectors.toList());
        hierarchyManager.deleteAllByIdNotIn(ids);
    }
}
