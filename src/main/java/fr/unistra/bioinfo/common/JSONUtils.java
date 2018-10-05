package fr.unistra.bioinfo.common;

import fr.unistra.bioinfo.model.Hierarchy;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JSONUtils {
    private static final Logger LOGGER = LogManager.getLogger();
    private static JSONObject database;

    public static File saveToFile(Path filePath, JSONObject json) throws IOException{
        File f = filePath.toFile();
        FileUtils.touch(f);
        try(BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)){
            writer.write(json.toString(0));
        }catch(IOException e){
            LOGGER.error("Erreur à la sauvegarde du fichier JSON '"+filePath.toAbsolutePath()+"'", e);
            throw e;
        }
        return f;
    }

    public static JSONObject toJSON(List<Hierarchy> hierarchies){
        JSONObject json = new JSONObject();
        JSONArray hierarchiesArray = new JSONArray();
        for(Hierarchy hierarchy : hierarchies){
            hierarchiesArray.put(hierarchy.toJSON());
        }
        json.put("hierarchies", hierarchiesArray);
        return json;
    }

    public static JSONObject readFromFile(Path filePath) throws IOException{
        File f = filePath.toFile();
        JSONObject json;
        if(f.exists() && f.isFile() && f.canRead()){
            try(BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)){
                String content = reader.lines().collect(Collectors.joining());
                json = new JSONObject(content);
            }catch (IOException e){
                LOGGER.error("Erreur à la lecture du fichier JSON '"+filePath.toAbsolutePath()+"'", e);
                throw e;
            }
        }else{
            LOGGER.error("La fichier JSON '"+filePath.toAbsolutePath()+"' n'existe pas, n'est pas un fichier ou n'est pas lisible.");
            throw new FileNotFoundException(filePath.toAbsolutePath().toString());
        }
        return json;
    }

    public static List<Hierarchy> fromJSON(JSONObject json){
        JSONArray hierarchiesArray = json.getJSONArray("hierarchies");
        List<Hierarchy> hierarchies = new ArrayList<>();
        hierarchiesArray.toList().forEach((hierarchy) -> hierarchies.add(new Hierarchy((JSONObject) hierarchy)));
        return hierarchies;
    }
}
