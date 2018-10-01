package fr.unistra.bioinfo.persistence.properties;

public enum HierarchyProperties implements PropertiesEnum<HierarchyProperties> {
    ID("H_ID", "id"),
    KINGDOM("H_KINGDOM", "kingdom"),
    GROUP("H_GROUP", "group"),
    SUBGROUP("H_SUBGROUP", "subgroup"),
    ORGANISM("H_ORGANISM", "organism");

    private final String columnName;
    private final String hibernateName;

    HierarchyProperties(String columnName, String hibernateName){
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
