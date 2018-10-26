package fr.unistra.bioinfo.persistence.entity.members;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;

public enum HierarchyMembers implements EntityMembers<HierarchyEntity> {

    ID("id"),
    KINGDOM("kingdom"),
    GROUP("group"),
    SUBGROUP("subgroup"),
    ORGANISM("organism");

    private String name;

    private HierarchyMembers(String name){
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<HierarchyEntity> getEntityClass() {
        return HierarchyEntity.class;
    }
}
