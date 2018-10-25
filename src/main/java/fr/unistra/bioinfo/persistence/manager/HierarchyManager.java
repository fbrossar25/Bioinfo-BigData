package fr.unistra.bioinfo.persistence.manager;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HierarchyManager extends IManager<HierarchyEntity, Long> {
    @Query("from HierarchyEntity")
    List<HierarchyEntity> getAll();

    HierarchyEntity getByOrganism(String organism);
}
