package fr.unistra.bioinfo.genbank;

public enum Reign {
    EUKARYOTES("Eukaryotes"),
    PROKARYOTES("Prokaryotes"),
    VIRUSES("Viruses");

    private String label;

    Reign(String label){
        this.label = label;
    }

    public String getlabel(){
        return label;
    }
}