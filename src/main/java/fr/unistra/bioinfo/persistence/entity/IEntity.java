package fr.unistra.bioinfo.persistence.entity;

import java.io.Serializable;

public interface IEntity<K extends Serializable>{
    K getId();
    void setId(K id);
}
