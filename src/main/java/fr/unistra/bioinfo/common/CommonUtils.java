package fr.unistra.bioinfo.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import fr.unistra.bioinfo.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public final class CommonUtils {
    private CommonUtils(){}

    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final SimpleDateFormat DATE_TO_INT_FORMATTER = new SimpleDateFormat("yyyyMMddHHmmss");

    public static final Path DATAS_PATH = Paths.get("Datas").toAbsolutePath();
    public static final Path RESULTS_PATH = Paths.get("Results").toAbsolutePath();

    public static final List<String> DINUCLEOTIDES_SORTED;
    public static final List<String> TRINUCLEOTIDES_SORTED;
    public static final Map<String, Integer> DINUCLEOTIDES;
    public static final Map<String, Integer> TRINUCLEOTIDES;
    public static final Set<String> TRINUCLEOTIDES_INIT = new TreeSet<>(Arrays.asList("ATG", "CTG", "TTG", "GTG", "ATA", "ATC", "ATT", "TTA"));
    public static final Set<String> TRINUCLEOTIDES_STOP = new TreeSet<>(Arrays.asList("TAA", "TAG", "TGA", "TTA"));
    static{
        Map<String, Integer> diset = new HashMap<>();
        Map<String, Integer> triset = new HashMap<>();
        String map = "ACGT";
        for ( char i = 0 ; i < map.length() ; i++ ) {
            for ( char j = 0 ; j < map.length() ; j++ ) {
                diset.put("" + map.charAt(i) + map.charAt(j), diset.size());
                for ( char k = 0 ; k < map.length() ; k++ ) {
                    triset.put("" + map.charAt(i) + map.charAt(j) + map.charAt(k), triset.size());
                }
            }
        }
        DINUCLEOTIDES = diset;
        TRINUCLEOTIDES = triset;
        LOGGER.trace("DINUCLEOTIDES : " + Arrays.toString(DINUCLEOTIDES.keySet().toArray(new String[0])));
        LOGGER.trace("TRINUCLEOTIDES : " + Arrays.toString(TRINUCLEOTIDES.keySet().toArray(new String[0])));
        DINUCLEOTIDES_SORTED = Collections.unmodifiableList(DINUCLEOTIDES.keySet().stream().sorted().collect(Collectors.toList()));
        TRINUCLEOTIDES_SORTED = Collections.unmodifiableList(TRINUCLEOTIDES.keySet().stream().sorted().collect(Collectors.toList()));
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

    private static boolean isHibernateLoggingDisabledInLogback(){
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        if(loggerContext == null)
            return true;
        ch.qos.logback.classic.Logger hibernateLogger = loggerContext.getLogger("org.hibernate.type.descriptor.sql");
        if(hibernateLogger == null){
            return true;
        }
        Level level = hibernateLogger.getLevel();
        return level == null || level.equals(Level.OFF);
    }

    /**
     * Définis le niveau de log d'hibernate à erreur uniquement
     */
    public static void disableHibernateLogging(){
        if(isHibernateLoggingDisabledInLogback())
            return;
        LOGGER.info("Désactivation des logs hibernate par '"+ Thread.currentThread().getStackTrace()[2] +"'");
        setLogLevelForPackage("org.hibernate.SQL", Level.ERROR);
        setLogLevelForPackage("org.hibernate.type.descriptor.sql", Level.ERROR);
    }

    /**
     * Active le logging des requêtes d'hibernate, sauf s'il est désactivé dans logback
     * @param preventBindingLogging si true, empêche le logging des binding (paramètres des requêtes)
     */
    public static void enableHibernateLogging(boolean preventBindingLogging){
        if(isHibernateLoggingDisabledInLogback())
            return;
        LOGGER.info("Activation des logs hibernate par '"+ Thread.currentThread().getStackTrace()[2] +"' "+(preventBindingLogging ? "sans" : "avec") + " les bindings");
        setLogLevelForPackage("org.hibernate.SQL", Level.DEBUG);
        if(!preventBindingLogging){
            setLogLevelForPackage("org.hibernate.type.descriptor.sql", Level.TRACE);
        }
    }

    public static URL getResourceURL(@NonNull String resourceName) throws IOException {
        return new ClassPathResource(resourceName).getURL();
    }

    public static String dateToInt(Date date) {
        return DATE_TO_INT_FORMATTER.format(date);
    }

    public static void closeQuietly(Closeable closeable){
        try{
            closeable.close();
        }catch (IOException e){
            //ignore
        }
    }
}
