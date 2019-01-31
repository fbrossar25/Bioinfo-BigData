package fr.unistra.bioinfo.persistence.entity;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.parsing.Phase;
import fr.unistra.bioinfo.parsing.RepliconType;
import fr.unistra.bioinfo.persistence.entity.converters.MapStringIntConverter;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.NaturalId;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
public class RepliconEntity implements IEntity<Long>, Comparable<RepliconEntity>{
    @Id
    @GeneratedValue
    private Long id;

    @NaturalId
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(nullable = false)
    private RepliconType type = RepliconType.DNA;

    @ManyToOne
    @JoinColumn(nullable = false)
    private HierarchyEntity hierarchyEntity;

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = MapStringIntConverter.class)
    private Map<String, Integer> dinucleotides = new HashMap<>();

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = MapStringIntConverter.class)
    private Map<String, Integer> trinucleotides = new HashMap<>();

    @Column(nullable = false)
    private boolean isDownloaded = false;

    @Column(nullable = false)
    private boolean isComputed = false;

    @Column(nullable = false)
    private boolean isParsed = false;

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

    /**
     * Nom du replicon, sans la version
     * @return Le nom du replicon sans sa version
     */
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

    /**
     * Version du replicon sur genbank
     * @return la version
     */
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * Indique si le fichier du replicon à été téléchargé
     * @return true si le fichier à été téléchargé, false sinon
     */
    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    /**
     * Indique si le fichier à été parsé, et le comptage effectué.
     * @return true si le parsing et le comptage sont faits, false sinon
     */
    public boolean isParsed() {
        return isParsed;
    }

    public void setParsed(boolean parsed) {
        isParsed = parsed;
    }

    /**
     * Indique si les statistiques ont été générées pour ce replicon.
     * @return true si les statistiques ont été générées, false sinon
     */
    public boolean isComputed() {
        return isComputed;
    }
    public void setComputed(boolean computed) {
        isComputed = computed;
    }

    public Integer getDinucleotideCount(@NonNull String dinucleotide,@NonNull Phase phase){
        String key  = dinucleotide.toUpperCase()+"-"+phase.toString();
        if(dinucleotides.containsKey(key)){
            return dinucleotides.get(key);
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' phase '"+phase+"' n'est pas un dinucleotide valide");
        }
    }

    public Integer getTrinucleotideCount(@NonNull String trinucleotide,@NonNull Phase phase){
        String key  = trinucleotide.toUpperCase()+"-"+phase.toString();
        if(trinucleotides.containsKey(key)){
            return trinucleotides.get(key);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' phase '"+phase+"' n'est pas un trinucleotide valide");
        }
    }

    public void incrementDinucleotideCount(@NonNull String dinucleotide,@NonNull Phase phase){
        String key  = dinucleotide.toUpperCase()+"-"+phase.toString();
        if(dinucleotides.containsKey(key)){
            dinucleotides.put(key, dinucleotides.get(key) + 1);
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' phase '"+phase+"' n'est pas un dinucleotide valide");
        }
    }

    public void incrementTrinucleotideCount(String trinucleotide,@NonNull Phase phase){
        String key  = trinucleotide.toUpperCase()+"-"+phase.toString();
        if(trinucleotides.containsKey(key)){
            trinucleotides.put(key, trinucleotides.get(key) + 1);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' phase '"+phase+"' n'est pas un trinucleotide valide");
        }
    }

    private void resetCounters() {
        for(Phase phase : Phase.values()){
            if(phase != Phase.PHASE_2) {
                for (String dinucleotide : CommonUtils.DINUCLEOTIDES) {
                    dinucleotides.put(dinucleotide+"-"+phase.toString(), 0);
                }
            }
            for (String trinucleotide : CommonUtils.TRINUCLEOTIDES) {
                trinucleotides.put(trinucleotide+"-"+phase.toString(), 0);
            }
        }
    }

    /**
     * Retourne le nom du réplicon concaténé à sa version.</br>
     * Example : NC_123456.1
     * @return Le nom genbank de ce replicon
     */
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

    public RepliconType getType() {
        return type;
    }

    public void setType(RepliconType type) {
        this.type = type;
    }
}
