package fr.unistra.bioinfo.persistence.entity.members;

import fr.unistra.bioinfo.persistence.entity.IEntity;

public interface EntityMembers<T extends IEntity> {
    String getName();

    Class<T> getEntityClass();
}
