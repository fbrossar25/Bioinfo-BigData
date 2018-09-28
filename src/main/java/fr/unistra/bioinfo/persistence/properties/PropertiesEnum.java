package fr.unistra.bioinfo.persistence.properties;

public interface PropertiesEnum<E extends Enum<E>> {
    String getColumnName();
    String getHibernateName();
}
