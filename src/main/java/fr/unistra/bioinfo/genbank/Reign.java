package fr.unistra.bioinfo.genbank;

public enum Reign {
    EUKARYOTES("Eukaryota","\"Eukaryotes\""),
    PROKARYOTES("Prokaryota","\"Prokaryotes\""),
    VIRUSES("Viruses","\"Viruses\""),
    ALL("All", "\"Eukaryotes\",\"Viruses\",\"Prokaryotes\"");

    private String searchTable;
    private String label;

    Reign(String label, String searchTable){
        this.searchTable = searchTable;
        this.label = label;
    }

    public String getlabel(){
        return label;
    }

    public String getSearchTable(){ return searchTable; }
}