package fr.unistra.bioinfo.persistence.entity.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Converter
public class ListIntConverter implements AttributeConverter<List<Integer>, String> {
    private static final Logger LOGGER  = LoggerFactory.getLogger(ListIntConverter.class);

    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
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
    public List<Integer> convertToEntityAttribute(String dbData) {
        List<Integer> list;
        ObjectMapper mapper = new ObjectMapper();
        try {
            list = mapper.readValue(dbData, new TypeReference<List<Integer>>(){});
        } catch (IOException e) {
            LOGGER.error("Erreur de conversion du json en map", e);
            list = new ArrayList<>();
        }
        return list;
    }
}
