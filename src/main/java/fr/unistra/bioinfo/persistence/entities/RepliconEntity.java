package fr.unistra.bioinfo.persistence.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "REPLICONS")
public class RepliconEntity extends PersistentEntity{
    private String replicon;
    private String trinucleotides;
    private String dinucleotides;
    private boolean isDownloaded;
    private boolean isComputed;
    private HierarchyEntity hierarchy;
    //TODO créer un DTO avec un mapper pour les tri/dinucleotides

    @Id
    @Column(name="ID")
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    public long getId() {
        return id;
    }

    @Column(name="REPLICON")
    public String getReplicon() {
        return replicon;
    }

    public void setReplicon(String replicon) {
        this.replicon = replicon;
    }

    @Column(name="TRINUCLEOTIDES")
    public String getTrinucleotides() {
        return trinucleotides;
    }

    public void setTrinucleotides(String trinucleotides) {
        this.trinucleotides = trinucleotides;
    }

    @Column(name="DINUCLEOTIDES")
    public String getDinucleotides() {
        return dinucleotides;
    }

    public void setDinucleotides(String dinucleotides){
        this.dinucleotides = dinucleotides;
    }

    @Column(name="DOWNLOADED")
    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    @Column(name="COMPUTED")
    public boolean isComputed() {
        return isComputed;
    }

    public void setComputed(boolean computed) {
        isComputed = computed;
    }

    @ManyToOne
    @JoinColumn(name = "HIERARCHY_ID")
    public HierarchyEntity getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(HierarchyEntity hierarchy) {
        this.hierarchy = hierarchy;
    }
}
