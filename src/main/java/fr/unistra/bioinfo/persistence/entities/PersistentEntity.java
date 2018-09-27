package fr.unistra.bioinfo.persistence.entities;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

public abstract class PersistentEntity<K extends Serializable> implements Serializable {
    private static Logger LOGGER = LogManager.getLogger();
    protected K id;

    public PersistentEntity(){
    }

    public abstract K getId();

    public abstract void setId(K id);
}
