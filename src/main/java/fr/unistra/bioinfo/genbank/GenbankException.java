package fr.unistra.bioinfo.genbank;

public class GenbankException extends RuntimeException {
    public GenbankException(String message, Throwable e){
        super(message, e);
    }
}
