package fr.unistra.bioinfo.persistence.manager;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RepliconManager extends CrudRepository<RepliconEntity, Long> {
    @Query("from RepliconEntity")
    List<HierarchyEntity> getAll();
}
