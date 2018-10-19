package fr.unistra.bioinfo.persistence.manager;

import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RepliconManager extends IManager<RepliconEntity, Long> {
    @Query("from RepliconEntity")
    List<RepliconEntity> getAll();
}
