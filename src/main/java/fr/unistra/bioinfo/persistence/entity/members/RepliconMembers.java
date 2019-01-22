package fr.unistra.bioinfo.persistence.entity.members;

import fr.unistra.bioinfo.persistence.entity.RepliconEntity;

public enum RepliconMembers implements EntityMembers<RepliconEntity> {

    ID("id"),
    HIERARCHY("hierarchyEntity"),
    NAME("name"),
    VERSION("version"),
    TYPE("type"),
    DINUCLEOTIDES("dinucleotides"),
    TRINUCLEOTIDES("trinucleotides"),
    IS_DOWNLOADED("isDownloaded"),
    IS_COMPUTED("isComputed");

    private String name;

    private RepliconMembers(String name){
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<RepliconEntity> getEntityClass() {
        return RepliconEntity.class;
    }
}
