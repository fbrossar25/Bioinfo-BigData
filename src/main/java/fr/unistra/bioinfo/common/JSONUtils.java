package fr.unistra.bioinfo.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import fr.unistra.bioinfo.model.Hierarchy;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class JSONUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void saveToFile(Path filePath, Collection<Hierarchy> hierarchies) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        File f = filePath.toFile();
        if(!f.exists()){
            FileUtils.touch(f);
        }
        mapper.writeValue(f, hierarchies);
    }

    public static List<Hierarchy> readFromFile(Path filePath) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(filePath.toFile(), mapper.getTypeFactory().constructCollectionType(List.class, Hierarchy.class));
    }

    public static class HierarchyFromGenbankDeserializer extends JsonDeserializer<Hierarchy> {

        @Override
        public Hierarchy deserialize(JsonParser p, DeserializationContext ctxt) throws IOException{
            ObjectCodec oc = p.getCodec();
            JsonNode node = oc.readTree(p);
            String kingdom = node.get("kingdom").textValue();
            String group = node.get("group").textValue();
            String subgroup = node.get("subgroup").textValue();
            String replicons = node.get("replicons").textValue();
            String organism = node.get("organism").textValue();
            return new Hierarchy(kingdom, group, subgroup, organism, replicons);
        }
    }

//    public static class HierarchySerializer extends JsonSerializer<Hierarchy> {
//
//        @Override
//        public void serialize(Hierarchy value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//            gen.writeStartObject();
//            gen.writeStringField("kingdom", value.getKingdom());
//            gen.writeStringField("group", value.getGroup());
//            gen.writeStringField("subgroup", value.getSubgroup());
//            gen.writeStringField("organism", value.getOrganism());
//            gen.writeArrayFieldStart("replicons");
//            ObjectMapper mapper = new ObjectMapper();
//            for(Replicon r : value.getReplicons().values()){
//                mapper.writeValue(gen, r);
//            }
//            gen.writeEndArray();
//            gen.writeEndObject();
//        }
//    }
}
