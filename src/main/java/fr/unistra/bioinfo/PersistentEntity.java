package fr.unistra.bioinfo;

import java.io.Serializable;

public abstract class PersistentEntity implements Serializable{
    protected long id;
    public abstract long getId();

    private void setId(long id){
        this.id = id;
    }
}
