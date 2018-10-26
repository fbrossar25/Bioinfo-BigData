package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;

public interface HierarchyService extends AbstractService<HierarchyEntity, Long> {
    HierarchyEntity getByOrganism(String organism);
}
