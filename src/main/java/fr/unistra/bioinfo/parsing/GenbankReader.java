package fr.unistra.bioinfo.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GenbankReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankReader.class);
    private final File file;
    private final StringBuilder buffer;
    private final StringBuilder origin = new StringBuilder();
    private final List<StringBuilder> cdsList = new ArrayList<>();
    private final List<StringBuilder> subsequences = new ArrayList<>();

    private GenbankReader(File file) throws IOException {
        this.file = file;
        buffer = new StringBuilder((int)file.length());
    }

    private void process() throws IOException{
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while((line = reader.readLine()) != null){
            buffer.append(line);
        }
        try{
            reader.close();
        }catch (IOException e){
            //ignore
        }
        //TODO
    }

    public static List<StringBuilder> parse(File file) throws IOException{
        GenbankReader reader = new GenbankReader(file);
        reader.process();
        return reader.subsequences;
    }
}
