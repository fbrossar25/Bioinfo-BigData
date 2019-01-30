package fr.unistra.bioinfo.genbank;

public class TooMuchGenbankRequestsException extends RuntimeException {
    public TooMuchGenbankRequestsException(String message){
        super(message);
    }

    public TooMuchGenbankRequestsException(){
        this("Nombre requêtes par secondes sur Genbank dépassées");
    }
}
