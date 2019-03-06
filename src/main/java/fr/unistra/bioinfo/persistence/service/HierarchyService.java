package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import org.springframework.lang.NonNull;

import java.util.List;

public interface HierarchyService extends AbstractService<HierarchyEntity, Long> {
    HierarchyEntity getByOrganism(@NonNull String organism);
    HierarchyEntity getByOrganism(@NonNull String organism, boolean createIfNotExists);
    List<HierarchyEntity> getBySubgroup(@NonNull String subgroup);
}
