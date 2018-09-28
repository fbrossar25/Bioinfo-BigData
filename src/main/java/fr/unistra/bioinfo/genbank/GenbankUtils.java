package fr.unistra.bioinfo.genbank;

import com.sun.istack.internal.NotNull;
import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.entities.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entities.RepliconEntity;
import fr.unistra.bioinfo.persistence.managers.HierarchyEntityManager;
import fr.unistra.bioinfo.persistence.managers.PersistentEntityManagerFactory;
import fr.unistra.bioinfo.persistence.managers.RepliconEntityManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.NoResultException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GenbankUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String EUTILS_BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
    /** Permet de faire jusqu'à 10 appels par seconde au lieu de 3 */
    public static final String EUTILS_API_KEY = "13aa4cb817db472b3fdd1dc0ca1655940809";
    public static final String EUTILS_EFETCH = "efetch.fcgi";
    public static final int BATCH_INSERT_SIZE = 1000;

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
     * @param fileName le nom du fichier sous la forme NC_X.Y, X est le nom du fichier, Y est la version.<br/>
     *                 Si Y n'est pas renseigné (i.e. on donns NC_X), la dernière version est récupérée.<br/>
     *                 Si la version n'existe pas, le serveur répond 400. Si fileName est vide, il répond 200.
     * @return l'url
     */
    public static URI getGBDownloadURL(@NotNull String fileName){
        URI uri = null;
        Map<String, String> params = new HashMap<>();
        params.put("db", "nucleotide");
        params.put("rettype", "gb");
        params.put("id", fileName);
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
    public static String getOrganismsListRequestURL(@NotNull Reign reign){
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
            builder.setParameter("limit", "0");
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
    public static String getKingdomCountersURL(@NotNull Reign reign){
        return "https://www.ncbi.nlm.nih.gov/Structure/ngram?limit=0&q=[hist(group,subgroup,kingdom)].from(GenomeAssemblies).usingschema(/schema/GenomeAssemblies).matching(tab==[\""
        + reign.getSearchTable()+"\"]).sort(replicons,desc)";
    }

    /**
     * La requête perettant de récuperer le nombre totale d'entrées pour un règne.
     * @param reign règne
     * @return l'URL de la requête
     */
    public static String getReignTotalEntriesNumberURL(@NotNull Reign reign){
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
    public static Path getPathOfOrganism(@NotNull String kingdom, @NotNull String group, @NotNull String subgroup, @NotNull String organism){
        return Paths.get(normalizeString(kingdom), normalizeString(group), normalizeString(subgroup), normalizeString(organism));
    }

    /**
     * Génère l'arborescence nécessaire pour tous les organismes. Prend du temps.
     * @param rootDirectory dossier devant contenir l'arborescence. Ne doit pas être null.
     * @param ncOnly Ne créer les dossiers que des organismes ayant au moins 1 replicons de type NC_*
     * @return true si tout c'est bien passé, false sinon
     */
    public static boolean createOrganismsTreeStructure(@NotNull Path rootDirectory, boolean ncOnly){
        HierarchyEntityManager hierarchyManager = PersistentEntityManagerFactory.getHierarchyManager();
        RepliconEntityManager repliconManager = PersistentEntityManagerFactory.getRepliconManager();
        List<HierarchyEntity> hierarchies = new ArrayList<>(BATCH_INSERT_SIZE);
        List<RepliconEntity> replicons = new ArrayList<>(BATCH_INSERT_SIZE);
        try(BufferedReader reader = readRequest(getFullOrganismsListRequestURL(ncOnly))){
            JSONObject json = new JSONObject(reader.lines().collect(Collectors.joining()));
            JSONArray entries = json.getJSONObject("ngout").getJSONObject("data").getJSONArray("content");
            LOGGER.info("Traitement de "+entries.length()+" entrées");
            for(Object obj : entries){
                JSONObject entry = (JSONObject)obj;
                String kingdom = entry.getString("kingdom");
                String group = entry.getString("group");
                String subgroup = entry.getString("subgroup");
                String organism = entry.getString("organism");
                //Création du dossier
                Path entryPath = rootDirectory.resolve(getPathOfOrganism(kingdom, group, subgroup, organism));
                FileUtils.forceMkdir(entryPath.toFile());
                //Création de l'entrée en BDD
                HierarchyEntity h = new HierarchyEntity(kingdom, group, subgroup, organism);
                hierarchies.add(h);
                if(hierarchies.size() >= BATCH_INSERT_SIZE){
                    //FIXME prendre en compte les doublons
                    LOGGER.info("Sauvegarde batchée de "+BATCH_INSERT_SIZE+" HierarchyEntity");
                    hierarchyManager.save(hierarchies);
                    hierarchies.clear();
                }
            }
            LOGGER.info(hierarchyManager.count()+" HierarchyEntity sauvegardés");
            hierarchies.clear();
            for(Object obj : entries) {
                JSONObject entry = (JSONObject)obj;
                String organism = entry.getString("organism");
                Session s = DBUtils.getSession();
                Query<HierarchyEntity> query = s.createQuery("from HierarchyEntity h where h.id = :id", HierarchyEntity.class).setParameter( "id",  organism);
                try{
                    HierarchyEntity h = query.getSingleResult();
                    replicons.addAll(extractRepliconsFromJSONEntry(entry, h));
                    if(replicons.size() >= BATCH_INSERT_SIZE){
                        LOGGER.info("Sauvegarde batchée de "+BATCH_INSERT_SIZE+" RepliconEntity");
                        repliconManager.save(replicons);
                        replicons.clear();
                    }
                }catch (NoResultException e){
                    //FIXME ne devrai pas arriver
                    LOGGER.error("HierarchyEntity non trouvée : '"+organism+"'", e);
                }
            }
            replicons.clear();
            LOGGER.info(repliconManager.count()+" RepliconEntity sauvegardés");
        }catch(IOException | NullPointerException e){
            LOGGER.error("Erreur de récupération de la liste des organismes",e);
            return false;
        }
        return true;
    }

    private static List<RepliconEntity> extractRepliconsFromJSONEntry(@NotNull JSONObject entry, @NotNull HierarchyEntity hierarchy) {
        List<RepliconEntity> replicons = new ArrayList<>();
        String[] repliconsJSONValues = entry.getString("replicons").split(";");
        Pattern p = Pattern.compile("^.*(NC_[0-9]+\\.[0-9]+).*$");
        Matcher m;
        for(String value : repliconsJSONValues){
            m = p.matcher(value);
            if(m.matches()){
                replicons.add(new RepliconEntity(m.group(1), hierarchy));
            }
        }
        return replicons;
    }

    /**
     *
     * @param reign règne
     * @return nombre d'entrées du royaumes
     */
    public static int getNumberOfEntries(@NotNull Reign reign){
        int numerOfEntries = -1;
        try(BufferedReader reader = readRequest(getReignTotalEntriesNumberURL(reign))){
            JSONObject json = new JSONObject(reader.lines().collect(Collectors.joining()));
            numerOfEntries = json.getJSONObject("ngout").getJSONObject("data").getInt("totalCount");
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
    public static BufferedReader readRequest(@NotNull String requestURL) throws IOException {
        return new BufferedReader(new InputStreamReader(new URL(requestURL).openStream()));
    }
}
