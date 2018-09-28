package fr.unistra.bioinfo.persistence.entities;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.persistence.MapToStringConverter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "REPLICONS", uniqueConstraints = {@UniqueConstraint(columnNames = {"REPLICON"}, name = "CONST_UNIQUE_REPLICON")})
public class RepliconEntity extends AbstractEntity<Long> {
    private String replicon;
    private Map<String, Integer> trinucleotides;
    private Map<String, Integer> dinucleotides;
    private boolean isDownloaded = false;
    private boolean isComputed = false;
    private Integer version;
    private HierarchyEntity hierarchy;

    //Pour Hibernate
    public RepliconEntity(){
        super();
        resetCounters();
    }

    public RepliconEntity(String replicon, HierarchyEntity hierarchy){
        this(replicon,1, hierarchy);
    }

    public RepliconEntity(String replicon, Integer version, HierarchyEntity hierarchy){
        super();
        setReplicon(replicon);
        setVersion(version);
        setHierarchy(hierarchy);
        resetCounters();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ID", updatable = false, nullable = false)
    public Long getId() {
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }


    @Column(name="REPLICON", nullable = false, unique = true)
    public String getReplicon() {
        return replicon;
    }

    public void setReplicon(String replicon) {
        this.replicon = replicon;
    }

    /**
     * @return la chaîne contenant les compteurs des trinucleotides
     */
    @Convert(converter = MapToStringConverter.class)
    @Column(name="TRINUCLEOTIDES", columnDefinition = "LONGVARCHAR")
    public Map<String, Integer> getTrinucleotides() {
        return trinucleotides;
    }

    public void setTrinucleotides(Map<String, Integer> trinucleotides) {
        this.trinucleotides = trinucleotides;
    }

    /**
     * @return la chaîne contenant les compteurs des dinucleotides
     */
    @Convert(converter = MapToStringConverter.class)
    @Column(name="DINUCLEOTIDES", columnDefinition = "LONGVARCHAR")
    public Map<String, Integer> getDinucleotides() {
        return dinucleotides;
    }

    public void setDinucleotides(Map<String, Integer> dinucleotides){
        this.dinucleotides = dinucleotides;
    }

    /**
     * @return indique si le fichier est présent en local
     */
    @Column(name="DOWNLOADED")
    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    /**
     * @return indique si le fichier à été traité, càd que les compteurs sont à jours
     */
    @Column(name="COMPUTED")
    public boolean isComputed() {
        return isComputed;
    }

    public void setComputed(boolean computed) {
        isComputed = computed;
    }

    /**
     * @return retourne la hiérarchie du replicon
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HIERARCHY", nullable = false, foreignKey = @ForeignKey(name = "FK_HIERARCHY"))
    public HierarchyEntity getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(HierarchyEntity hierarchy) {
        this.hierarchy = hierarchy;
    }

    /**
     * @return Retourne la version local du replicon
     */
    @Column(name = "VERSION")
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void resetCounters(){
        Map<String, Integer> diMap = new HashMap<>();
        Map<String, Integer> triMap = new HashMap<>();
        for(String dinucleotide : CommonUtils.DINUCLEOTIDES){
            diMap.put(dinucleotide, 0);
        }
        for(String trinucleotide : CommonUtils.TRINUCLEOTIDES){
            triMap.put(trinucleotide, 0);
        }
        setDinucleotides(diMap);
        setTrinucleotides(triMap);
    }
}
