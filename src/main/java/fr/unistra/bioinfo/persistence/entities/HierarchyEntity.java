package fr.unistra.bioinfo.persistence.entities;


import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "HIERARCHY")
public class HierarchyEntity extends PersistentEntity<String>{
    private String kingdom;
    private String group;
    private String subgroup;

    //Pour Hibernate
    public HierarchyEntity(){super();}

    public HierarchyEntity(String kingdom, String group, String subgroup, String organism) {
        setId(organism);
        setKingdom(kingdom);
        setGroup(group);
        setSubgroup(subgroup);
    }

    @Id
    @Column(name="HIERARCHY_ORGANISM", nullable = false)
    public String getId() {
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    @Transient
    public String getOrganism() {
        return getId();
    }

    public void setOrganism(String organism) {
        setId(organism);
    }

    @Column(name="HIERARCHY_KINGDOM", nullable = false)
    public String getKingdom() {
        return kingdom;
    }

    public void setKingdom(String kingdom) {
        this.kingdom = kingdom;
    }

    @Column(name="HIERARCHY_GROUP", nullable = false)
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Column(name="HIERARCHY_SUBGROUP", nullable = false)
    public String getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(String subgroup) {
        this.subgroup = subgroup;
    }

}
