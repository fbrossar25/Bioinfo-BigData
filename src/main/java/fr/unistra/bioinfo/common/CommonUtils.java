package fr.unistra.bioinfo.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import fr.unistra.bioinfo.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class CommonUtils {
    private CommonUtils(){}

    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static final Path DATABASE_PATH = Paths.get("database", "database.json").toAbsolutePath();
    public static final Path RESULTS_PATH = Paths.get("Results").toAbsolutePath();

    public static final Set<String> DINUCLEOTIDES;
    public static final Set<String> TRINUCLEOTIDES;
    public static final Set<String> TRINUCLEOTIDES_INIT = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("ATG", "CTG", "TTG", "GTG", "ATA", "ATC", "ATT", "TTA")));
    public static final Set<String> TRINUCLEOTIDES_STOP = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("TAA", "TAG", "TGA", "TTA")));
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

    /**
     * Définis le niveau de log du package donné
     * @param packageName le package
     * @param level le niveau de log
     * @return l'ancien niveau de log du package ou null s'il n'a pas été trouvé
     */
    public static Level setLogLevelForPackage(String packageName, Level level){
        if(packageName == null){
            return null;
        }
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(packageName);
        if(rootLogger == null){
            return null;
        }
        Level previousLevel = rootLogger.getLevel();
        rootLogger.setLevel(level);
        return previousLevel;
    }

    /**
     * Définis le niveau de log d'hibernate à erreur uniquement
     */
    public static void disableHibernateLogging(){
        LOGGER.info("Désactivation des logs hibernate par '"+ Thread.currentThread().getStackTrace()[2] +"'");
        setLogLevelForPackage("org.hibernate.SQL", Level.ERROR);
        setLogLevelForPackage("org.hibernate.type.descriptor.sql", Level.ERROR);
    }

    /**
     * Active le logging des requêtes d'hibernate
     * @param preventBindingLogging si true, empêche le logging des binding (paramètres des requêtes)
     */
    public static void enableHibernateLogging(boolean preventBindingLogging){
        LOGGER.info("Activation des logs hibernate par '"+ Thread.currentThread().getStackTrace()[2] +"' "+(preventBindingLogging ? "avec" : "sans") + " les bindings");
        setLogLevelForPackage("org.hibernate.SQL", Level.DEBUG);
        if(!preventBindingLogging){
            setLogLevelForPackage("org.hibernate.type.descriptor.sql", Level.TRACE);
        }
    }

    public static URL getResourceURL(@NonNull String resourceName) throws IOException {
        return new ClassPathResource(resourceName).getURL();
    }
}
