package fr.unistra.bioinfo.persistence.entities;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

public abstract class PersistentEntity implements Serializable {
    private static Logger LOGGER = LogManager.getLogger();
    private static long idCounter = -1;
    protected long id;

    protected PersistentEntity(){
        setId(idCounter--);
        LOGGER.debug("New PersistentEntity id : "+id);
    }

    public abstract long getId();

    private void setId(long id){
        this.id = id;
    }
}
