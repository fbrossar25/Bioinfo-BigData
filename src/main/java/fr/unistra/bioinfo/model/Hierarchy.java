package fr.unistra.bioinfo.model;


import fr.unistra.bioinfo.genbank.GenbankUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;
import java.util.TreeSet;

public class Hierarchy implements Comparable<Hierarchy>{

    private String organism;
    private String subgroup;
    private String group;
    private String kingdom;
    private Set<Replicon> replicons = new TreeSet<>();

    public Hierarchy (JSONObject json){
        kingdom = json.getString("kingdom");
        group = json.getString("group");
        subgroup = json.getString("subgroup");
        organism = json.getString("organism");
        try{
            JSONArray repliconsArray = json.getJSONArray("replicons");
        }catch(JSONException e){
            //ignore
        }
    }

    public Hierarchy(String kingdom, String group, String subgroup, String organism) {
        super();
        setOrganism(organism);
        setKingdom(kingdom);
        setGroup(group);
        setSubgroup(subgroup);
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

    public Set<Replicon> getReplicons() {
        return replicons;
    }

    public void setReplicons(Set<Replicon> replicons) {
        this.replicons = replicons;
    }

    public JSONObject toJSON(){
        JSONObject json = new JSONObject();
        JSONArray repliconsArray = new JSONArray();
        json.put("organism",this.organism);
        json.put("subgroup",this.subgroup);
        json.put("group",this.group);
        json.put("kingdom",this.kingdom);
        for(Replicon replicon : replicons){
            repliconsArray.put(replicon.toJson());
        }
        json.put("replicons", repliconsArray);
        return json;
    }

    @Override
    public String toString(){
        return GenbankUtils.getPathOfOrganism(kingdom, group, subgroup, organism).toString();
    }

    @Override
    public int compareTo(Hierarchy o) {
        return ObjectUtils.compare(organism, o.organism);
    }
}
