package fr.unistra.bioinfo.genbank;

public enum Reign {
    EUKARYOTES("Eukaryotes","Eukaryota"),
    PROKARYOTES("Prokaryotes","Prokaryota"),
    VIRUSES("Viruses","Virus");

    private String searchTable;
    private String label;

    Reign(String searchTable, String label){
        this.searchTable = searchTable;
        this.label = label;
    }

    public String getlabel(){
        return label;
    }

    public String getSearchTable(){ return searchTable; }
}