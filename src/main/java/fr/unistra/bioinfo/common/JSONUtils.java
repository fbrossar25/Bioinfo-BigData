package fr.unistra.bioinfo.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;

import java.io.IOException;

public final class JSONUtils {
    private JSONUtils(){}
    public static class HierarchyFromGenbankDeserializer extends JsonDeserializer<HierarchyEntity> {

        @Override
        public HierarchyEntity deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectCodec oc = p.getCodec();
            JsonNode node = oc.readTree(p);
            String kingdom = node.get("kingdom").textValue();
            String group = node.get("group").textValue();
            String subgroup = node.get("subgroup").textValue();
            String organism = node.get("organism").textValue();
            return new HierarchyEntity(kingdom, group, subgroup, organism);
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
