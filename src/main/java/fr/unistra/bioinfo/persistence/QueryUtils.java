package fr.unistra.bioinfo.persistence;

import com.sun.istack.internal.NotNull;
import fr.unistra.bioinfo.persistence.properties.PropertiesEnum;

import java.util.Collection;

public class QueryUtils {
    public static String equals(@NotNull PropertiesEnum property, @NotNull Object value){
        return equals(property.getHibernateName(), value);
    }

    public static String equals(@NotNull String column, @NotNull Object value){
        if(String.class.equals(value.getClass())){
            return " "+column+" = '"+value.toString()+"' ";
        }else{
            return " "+column+" = "+value.toString()+" ";
        }
    }

    public static String in(@NotNull PropertiesEnum property, @NotNull Collection<?> values){
        return in(property.getHibernateName(), values);
    }

    public static String in(@NotNull String column, @NotNull Collection<?> values){
        StringBuilder in = new StringBuilder(column + " IN ( " );
        int remaining = values.size();
        for(Object value : values){
            if(String.class.equals(value.getClass())){
                in.append("'").append((String) value).append("'");
            }else{
                in.append(value.toString());
            }
            if(remaining-- <= 1){
                in.append(", ");
            }
        }
        in.append(" )");
        return in.toString();
    }
}
