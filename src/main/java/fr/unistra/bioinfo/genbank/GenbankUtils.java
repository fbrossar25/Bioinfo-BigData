package fr.unistra.bioinfo.genbank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.util.concurrent.RateLimiter;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.common.JSONUtils;
import fr.unistra.bioinfo.common.RegexUtils;
import fr.unistra.bioinfo.gui.MainWindowController;
import fr.unistra.bioinfo.model.Hierarchy;
import fr.unistra.bioinfo.model.Replicon;
import javafx.application.Platform;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class GenbankUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final Integer REQUEST_LIMIT = 10;
    public static final String EUTILS_BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
    /** Permet de faire jusqu'à 10 appels par seconde au lieu de 3 */
    public static final String EUTILS_API_KEY = "13aa4cb817db472b3fdd1dc0ca1655940809";
    public static final String EUTILS_EFETCH = "efetch.fcgi";
    private static Map<String, Hierarchy> HIERARCHY_DB = new HashMap<>();
    static{
        loadLocalDatabase();
    }

    public static List<Replicon> getAllReplicons(){
        List<Replicon> replicons = new ArrayList<>();
        HIERARCHY_DB.values().forEach(h -> replicons.addAll(h.getReplicons().values()));
        return replicons;
    }

    public static void downloadReplicons(List<Replicon> replicons, final CompletableFuture<List<File>> callback) {
        final ExecutorService ses = Executors.newFixedThreadPool(GenbankUtils.REQUEST_LIMIT);
        final List<DownloadRepliconTask> tasks = new ArrayList<>(replicons.size());
        final RateLimiter rateLimiter = RateLimiter.create(GenbankUtils.REQUEST_LIMIT);

        replicons.forEach(r -> tasks.add(new DownloadRepliconTask(r, rateLimiter)));
        try {
            final List<Future<File>> futuresFiles = ses.invokeAll(tasks);
            if(callback != null){
                new Thread(() -> {
                    try {
                        ses.shutdown();
                        ses.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
                        List<File> files = new ArrayList<>(replicons.size());
                        for(Future<File> future : futuresFiles){
                            files.add(future.get());
                        }
                        callback.complete(files);
                    } catch (ExecutionException | InterruptedException e) {
                        LOGGER.error("Erreur d'attente de terminaison des téléchargements",e);
                    }
                }).start();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Erreur durant les téléchargements des replicons",e);
        }
    }

    public static void downloadAllReplicons(CompletableFuture<List<File>> callback) throws IOException{
        downloadReplicons(getAllReplicons(), callback);
    }

    public static URI getEUtilsLink(String application, Map<String, String> params) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(EUTILS_BASE_URL+application);
        for(Map.Entry<String, String> param : params.entrySet()){
            if(StringUtils.isNotBlank(param.getKey())){
                builder.setParameter(param.getKey(), param.getValue());
            }
        }
        if(!params.containsKey("api_key")){
            builder.setParameter("api_key", EUTILS_API_KEY);
        }
        return builder.build();
    }

    /**
     * Retourne l'URL permettant de télécharger le fichier dont le nom est donné.
     * @param replicon le replicon à télécharger
     * @return l'url
     */
    public static URI getGBDownloadURL(Replicon replicon){
        URI uri = null;
        Map<String, String> params = new HashMap<>();
        params.put("db", "nucleotide");
        params.put("rettype", "gb");
        params.put("id", replicon.getReplicon()+"."+replicon.getVersion());
        try{
            uri = getEUtilsLink(EUTILS_EFETCH, params);
        }catch(URISyntaxException e){
            LOGGER.error("Syntaxe URI incorrecte", e);
        }
        return uri;
        //return "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nucleotide&rettype=gb&id="+fileName;
    }

    /**
     * Retourne l'URL permettant d'obtenir au format JSON la liste entière des organismes d'un royaume
     * @param reign règne
     * @return l'URL de la requête
     */
    public static String getOrganismsListRequestURL(Reign reign){
        String uri = "";
        try{
            URIBuilder builder = new URIBuilder("https://www.ncbi.nlm.nih.gov/Structure/ngram");
            builder.setParameter("limit", "0");
            builder.setParameter("q","[display(),hist(group,subgroup,level)].from(GenomeAssemblies).usingschema(/schema/GenomeAssemblies).matching(tab==[\""+ reign.getSearchTable()+"\"]).sort(replicons,desc)");
            uri = builder.build().toString();
        }catch(URISyntaxException e){
            // ignore
        }
        return uri;
    }

    /**
     * Retourne la requête donnant au format JSON la liste complètes des organismes avec leurs noms, sous-groupes, groupes et royaumes respectifs.
     * @param ncOnly Ne récupère que les organismes ayant au moins 1 replicons de type NC_*
     * @return l'URL de la requête
     */
    public static String getFullOrganismsListRequestURL(boolean ncOnly){
        String uri = "";
        try{
            URIBuilder builder = new URIBuilder("https://www.ncbi.nlm.nih.gov/Structure/ngram");
            builder.setParameter("limit", "0"); // 0 -> charge tous les résultats
            //builder.setParameter("limit", "5"); // charge les 5 premiers résultats
            builder.setParameter("q","[display(organism,kingdom,group,subgroup,replicons)].from(GenomeAssemblies).usingschema(/schema/GenomeAssemblies).matching(tab==[\"Eukaryotes\",\"Viruses\",\"Prokaryotes\"]"+(ncOnly ? " and replicons like \"*NC_*\"" : "")+")");
            uri = builder.build().toString();
        }catch(URISyntaxException e){
            // ignore
        }
        return uri;
    }

    /**
     * Retoure une requête permettant de récuperer les compteurs d'un royaumes</br>
     * (nombres de groupes, sous-groupe avec leurs noms et leurs nombres d'entrées respectifs)
     * @param reign règne
     * @return l'URL de la requête
     */
    public static String getKingdomCountersURL(Reign reign){
        return "https://www.ncbi.nlm.nih.gov/Structure/ngram?limit=0&q=[hist(group,subgroup,kingdom)].from(GenomeAssemblies).usingschema(/schema/GenomeAssemblies).matching(tab==[\""
        + reign.getSearchTable()+"\"]).sort(replicons,desc)";
    }

    /**
     * La requête perettant de récuperer le nombre totale d'entrées pour un règne.
     * @param reign règne
     * @return l'URL de la requête
     */
    public static String getReignTotalEntriesNumberURL(Reign reign){
        return "https://www.ncbi.nlm.nih.gov/Structure/ngram?&q=[display()].from(GenomeAssemblies).matching(tab==[\""+ reign.getSearchTable()+"\"])&limit=1";
    }

    /**
     * Retourne la chaîne de caractères normalisés, supprimant les caractères interdits pour la création des dossiers sous Windows.
     * @param s la chaîne à normaliser
     * @return la chaîne normalisée
     */
    public static String normalizeString(String s){
        return StringUtils.isBlank(s) ? "" : s.replaceAll("[/\\\\:*<>?|]+","").trim();
    }

    /**
     * Retourne le chemin de l'organisme à l'intérieur de l'arborescence des organismes.</br>
     * Tous les paramètres sont normalisés et ne doivent pas être null.
     * @param kingdom royaume de l'organisme
     * @param group groupe de l'organisme
     * @param subgroup sous-groupe de l'organisme
     * @param organism nom de l'organism
     * @return Le chemin à l'intérieur de l'arborescence des organismes
     */
    public static Path getPathOfOrganism(String kingdom, String group, String subgroup, String organism){
        return Paths.get(normalizeString(kingdom), normalizeString(group), normalizeString(subgroup), normalizeString(organism));
    }

    public static Path getPathOfOrganism(Hierarchy h){
        return Paths.get(h.getKingdom(), h.getGroup(), h.getSubgroup(), h.getOrganism());
    }

    public static Path getPathOfReplicon(Replicon h){
        return getPathOfOrganism(h.getHierarchy()).resolve(h.getReplicon()+".gb");
    }

    /**
     * Lit le fichier JSON CommonUtils.DATABASE_PATH s'il existe et le met en jour avec les données téléchargées depuis genbank.
     * @return le singleton de la base de données des hierarchy
     * @throws IOException
     */
    public static Map<String, Hierarchy> updateNCDatabase() throws IOException {
        JsonNode genbankJSON;
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule genBankModule = new SimpleModule();
        genBankModule.addDeserializer(Hierarchy.class, new JSONUtils.HierarchyFromGenbankDeserializer());
        mapper.registerModule(genBankModule);
        try(BufferedReader reader = readRequest(getFullOrganismsListRequestURL(true))) {
            LOGGER.info("Lecture de la base de données genbank, cette opération prend quelques secondes...");
            genbankJSON = mapper.readTree(reader.lines().collect(Collectors.joining()));
        }catch (IOException e){
            throw new IOException("Erreur lors du téléchargement de la liste des entrées", e);
        }
        JsonNode dataNode = genbankJSON.get("ngout").get("data");
        JsonNode contentNode = dataNode.get("content");
        int numberOfOrganisms = dataNode.get("totalCount").intValue(), i =0;
        LOGGER.info("Traitement de "+numberOfOrganisms+" entrées");
        for(JsonNode entry : contentNode) {
            String organism = entry.get("organism").textValue();
            //Création de l'entrée en BDD
            if(!HIERARCHY_DB.containsKey(organism)){
                HIERARCHY_DB.put(organism, mapper.treeToValue(entry, Hierarchy.class));
            }
            Hierarchy h = HIERARCHY_DB.get(organism);
            h.updateReplicons(extractRepliconsFromJSONEntry(entry.get("replicons").textValue(), h));
            if(++i % 100 == 0) {
                LOGGER.debug(i + "/" + numberOfOrganisms + " organismes traités");
                float d = ((float)i / (float)numberOfOrganisms);
                MainWindowController.get().progressBar.setProgress(d);
                final int j = i;
                Platform.runLater(()->MainWindowController.get().labelDownload.setText(j+"/"+numberOfOrganisms+" fichiers traités"));
            }
        }
        MainWindowController.get().progressBar.setProgress(1.0F);
        Platform.runLater(()->MainWindowController.get().labelDownload.setText(numberOfOrganisms+"/"+numberOfOrganisms+" fichiers traités"));
        JSONUtils.saveToFile(CommonUtils.DATABASE_PATH, HIERARCHY_DB.values());
        //JSONUtils.saveToFile(CommonUtils.DATABASE_PATH, JSONUtils.toJSON(new ArrayList<>(HIERARCHY_DB.values())));
        return HIERARCHY_DB;
    }

    public static boolean createAllOrganismsDirectories(Path rootDirectory){
        try {
            for(Hierarchy hierarchy : HIERARCHY_DB.values()){
                //Création du dossier
                Path entryPath = rootDirectory.resolve(getPathOfOrganism(hierarchy.getKingdom(), hierarchy.getGroup(), hierarchy.getSubgroup(), hierarchy.getOrganism()));
                FileUtils.forceMkdir(entryPath.toFile());
            }
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la création de l'arborescence des organismes dans '"+rootDirectory+"'", e);
            return false;
        }
        return true;
    }

    /**
     * @return Map des hierarchies le fichier JSON CommonUtils.DATABASE_PATH. Les clés sont les valeurs d'organism de chaque hierarchy.
     */
    private static void loadLocalDatabase() {
        File dbFile = CommonUtils.DATABASE_PATH.toFile();
        if(dbFile.exists() && dbFile.isFile() && dbFile.canRead()){
            try {
                List<Hierarchy> hierarchies = JSONUtils.readFromFile(CommonUtils.DATABASE_PATH);
                int i = 0, numberOfHierarchies = hierarchies.size();
                LOGGER.info("Le fichier '"+dbFile.getAbsolutePath()+"' contient "+numberOfHierarchies+" entrées");
                for(Hierarchy hierarchy : hierarchies){
                    HIERARCHY_DB.put(hierarchy.getOrganism(), hierarchy);
                    hierarchy.getReplicons().values().forEach(r -> r.setHierarchy(hierarchy));
                    if(++i % 100 == 0){
                        LOGGER.debug(i+"/"+numberOfHierarchies+" organismes chargés");
                    }
                }
                LOGGER.info("Tout les organismes et leurs replicons ont été chargés depuis le fichier '"+dbFile.getAbsolutePath()+"'");
            } catch (IOException e) {
                LOGGER.error("Erreur de lecture de la base de données '"+CommonUtils.DATABASE_PATH+"'",e);
            }
        }else{
            LOGGER.warn("Le fichier '"+dbFile.getAbsolutePath()+"' n'existe pas ou n'est pas accessible.");
        }
    }

    public static List<Replicon> extractRepliconsFromJSONEntry(String repliconsList, Hierarchy hierarchy) {
        List<Replicon> replicons = new ArrayList<>();
        String[] repliconsJSONValues = repliconsList.split(";");
        Matcher m;
        for(String value : repliconsJSONValues){
            m = RegexUtils.REPLICON_PATTERN.matcher(value);
            if(m.matches()){
                replicons.add(new Replicon(m.group(1), Integer.parseInt(m.group(2)), hierarchy));
            }
        }
        return replicons;
    }

    /**
     *
     * @param reign règne
     * @return nombre d'entrées du royaumes
     */
    public static int getNumberOfEntries(Reign reign){
        int numerOfEntries = -1;
        try(BufferedReader reader = readRequest(getReignTotalEntriesNumberURL(reign))){
            JsonNode json = new ObjectMapper().readTree(reader.lines().collect(Collectors.joining()));
            numerOfEntries = json.get("ngout").get("data").get("totalCount").intValue();
        }catch(IOException | NullPointerException e){
            LOGGER.error("Erreur de récupération du nombre total d'entrées du règne '"+ reign.getSearchTable()+"'",e);
        }
        return numerOfEntries;
    }

    /**
     * Retoure un BufferedReader permettant de lire la réponse de la requête donnée
     * @param requestURL la requête
     * @return le BufferedReader
     * @throws IOException Exception lancée si un problème survient à l'instanciation (URL malformée, ...)
     */
    public static BufferedReader readRequest(String requestURL) throws IOException {
        return new BufferedReader(new InputStreamReader(new URL(requestURL).openStream()));
    }

    public static BufferedReader readRequest(URL requestURL) throws IOException {
        return new BufferedReader(new InputStreamReader(requestURL.openStream()));
    }

    /**
     * @return le singleton de la map des hierarchy sans la mettre à jour
     */
    public static Map<String, Hierarchy> getHierarchyDatabase(){
        return HIERARCHY_DB;
    }
}
