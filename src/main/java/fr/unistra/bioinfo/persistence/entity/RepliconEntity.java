package fr.unistra.bioinfo.persistence.entity;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.NaturalId;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private HierarchyEntity hierarchyEntity;

    @OneToOne(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinColumn(nullable = false)
    private CountersEntity counters;


    @Column(nullable = false)
    private boolean computed = false;

    @Column(nullable = false)
    private boolean parsed = false;

    @Column(nullable = false)
    private Integer validsCDS = 0;

    @Column(nullable = false)
    private Integer invalidsCDS = 0;

    public RepliconEntity() { }

    public RepliconEntity(String replicon, HierarchyEntity hierarchy) {
        this(replicon, 1, hierarchy);
    }

    public RepliconEntity(String replicon, Integer version, HierarchyEntity hierarchy) {
        setName(replicon);
        setVersion(version);
        setHierarchyEntity(hierarchy);
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
     * Indique si le fichier à été parsé, et le comptage effectué.
     * @return true si le parsing et le comptage sont faits, false sinon
     */
    public boolean isParsed() {
        return parsed;
    }

    public void setParsed(boolean parsed) {
        this.parsed = parsed;
    }

    /**
     * Indique si les statistiques ont été générées pour ce replicon.
     * @return true si les statistiques ont été générées, false sinon
     */
    public boolean isComputed() {
        return computed;
    }
    public void setComputed(boolean computed) {
        this.computed = computed;
    }

    /**
     * Retourne le nom du réplicon concaténé à sa version.</br>
     * Example : NC_123456.1
     * @return Le nom genbank de ce replicon
     */
    public String getGenbankName(){
        return ""+name+"."+version;
    }

    public Integer getValidsCDS() {
        return validsCDS;
    }

    public void setValidsCDS(Integer validsCDS) {
        this.validsCDS = validsCDS;
    }

    public Integer getInvalidsCDS() {
        return invalidsCDS;
    }

    public void setInvalidsCDS(Integer invalidsCDS) {
        this.invalidsCDS = invalidsCDS;
    }

    public void setCounters(CountersEntity entity){
        this.counters = entity;
    }

    public CountersEntity getCounters(){
        if(this.counters == null){
            this.counters = new CountersEntity();
        }
        return this.counters;
    }

    public void setDinucleotideCount(@NonNull String din, @NonNull Phase ph, @NonNull long value ) {
        getCounters().setDinucleotideCount(din, ph, value);
    }
    public void setTrinucleotideCount(@NonNull String tri, @NonNull Phase ph, @NonNull Long value ) {
        getCounters().setTrinucleotideCount(tri, ph, value);
    }

    public Long getDinucleotideCount(@NonNull String dinucleotide, @NonNull Phase phase){
        return getCounters().getDinucleotideCount(dinucleotide, phase);
    }

    public Long getTrinucleotideCount(@NonNull String trinucleotide, @NonNull Phase phase){
        return getCounters().getTrinucleotideCount(trinucleotide, phase);
    }

    public void incrementDinucleotideCount(@NonNull String dinucleotide,@NonNull Phase phase){
        getCounters().incrementDinucleotideCount(dinucleotide, phase);
    }

    public void incrementTrinucleotideCount(@NonNull String trinucleotide,@NonNull Phase phase){
        getCounters().incrementTrinucleotideCount(trinucleotide, phase);
    }

    public Long getPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase){
        return getCounters().getPhasePrefTrinucleotide(trinucleotide, phase);
    }

    public void setPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase){
        getCounters().setPhasePrefTrinucleotide(trinucleotide, phase);
    }

    public void setPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase, @NonNull Long value){
        getCounters().setPhasePrefTrinucleotide(trinucleotide, phase, value);
    }

    public void unsetPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase){
        getCounters().unsetPhasePrefTrinucleotide(trinucleotide, phase);
    }

    public void setPhasesPrefsTrinucleotide(@NonNull String trinucleotide, @NonNull Phase... phase){
        getCounters().setPhasesPrefsTrinucleotide(trinucleotide, phase);
    }

    public Long getPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase){
        return getCounters().getPhasePrefDinucleotide(dinucleotide, phase);
    }

    public void setPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase){
        getCounters().setPhasePrefDinucleotide(dinucleotide, phase);
    }

    public void setPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase, @NonNull Long value){
        getCounters().setPhasePrefDinucleotide(dinucleotide, phase, value);
    }

    public void unsetPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase){
        getCounters().unsetPhasePrefDinucleotide(dinucleotide, phase);
    }

    public void setPhasesPrefsDinucleotide(@NonNull String dinucleotide, @NonNull Phase... phase){
        for(Phase p : phase){
            if(p == Phase.PHASE_2){
                throw new IllegalArgumentException("La phase 2 n'existe pas pour les dinucléotides");
            }
            setPhasePrefDinucleotide(dinucleotide, p);
        }
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

    public RepliconType getType() {
        return type;
    }

    public void setType(RepliconType type) {
        this.type = type;
    }

    public void incrementInvalidsCDS() {
        invalidsCDS++;
    }

    public void incrementValidsCDS() {
        validsCDS++;
    }

    public void decrementValidsCDS() {
        validsCDS--;
    }

    public static RepliconEntity add(RepliconEntity e, RepliconEntity ee) {
        if (e.getType() != ee.getType()) {
            return null;
        }

        RepliconEntity result = new RepliconEntity();
        String din = null;
        String tri = null;

        result.setType(e.getType());

        for (char c : "ACGT".toCharArray()) {
            for (char cc : "ACGT".toCharArray()) {
                din = "" + c + cc;

                result.setDinucleotideCount(
                        din,
                        Phase.PHASE_0,
                        e.getDinucleotideCount(din, Phase.PHASE_0) + ee.getDinucleotideCount(din, Phase.PHASE_0)
                );
                result.setDinucleotideCount(
                        din,
                        Phase.PHASE_1,
                        e.getDinucleotideCount(din, Phase.PHASE_1) + ee.getDinucleotideCount(din, Phase.PHASE_1)
                );

                result.setPhasePrefDinucleotide(
                        din,
                        Phase.PHASE_0,
                        e.getPhasePrefDinucleotide(din, Phase.PHASE_0) + ee.getPhasePrefDinucleotide(din, Phase.PHASE_0)
                );
                result.setPhasePrefDinucleotide(
                        din,
                        Phase.PHASE_1,
                        e.getPhasePrefDinucleotide(din, Phase.PHASE_1) + ee.getPhasePrefDinucleotide(din, Phase.PHASE_1)
                );

                for (char ccc : "ACGT".toCharArray()) {
                    tri = din + ccc;
                    for (Phase ph : Phase.values()) {
                        result.setTrinucleotideCount(
                                tri,
                                ph,
                                e.getTrinucleotideCount(tri, ph) + ee.getTrinucleotideCount(tri, ph)
                        );
                        result.setPhasePrefTrinucleotide(
                                tri,
                                ph,
                                e.getPhasePrefTrinucleotide(tri, ph) + ee.getPhasePrefTrinucleotide(tri, ph)
                        );
                    }
                }
            }
        }

        return result;
    }

    public void resetCounters(){
        getCounters().resetCounters();
    }

    public RepliconEntity add(RepliconEntity r) {
        return RepliconEntity.add(this, r);
    }

    public static RepliconEntity add(List<RepliconEntity> replicons) {
        if (replicons.size() == 1) {
            return replicons.get(0);
        }

        RepliconEntity pre_result = replicons.get(0).add(replicons.get(1));

        for (int i = 2; i < replicons.size(); i++) {
            pre_result = pre_result.add(replicons.get(i));
        }

        return pre_result;
    }

    public Long getTotalTrinucleotides(Phase ph) {
        Long r = 0L;
        String tri = null;

        for (char c : "ACGT".toCharArray()) {
            for (char cc : "ACGT".toCharArray()) {
                for (char ccc : "ACGT".toCharArray()) {
                    tri = "" + c + cc + ccc;
                    r += this.getTrinucleotideCount(tri.toUpperCase(), ph);
                }
            }
        }
        return r;
    }

    public Long getTotalDinucleotides(Phase ph) {
        Long r = 0L;
        String din = null;

        for (char c : "ACGT".toCharArray()) {
            for (char cc : "ACGT".toCharArray()) {
                din = "" + c + cc;
                r += this.getDinucleotideCount(din.toUpperCase(), ph);
            }
        }
        return r;
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
                .append(isComputed(), o.isComputed())
                .build();
    }
}
