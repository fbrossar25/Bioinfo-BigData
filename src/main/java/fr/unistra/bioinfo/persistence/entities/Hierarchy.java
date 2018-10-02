package fr.unistra.bioinfo.persistence.entities;


import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Hierarchy extends AbstractEntity<Long> implements Comparable<Hierarchy>{

    @Id
    @Column
    @GeneratedValue
    private Long id;

    @NaturalId
    private String organism;

    @Column(nullable = false)
    private String subgroup;

    @Column(nullable = false, name = "h_group")
    private String group;

    @Column(nullable = false)
    private String kingdom;

//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "hierarchy")
//    private List<Replicon> replicons = new ArrayList<>();

    public Hierarchy(){}

    public Hierarchy(String kingdom, String group, String subgroup, String organism) {
        super();
        setOrganism(organism);
        setKingdom(kingdom);
        setGroup(group);
        setSubgroup(subgroup);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(String subgroup) {
        this.subgroup = subgroup;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getKingdom() {
        return kingdom;
    }

    public void setKingdom(String kingdom) {
        this.kingdom = kingdom;
    }

//    public List<Replicon> getReplicons() {
//        return replicons;
//    }
//
//    public void setReplicons(List<Replicon> replicons) {
//        this.replicons = replicons;
//    }

    @Override
    public int compareTo(Hierarchy o) {
        // Utilisé par Collection.contains()
        return ObjectUtils.compare(id, o.id);
    }
}
