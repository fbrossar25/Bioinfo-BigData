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
public class ListLongConverter implements AttributeConverter<List<Long>, String> {
    private static final Logger LOGGER  = LoggerFactory.getLogger(ListLongConverter.class);

    @Override
    public String convertToDatabaseColumn(List<Long> attribute) {
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
    public List<Long> convertToEntityAttribute(String dbData) {
        List<Long> list;
        ObjectMapper mapper = new ObjectMapper();
        try {
            list = mapper.readValue(dbData, new TypeReference<List<Long>>(){});
        } catch (IOException e) {
            LOGGER.error("Erreur de conversion du json en map", e);
            list = new ArrayList<>();
        }
        return list;
    }
}
