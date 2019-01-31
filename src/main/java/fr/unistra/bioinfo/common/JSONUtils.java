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
}
