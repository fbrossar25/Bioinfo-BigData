package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import org.springframework.lang.NonNull;

public interface HierarchyService extends AbstractService<HierarchyEntity, Long> {
    HierarchyEntity getByOrganism(@NonNull String organism);
    HierarchyEntity getByOrganism(@NonNull String organism, boolean createIfNotExists);
}
