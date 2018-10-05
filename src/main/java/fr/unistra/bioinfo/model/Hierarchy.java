package fr.unistra.bioinfo.model;


import fr.unistra.bioinfo.genbank.GenbankUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Hierarchy implements Comparable<Hierarchy>{

    private String organism;
    private String subgroup;
    private String group;
    private String kingdom;
    private Map<String, Replicon> replicons = new HashMap<>();

    public Hierarchy (JSONObject json){
        kingdom = json.getString("kingdom");
        group = json.getString("group");
        subgroup = json.getString("subgroup");
        organism = json.getString("organism");
        try{
            JSONArray repliconsArray = json.getJSONArray("replicons");
            for(Object replicon : repliconsArray){
                JSONObject repliconJSON = (JSONObject) replicon;
                replicons.put(repliconJSON.getString("replicon"), new Replicon(repliconJSON, this));
            }
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

    public Map<String, Replicon> getReplicons() {
        return replicons;
    }

    public void setReplicons(Map<String, Replicon> replicons) {
        this.replicons = replicons;
    }

    public JSONObject toJSON(){
        JSONObject json = new JSONObject();
        JSONArray repliconsArray = new JSONArray();
        json.put("organism",this.organism);
        json.put("subgroup",this.subgroup);
        json.put("group",this.group);
        json.put("kingdom",this.kingdom);
        for(Replicon replicon : replicons.values()){
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

    public void updateReplicons(Collection<Replicon> repliconsToUpdate) {
        for(Replicon repliconToUpdate : repliconsToUpdate){
            if(!replicons.containsKey(repliconToUpdate.getReplicon())){
                replicons.put(repliconToUpdate.getReplicon(), repliconToUpdate);
            }else{
                replicons.get(repliconToUpdate.getReplicon()).updateWith(repliconToUpdate);
            }
        }
    }
}
