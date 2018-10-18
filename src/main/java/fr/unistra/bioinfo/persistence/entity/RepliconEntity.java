package fr.unistra.bioinfo.persistence.entity;

import fr.unistra.bioinfo.common.CommonUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
public class RepliconEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer version = 1;

    @ManyToOne(cascade = {CascadeType.REFRESH, CascadeType.MERGE, CascadeType.DETACH})
    @JoinColumn(nullable = false)
    private HierarchyEntity hierarchyEntity;

    @ElementCollection
    @Column(nullable = false)
    @CollectionTable
    @MapKeyColumn
    private Map<String, Integer> dinucleotides = new HashMap<>();

    @ElementCollection
    @Column(nullable = false)
    @CollectionTable
    @MapKeyColumn
    private Map<String, Integer> trinucleotides = new HashMap<>();

    @Column(nullable = false)
    private boolean isDownloaded = false;

    @Column(nullable = false)
    private boolean isComputed = false;

    public RepliconEntity() {
        resetCounters();
    }

    public RepliconEntity(String replicon, HierarchyEntity hierarchy) {
        this(replicon, 1, hierarchy);
    }

    public RepliconEntity(String replicon, Integer version, HierarchyEntity hierarchy) {
        setName(replicon);
        setVersion(version);
        setHierarchyEntity(hierarchy);
        resetCounters();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HierarchyEntity getHierarchyEntity() {
        return hierarchyEntity;
    }

    public void setHierarchyEntity(HierarchyEntity hierarchyEntity) {
        setHirarchyEntity(hierarchyEntity, true);
    }

    public void setHirarchyEntity(HierarchyEntity hierarchyEntity, boolean add) {
        if(this.hierarchyEntity == null || !this.hierarchyEntity.equals(hierarchyEntity)){
            this.hierarchyEntity = hierarchyEntity;
            if(add && hierarchyEntity != null){
                hierarchyEntity.addRepliconEntity(this, false);
            }
        }
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public boolean isComputed() {
        return isComputed;
    }

    public void setComputed(boolean computed) {
        isComputed = computed;
    }

    public Integer getDinucleotideCount(String dinucleotide){
        if(CommonUtils.DINUCLEOTIDES.contains(dinucleotide.toUpperCase())){
            return dinucleotides.get(dinucleotide);
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' n'est pas un dinucleotide valide");
        }
    }

    public void setDinucleotideCount(String dinucleotide, Integer count){
        if(CommonUtils.DINUCLEOTIDES.contains(dinucleotide.toUpperCase())){
            dinucleotides.put(dinucleotide, count == null ? 0 : count);
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' n'est pas un dinucleotide valide");
        }
    }

    public Integer getTrinucleotideCount(String trinucleotide){
        if(CommonUtils.TRINUCLEOTIDES.contains(trinucleotide.toUpperCase())){
            return trinucleotides.get(trinucleotide);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' n'est pas un trinucleotide valide");
        }
    }

    public void setTrinucleotideCount(String trinucleotide, Integer count){
        if(CommonUtils.TRINUCLEOTIDES.contains(trinucleotide.toUpperCase())){
            trinucleotides.put(trinucleotide, count == null ? 0 : count);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' n'est pas un trinucleotide valide");
        }
    }

    public void incrementDinucleotideCount(String dinucleotide){
        if(CommonUtils.DINUCLEOTIDES.contains(dinucleotide.toUpperCase())){
            trinucleotides.put(dinucleotide, trinucleotides.get(dinucleotide) + 1);
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' n'est pas un dinucleotide valide");
        }
    }

    public void incrementTrinucleotideCount(String trinucleotide){
        if(CommonUtils.TRINUCLEOTIDES.contains(trinucleotide.toUpperCase())){
            trinucleotides.put(trinucleotide, trinucleotides.get(trinucleotide) + 1);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' n'est pas un trinucleotide valide");
        }
    }

    public void resetCounters() {
        for (String dinucleotide : CommonUtils.DINUCLEOTIDES) {
            trinucleotides.put(dinucleotide, 0);
        }
        for (String trinucleotide : CommonUtils.TRINUCLEOTIDES) {
            trinucleotides.put(trinucleotide, 0);
        }
    }

    public String getGenbankName(){
        return ""+name+"."+version;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof RepliconEntity))
            return false;

        final RepliconEntity a = (RepliconEntity)object;

        if (id != null && a.getId() != null) {
            return id.equals(a.getId());
        }
        return false;
    }

    @Override
    public String toString(){
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append(id)
                .append(name)
                .append(version)
                .build();
    }
}
