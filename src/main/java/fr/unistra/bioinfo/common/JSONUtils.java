package fr.unistra.bioinfo.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.unistra.bioinfo.model.Hierarchy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class JSONUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void saveToFile(Path filePath, Collection<Hierarchy> hierarchies) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(filePath.toFile(), hierarchies);
    }

    public static List<Hierarchy> readFromFile(Path filePath) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(filePath.toFile(), mapper.getTypeFactory().constructCollectionType(List.class, Hierarchy.class));
    }
}
