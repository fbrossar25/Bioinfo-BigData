package fr.unistra.bioinfo.common;

import fr.unistra.bioinfo.model.Hierarchy;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

public class JSONUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    public static File saveToFile(Path filePath, JSONObject json) throws IOException{
        File f = filePath.toFile();
        FileUtils.touch(f);
        try(BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)){
            writer.write(json.toString(4));
        }catch(IOException e){
            LOGGER.error("Erreur à la sauvegarde du fichier JSON '"+filePath.toAbsolutePath()+"'", e);
            throw e;
        }
        return f;
    }

    public static JSONObject toJSON(Collection<Hierarchy> hierarchies){
        JSONObject json = new JSONObject();
        JSONArray hierarchiesArray = new JSONArray();
        for(Hierarchy hierarchy : hierarchies){
            hierarchiesArray.put(hierarchy.toJSON());
        }
        json.put("hierarchies", hierarchiesArray);
        return json;
    }


}
