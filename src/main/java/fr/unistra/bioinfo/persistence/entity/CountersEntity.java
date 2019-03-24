package fr.unistra.bioinfo.persistence.entity;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.persistence.entity.converters.MapStringIntConverter;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
public class CountersEntity implements IEntity<Long>, Comparable<CountersEntity>{
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = MapStringIntConverter.class)
    private Map<String, Integer> trinucleotides = new HashMap<>();

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = MapStringIntConverter.class)
    private Map<String, Integer> trinucleotides_pref = new HashMap<>();

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = MapStringIntConverter.class)
    private Map<String, Integer> dinucleotides = new HashMap<>();

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = MapStringIntConverter.class)
    private Map<String, Integer> dinucleotides_pref = new HashMap<>();

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


    public void setDinucleotideCount(@NonNull String din, @NonNull Phase ph, @NonNull Integer value ) {
        String key = din.toUpperCase() + "-" + ph.toString();
        this.dinucleotides.put(key, value);
    }
    public void setTrinucleotideCount(@NonNull String tri, @NonNull Phase ph, @NonNull Integer value ) {
        String key = tri.toUpperCase() + "-" + ph.toString();
        this.trinucleotides.put(key, value);
    }

    public Integer getDinucleotideCount(@NonNull String dinucleotide,@NonNull Phase phase){
        String key  = getNucleotidePhaseKey(dinucleotide, phase);
        if(dinucleotides.containsKey(key)){
            return dinucleotides.get(key);
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' phase '"+phase+"' n'est pas un dinucleotide valide");
        }
    }

    public Integer getTrinucleotideCount(@NonNull String trinucleotide,@NonNull Phase phase){
        String key  = getNucleotidePhaseKey(trinucleotide, phase);
        if(trinucleotides.containsKey(key)){
            return trinucleotides.get(key);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' phase '"+phase+"' n'est pas un trinucleotide valide");
        }
    }

    public void incrementDinucleotideCount(@NonNull String dinucleotide,@NonNull Phase phase){
        String key  = getNucleotidePhaseKey(dinucleotide, phase);
        if(dinucleotides.containsKey(key)){
            dinucleotides.put(key, dinucleotides.get(key) + 1);
        }else{
            throw new IllegalArgumentException("'"+dinucleotide+"' phase '"+phase+"' n'est pas un dinucleotide valide");
        }
    }

    public void incrementTrinucleotideCount(@NonNull String trinucleotide,@NonNull Phase phase){
        String key  = getNucleotidePhaseKey(trinucleotide, phase);
        if(trinucleotides.containsKey(key)){
            trinucleotides.put(key, trinucleotides.get(key) + 1);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' phase '"+phase+"' n'est pas un trinucleotide valide");
        }
    }

    public Integer getPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase){
        String key = getNucleotidePhaseKey(trinucleotide, phase);
        if(trinucleotides_pref.containsKey(key)){
            return trinucleotides_pref.get(key);
        }else{
            throw new IllegalArgumentException("("+trinucleotide+","+phase+") n'est pas un couple trinucleotide-phase valide");
        }
    }

    public void setPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase){
        String key = getNucleotidePhaseKey(trinucleotide, phase);
        if(trinucleotides_pref.containsKey(key)){
            trinucleotides_pref.put(key, 1);
        }else{
            throw new IllegalArgumentException("("+trinucleotide+","+phase+") n'est pas un couple trinucleotide-phase valide");
        }
    }

    public void setPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase, @NonNull Integer value){
        String key = getNucleotidePhaseKey(trinucleotide, phase);
        if(trinucleotides_pref.containsKey(key)){
            trinucleotides_pref.put(key, value);
        }else{
            throw new IllegalArgumentException("("+trinucleotide+","+phase+") n'est pas un couple trinucleotide-phase valide");
        }
    }

    public void unsetPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase){
        String key = getNucleotidePhaseKey(trinucleotide, phase);
        if(trinucleotides_pref.containsKey(key)){
            trinucleotides_pref.put(key, 0);
        }else{
            throw new IllegalArgumentException("("+trinucleotide+","+phase+") n'est pas un couple trinucleotide-phase valide");
        }
    }

    public void setPhasesPrefsTrinucleotide(@NonNull String trinucleotide, @NonNull Phase... phase){
        for(Phase p : phase){
            setPhasePrefTrinucleotide(trinucleotide, p);
        }
    }

    public Integer getPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase){
        String key = getNucleotidePhaseKey(dinucleotide, phase);
        if(dinucleotides_pref.containsKey(key)){
            return dinucleotides_pref.get(key);
        }else{
            throw new IllegalArgumentException("("+dinucleotide+","+phase+") n'est pas un couple dinucleotide-phase valide");
        }
    }

    public void setPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase){
        String key = getNucleotidePhaseKey(dinucleotide, phase);
        if(dinucleotides_pref.containsKey(key)){
            dinucleotides_pref.put(key, 1);
        }else{
            throw new IllegalArgumentException("("+dinucleotide+","+phase+") n'est pas un couple dinucleotide-phase valide");
        }
    }

    public void setPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase, @NonNull Integer value){
        String key = getNucleotidePhaseKey(dinucleotide, phase);
        if(dinucleotides_pref.containsKey(key)){
            dinucleotides_pref.put(key, value);
        }else{
            throw new IllegalArgumentException("("+dinucleotide+","+phase+") n'est pas un couple dinucleotide-phase valide");
        }
    }

    public void unsetPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase){
        String key = getNucleotidePhaseKey(dinucleotide, phase);
        if(dinucleotides_pref.containsKey(key)){
            dinucleotides_pref.put(key, 0);
        }else{
            throw new IllegalArgumentException("("+dinucleotide+","+phase+") n'est pas un couple dinucleotide-phase valide");
        }
    }

    public void setPhasesPrefsDinucleotide(@NonNull String dinucleotide, @NonNull Phase... phase){
        for(Phase p : phase){
            if(p == Phase.PHASE_2){
                throw new IllegalArgumentException("La phase 2 n'existe pas pour les dinucléotides");
            }
            setPhasePrefDinucleotide(dinucleotide, p);
        }
    }


    private String getNucleotidePhaseKey(@NonNull String nucleotide, @NonNull Phase phase){
        return nucleotide.toUpperCase() + "-" + phase.toString();
    }

    private void resetCounters() {
        for(Phase phase : Phase.values()){
            if(phase != Phase.PHASE_2) {
                for (String dinucleotide : CommonUtils.DINUCLEOTIDES) {
                    dinucleotides.put(getNucleotidePhaseKey(dinucleotide, phase), 0);
                    dinucleotides_pref.put(getNucleotidePhaseKey(dinucleotide, phase), 0);
                }
            }
            for (String trinucleotide : CommonUtils.TRINUCLEOTIDES) {
                trinucleotides.put(getNucleotidePhaseKey(trinucleotide, phase), 0);
                trinucleotides_pref.put(getNucleotidePhaseKey(trinucleotide, phase), 0);
            }
        }
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
