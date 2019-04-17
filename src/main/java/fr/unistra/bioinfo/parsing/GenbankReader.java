package fr.unistra.bioinfo.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GenbankReader {
    enum Operator{
        JOIN, COMPLEMENT, COMPLEMENT_JOIN, NONE
    }

    class CDS{
        int begin;
        int end;
        Operator op;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankReader.class);
    private File file;
    private BufferedReader buffer;
    private StringBuilder origin = new StringBuilder();
    private List<CDS> cdsList = new ArrayList<>();
    private List<StringBuilder> processedCdsList = new ArrayList<>();

    private GenbankReader(File file) throws IOException {
        this.file = file;
        buffer = new BufferedReader(new FileReader(file));
    }

    private void process() throws IOException{
        String line;
        while((line = buffer.readLine()) != null){
            processLine(line);
        }
    }

    private void processLine(String line){
        //TODO
    }

    public static List<StringBuilder> parse(File file) throws IOException{
        GenbankReader reader = new GenbankReader(file);
        reader.process();
        return reader.processedCdsList;
    }
}
