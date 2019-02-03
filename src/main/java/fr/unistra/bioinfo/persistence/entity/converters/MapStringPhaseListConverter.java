package fr.unistra.bioinfo.persistence.entity.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.unistra.bioinfo.parsing.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Converter
public class MapStringPhaseListConverter implements AttributeConverter<Map<String, List<Phase>>, String> {
    private static final Logger LOGGER  = LoggerFactory.getLogger(MapStringPhaseListConverter.class);


    @Override
    public String convertToDatabaseColumn(Map<String, List<Phase>> attribute) {
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
    public Map<String, List<Phase>> convertToEntityAttribute(String dbData) {
        Map<String, List<Phase>> map;
        ObjectMapper mapper = new ObjectMapper();
        try {
            map = mapper.readValue(dbData, new TypeReference<Map<String, List<Phase>>>(){});
        } catch (IOException e) {
            LOGGER.error("Erreur de conversion du json en map", e);
            map = new HashMap<>();
        }
        return map;
    }
}
