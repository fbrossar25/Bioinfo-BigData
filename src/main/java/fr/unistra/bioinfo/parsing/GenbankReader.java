package fr.unistra.bioinfo.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenbankReader {
    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\s*(.+?)(\\s+(.+))?$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(NC_\\d+?).(\\d+)$");

    enum Operator{
        JOIN, COMPLEMENT, COMPLEMENT_JOIN, NONE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankReader.class);
    private File file;
    private BufferedReader reader;
    private StringBuilder origin = new StringBuilder();
    private List<CDS> cdsList = new ArrayList<>();
    private List<StringBuilder> processedCdsList = new ArrayList<>();
    private String name = null;
    private int version = 0;

    GenbankReader(@NonNull File file){
        this.file = file;
    }

    private void process() throws IOException {
        reader = new BufferedReader(new FileReader(file));
        String line;
        while((line = reader.readLine()) != null){
            processLine(line);
        }
    }

    private void processLine(String line){
        Matcher sectionMatcher = SECTION_PATTERN.matcher(line);
        String sectionName;
        String sectionLine = null;
        if(sectionMatcher.matches()){
            sectionName = sectionMatcher.group(1);
            if(sectionMatcher.groupCount() == 3){
                sectionLine = sectionMatcher.group(3);
            }
            processSection(sectionName, sectionLine);
        }
    }

    private void processSection(String name, String value){
        switch (name.toUpperCase()){
            case "VERSION":
                processVersion(value);
                break;
            case "CDS":
                processCDS(value);
                break;
            case "ORIGIN":
                processORIGIN();
                break;
            default:
        }
    }

    private void processVersion(String versionValue){
        Matcher versionMatcher = VERSION_PATTERN.matcher(versionValue);
        if(versionMatcher.matches()){
            name = versionMatcher.group(1);
            version = Integer.parseInt(versionMatcher.group(2));
        }
    }

    private void processCDS(String cdsValue){
        //TODO
    }

    private void processORIGIN(){
        //TODO
    }

    public List<StringBuilder> getProcessedCdsList(){
        return processedCdsList;
    }

    public int getValidsCDS(){
        return -1;
    }

    public int getInvalidsCDS(){
        return -1;
    }

    public String getName(){
        return name;
    }

    public int getVersion(){
        return version;
    }

    public static GenbankReader parse(File file) throws IOException{
        GenbankReader genbankReader = new GenbankReader(file);
        genbankReader.process();
        return genbankReader;
    }
}
