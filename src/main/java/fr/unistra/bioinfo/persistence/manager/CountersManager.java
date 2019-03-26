package fr.unistra.bioinfo.persistence.manager;

import fr.unistra.bioinfo.persistence.entity.CountersEntity;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CountersManager extends IManager<CountersEntity, Long> {
    @Query("from CountersEntity")
    List<CountersEntity> getAll();
}
