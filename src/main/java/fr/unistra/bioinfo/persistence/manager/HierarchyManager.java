package fr.unistra.bioinfo.persistence.manager;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface HierarchyManager extends CrudRepository<HierarchyEntity, Long> {
    @Query("from HierarchyEntity")
    List<HierarchyEntity> getAll();
}
