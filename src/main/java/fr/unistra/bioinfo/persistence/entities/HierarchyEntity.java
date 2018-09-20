package fr.unistra.bioinfo.persistence.entities;


import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "HIERARCHY")
public class HierarchyEntity extends PersistentEntity{
    private String Kingdom;
    private String Group;
    private String Subgroup;
    private String Organism;

    @Id
    @Column(name="HIERARCHY_ID")
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    public long getId() {
        return id;
    }

    @Column(name="HIERARCHY_KINGDOM")
    public String getKingdom() {
        return Kingdom;
    }

    public void setKingdom(String kingdom) {
        Kingdom = kingdom;
    }

    @Column(name="HIERARCHY_GROUP")
    public String getGroup() {
        return Group;
    }

    public void setGroup(String group) {
        Group = group;
    }

    @Column(name="HIERARCHY_SUBGROUP")
    public String getSubgroup() {
        return Subgroup;
    }

    public void setSubgroup(String subgroup) {
        Subgroup = subgroup;
    }

    @Column(name="HIERARCHY_ORGANISM")
    public String getOrganism() {
        return Organism;
    }

    public void setOrganism(String organism) {
        Organism = organism;
    }

}
