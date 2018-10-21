package fr.unistra.bioinfo.model;


import fr.unistra.bioinfo.genbank.GenbankUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

//@JsonDeserialize(using = JSONUtils.HierarchyDeserializer.class)
//@JsonSerialize(using = JSONUtils.HierarchySerializer.class)
public class Hierarchy implements Comparable<Hierarchy>{

    private String organism;
    private String subgroup;
    private String group;
    private String kingdom;
    private Map<String, Replicon> replicons = new HashMap<>();

    public Hierarchy(){}

    public Hierarchy(String kingdom, String group, String subgroup, String organism, String replicons) {
        super();
        setOrganism(organism);
        setKingdom(kingdom);
        setGroup(group);
        setSubgroup(subgroup);
        for(Replicon r : GenbankUtils.extractRepliconsFromJSONEntry(replicons, this)){
            this.replicons.put(r.getReplicon(), r);
        }
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(String subgroup) {
        this.subgroup = subgroup;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getKingdom() {
        return kingdom;
    }

    public void setKingdom(String kingdom) {
        this.kingdom = kingdom;
    }

    public Map<String, Replicon> getReplicons() {
        return replicons;
    }

    public void setReplicons(Map<String, Replicon> replicons) {
        this.replicons = replicons;
    }

    @Override
    public String toString(){
        return GenbankUtils.getPathOfOrganism(kingdom, group, subgroup, organism).toString();
    }

    @Override
    public int compareTo(Hierarchy o) {
        return ObjectUtils.compare(organism, o.organism);
    }

    public void updateReplicons(Collection<Replicon> repliconsToUpdate) {
        for(Replicon repliconToUpdate : repliconsToUpdate){
            repliconToUpdate.setHierarchy(this);
            if(!replicons.containsKey(repliconToUpdate.getReplicon())){
                replicons.put(repliconToUpdate.getReplicon(), repliconToUpdate);
            }else{
                replicons.get(repliconToUpdate.getReplicon()).updateWith(repliconToUpdate);
            }
        }
    }
}
