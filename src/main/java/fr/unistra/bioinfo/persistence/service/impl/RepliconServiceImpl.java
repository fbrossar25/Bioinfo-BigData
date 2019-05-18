package fr.unistra.bioinfo.persistence.service.impl;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.persistence.entity.CountersEntity;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.members.HierarchyMembers;
import fr.unistra.bioinfo.persistence.manager.CountersManager;
import fr.unistra.bioinfo.persistence.manager.HierarchyManager;
import fr.unistra.bioinfo.persistence.manager.RepliconManager;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import fr.unistra.bioinfo.stats.GeneralInformationSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RepliconServiceImpl extends AbstractServiceImpl<RepliconEntity, Long> implements RepliconService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepliconServiceImpl.class);
    private final RepliconManager repliconManager;
    private final HierarchyManager hierarchyManager;
    private final CountersManager countersManager;

    @Autowired
    public RepliconServiceImpl(RepliconManager repliconManager, HierarchyManager hierarchyManager, CountersManager countersManager) {
        super();
        this.repliconManager = repliconManager;
        this.hierarchyManager = hierarchyManager;
        this.countersManager = countersManager;
    }

    private void saveForeignEntities(@NonNull RepliconEntity entity){
        HierarchyEntity h = entity.getHierarchyEntity();
        if(h != null && h.getId() == null){
            hierarchyManager.save(h);
        }
        CountersEntity c = entity.getCounters();
        if(c == null){
            c = new CountersEntity();
            entity.setCounters(c);
            countersManager.save(c);
        }else if(c.getId() == null){
            countersManager.save(c);
        }
    }

    @Override
    public RepliconEntity save(@NonNull RepliconEntity entity) {
        saveForeignEntities(entity);
        return super.save(entity);
    }

    @Override
    public List<RepliconEntity> saveAll(@NonNull List<RepliconEntity> entities) {
        for(RepliconEntity entity : entities){
            saveForeignEntities(entity);
        }
        return super.saveAll(entities);
    }

    public RepliconManager getManager(){
        return repliconManager;
    }

    @Override
    public List<RepliconEntity> getByHierarchy(@NonNull HierarchyEntity hierarchy) {
        return repliconManager.getAllByHierarchyEntity(hierarchy);
    }

    @Override
    public RepliconEntity getByName(@NonNull String name) {
        return repliconManager.getByName(name);
    }

    @Override
    public void deleteWhereNameIsNotIn(@NonNull Collection<String> names) {
        if(!names.isEmpty())
            repliconManager.deleteAllByNameNotIn(names);
    }

    @Override
    public List<RepliconEntity> getNotDownloadedReplicons() {
        return repliconManager.getAllNotDownloaded();
    }

    @Override
    public List<RepliconEntity> getByNameIn(@NonNull List<String> repliconsNames){
        return repliconManager.getAllByNameIn(repliconsNames);
    }

    @Override
    public Boolean hasRepliconToProceedForSubgroup(@NonNull HierarchyEntity hierarchyEntity) {
        return repliconManager.existsByComputedFalseAndParsedTrueAndHierarchyEntityKingdomAndHierarchyEntityGroupAndHierarchyEntitySubgroup(hierarchyEntity.getKingdom(), hierarchyEntity.getGroup(), hierarchyEntity.getSubgroup());
    }
    @Override
    public Boolean hasRepliconToProceedForGroup(@NonNull HierarchyEntity hierarchyEntity) {
        return repliconManager.existsByComputedFalseAndParsedTrueAndHierarchyEntityKingdomAndHierarchyEntityGroup(hierarchyEntity.getKingdom(), hierarchyEntity.getGroup());
    }
    @Override
    public Boolean hasRepliconToProceedForKingdom(@NonNull HierarchyEntity hierarchyEntity) {
        return repliconManager.existsByComputedFalseAndParsedTrueAndHierarchyEntityKingdom(hierarchyEntity.getKingdom());
    }

    @Override
    public void setAllComputedFalse(){
        Pageable pageable = PageRequest.of(0, 500);
        Page<RepliconEntity> page;
        while(true){
            page = getAll(pageable);
            saveAll(page.getContent().stream().peek(r -> r.setComputed(false)).collect(Collectors.toList()));
            if(page.hasNext()){
                pageable = page.nextPageable();
            }else{
                break;
            }
        }
    }
}
