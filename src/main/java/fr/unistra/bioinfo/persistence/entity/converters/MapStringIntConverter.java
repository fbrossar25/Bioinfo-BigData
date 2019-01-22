package fr.unistra.bioinfo.persistence.entity.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Converter
public class MapStringIntConverter implements AttributeConverter<Map<String, Integer>, String> {
    private static final Logger LOGGER  = LoggerFactory.getLogger(MapStringIntConverter.class);

    @Override
    public String convertToDatabaseColumn(Map<String, Integer> attribute) {
        String json;
        try{
             json = new ObjectMapper().writeValueAsString(attribute);
        }catch(JsonProcessingException e){
            LOGGER.error("Erreur de conversion de la map en json", e);
            json = "{}";
        }
        return json;
    }

    @Override
    public Map<String, Integer> convertToEntityAttribute(String dbData) {
        Map<String, Integer> map;
        ObjectMapper mapper = new ObjectMapper();
        try {
            map = mapper.readValue(dbData, new TypeReference<Map<String, Integer>>(){});
        } catch (IOException e) {
            LOGGER.error("Erreur de conversion du json en map", e);
            map = new HashMap<>();
        }
        return map;
    }
}
