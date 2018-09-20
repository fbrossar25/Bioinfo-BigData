package fr.unistra.bioinfo.genbank;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
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
        request += "&q=[display(),hist(group,subgroup,level)].from(GenomeAssemblies).usingschema(/schema/GenomeAssemblies).matching(tab==[\""+ reign.getlabel()+"\"]).sort(replicons,desc)";
        return request;
    }

    /**
     * Retoure une requête permettant de récuperer les compteurs d'un royaumes</br>
     * (nombres de groupes, sous-groupe avec leurs noms et leurs nombres d'entrées respectifs)
     * @param reign règne
     * @return l'URL de la requête
     */
    public static String getKingdomCountersURL(Reign reign){
        return "https://www.ncbi.nlm.nih.gov/Structure/ngram?limit=0&q=[hist(group,subgroup,kingdom)].from(GenomeAssemblies).usingschema(/schema/GenomeAssemblies).matching(tab==[\""
        + reign.getlabel()+"\"]).sort(replicons,desc)";
    }

    public static String getReignTotalURL(Reign reign){
        return "https://www.ncbi.nlm.nih.gov/Structure/ngram?&q=[display()].from(GenomeAssemblies).matching(tab==[\""+ reign.getlabel()+"\"])&limit=1";
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
            LOGGER.error("Erreur de récupération du nombre total d'entrées du règne '"+ reign.getlabel()+"'",e);
        }
        return numerOfEntries;
    }

    public static BufferedReader readRequest(String requestURL) throws IOException {
        return new BufferedReader(new InputStreamReader(new URL(requestURL).openStream()));
    }
}
