package fr.unistra.bioinfo.persistence.entities;

import fr.unistra.bioinfo.common.CommonUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
public class Replicon extends AbstractEntity<Long> implements Comparable<Replicon> {

    @Id
    @GeneratedValue
    @Column
    private Long id;

    @NaturalId
    private String replicon;

    @Column(nullable = false)
    private Integer version = 1;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Hierarchy hierarchy;

    @ElementCollection
    @Column(nullable = false)
    @CollectionTable
    @MapKeyColumn
    private Map<String, Integer> trinucleotides;

    @ElementCollection
    @Column(nullable = false)
    @CollectionTable
    @MapKeyColumn
    private Map<String, Integer> dinucleotides;

    @Column
    private boolean isDownloaded = false;

    @Column
    private boolean isComputed = false;

    public Replicon() {
        resetCounters();
    }

    public Replicon(String replicon, Hierarchy hierarchy) {
        this(replicon, 1, hierarchy);
    }

    public Replicon(String replicon, Integer version, Hierarchy hierarchy) {
        super();
        setReplicon(replicon);
        setVersion(version);
        setHierarchy(hierarchy);
        resetCounters();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getReplicon() {
        return replicon;
    }

    public void setReplicon(String replicon) {
        this.replicon = replicon;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Hierarchy getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(Hierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    public Map<String, Integer> getTrinucleotides() {
        return trinucleotides;
    }

    public void setTrinucleotides(Map<String, Integer> trinucleotides) {
        this.trinucleotides = trinucleotides;
    }

    public Map<String, Integer> getDinucleotides() {
        return dinucleotides;
    }

    public void setDinucleotides(Map<String, Integer> dinucleotides) {
        this.dinucleotides = dinucleotides;
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

    public void resetCounters() {
        Map<String, Integer> diMap = new HashMap<>();
        Map<String, Integer> triMap = new HashMap<>();
        for (String dinucleotide : CommonUtils.DINUCLEOTIDES) {
            diMap.put(dinucleotide, 0);
        }
        for (String trinucleotide : CommonUtils.TRINUCLEOTIDES) {
            triMap.put(trinucleotide, 0);
        }
        setDinucleotides(diMap);
        setTrinucleotides(triMap);
    }

    @Override
    public int compareTo(Replicon o) {
        // Utilisé par Collection.contains()
        return ObjectUtils.compare(id, o.id);
    }

}
