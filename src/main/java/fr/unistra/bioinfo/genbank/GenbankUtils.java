package fr.unistra.bioinfo.genbank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.util.concurrent.RateLimiter;
import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.common.EventUtils;
import fr.unistra.bioinfo.common.JSONUtils;
import fr.unistra.bioinfo.common.RegexUtils;
import fr.unistra.bioinfo.gui.MainWindowController;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconType;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import javafx.application.Platform;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GenbankUtils {
    private static RepliconService repliconService;
    private static HierarchyService hierarchyService;

    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final String NGRAM_BASE_URL = "https://www.ncbi.nlm.nih.gov/Structure/ngram";
    private static final String DDL_BASE_URL = "https://www.ncbi.nlm.nih.gov/sviewer/viewer.fcgi";
    private static final String EFETCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.cgi";
    /** 10 requête max avec un clé API d'après la doc de genbank, mais bizarrement ça marche pas, donc 3 par défaut */
    private static final Integer REQUEST_LIMIT = 3;
    // Nombre de téléchargement concurrents max, en respectant REQUEST_LIMIT, maximum 32 threads
    public static final Integer DOWNLOAD_THREAD_POOL_SIZE = Math.min(Runtime.getRuntime().availableProcessors() * 4, 32);
    /** Match une entrée replicon récupérée dans le JSON genbank. Example : mitochondrion MT:NC_040902.1/ */
    private static final Pattern REPLICON_JSON_ENTRY_PATTERN = Pattern.compile("^(.+):(.+)$");

    /** Pour éviter le ban, on s'octroie une marge de 5% du nombre de requetes par secondes max */
    public static final RateLimiter GENBANK_REQUEST_LIMITER = RateLimiter.create(GenbankUtils.REQUEST_LIMIT * 0.95);
    private static final EventUtils.EventListener DOWNLOAD_END_LISTENER = (event) -> {
        if(event.getType() == EventUtils.EventType.DOWNLOAD_REPLICON_END && event.getReplicon() != null){
            repliconService.save(event.getReplicon());
        }
    };

    static{
        EventUtils.subscribe(DOWNLOAD_END_LISTENER);
        LOGGER.info("Taille du pool de thread de téléchargement fixé à {}", DOWNLOAD_THREAD_POOL_SIZE);
    }

    public static void downloadReplicons(List<RepliconEntity> replicons, final CompletableFuture<List<RepliconEntity>> callback) {
        final ExecutorService threadPool = Executors.newFixedThreadPool(DOWNLOAD_THREAD_POOL_SIZE);
        final List<DownloadRepliconTask> tasks = new ArrayList<>(replicons.size());

        replicons.stream().filter(Objects::nonNull).forEach(r -> tasks.add(new DownloadRepliconTask(r, repliconService)));
        LOGGER.info("Débuts des téléchargements ({} replicons)", tasks.size());
        EventUtils.sendEvent(EventUtils.EventType.DOWNLOAD_BEGIN, ""+tasks.size());
            new Thread(() -> {
                try {
                    final List<Future<RepliconEntity>> futuresFiles = threadPool.invokeAll(tasks);
                    threadPool.shutdown();
                    threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
                    LOGGER.info("Fin des téléchargements");
                    List<RepliconEntity> files = new ArrayList<>(replicons.size());
                    for(Future<RepliconEntity> future : futuresFiles){
                        RepliconEntity f = future.get();
                        if(f != null){
                            files.add(f);
                        }
                    }
                    callback.complete(files);
                } catch (ExecutionException | InterruptedException e) {
                    LOGGER.error("Erreur d'attente de terminaison des téléchargements",e);
                    callback.completeExceptionally(e);
                }
            }).start();
    }

    /**
     * Télécharge tous les replicons présent en BDD, un update est nécessaire avant d'appeler cette fonction
     * @param callback la callback contenant les données une fois les téléchargements tous terminés
     * @see GenbankUtils#updateNCDatabase
     */
    public static void downloadAllReplicons(CompletableFuture<List<RepliconEntity>> callback){
        downloadReplicons(repliconService.getAll(), callback);
    }

    public static URIBuilder buildURL(String baseURL, Map<String, String> params) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(baseURL);
        for(Map.Entry<String, String> param : params.entrySet()){
            if(StringUtils.isNotBlank(param.getKey())){
                builder.setParameter(param.getKey(), param.getValue());
            }
        }
        return builder;
    }

    public static URI getDownloadLink(Map<String, String> params) throws URISyntaxException {
        return buildURL(DDL_BASE_URL, params).build();
    }

    /**
     * Retourne l'URL permettant de télécharger le fichier dont le nom est donné.
     * @param replicon le replicon à télécharger
     * @return l'url
     */
    public static URI getGBDownloadURL(RepliconEntity replicon){
        //return getGBDownloadURL(replicon.getGenbankName());
        return getEFetchDownloadURI(replicon.getGenbankName());
    }

    /**
     * Retourne l'URL permettant de télécharger un fichier contenant les replicons dont les ids sont donnés</br>
     * Les ids doivent être séparé par une virgule.
     * @param ids liste des ids des replicons à télécharger. Ex: "NC_1.1,NC_2.1,NC_3.1"
     * @return l'url
     */
    public static URI getGBDownloadURL(String ids){
        URI uri = null;
        Map<String, String> params = new HashMap<>();
        params.put("db", "nuccore");
        params.put("basic_feat", "on");
        params.put("withparts", "on");
        params.put("retmode", "raw");
        params.put("id", ids);
        try{
            uri = getDownloadLink(params);
        }catch(URISyntaxException e){
            LOGGER.error("Syntaxe URI incorrecte", e);
        }
        return uri;
    }

    private static URI getEFetchDownloadURI(String ids){
        Map<String, String> params = new HashMap<>();
        params.put("tool", "fbrossar-bioinfo");
        params.put("email", "florian.brossard@etu.unistra.fr");
        params.put("api_key", "d2780ecb17536153e4d7a8a8a77886afce08");
        params.put("db", "nuccore");
        params.put("rettype", "gbwithparts");
        params.put("retmode", "text");
        params.put("id", ids);
        try{
            return buildURL(DDL_BASE_URL, params).build();
        }catch(URISyntaxException e){
            LOGGER.error("Syntaxe URI incorrecte", e);
            return null;
        }
    }

    /**
     * Retourne la requête donnant au format JSON la liste complètes des organismes avec leurs noms, sous-groupes, groupes et royaumes respectifs.
     * @param limit Nombre d'éléments à charger (0 pour tout charger)
     * @return l'URL de la requête
     */
    private static String getFullOrganismsListRequestURL(int limit){
        return buildNgramURL(Reign.ALL, "replicons like \"*NC_*\"", limit, "organism", "kingdom", "group", "subgroup", "replicons");
        //return buildNgramURL(Reign.ALL, "replicons like \"*NC_*\"", limit, "organism", "kingdom", "group", "subgroup", "replicons");
    }

    /**
     * Retoure une requête permettant de récuperer les compteurs d'un royaumes</br>
     * (nombres de groupes, sous-groupe avec leurs noms et leurs nombres d'entrées respectifs)
     * @param reign règne
     * @return l'URL de la requête
     */
    static String getKingdomCountersURL(Reign reign){
        return buildNgramURL(reign, null, "group", "subgroup", "kingdom");
    }

    /**
     * La requête perettant de récuperer le nombre totale d'entrées pour un règne.
     * @param reign règne
     * @return l'URL de la requête
     */
    static String getReignTotalEntriesNumberURL(Reign reign){
        return buildNgramURL(reign, null, 1);
    }

    /**
     * Retourne la chaîne de caractères normalisés, supprimant les caractères interdits pour la création des dossiers sous Windows.
     * @param s la chaîne à normaliser
     * @return la chaîne normalisée
     */
    private static String normalizeString(String s){
        return StringUtils.isBlank(s) ? "" : s.replaceAll("[/\\\\:*<>?|]+","").trim();
    }

    /**
     * Retourne le chemin de l'organisme à l'intérieur de l'arborescence des organismes.</br>
     * Tous les paramètres sont normalisés et ne doivent pas être null.
     * @param kingdom royaume de l'organisme
     * @param group groupe de l'organisme
     * @param subgroup sous-groupe de l'organisme
     * @param organism nom de l'organism
     * @return Le chemin de l'organisme
     * @see CommonUtils#RESULTS_PATH
     */
    private static Path getPathOfOrganism(String kingdom, String group, String subgroup, String organism){
        return Paths.get(normalizeString(kingdom), normalizeString(group), normalizeString(subgroup), normalizeString(organism));
    }

    /**
     * Retourne le chemin de l'organisme à l'intérieur de l'arborescence des organismes.</br>
     * Tous les paramètres sont normalisés et ne doivent pas être null.
     * @param hierarchy hierarchy du dossier
     * @return Le chemin du dossier de l'organisme
     * @see CommonUtils#RESULTS_PATH
     */
    static Path getPathOfOrganism(HierarchyEntity hierarchy){
        return CommonUtils.RESULTS_PATH.resolve(Paths.get(hierarchy.getKingdom(), hierarchy.getGroup(), hierarchy.getSubgroup(), hierarchy.getOrganism()));
    }


    /**
     * Lit le fichier JSON CommonUtils.DATABASE_PATH s'il existe et le met en jour avec les données téléchargées depuis genbank.</br>
     * Les logs d'hibernante sont désactivés pour cette méthode
     * @throws GenbankException si un problème interviens lors de la requête à genbank
     */
    public static void updateNCDatabase() throws GenbankException {
        GenbankUtils.updateNCDatabase(0);
    }

    /**
     * Retourne les données de genbank pour une limite donnée
     * @return les données en JSON
     * @throws IOException si une erreur suirviens lors du téléchargement
     * @throws JSONException si le json n'as pas de données
     */
    private static JsonNode getGenbankDatas(int limit) throws IOException, JSONException{
        JsonNode genbankJSON;
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule genBankModule = new SimpleModule();
        genBankModule.addDeserializer(HierarchyEntity.class, new JSONUtils.HierarchyFromGenbankDeserializer());
        mapper.registerModule(genBankModule);
        GENBANK_REQUEST_LIMITER.acquire();
        try(BufferedReader reader = readRequest(getFullOrganismsListRequestURL(limit))) {
            LOGGER.info("Lecture de la base de données genbank, cette opération prend quelques secondes...");
            genbankJSON = mapper.readTree(reader.lines().collect(Collectors.joining()));
        }catch (IOException e){
            throw new IOException("Erreur lors du téléchargement de la liste des entrées", e);
        }
        if(!genbankJSON.has("ngout") || !genbankJSON.get("ngout").has("data")){
            throw new JSONException("Pas de noeud data dans le JSON");
        }
        return genbankJSON.get("ngout").get("data");
    }

    private static HierarchyEntity getOrCreateHierarchyFromJsonNode(JsonNode json){
        String organism = json.get("organism").textValue();
        HierarchyEntity h = hierarchyService.getByOrganism(organism);
        if(h == null){
            String kingdom = json.get("kingdom").textValue();
            String group = json.get("group").textValue();
            String subgroup = json.get("subgroup").textValue();
            h = new HierarchyEntity(kingdom, group, subgroup, organism);
            hierarchyService.save(h);
        }
        return h;
    }

    private static List<RepliconEntity> jsonEntryToReplicon(JsonNode organismJson){
        return extractRepliconsFromJSONEntry(organismJson.get("replicons").textValue(), getOrCreateHierarchyFromJsonNode(organismJson));
    }

    /**
     * Charge et met à jours les metadonnées des replicons
     * @param limit limite d'entité à charger (pour les tests principalement)
     * @throws GenbankException Si une erreur empêche la mise à jours des métadonnées
     */
    public static void updateNCDatabase(int limit) throws GenbankException  {
        int batchSaveSize = 100;
        MainWindowController controller = MainWindowController.get();
        CommonUtils.disableHibernateLogging();
        JsonNode dataNode;
        try{
            dataNode = getGenbankDatas(limit);
        }catch(IOException | JSONException e){
            LOGGER.error("Erreur lors de la récupération des données de GenBank", e);
            throw new GenbankException("Erreur lors de la récupération des données de GenBank", e);
        }
        JsonNode contentNode = dataNode.get("content");
        int organismCount = 0, numberOfOrganisms = dataNode.get("content").size();
        LOGGER.info("Traitement de {} organismes", numberOfOrganisms);
        List<RepliconEntity> replicons = new ArrayList<>(128);
        List<String> repliconsNames = new ArrayList<>(128);
        for(JsonNode organismJson : contentNode) {
            replicons.addAll(jsonEntryToReplicon(organismJson));
            if(++organismCount % batchSaveSize == 0){
                repliconService.saveAll(replicons);
                //sauvegarde régulière pour éviter un pic de mémoire trop élevé
                repliconsNames.addAll(replicons.stream().map(RepliconEntity::getName).collect(Collectors.toList()));
                replicons.clear();
                LOGGER.info("{}/{} organismes traités", organismCount, numberOfOrganisms);
                float d = ((float)organismCount / numberOfOrganisms);
                if(controller != null){
                    controller.getProgressBar().setProgress(d);
                    final int j = organismCount;
                    Platform.runLater(()->controller.getDownloadLabel().setText(j+"/"+numberOfOrganisms+" organismes mis à jour"));
                }
            }
        }
        if(!replicons.isEmpty()){
            repliconService.saveAll(replicons);
            //sauvegarde des réplicons restants
            repliconsNames.addAll(replicons.stream().map(RepliconEntity::getName).collect(Collectors.toList()));
            replicons.clear();
        }
        if(controller != null){
            Platform.runLater(()->controller.getDownloadLabel().setText(numberOfOrganisms+"/"+numberOfOrganisms+" organismes mis à jour"));
            controller.getProgressBar().setProgress(1.0F);
        }
        //On supprime la différence entre genbank et la base de données
        repliconService.deleteWhereNameIsNotIn(repliconsNames.stream().distinct().collect(Collectors.toList()));
        hierarchyService.deleteHierarchyWithoutReplicons();
        CommonUtils.enableHibernateLogging(true);
        EventUtils.sendEvent(EventUtils.EventType.METADATA_END);
    }

    public static boolean createAllOrganismsDirectories(Path rootDirectory){
        try {
            for(HierarchyEntity hierarchy : hierarchyService.getAll()){
                //Création du dossier
                Path entryPath = rootDirectory.resolve(getPathOfOrganism(hierarchy.getKingdom(), hierarchy.getGroup(), hierarchy.getSubgroup(), hierarchy.getOrganism()));
                FileUtils.forceMkdir(entryPath.toFile());
            }
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la création de l'arborescence des organismes dans '{}'", rootDirectory, e);
            return false;
        }
        return true;
    }

    /**
     * Retourne La liste des replicons d'un json.Les replicons déjà existants en base sont retourés</br>
     * et mis à jour si la version en base est inférieure à la version indiquée dans le JSON.
     * </br> Les replicons, nouveaux comme existants ne sont pas sauvegardés.
     * @param repliconsList La liste des replicon en chaîne de caractère
     * @param hierarchy Le hierarchy des replicons
     * @return La liste des replicons, les nouveaux instanciés, et les existants récupérés dans la base et mis à jour.
     */
    private static List<RepliconEntity> extractRepliconsFromJSONEntry(String repliconsList, HierarchyEntity hierarchy) {
        List<String> repliconsJSONValues = Arrays.asList(repliconsList.split(";"));
        List<RepliconEntity> replicons = new ArrayList<>(repliconsJSONValues.size());
        for(String value : repliconsJSONValues) {
            Matcher m = RegexUtils.REPLICON_PATTERN.matcher(value);
            if(m.matches()){
                String name = m.group(1);
                Integer version = Integer.parseInt(m.group(2));
                RepliconEntity replicon = repliconService.getByName(name);
                if(replicon == null) {
                    replicon = new RepliconEntity(name, version, hierarchy);
                    replicon.setType(getTypeFromRepliconJsonEntry(value));
                    LOGGER.trace("Nouveau Replicon trouvé : {}.{}", name, version);
                    repliconService.save(replicon);
                }else{
                    //Mise à jour du replicon
                    if(replicon.getVersion() < version){
                        replicon.setComputed(false);
                        replicon.setParsed(false);
                        replicon.setVersion(version);
                    }
                    LOGGER.trace("Replicon '{}' mis à jour", replicon);
                }
                replicons.add(replicon);
            }
        }
        return replicons;
    }

    public static RepliconType getRepliconTypeFromRepliconName(String name) {
        String repliconsList = getRepliconsListFromName(name);
        if(StringUtils.isNotBlank(repliconsList)){
            String[] repliconsJSONValues = repliconsList.split(";");
            for(String s : repliconsJSONValues){
                if(s.contains(name)){
                    return getTypeFromRepliconJsonEntry(s);
                }
            }
        }else{
            LOGGER.warn("La récupération du type du replicon '{}' à échouée", name);
        }
        return RepliconType.DNA;
    }

    private static String getRepliconsListFromName(String name) {
        GENBANK_REQUEST_LIMITER.acquire();
        try(BufferedReader reader = readRequest(
                buildNgramURL(
                        Reign.ALL,
                        "replicons like \"*"+name+"*\"",
                        1,
                        "replicons"))){
            JsonNode json = new ObjectMapper().readTree(reader.lines().collect(Collectors.joining()));
            JsonNode content = json.get("ngout").get("data").get("content");
            if(content.isArray() && content.size() == 1 && content.get(0).has("replicons")){
                return content.get(0).get("replicons").textValue();
            }
        }catch(IOException e){
            LOGGER.error("Erreur lors de la récupération des informations du replicon '{}'", name, e);
        }
        return null;
    }

    private static RepliconType getTypeFromRepliconJsonEntry(String value) {
        Matcher m = REPLICON_JSON_ENTRY_PATTERN.matcher(value);
        if(m.matches()){
            String type = m.group(1);
            if(type.contains("chromosome")){
                return RepliconType.CHROMOSOME;
            }else if(type.contains("mitochondrion")){
                return RepliconType.MITOCHONDRION;
            }else if(type.contains("plast")){
                return RepliconType.PLAST;
            }else if(type.contains("plasmid")){
                return RepliconType.PLASMID;
            }else if(type.contains("linkage")){
                return RepliconType.LINKAGE;
            }
        }
        return RepliconType.DNA;
    }

    /**
     *
     * @param reign règne
     * @return nombre d'entrées du royaumes
     */
    static int getNumberOfEntries(Reign reign){
        int numerOfEntries = -1;
        GENBANK_REQUEST_LIMITER.acquire();
        try(BufferedReader reader = readRequest(getReignTotalEntriesNumberURL(reign))){
            JsonNode json = new ObjectMapper().readTree(reader.lines().collect(Collectors.joining()));
            numerOfEntries = json.get("ngout").get("data").get("totalCount").intValue();
        }catch(IOException | NullPointerException e){
            LOGGER.error("Erreur de récupération du nombre total d'entrées du règne '{}'", reign.getSearchTable(),e);
        }
        return numerOfEntries;
    }

    /**
     * Retoure un BufferedReader permettant de lire la réponse de la requête donnée
     * @param requestURL la requête
     * @return le BufferedReader
     * @throws IOException Exception lancée si un problème survient à l'instanciation (URL malformée, ...)
     */
    private static BufferedReader readRequest(String requestURL) throws IOException {
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

    private static String buildNgramURL(Reign reign, String condition,  String... fields){
        return buildNgramURL(reign, condition, 0, fields);
    }

    /**
     * Retourne l'URL permettant d'avoir les meta-données de Genbank sur les replicons
     * @param reign Le reigne souhaité
     * @param condition La condition permettant de filtrer les résultat
     * @param limit Le nombre de résultat max à retourner
     * @param fields Les champs souhaités dans les données
     * @return L'URL à appeler pour obtenir les données au format JSON
     */
    private static String buildNgramURL(Reign reign, String condition, int limit, String... fields){
        try{
            URIBuilder builder = new URIBuilder(NGRAM_BASE_URL);
            builder.setParameter("q",GenbankUtils.buildNgramQueryString(reign, condition, fields));
            builder.setParameter("limit", Integer.toString(limit));
            return builder.build().toString();
        }catch(URISyntaxException e){
            // ignore
        }
        return null;
    }

    static String buildNgramQueryString(Reign reign, String condition, String... fields){
        StringBuilder builder = new StringBuilder(128);
        builder.append("[").append("display(");
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
            if(StringUtils.isNotBlank(condition)){
                builder.append(" and ");
            }
        }
        if(StringUtils.isNotBlank(condition)){
            builder.append(condition);
        }
        builder.append(")");
        return builder.toString();
    }

    public static HierarchyEntity getHierarchyInfoByOrganism(String organism) {
        GENBANK_REQUEST_LIMITER.acquire();
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
                    entity = getOrCreateHierarchyFromJsonNode(content.get(0));
                }
        }catch(IOException e){
            LOGGER.error("Erreur lors de la récupération des informations de l'organisme '{}'", organism, e);
        }
        return entity;
    }

    public static HierarchyEntity getHierarchyInfoByRepliconName(String repliconName) {
        GENBANK_REQUEST_LIMITER.acquire();
        HierarchyEntity entity = null;
        try(BufferedReader reader = readRequest(
                buildNgramURL(
                        Reign.ALL,
                        "replicons like \"*"+repliconName+"*\"",
                        1,
                        "organism", "kingdom", "group", "subgroup"))){
            JsonNode json = new ObjectMapper().readTree(reader.lines().collect(Collectors.joining()));
            JsonNode content = json.get("ngout").get("data").get("content");
            if(content.isArray() && content.size() == 1){
                entity = getOrCreateHierarchyFromJsonNode(content.get(0));
            }
        }catch(IOException e){
            LOGGER.error("Erreur lors de la récupération des informations du replicon '{}'", repliconName, e);
        }
        return entity;
    }

    public static String getRepliconsIdsString(List<RepliconEntity> replicons) {
        int numberOfReplicons = replicons.size();
        StringBuilder builder = new StringBuilder(numberOfReplicons * 10);
        for(int i=0; i<numberOfReplicons-1; i++){
            builder.append(replicons.get(i).getGenbankName()).append(",");
        }
        builder.append(replicons.get(numberOfReplicons-1).getGenbankName());
        return builder.toString();
    }
}
