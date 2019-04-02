package fr.unistra.bioinfo.common;

import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class EventUtils {
    private static List<EventListener> listeners = new ArrayList<>();

    private EventUtils(){}

    /** Type d'un évenement */
    public enum EventType{
        /** Téléchargement d'un fichier replicon démarré */
        DOWNLOAD_BEGIN,
        /** Téléchargemetn d'un fichier replicon terminé */
        DOWNLOAD_END,
        /** Parsing d'un fichier démarré */
        PARSING_BEGIN,
        /** Parsing d'un fichier terminé */
        PARSING_END,
        /** Génération des statistiques pour un replicon terminée */
        STATS_END,
        /** Téléchargement de toutes les méta-données terminés */
        METADATA_END
    }

    /**
     * Décris un événement avec un type et pour un replicon
     */
    public static class Event{
        /** Type de l'évenement*/
        private EventType type;
        /** Le replicon concerné par l'évenement */
        private RepliconEntity replicon;

        Event(@NonNull EventType type, RepliconEntity replicon){
            this.type = type;
            this.replicon = replicon;
        }

        /** Type de l'évenement. Non null. */
        public EventType getType(){
            return type;
        }

        /** Replicon concerné par l'événement. Peut être null. */
        public RepliconEntity getReplicon(){
            return replicon;
        }
    }

    public interface EventListener{
        void onEvent(Event event);
    }

    /** Envoie un nouvel événement à tous les listener inscrits via la méthode subscribe.
     * @see EventUtils#subscribe(EventListener) */
    public static void sendEvent(EventType type, RepliconEntity replicon){
        Event event = new Event(type, replicon);
        for(EventListener listener : listeners){
            listener.onEvent(event);
        }
    }

    /** Inscrit un listener pour écouter les événements */
    public static void subscribe(EventListener listener){
        listeners.add(listener);
    }

    /** Désinscrit le listener, s'il existe dans la liste des inscrits*/
    public static void unsubscribe(EventListener listener){
        listeners.remove(listener);
    }
}
