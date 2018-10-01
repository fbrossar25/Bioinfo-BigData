package fr.unistra.bioinfo.persistence.entities;


import javax.persistence.*;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "HIERARCHY", uniqueConstraints = {@UniqueConstraint(columnNames = {"H_ORGANISM"}, name = "CONST_UNIQUE_ORGANISM")})
public class HierarchyEntity extends AbstractEntity<Long> {
    private String kingdom;
    private String group;
    private String subgroup;
    private String organism;
    private Set<RepliconEntity> replicons = new TreeSet<>();

    //Pour Hibernate
    public HierarchyEntity(){super();}

    public HierarchyEntity(String kingdom, String group, String subgroup, String organism) {
        super();
        setOrganism(organism);
        setKingdom(kingdom);
        setGroup(group);
        setSubgroup(subgroup);
    }

    @Id
    @Column(name = "H_ID", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }


    @Column(name="H_ORGANISM", nullable = false)
    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    @Column(name="H_KINGDOM", nullable = false)
    public String getKingdom() {
        return kingdom;
    }

    public void setKingdom(String kingdom) {
        this.kingdom = kingdom;
    }

    @Column(name="H_GROUP", nullable = false)
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Column(name="H_SUBGROUP", nullable = false)
    public String getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(String subgroup) {
        this.subgroup = subgroup;
    }

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name="R_ID", foreignKey = @ForeignKey(name = "FK_HIERARCHY_REF_REPLICONS"))
    public Set<RepliconEntity> getReplicons() {
        return replicons;
    }

    public void setReplicons(Set<RepliconEntity> replicons) {
        this.replicons = replicons;
    }
}
