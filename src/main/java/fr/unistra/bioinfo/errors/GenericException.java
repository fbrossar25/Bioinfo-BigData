package fr.unistra.bioinfo.errors;

public class GenericException extends Exception{
    public GenericException(String message){
        super(message);
    }

    public GenericException(Throwable t){
        super(t);
    }
}
