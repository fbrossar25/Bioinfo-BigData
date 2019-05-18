package fr.unistra.bioinfo.persistence.manager;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HierarchyManager extends IManager<HierarchyEntity, Long> {
    @Query("from HierarchyEntity")
    List<HierarchyEntity> getAll();
    List<HierarchyEntity> getByIdIn(List<Long> ids);

    void deleteAllByIdNotIn(List<Long> ids);

    HierarchyEntity getByOrganism(String organism);

    List<HierarchyEntity> getHierarchyEntitiesByKingdomAndGroupAndSubgroup(String kingdom, String group, String subgroup);
    List<HierarchyEntity> getHierarchyEntitiesByKingdomAndGroup(String kingdom, String group);
    List<HierarchyEntity> getHierarchyEntitiesByKingdom(String kingdom);
}
