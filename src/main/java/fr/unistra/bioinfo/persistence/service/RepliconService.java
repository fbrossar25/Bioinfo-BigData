package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;

import java.util.List;

public interface RepliconService extends AbstractService<RepliconEntity, Long> {
    List<RepliconEntity> getByHierarchy(HierarchyEntity hierarchy);
}
