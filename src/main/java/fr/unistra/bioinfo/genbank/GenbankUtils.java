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
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import javafx.application.Platform;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
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
import java.util.*;
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
    private static final String EUTILS_API_KEY = "d2780ecb17536153e4d7a8a8a77886afce08";
    /** 10 requête max avec un clé API d'après la doc de genbank, mais bizarrement ça marche pas, donc 3 par défaut */
    private static final Integer REQUEST_LIMIT = 3;
    private static final Integer REPLICONS_BATCH_SIZE = 10;
    private static final String EUTILS_EFETCH = "efetch.fcgi";
    public static final RateLimiter GENBANK_REQUEST_LIMITER = RateLimiter.create(GenbankUtils.REQUEST_LIMIT);
    private static final EventUtils.EventListener DOWNLOAD_END_LISTENER = (event) -> {
        if(event.getType() == EventUtils.EventType.DOWNLOAD_END && event.getReplicon() != null){
            repliconService.save(event.getReplicon());
        }
    };

    static{
        EventUtils.subscribe(DOWNLOAD_END_LISTENER);
    }

    public static void downloadReplicons(List<RepliconEntity> replicons, final CompletableFuture<List<File>> callback) {
        final ExecutorService ses = Executors.newFixedThreadPool(GenbankUtils.REQUEST_LIMIT);
        List<List<RepliconEntity>> splittedRepliconsList = ListUtils.partition(replicons, REPLICONS_BATCH_SIZE);
        final List<DownloadRepliconTask> tasks = new ArrayList<>(splittedRepliconsList.size());

        splittedRepliconsList.forEach(repliconsSubList -> tasks.add(new DownloadRepliconTask(repliconsSubList, repliconService)));
        try {
            LOGGER.info("Débuts des téléchargements ({} fichiers)", tasks.size());
            final List<Future<File>> futuresFiles = ses.invokeAll(tasks);
            if(callback != null){
                new Thread(() -> {
                    try {
                        ses.shutdown();
                        ses.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
                        LOGGER.info("Fin des téléchargements");
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

    /**
     * Télécharge tous les replicons présent en BDD, un update est nécessaire avant d'appeler cette fonction
     * @param callback la callback contenant les données une fois les téléchargements tous terminés
     * @throws IOException En cas d'erreurs pendant le téléchargement
     * @see GenbankUtils#updateNCDatabase
     */
    public static void downloadAllReplicons(CompletableFuture<List<File>> callback){
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
        return getGBDownloadURL(replicon.getGenbankName());
    }

    /**
     * Retourne l'URL permettant de télécharger un fichier contenant les replicons donnés
     * @param replicons liste des replicons à télécharger
     * @return l'url
     */
    public static URI getGBDownloadURL(List<RepliconEntity> replicons){
        return getGBDownloadURL(getRepliconsIdsString(replicons));
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
        params.put("db", "nucleotide");
        params.put("rettype", "gb");
        params.put("id", ids);
        try{
            uri = getEUtilsLink(EUTILS_EFETCH, params);
        }catch(URISyntaxException e){
            LOGGER.error("Syntaxe URI incorrecte", e);
        }
        return uri;
    }

    /**
     * Retourne la requête donnant au format JSON la liste complètes des organismes avec leurs noms, sous-groupes, groupes et royaumes respectifs.
     * @param ncOnly Ne récupère que les organismes ayant au moins 1 replicons de type NC_*
     * @param pageNumber Nombre de page à charger (0 pour tout charger)
     * @return l'URL de la requête
     */
    private static String getFullOrganismsListRequestURL(boolean ncOnly, int pageNumber){
        return buildNgramURL(Reign.ALL, ncOnly ? "replicons like \"NC_\"" : "", pageNumber, "organism", "kingdom", "group", "subgroup", "replicons");
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
     * Retourne le chemin  du replicon à l'intérieur de l'arborescence des organismes avec le nom du fichier *.gb.</br>
     * Le replicon doit avoir un organism non-null.
     * @param r le replicon
     * @return le chemin du fichier *.gb
     */
    static Path getPathOfReplicon(RepliconEntity r){
        return getPathOfOrganism(r.getHierarchyEntity()).resolve(r.getName()+".gb");
    }


    /**
     * Lit le fichier JSON CommonUtils.DATABASE_PATH s'il existe et le met en jour avec les données téléchargées depuis genbank.</br>
     * Les logs d'hibernante sont désactivés pour cette méthode
     * @throws GenbankException si un problème interviens lors de la requête à genbank
     */
    public static void updateNCDatabase() throws GenbankException {
        GenbankUtils.updateNCDatabase(1000);
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
        try(BufferedReader reader = readRequest(getFullOrganismsListRequestURL(true, limit))) {
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

    private static List<RepliconEntity> jsonEntryToReplicon(JsonNode organismJson){
        String organism = organismJson.get("organism").textValue();
        HierarchyEntity h = hierarchyService.getByOrganism(organism);
        if(h == null){
            String kingdom = organismJson.get("kingdom").textValue();
            String group = organismJson.get("group").textValue();
            String subgroup = organismJson.get("subgroup").textValue();
            h = new HierarchyEntity(kingdom, group, subgroup, organism);
            hierarchyService.save(h);
        }
        return extractRepliconsFromJSONEntry(organismJson.get("replicons").textValue(), h);
    }

    /**
     * Charge et met à jours les metadonnées des replicons
     * @param limit limite d'entité à charger (pour les tests principalement)
     * @throws GenbankException Si une erreur empêche la mise à jours des métadonnées
     */
    public static void updateNCDatabase(int limit) throws GenbankException  {
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
        //int organismCount = 0, numberOfOrganisms = dataNode.get("totalCount").intValue();
        int organismCount = 0, numberOfOrganisms = dataNode.get("content").size();
        LOGGER.info("Traitement de {} organismes", numberOfOrganisms);
        //12331 replicons à lors du développement
        List<RepliconEntity> replicons = new ArrayList<>(13000);
        List<String> repliconsNames = new ArrayList<>(13000);
        for(JsonNode organismJson : contentNode) {
            replicons.addAll(jsonEntryToReplicon(organismJson));
            if(++organismCount % 100 == 0){
                repliconService.saveAll(replicons);
                //sauvegarde régulière pour éviter un pic de mémoire trop élevé
                repliconsNames.addAll(replicons.stream().map(RepliconEntity::getName).distinct().collect(Collectors.toList()));
                replicons.clear();
                LOGGER.info("{}/{} organismes traités", organismCount, numberOfOrganisms);
            }
            float d = ((float)organismCount / numberOfOrganisms);
            if(controller != null){
                controller.getProgressBar().setProgress(d);
                final int j = organismCount;
                Platform.runLater(()->controller.getDownloadLabel().setText(j+"/"+numberOfOrganisms+" organismes mis à jour"));
            }
        }
        if(!replicons.isEmpty()){
            repliconService.saveAll(replicons);
            //sauvegarde des réplicons restants
            repliconsNames.addAll(replicons.stream().map(RepliconEntity::getName).distinct().collect(Collectors.toList()));
            replicons.clear();
        }
        if(controller != null){
            Platform.runLater(()->controller.getDownloadLabel().setText(numberOfOrganisms+"/"+numberOfOrganisms+" organismes mis à jour"));
            controller.getProgressBar().setProgress(1.0F);
        }
        //On supprime la différence entre genbank et la base de données
        repliconService.deleteWhereNameIsNotIn(repliconsNames);
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
        List<RepliconEntity> replicons = new ArrayList<>();
        String[] repliconsJSONValues = repliconsList.split(";");
        Matcher m;
        for(String value : repliconsJSONValues){
            m = RegexUtils.REPLICON_PATTERN.matcher(value);
            if(m.matches()){
                String name = m.group(1);
                Integer version = Integer.parseInt(m.group(2));
                RepliconEntity replicon = repliconService.getByName(name);
                if(replicon == null) {
                    replicon = new RepliconEntity(name, version, hierarchy);
                    LOGGER.trace("Replicon '{}' ajouté en base", replicon);
                }else{
                    //Mise à jour du replicon
                    if(replicon.getVersion() < version){
                        replicon.setDownloaded(false);
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

    /**
     *
     * @param reign règne
     * @return nombre d'entrées du royaumes
     */
    static int getNumberOfEntries(Reign reign){
        int numerOfEntries = -1;
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
            LOGGER.error("Erreur lors de la récupération des informations de l'organisme '{}'", organism, e);
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
