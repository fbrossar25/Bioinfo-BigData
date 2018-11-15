package fr.unistra.bioinfo.persistence.entity;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.parsing.Phase;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
public class RepliconEntity implements IEntity<Long>, Comparable<RepliconEntity>{
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer version = 1;

    @ManyToOne
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
        this.hierarchyEntity = hierarchyEntity;
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

    public Integer getDinucleotideCount(String dinucleotide, Phase phase){
        if(CommonUtils.DINUCLEOTIDES.contains(dinucleotide.toUpperCase()) && phase != null){
            return dinucleotides.get(dinucleotide.toUpperCase()+"-"+phase.toString());
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' phase '"+phase+"' n'est pas un dinucleotide valide");
        }
    }

    public void setDinucleotideCount(String dinucleotide, Phase phase, Integer count){
        if(CommonUtils.DINUCLEOTIDES.contains(dinucleotide.toUpperCase()) && phase != null){
            dinucleotides.put(dinucleotide.toUpperCase()+"-"+phase.toString(), count == null ? 0 : count);
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' phase '"+phase+"' n'est pas un dinucleotide valide");
        }
    }

    public Integer getTrinucleotideCount(String trinucleotide, Phase phase){
        if(CommonUtils.TRINUCLEOTIDES.contains(trinucleotide.toUpperCase()) && phase != null){
            return trinucleotides.get(trinucleotide+"-"+phase.toString());
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' phase '"+phase+"' n'est pas un trinucleotide valide");
        }
    }

    public void setTrinucleotideCount(String trinucleotide, Phase phase, Integer count){
        if(CommonUtils.TRINUCLEOTIDES.contains(trinucleotide.toUpperCase()) && phase != null){
            trinucleotides.put(trinucleotide+"-"+phase.toString(), count == null ? 0 : count);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' phase '"+phase+"' n'est pas un trinucleotide valide");
        }
    }

    public void incrementDinucleotideCount(String dinucleotide, Phase phase){
        if(CommonUtils.DINUCLEOTIDES.contains(dinucleotide.toUpperCase()) && phase != null){
            trinucleotides.put(dinucleotide+"-"+phase.toString(), trinucleotides.get(dinucleotide) + 1);
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' phase '"+phase+"' n'est pas un dinucleotide valide");
        }
    }

    public void incrementTrinucleotideCount(String trinucleotide, Phase phase){
        if(CommonUtils.TRINUCLEOTIDES.contains(trinucleotide.toUpperCase()) && phase != null){
            trinucleotides.put(trinucleotide+"-"+phase.toString(), trinucleotides.get(trinucleotide) + 1);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' phase '"+phase+"' n'est pas un trinucleotide valide");
        }
    }

    public void resetCounters() {
        for(Phase phase : Phase.values()){
            for (String dinucleotide : CommonUtils.DINUCLEOTIDES) {
                trinucleotides.put(dinucleotide+"-"+phase.toString(), 0);
            }
            for (String trinucleotide : CommonUtils.TRINUCLEOTIDES) {
                trinucleotides.put(trinucleotide+"-"+phase.toString(), 0);
            }
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

    @Override
    public int compareTo(RepliconEntity o) {
        return new CompareToBuilder()
                .append(getGenbankName(), o.getGenbankName())
                .append(isComputed, o.isComputed)
                .append(isDownloaded, o.isDownloaded)
                .build();
    }
}
