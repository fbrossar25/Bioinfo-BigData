package fr.unistra.bioinfo.persistence.entity;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class HierarchyEntity implements IEntity<Long>, Comparable<HierarchyEntity>{
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String kingdom;

    @Column(nullable = false, name = "H_GROUP")
    private String group;

    @Column(nullable = false)
    private String subgroup;

    @Column(nullable = false)
    private String organism;

//    @OneToMany(mappedBy = "hierarchyEntity", orphanRemoval = true, cascade = CascadeType.REMOVE)
//    @OnDelete(action = OnDeleteAction.CASCADE)
//    private List<RepliconEntity> repliconEntities = new ArrayList<>();

    public HierarchyEntity(){}

    public HierarchyEntity(String kingdom, String group, String subgroup, String organism) {
        super();
        setOrganism(organism);
        setKingdom(kingdom);
        setGroup(group);
        setSubgroup(subgroup);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKingdom() {
        return kingdom;
    }

    public void setKingdom(String kingdom) {
        this.kingdom = kingdom;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(String subgroup) {
        this.subgroup = subgroup;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

//    /**
//     * @return une copie défensive de la liste des repliconEntities de l'auteur
//     */
//    public List<RepliconEntity> getRepliconEntities() {
//        return new ArrayList<>(repliconEntities);
//    }
//
//    public void addRepliconEntity(RepliconEntity repliconEntity){
//        addRepliconEntity(repliconEntity, true);
//    }
//
//    public void addRepliconEntity(RepliconEntity repliconEntity, boolean set){
//        if(repliconEntity != null){
//            if(repliconEntities.contains(repliconEntity)){
//                repliconEntities.set(repliconEntities.indexOf(repliconEntity), repliconEntity);
//            }else{
//                repliconEntities.add(repliconEntity);
//            }
//            if(set){
//                repliconEntity.setHirarchyEntity(this, false);
//            }
//        }
//    }
//
//    public void removeRepliconEntity(RepliconEntity repliconEntity){
//        if(repliconEntity != null && repliconEntities.contains(repliconEntity)){
//            repliconEntities.remove(repliconEntity);
//            repliconEntity.setHierarchyEntity(null);
//        }
//    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof HierarchyEntity))
            return false;

        final HierarchyEntity a = (HierarchyEntity)object;

        if (id != null && a.getId() != null) {
            return id.equals(a.getId());
        }
        return false;
    }

    @Override
    public String toString(){
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append(id)
                .append(kingdom)
                .append(group)
                .append(subgroup)
                .append(organism)
                .build();
    }

    @Override
    public int compareTo(HierarchyEntity o) {
        return new CompareToBuilder()
                .append(kingdom, o.kingdom)
                .append(group, o.group)
                .append(subgroup, o.subgroup)
                .append(organism, o.organism)
                .build();
    }

//    public boolean containsReplicon(RepliconEntity r) {
//        return this.repliconEntities.contains(r);
//    }
}
