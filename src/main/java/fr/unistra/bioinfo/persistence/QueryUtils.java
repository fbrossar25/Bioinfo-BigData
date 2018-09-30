package fr.unistra.bioinfo.persistence;

import fr.unistra.bioinfo.persistence.properties.PropertiesEnum;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;

public class QueryUtils {
    public static String equals(PropertiesEnum property, Object value){
        return equals(property.getHibernateName(), value);
    }

    public static String equals(String column, Object value){
        if(String.class.equals(value.getClass())){
            return " "+column+" = '"+value.toString()+"' ";
        }else{
            return " "+column+" = "+value.toString()+" ";
        }
    }

    public static String in(PropertiesEnum property, Collection<?> values){
        return in(property.getHibernateName(), values);
    }

    public static String in(String column, Collection<?> values){
        if(CollectionUtils.isEmpty(values)){
            return " "+ column + " IN () ";
        }

        StringBuilder in = new StringBuilder(" " + column + " IN ( " );
        int remaining = values.size();
        for(Object value : values){
            if(String.class.equals(value.getClass())){
                in.append("'").append((String) value).append("'");
            }else{
                in.append(value.toString());
            }
            if(remaining-- > 1){
                in.append(", ");
            }
        }
        in.append(" ) ");
        return in.toString();
    }
}
