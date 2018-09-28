package fr.unistra.bioinfo.persistence.properties;

public enum HierarchyProperties implements PropertiesEnum<HierarchyProperties> {
    ID("HIERARCHY_ID", "id"),
    KINGDOM("HIERARCHY_KINGDOM", "kingdom"),
    GROUP("HIERARCHY_GROUP", "group"),
    SUBGROUP("HIERARCHY_SUBGROUP", "subgroup"),
    ORGANISM("HIERARCHY_ORGANISM", "organism");

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
