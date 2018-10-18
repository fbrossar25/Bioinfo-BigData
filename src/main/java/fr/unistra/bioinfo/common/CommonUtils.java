package fr.unistra.bioinfo.common;

import fr.unistra.bioinfo.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class CommonUtils {

    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static final Path DATABASE_PATH = Paths.get("database", "database.json").toAbsolutePath();

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

    public static URL getResourceURL(@NonNull String resourceName) throws IOException {
        return new ClassPathResource(resourceName).getURL();
    }
}
