package fr.unistra.bioinfo.persistence.entities;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.persistence.MapToStringConverter;
import fr.unistra.bioinfo.persistence.managers.PersistentEntityManager;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "REPLICONS")
public class RepliconEntity extends PersistentEntity{
    private String replicon;
    private Map<String, Integer> trinucleotides;
    private Map<String, Integer> dinucleotides;
    private boolean isDownloaded = false;
    private boolean isComputed = false;
    private Integer version;
    private HierarchyEntity hierarchy;

    //Pour Hibernate
    private RepliconEntity(){resetCounters();}

    public RepliconEntity(String replicon, HierarchyEntity hierarchy){
        setReplicon(replicon);
        setHierarchy(hierarchy);
        resetCounters();
    }

    @Id
    @Column(name="ID")
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    public long getId() {
        return id;
    }

    /**
     * @return Le nom du replicon (NC_*)
     */
    @Column(name="REPLICON", nullable = false)
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
    @ManyToOne(optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "HIERARCHY_ID")
    public HierarchyEntity getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(HierarchyEntity hierarchy) {
        this.hierarchy = hierarchy;
        if(hierarchy!=null && hierarchy.getId() < 0){
            PersistentEntityManager<HierarchyEntity> mgr = PersistentEntityManager.create(HierarchyEntity.class);
            mgr.save(this.hierarchy);
        }
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
