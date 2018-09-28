package fr.unistra.bioinfo.persistence.entities;


import javax.persistence.*;

@Entity
@Table(name = "HIERARCHY", uniqueConstraints = {@UniqueConstraint(columnNames = {"HIERARCHY_ORGANISM"}, name = "CONST_UNIQUE_ORGANISM")})
public class HierarchyEntity extends AbstractEntity<Long> {
    private String kingdom;
    private String group;
    private String subgroup;
    private String organism;

    //Pour Hibernate
    public HierarchyEntity(){super();}

    public HierarchyEntity(String kingdom, String group, String subgroup, String organism) {
        setOrganism(organism);
        setKingdom(kingdom);
        setGroup(group);
        setSubgroup(subgroup);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HIERARCHY_ID", updatable = false, nullable = false)
    public Long getId() {
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }


    @Column(name="HIERARCHY_ORGANISM", nullable = false)
    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
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
