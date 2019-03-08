package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import org.springframework.lang.NonNull;

import java.util.List;

public interface HierarchyService extends AbstractService<HierarchyEntity, Long> {
    HierarchyEntity getByOrganism(@NonNull String organism);
    HierarchyEntity getByOrganism(@NonNull String organism, boolean createIfNotExists);
    List<HierarchyEntity> getBySubgroup(@NonNull String subgroup);
    List<HierarchyEntity> getByGroup(@NonNull String group);
    List<HierarchyEntity> getByKingdom(@NonNull String kingdom);

    /**
     * Liste les organismes contenants au moins un replicon dont le fichier l'excel n'as pas été généré
     * @return Liste des organismes
     */
    List<HierarchyEntity> getOrganismToUpdateExcel();
    List<HierarchyEntity> getByIds(List<Long> ids);

    void deleteHierarchyWithoutReplicons();
}
