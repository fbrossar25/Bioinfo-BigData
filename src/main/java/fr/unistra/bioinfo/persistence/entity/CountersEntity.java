package fr.unistra.bioinfo.persistence.entity;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.persistence.entity.converters.ListIntConverter;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
public class CountersEntity implements IEntity<Long>, Comparable<CountersEntity>{
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = ListIntConverter.class)
    private List<Integer> trinucleotides = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = ListIntConverter.class)
    private List<Integer> trinucleotides_pref = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = ListIntConverter.class)
    private List<Integer> dinucleotides = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = ListIntConverter.class)
    private List<Integer> dinucleotides_pref = new ArrayList<>();

    public CountersEntity() {
        super();
        resetCounters();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Integer> getTrinucleotides() {
        return trinucleotides;
    }

    public void setTrinucleotides(List<Integer> trinucleotides) {
        this.trinucleotides = trinucleotides;
    }

    public List<Integer> getTrinucleotides_pref() {
        return trinucleotides_pref;
    }

    public void setTrinucleotides_pref(List<Integer> trinucleotides_pref) {
        this.trinucleotides_pref = trinucleotides_pref;
    }

    public List<Integer> getDinucleotides() {
        return dinucleotides;
    }

    public void setDinucleotides(List<Integer> dinucleotides) {
        this.dinucleotides = dinucleotides;
    }

    public List<Integer> getDinucleotides_pref() {
        return dinucleotides_pref;
    }

    public void setDinucleotides_pref(List<Integer> dinucleotides_pref) {
        this.dinucleotides_pref = dinucleotides_pref;
    }

    void setDinucleotideCount(@NonNull String din, @NonNull Phase ph, @NonNull Integer value ) {
        Integer key = getNucleotidePhaseKey(din, ph);
        if(key != null){
            this.dinucleotides.set(key, value);
        }else{
            throw new IllegalArgumentException("'"+din+"' phase '"+ph+"' n'est pas un dinucleotide valide");
        }
    }
    void setTrinucleotideCount(@NonNull String tri, @NonNull Phase ph, @NonNull Integer value ) {
        Integer key = getNucleotidePhaseKey(tri, ph);
        if(key != null){
            trinucleotides.set(key, value);
        }else{
            throw new IllegalArgumentException("'"+tri+"' phase '"+ph+"' n'est pas un trinucleotide valide");
        }
    }

    Integer getDinucleotideCount(@NonNull String dinucleotide,@NonNull Phase phase){
        Integer key = getNucleotidePhaseKey(dinucleotide, phase);
        if(key != null){
            return dinucleotides.get(key);
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' phase '"+phase+"' n'est pas un dinucleotide valide");
        }
    }

    Integer getTrinucleotideCount(@NonNull String trinucleotide,@NonNull Phase phase){
        Integer key = getNucleotidePhaseKey(trinucleotide, phase);
        if(key != null){
            return trinucleotides.get(key);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' phase '"+phase+"' n'est pas un trinucleotide valide");
        }
    }

    void incrementDinucleotideCount(@NonNull String dinucleotide,@NonNull Phase phase){
        Integer key = getNucleotidePhaseKey(dinucleotide, phase);
        if(key != null){
            dinucleotides.set(key, dinucleotides.get(key) + 1);
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' phase '"+phase+"' n'est pas un dinucleotide valide");
        }
    }

    void incrementTrinucleotideCount(@NonNull String trinucleotide,@NonNull Phase phase){
        Integer key = getNucleotidePhaseKey(trinucleotide, phase);
        if(key != null){
            trinucleotides.set(key, trinucleotides.get(key) + 1);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' phase '"+phase+"' n'est pas un trinucleotide valide");
        }
    }

    Integer getPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase){
        Integer key = getNucleotidePhaseKey(trinucleotide, phase);
        if(key != null){
            return trinucleotides_pref.get(key);
        }else{
            throw new IllegalArgumentException("("+trinucleotide+","+phase+") n'est pas un couple trinucleotide-phase valide");
        }
    }

    void setPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase){
        Integer key = getNucleotidePhaseKey(trinucleotide, phase);
        if(key != null){
            trinucleotides_pref.set(key, 1);
        }else{
            throw new IllegalArgumentException("("+trinucleotide+","+phase+") n'est pas un couple trinucleotide-phase valide");
        }
    }

    void setPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase, @NonNull Integer value){
        Integer key = getNucleotidePhaseKey(trinucleotide, phase);
        if(key != null){
            trinucleotides_pref.set(key, value);
        }else{
            throw new IllegalArgumentException("("+trinucleotide+","+phase+") n'est pas un couple trinucleotide-phase valide");
        }
    }

    void unsetPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase){
        Integer key = getNucleotidePhaseKey(trinucleotide, phase);
        if(key != null){
            trinucleotides_pref.set(key, 0);
        }else{
            throw new IllegalArgumentException("("+trinucleotide+","+phase+") n'est pas un couple trinucleotide-phase valide");
        }
    }

    void setPhasesPrefsTrinucleotide(@NonNull String trinucleotide, @NonNull Phase... phase){
        for(Phase p : phase){
            setPhasePrefTrinucleotide(trinucleotide, p);
        }
    }

    Integer getPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase){
        Integer key = getNucleotidePhaseKey(dinucleotide, phase);
        if(key != null){
            return dinucleotides_pref.get(key);
        }else{
            throw new IllegalArgumentException("("+dinucleotide+","+phase+") n'est pas un couple dinucleotide-phase valide");
        }
    }

    void setPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase){
        Integer key = getNucleotidePhaseKey(dinucleotide, phase);
        if(key != null){
            dinucleotides_pref.set(key, 1);
        }else{
            throw new IllegalArgumentException("("+dinucleotide+","+phase+") n'est pas un couple dinucleotide-phase valide");
        }
    }

    void setPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase, @NonNull Integer value){
        Integer key = getNucleotidePhaseKey(dinucleotide, phase);
        if(key != null){
            dinucleotides_pref.set(key, value);
        }else{
            throw new IllegalArgumentException("("+dinucleotide+","+phase+") n'est pas un couple dinucleotide-phase valide");
        }
    }

    void unsetPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase){
        Integer key = getNucleotidePhaseKey(dinucleotide, phase);
        if(key != null){
            dinucleotides_pref.set(key, 0);
        }else{
            throw new IllegalArgumentException("("+dinucleotide+","+phase+") n'est pas un couple dinucleotide-phase valide");
        }
    }

    void setPhasesPrefsDinucleotide(@NonNull String dinucleotide, @NonNull Phase... phase){
        for(Phase p : phase){
            if(p == Phase.PHASE_2){
                throw new IllegalArgumentException("La phase 2 n'existe pas pour les dinucléotides");
            }
            setPhasePrefDinucleotide(dinucleotide, p);
        }
    }


    private Integer getNucleotidePhaseKey(@NonNull String nucleotide, @NonNull Phase phase){
        Integer idx;
        switch(nucleotide.length()){
            case 2:
                idx = CommonUtils.DINUCLEOTIDES.get(nucleotide.toUpperCase());
                break;
            case 3:
                idx = CommonUtils.TRINUCLEOTIDES.get(nucleotide.toUpperCase());
                break;
            default : return null;
        }
        return idx == null ? null : (idx * nucleotide.length()) + phase.getIdx();
    }

    public void resetCounters() {
        dinucleotides = new ArrayList<>(Collections.nCopies(CommonUtils.DINUCLEOTIDES.size() * 2, 0));
        dinucleotides_pref = new ArrayList<>(Collections.nCopies(dinucleotides.size(), 0));
        trinucleotides = new ArrayList<>(Collections.nCopies(CommonUtils.TRINUCLEOTIDES.size() * 3, 0));
        trinucleotides_pref = new ArrayList<>(Collections.nCopies(trinucleotides.size(), 0));
    }

    public boolean deepEquals(CountersEntity other){
        return new EqualsBuilder()
                .append(dinucleotides, other.dinucleotides)
                .append(dinucleotides_pref, other.dinucleotides_pref)
                .append(trinucleotides, other.trinucleotides)
                .append(trinucleotides_pref, other.trinucleotides_pref).build();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        if (!(object instanceof CountersEntity))
            return false;

        final CountersEntity a = (CountersEntity)object;

        if (id != null && a.getId() != null) {
            return id.equals(a.getId());
        }
        return false;
    }

    @Override
    public int compareTo(@NonNull CountersEntity o) {
        return new CompareToBuilder()
                .append(id, o.id)
                .build();
    }
}
