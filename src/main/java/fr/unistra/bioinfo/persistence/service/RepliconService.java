package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;

public interface RepliconService extends AbstractService<RepliconEntity, Long> {
    List<RepliconEntity> getByHierarchy(@NonNull HierarchyEntity hierarchy);
    RepliconEntity getByName(@NonNull String name);

    void deleteWhereNameIsNotIn(@NonNull Collection<String> names);

    List<RepliconEntity> getNotDownloadedReplicons();

    List<RepliconEntity> getByNameIn(@NonNull List<String> repliconsNames);
}
