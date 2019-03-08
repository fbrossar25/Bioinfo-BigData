package fr.unistra.bioinfo.persistence.entity;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.persistence.entity.converters.MapStringIntConverter;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.NaturalId;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.util.HashMap;
import java.util.List;
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

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = MapStringIntConverter.class)
    private Map<String, Integer> dinucleotides_pref = new HashMap<>();

    @Column(nullable = false, columnDefinition = "text")
    @Convert(converter = MapStringIntConverter.class)
    private Map<String, Integer> trinucleotides_pref = new HashMap<>();

    @Column(nullable = false)
    private boolean isDownloaded = false;

    @Column(nullable = false)
    private boolean isComputed = false;

    @Column(nullable = false)
    private boolean isParsed = false;

    @Column(nullable = false)
    private Integer validsCDS = 0;

    @Column(nullable = false)
    private Integer invalidsCDS = 0;

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

    public void setDinucleotideCount( String din, Phase ph, Integer value )
    {
        String key = din.toUpperCase() + "-" + ph.toString();
        this.dinucleotides.put(key, value);
    }
    public void setTrinucleotidesCount( String tri, Phase ph, Integer value )
    {
        String key = tri.toUpperCase() + "-" + ph.toString();
        this.trinucleotides.put(key, value);
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

    private String getNucleotidePhaseKey(@NonNull String nucleotide, @NonNull Phase phase){
        return nucleotide.toUpperCase() + "-" + phase.toString();
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

    public void incrementTrinucleotideCount(String trinucleotide,@NonNull Phase phase){
        String key  = getNucleotidePhaseKey(trinucleotide, phase);
        if(trinucleotides.containsKey(key)){
            trinucleotides.put(key, trinucleotides.get(key) + 1);
        }else{
            throw new IllegalArgumentException("'"+trinucleotide+"' phase '"+phase+"' n'est pas un trinucleotide valide");
        }
    }

    public void resetCounters() {
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

    public String getGenbankName(){
        return ""+name+"."+version;
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

    public void setPhasePrefTrinucleotide(@NonNull String trinucleotide, @NonNull Phase phase, Integer value){
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

    public void setPhasePrefDinucleotide(@NonNull String dinucleotide, @NonNull Phase phase, Integer value){
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

    public String getFileName() {
        return getGenbankName()+".gb";
    }


    public static RepliconEntity add ( RepliconEntity e, RepliconEntity ee )
    {
        if ( e.getType() != ee.getType() )
        {
            return null;
        }

        RepliconEntity result = new RepliconEntity();
        String din = null;
        String tri = null;

        result.setType(e.getType());

        for ( char c : "ACGT".toCharArray() )
        {
            for ( char cc : "ACGT".toCharArray() )
            {
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

                for ( char ccc : "ACGT".toCharArray() )
                {
                    tri = din + ccc;
                    for ( Phase ph : Phase.values() )
                    {
                        result.setTrinucleotidesCount(
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

    public RepliconEntity add( RepliconEntity r )
    {
        return RepliconEntity.add(this, r);
    }

    public static RepliconEntity add ( List<RepliconEntity> replicons )
    {
        // TODO : clone if size == 1
        if ( replicons.size() == 1 ) { return replicons.get(0); }

        RepliconEntity pre_result = replicons.get(0).add(replicons.get(1));

        for ( int i = 2 ; i < replicons.size() ; i++ )
        {
            pre_result = pre_result.add(replicons.get(i));
        }

        return pre_result;
    }

    public Integer getTotalTrinucleotides( Phase ph )
    {
        Integer r = 0;
        String tri = null;

        for ( char c : "ACGT".toCharArray() )
        {
            for ( char cc : "ACGT".toCharArray() )
            {
                for ( char ccc : "ACGT".toCharArray() )
                {
                    tri = "" + c + cc + ccc;
                    r += this.getTrinucleotideCount(tri.toUpperCase(), ph);
                }
            }
        }
        return r;
    }

    public Integer getTotalDinucleotides( Phase ph )
    {
        Integer r = 0;
        String din = null;

        for ( char c : "ACGT".toCharArray() )
        {
            for ( char cc : "ACGT".toCharArray() )
            {
                din = "" + c + cc;
                r += this.getDinucleotideCount(din.toUpperCase(), ph);
            }
        }
        return r;
    }

    public void incrementInvalidsCDS() {
        invalidsCDS++;
    }

    public void incrementValidsCDS() {
        validsCDS++;
    }
}
