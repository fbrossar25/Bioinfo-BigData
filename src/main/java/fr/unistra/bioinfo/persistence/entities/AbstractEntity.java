package fr.unistra.bioinfo.persistence.entities;


import java.io.Serializable;

public abstract class AbstractEntity<K extends Serializable> implements Serializable {
    protected K id;

    public AbstractEntity(){
    }

    public abstract K getId();

    public abstract void setId(K id);
}
