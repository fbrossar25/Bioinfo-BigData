package fr.unistra.bioinfo.genbank;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class GenbankUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Retourne l'URL permettant de télécharger le fichier dont le nom est donné.
     * @param fileName le nom du fichier sous la forme NC_X.Y, X est le nom du fichier, Y est la version.<br/>
     *                 Si Y n'est pas renseigné (i.e. on donns NC_X), la dernière version est récupérée.<br/>
     *                 Si la version n'existe pas, le serveur répond 400. Si fileName est vide, il répond 200.
     * @return l'url
     */
    public static String getGBDownloadURL(String fileName){
        return "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nucleotide&rettype=gb&id="+fileName;
    }

    /**
     * Retourne l'URL permettant d'obtenir au format JSON la liste entière des organismes d'un royaume
     * @param reign règne
     * @return l'URL de la requête
     */
    public static String getOrganismsListRequestURL(Reign reign){
        String request = "https://www.ncbi.nlm.nih.gov/Structure/ngram?";
        request += "&limit=0";
        request += "&q=[display(),hist(group,subgroup,level)].from(GenomeAssemblies).usingschema(/schema/GenomeAssemblies).matching(tab==[\""+ reign.getSearchTable()+"\"]).sort(replicons,desc)";
        return request;
    }

    public static String getFullOrganismsListRequestURL(){
        //return "https://www.ncbi.nlm.nih.gov/Structure/ngram?&limit=50&q=[display(organism,kingdom,group,subgroup)].from(GenomeAssemblies).usingschema(/schema/GenomeAssemblies).matching(tab==[\"Eukaryotes\",\"Viruses\",\"Prokaryotes\"])";
        return "https://www.ncbi.nlm.nih.gov/Structure/ngram?&limit=0&q=[display(organism,kingdom,group,subgroup)].from(GenomeAssemblies).usingschema(/schema/GenomeAssemblies).matching(tab==[\"Eukaryotes\"])";
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

    public static String getReignTotalURL(Reign reign){
        return "https://www.ncbi.nlm.nih.gov/Structure/ngram?&q=[display()].from(GenomeAssemblies).matching(tab==[\""+ reign.getSearchTable()+"\"])&limit=1";
    }

    private static String normalizeString(String s){
        return s.replaceAll("[/\\\\:*<>?|]+","").trim();
    }

    public static Path getPathOfOrganism(String kingdom, String group, String subgroup, String organism){
        return Paths.get(normalizeString(kingdom), normalizeString(group), normalizeString(subgroup), normalizeString(organism));
    }

    public static void createOrganismsTreeStructure(Path rootDirectory){
        try(BufferedReader reader = readRequest(getFullOrganismsListRequestURL())){
            JSONObject json = new JSONObject(reader.lines().collect(Collectors.joining()));
            JSONArray entries = json.getJSONObject("ngout").getJSONObject("data").getJSONArray("content");
            for(Object obj : entries){
                JSONObject entry = (JSONObject)obj;
                Path entryPath = rootDirectory.resolve(getPathOfOrganism(entry.getString("kingdom"), entry.getString("group"), entry.getString("subgroup"), entry.getString("organism")));
                FileUtils.forceMkdir(entryPath.toFile());
            }
        }catch(IOException | NullPointerException e){
            LOGGER.error("Erreur de récupération de la liste des organismes",e);
        }
    }

    /**
     *
     * @param reign règne
     * @return nombre d'entrées du royaumes
     */
    public static int getNumberOfEntries(Reign reign){
        int numerOfEntries = -1;
        try(BufferedReader reader = readRequest(getReignTotalURL(reign))){
            JSONObject json = new JSONObject(reader.lines().collect(Collectors.joining()));
            numerOfEntries = json.getJSONObject("ngout").getJSONObject("data").getInt("totalCount");
        }catch(IOException | NullPointerException e){
            LOGGER.error("Erreur de récupération du nombre total d'entrées du règne '"+ reign.getSearchTable()+"'",e);
        }
        return numerOfEntries;
    }

    public static BufferedReader readRequest(String requestURL) throws IOException {
        return new BufferedReader(new InputStreamReader(new URL(requestURL).openStream()));
    }
}
