package fr.unistra.bioinfo.persistence;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.HashMap;
import java.util.Map;

@Converter
public class MapToStringConverter implements AttributeConverter<Map<String, Integer>, String> {
    //TODO zipper/dézipper les chaînes vers et depuis la BDD pour réduire sa taille
    @Override
    public String convertToDatabaseColumn(Map<String, Integer> data) {
        if(data == null){
            return new JSONObject().toString();
        }
        return new JSONObject(data).toString();
    }

    @Override
    public Map<String, Integer> convertToEntityAttribute(String data) {
        if(StringUtils.isBlank(data)){
            return new HashMap<>();
        }
        Map<String, Object> map = new JSONObject(data).toMap();
        Map<String, Integer> targetMap = new HashMap<>();
        if(map != null){
            for(Map.Entry<String, Object> entry : map.entrySet()){
                if(entry.getValue() instanceof Integer){
                    targetMap.put(entry.getKey(), (Integer)entry.getValue());
                }
            }
        }
        return targetMap;
    }
}
