package fr.unistra.bioinfo.persistence.manager;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface RepliconManager extends IManager<RepliconEntity, Long> {
    @Query("from RepliconEntity")
    List<RepliconEntity> getAll();

    List<RepliconEntity> getAllByHierarchyEntity(HierarchyEntity hierarchyEntity);

    void deleteAllByHierarchyEntity(HierarchyEntity hierarchyEntity);

    void deleteAllByHierarchyEntityIn(List<HierarchyEntity> hierarchyEntities);

    void deleteAllByNameNotIn(Collection<String> names);

    RepliconEntity getByName(String name);

    List<RepliconEntity> getAllByComputed(boolean computed);

    List<RepliconEntity> getAllByParsed(boolean parsed);

    @Query("from RepliconEntity where fileName is null or fileName not like '%_.gb'")
    List<RepliconEntity> getAllNotDownloaded();

    List<RepliconEntity> getAllByNameIn(List<String> repliconsNames);
}
