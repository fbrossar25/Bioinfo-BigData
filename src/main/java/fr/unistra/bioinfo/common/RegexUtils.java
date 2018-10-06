package fr.unistra.bioinfo.common;

import java.util.regex.Pattern;

public class RegexUtils {
    /**
     * Pattern matchant une chaine type NC_123456.1 où le groupe 1 match la partie NC_123456 et le groupe 2 match le numéro de version (ici 1)
     */
    public static final Pattern REPLICON_PATTERN = Pattern.compile("^.*(NC_[0-9]+)\\.([0-9]+).*$");
}
