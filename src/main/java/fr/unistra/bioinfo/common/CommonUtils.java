package fr.unistra.bioinfo.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class CommonUtils {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Path DATABASE_PATH = Paths.get("database").toAbsolutePath();

    public static final Set<String> DINUCLEOTIDES;
    public static final Set<String> TRINUCLEOTIDES;
    static{
        Set<String> diset = new TreeSet<>();
        Set<String> triset = new TreeSet<>();
        String map = "ACGT";
        for ( char i = 0 ; i < map.length() ; i++ ) {
            for ( char j = 0 ; j < map.length() ; j++ ) {
                diset.add("" + map.charAt(i) + map.charAt(j));
                for ( char k = 0 ; k < map.length() ; k++ ) {
                    triset.add("" + map.charAt(i) + map.charAt(j) + map.charAt(k));
                }
            }
        }
        DINUCLEOTIDES = Collections.unmodifiableSet(diset);
        TRINUCLEOTIDES = Collections.unmodifiableSet(triset);
        LOGGER.trace("DINUCLEOTIDES : " + Arrays.toString(DINUCLEOTIDES.toArray()));
        LOGGER.trace("TRINUCLEOTIDES : " + Arrays.toString(TRINUCLEOTIDES.toArray()));
    }
}
