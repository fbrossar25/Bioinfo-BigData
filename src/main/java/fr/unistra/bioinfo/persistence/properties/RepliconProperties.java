package fr.unistra.bioinfo.persistence.properties;

public enum RepliconProperties implements PropertiesEnum<RepliconProperties> {
    ID("ID", "id"),
    TRINUCLEOTIDES("TRINUCLEOTIDES", "trinucleotides"),
    DINUCLEOTIDES("DINUCLEOTIDES", "dinucleotides"),
    DOWNLOADED("DOWNLOADED", "downloaded"),
    COMPUTED("COMPUTED", "computed"),
    HIERARCHY("HIERARCHY", "hierarchy"),
    VERSION("VERSION", "version"),
    REPLICON("REPLICON", "replicon");

    private final String columnName;
    private final String hibernateName;

    RepliconProperties(String columnName, String hibernateName){
        this.columnName = columnName;
        this.hibernateName = hibernateName;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public String getHibernateName() {
        return hibernateName;
    }
}
