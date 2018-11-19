package fr.unistra.bioinfo.genbank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.util.concurrent.RateLimiter;
import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.common.JSONUtils;
import fr.unistra.bioinfo.common.RegexUtils;
import fr.unistra.bioinfo.gui.MainWindowController;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import javafx.application.Platform;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class GenbankUtils {
    private static RepliconService repliconService;
    private static HierarchyService hierarchyService;

    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final String NGRAM_BASE_URL = "https://www.ncbi.nlm.nih.gov/Structure/ngram";
    private static final String EUTILS_BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
    /** Permet de faire jusqu'à 10 appels par seconde au lieu de 3, mais seulement à partir du 1 décembre 2018 à 12H */
    private static final String EUTILS_API_KEY = "13aa4cb817db472b3fdd1dc0ca1655940809";
    private static final Integer REQUEST_LIMIT = (StringUtils.isNotBlank(EUTILS_API_KEY) && LocalDateTime.now().isAfter(LocalDateTime.of(2018, Month.DECEMBER, 1, 12, 0))) ?  10 : 3;
    private static final String EUTILS_EFETCH = "efetch.fcgi";

    static{
        LOGGER.info("Nombre de requêtes limitées à "+REQUEST_LIMIT+" par secondes");
    }

    public static void downloadReplicons(List<RepliconEntity> replicons, final CompletableFuture<List<File>> callback) {
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
        downloadReplicons(repliconService.getAll(), callback);
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
    public static URI getGBDownloadURL(RepliconEntity replicon){
        URI uri = null;
        Map<String, String> params = new HashMap<>();
        params.put("db", "nucleotide");
        params.put("rettype", "gb");
        params.put("id", replicon.getName()+"."+replicon.getVersion());
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
        return buildNgramURL(reign, null, "group", "subgroup", "level");
    }

    /**
     * Retourne la requête donnant au format JSON la liste complètes des organismes avec leurs noms, sous-groupes, groupes et royaumes respectifs.
     * @param ncOnly Ne récupère que les organismes ayant au moins 1 replicons de type NC_*
     * @return l'URL de la requête
     */
    public static String getFullOrganismsListRequestURL(boolean ncOnly){
        return buildNgramURL(Reign.ALL, ncOnly ? "replicons like \"NC_\"" : "", "organism", "kingdom", "group", "subgroup", "replicons");
    }

    /**
     * Retoure une requête permettant de récuperer les compteurs d'un royaumes</br>
     * (nombres de groupes, sous-groupe avec leurs noms et leurs nombres d'entrées respectifs)
     * @param reign règne
     * @return l'URL de la requête
     */
    public static String getKingdomCountersURL(Reign reign){
        return buildNgramURL(reign, null, true,"group", "subgroup", "kingdom");
    }

    /**
     * La requête perettant de récuperer le nombre totale d'entrées pour un règne.
     * @param reign règne
     * @return l'URL de la requête
     */
    public static String getReignTotalEntriesNumberURL(Reign reign){
        return buildNgramURL(reign, null);
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

    public static Path getPathOfOrganism(HierarchyEntity h){
        return Paths.get(h.getKingdom(), h.getGroup(), h.getSubgroup(), h.getOrganism());
    }

    /**
     * Retourne le chemin  du replicon à l'intérieur de l'arborescence des organismes avec le nom du fichier *.gb.</br>
     * Le replicon doit avoir un organism non-null.
     * @param r le replicon
     * @return le chemin du fichier *.gb
     */
    public static Path getPathOfReplicon(RepliconEntity r){
        return getPathOfOrganism(r.getHierarchyEntity()).resolve(r.getName()+".gb");
    }

    /**
     * Lit le fichier JSON CommonUtils.DATABASE_PATH s'il existe et le met en jour avec les données téléchargées depuis genbank.</br>
     * Les logs d'hibernante sont désactivés pour cette méthode
     * @throws IOException si un problème interviens lors de la requête à genbank
     */
    public static void updateNCDatabase() throws IOException {
        MainWindowController controller = MainWindowController.get();
        CommonUtils.disableHibernateLogging();
        JsonNode genbankJSON;
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule genBankModule = new SimpleModule();
        genBankModule.addDeserializer(HierarchyEntity.class, new JSONUtils.HierarchyFromGenbankDeserializer());
        mapper.registerModule(genBankModule);
        try(BufferedReader reader = readRequest(getFullOrganismsListRequestURL(true))) {
            LOGGER.info("Lecture de la base de données genbank, cette opération prend quelques secondes...");
            genbankJSON = mapper.readTree(reader.lines().collect(Collectors.joining()));
        }catch (IOException e){
            throw new IOException("Erreur lors du téléchargement de la liste des entrées", e);
        }
        JsonNode dataNode = genbankJSON.get("ngout").get("data");
        JsonNode contentNode = dataNode.get("content");
        int organismCount = 0, numberOfOrganisms = dataNode.get("totalCount").intValue();
        LOGGER.info("Traitement de "+numberOfOrganisms+" organismes");
        List<RepliconEntity> replicons = new ArrayList<>(numberOfOrganisms);
        for(JsonNode entry : contentNode) {
            String organism = entry.get("organism").textValue();
            HierarchyEntity h = hierarchyService.getByOrganism(organism);
            if(h == null){
                String kingdom = entry.get("kingdom").textValue();
                String group = entry.get("group").textValue();
                String subgroup = entry.get("subgroup").textValue();
                h = new HierarchyEntity(kingdom, group, subgroup, organism);
                hierarchyService.save(h);
            }
            if(++organismCount % 100 == 0){
                LOGGER.debug(organismCount+"/"+numberOfOrganisms+" organismes traités");


            }
            float d = ((float)organismCount / numberOfOrganisms);
            if(controller != null){
                controller.getProgressBar().setProgress(d);
                final int j = organismCount;
                Platform.runLater(()->controller.getDownloadLabel().setText(j+"/"+numberOfOrganisms+" organismes mis à jour"));
            }
            List<RepliconEntity> extractedReplicons = extractRepliconsFromJSONEntry(entry.get("replicons").textValue(), h);
            replicons.addAll(extractedReplicons);
            for(RepliconEntity r : extractedReplicons){
                r.setHierarchyEntity(h);
            }
        }
        if(controller != null){
            Platform.runLater(()->controller.getDownloadLabel().setText(numberOfOrganisms+"/"+numberOfOrganisms+" organismes mis à jour"));
            controller.getProgressBar().setProgress(1.0F);
        }
        LOGGER.info("Sauvegarde de "+replicons.size()+" replicons");
        repliconService.saveAll(replicons);
        CommonUtils.enableHibernateLogging(false);
    }

    public static boolean createAllOrganismsDirectories(Path rootDirectory){
        try {
            for(HierarchyEntity hierarchy : hierarchyService.getAll()){
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

    private static List<RepliconEntity> extractRepliconsFromJSONEntry(String repliconsList, HierarchyEntity hierarchy) {
        List<RepliconEntity> replicons = new ArrayList<>();
        String[] repliconsJSONValues = repliconsList.split(";");
        Matcher m;
        for(String value : repliconsJSONValues){
            m = RegexUtils.REPLICON_PATTERN.matcher(value);
            if(m.matches()){
                replicons.add(new RepliconEntity(m.group(1), Integer.parseInt(m.group(2)), hierarchy));
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

    /**
     * Retoure un BufferedReader permettant de lire la réponse de la requête donnée
     * @param requestURL la requête
     * @return le BufferedReader
     * @throws IOException Exception lancée si un problème survient à l'instanciation (URL malformée, ...)
     */
    public static BufferedReader readRequest(URL requestURL) throws IOException {
        return new BufferedReader(new InputStreamReader(requestURL.openStream()));
    }

    public static void setRepliconService(@NonNull RepliconService repliconService) {
        GenbankUtils.repliconService = repliconService;
    }

    public static void setHierarchyService(@NonNull HierarchyService hierarchyService) {
        GenbankUtils.hierarchyService = hierarchyService;
    }

    public static String buildNgramURL(Reign reign, String condition, String... fields){
        return buildNgramURL(reign, condition, 0, false, fields);
    }

    public static String buildNgramURL(Reign reign, String condition, boolean useHist, String... fields){
        return buildNgramURL(reign, condition, 0, useHist, fields);
    }

    public static String buildNgramURL(Reign reign, String condition, int limit, String... fields){
        return buildNgramURL(reign, condition, limit, false, fields);
    }

    private static String buildNgramURL(Reign reign, String condition, int limit, boolean useHist, String... fields){
        try{
            URIBuilder builder = new URIBuilder(NGRAM_BASE_URL);
            builder.setParameter("q",GenbankUtils.buildNgramQueryString(reign, condition, useHist, fields));
            builder.setParameter("limit", Integer.toString(limit));
            return builder.build().toString();
        }catch(URISyntaxException e){
            // ignore
        }
        return null;
    }

    public static String buildNgramQueryString(Reign reign, String condition, String... fields){
        return buildNgramQueryString(reign, condition, false, fields);
    }

    public static String buildNgramQueryString(Reign reign, String condition, boolean useHist, String... fields){
        StringBuilder builder = new StringBuilder(128);
        builder.append("[").append(useHist ? "hist(" : "display(");
        if(ArrayUtils.isNotEmpty(fields)){
            int lastIdx = fields.length-1;
            for(int i=0; i<lastIdx; i++){
                builder.append(fields[i]).append(",");
            }
            builder.append(fields[lastIdx]);
        }
        builder.append(")].from(GenomeAssemblies).matching(");
        if(reign != null){
            builder.append("tab==[");
            builder.append(reign.getSearchTable());
            builder.append("]");
        }
        if(StringUtils.isNotBlank(condition)){
            builder.append(" and ").append(condition);
        }
        builder.append(")");
        return builder.toString();
    }

    public static HierarchyEntity getHierarchyInfoByOrganism(String organism) {
        HierarchyEntity entity = null;
        try(BufferedReader reader = readRequest(
                buildNgramURL(
                        Reign.ALL,
                        "organism == \""+organism+"\"",
                        1,
                        "organism", "kingdom", "group", "subgroup"))){
                JsonNode json = new ObjectMapper().readTree(reader.lines().collect(Collectors.joining()));
                JsonNode content = json.get("ngout").get("data").get("content");
                if(content.isArray() && content.size() == 1){
                    JsonNode jsonHierarchy = content.get(0);
                    entity = new HierarchyEntity();
                    entity.setKingdom(jsonHierarchy.get("kingdom").textValue());
                    entity.setGroup(jsonHierarchy.get("group").textValue());
                    entity.setSubgroup(jsonHierarchy.get("subgroup").textValue());
                    entity.setOrganism(jsonHierarchy.get("organism").textValue());
                }
        }catch(IOException e){
            LOGGER.error("Erreur lors de la récupération des informations de l'organisme '"+organism+"'", e);
        }
        return entity;
    }
}
