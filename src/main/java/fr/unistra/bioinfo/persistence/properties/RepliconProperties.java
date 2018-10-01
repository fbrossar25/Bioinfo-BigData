package fr.unistra.bioinfo.persistence.properties;

public enum RepliconProperties implements PropertiesEnum<RepliconProperties> {
    ID("R_ID", "id"),
    TRINUCLEOTIDES("R_TRINUCLEOTIDES", "trinucleotides"),
    DINUCLEOTIDES("R_DINUCLEOTIDES", "dinucleotides"),
    DOWNLOADED("R_DOWNLOADED", "downloaded"),
    COMPUTED("R_COMPUTED", "computed"),
    HIERARCHY("R_HIERARCHY", "hierarchy"),
    VERSION("R_VERSION", "version"),
    REPLICON("R_REPLICON", "replicon");

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
