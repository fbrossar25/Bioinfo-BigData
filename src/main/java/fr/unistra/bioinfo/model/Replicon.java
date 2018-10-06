package fr.unistra.bioinfo.model;

import fr.unistra.bioinfo.common.CommonUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Replicon implements Comparable<Replicon> {
    private String replicon;
    private Integer version = 1;
    private Hierarchy hierarchy;
    private Map<String, Integer> trinucleotides = new HashMap<>();
    private Map<String, Integer> dinucleotides = new HashMap<>();
    private boolean isDownloaded = false;
    private boolean isComputed = false;

    public Replicon (JSONObject json, Hierarchy hierarchy){
        resetCounters();
        replicon = (String)json.get("replicon");
        this.hierarchy = hierarchy;
        this.isDownloaded = json.getBoolean("isDownloaded");
        this.isComputed = json.getBoolean("isComputed");
        this.version = json.getInt("version");
        int i = 0;
        JSONArray diArray = json.getJSONArray("dinucleotides");
        for(String di : CommonUtils.DINUCLEOTIDES){
            dinucleotides.put(di, i >= diArray.length() ? 0 : (Integer)diArray.get(i));
            i++;
        }
        i = 0;
        JSONArray triArray = json.getJSONArray("trinucleotides");
        for(String tri : CommonUtils.TRINUCLEOTIDES){
            trinucleotides.put(tri, i >= triArray.length() ? 0 : (Integer)triArray.get(i));
            i++;
        }
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

    public Integer getTrinucleotide(String trinucleotide) {
        return trinucleotides.get(trinucleotide);
    }

    public void setTrinucleotide(String trinucleotide, Integer value) {
        this.trinucleotides.put(trinucleotide, value);
    }

    public Integer getDinucleotide(String dinucleotide) {
        return dinucleotides.get(dinucleotide);
    }

    public void setDinucleotide(String dinucleotide, Integer value) {
        this.dinucleotides.put(dinucleotide, value);
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
        for(String tri : CommonUtils.TRINUCLEOTIDES){
            trinucleotides.put(tri, 0);
        }

        for(String di : CommonUtils.DINUCLEOTIDES){
            dinucleotides.put(di, 0);
        }
    }

    @Override
    public String toString(){
        return replicon + "." + version;
    }

    @Override
    public int compareTo(Replicon o) {
        return ObjectUtils.compare(replicon + version, o.replicon + o.version);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("replicon",this.replicon);
        json.put("version",this.version);
        json.put("isDownloaded",this.isDownloaded);
        json.put("isComputed",this.isComputed);
        json.put("trinucleotides",this.trinucleotides.values());
        json.put("dinucleotides",this.dinucleotides.values());
        return json;
    }

    public void updateWith(Replicon other) {
        if(other.version > this.version){
            //Mise à jour
            this.isDownloaded = false;
            this.isComputed = false;
            this.version = other.version;
            resetCounters();
        }
    }
}
