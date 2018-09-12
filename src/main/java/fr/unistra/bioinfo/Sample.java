package fr.unistra.bioinfo;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "SAMPLE")
public class Sample extends PersistentEntity {
    private String description;

    @Id
    @Column(name="ID")
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    public long getId() {
        return id;
    }

    @Column(name = "DESCRIPTION")
    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }
}
