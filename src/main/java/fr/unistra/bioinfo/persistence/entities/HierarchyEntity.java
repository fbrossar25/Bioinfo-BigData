package fr.unistra.bioinfo.persistence.entities;


import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "HIERARCHY",
        uniqueConstraints = @UniqueConstraint(
                name="CONST_UNIQUE_ALL_HIERARCHY",
                columnNames={"HIERARCHY_KINGDOM", "HIERARCHY_GROUP", "HIERARCHY_SUBGROUP", "HIERARCHY_ORGANISM"}
                )
)
public class HierarchyEntity extends PersistentEntity{
    private String kingdom;
    private String group;
    private String subgroup;
    private String organism;

    //Pour Hibernate
    private HierarchyEntity(){}

    public HierarchyEntity(String kingdom, String group, String subgroup, String organism) {
        setKingdom(kingdom);
        setGroup(group);
        setSubgroup(subgroup);
        setOrganism(organism);
    }

    @Id
    @Column(name="HIERARCHY_ID")
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    public long getId() {
        return id;
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

    @Column(name="HIERARCHY_ORGANISM", nullable = false)
    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

}
