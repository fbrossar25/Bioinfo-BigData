package fr.unistra.bioinfo.persistence;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class DBUtils {
    private static SessionFactory sessionFactory;
    private static Session session;
    private static Configuration configuration;

    public static Session getSession(){
        ensureExistsSession();
        return session;
    }

    private static void ensureExistsSession(){
        ensureExistsSessionFactory();
        if(session == null || !session.isOpen()){
            try{
                session = sessionFactory.openSession();
            }catch (HibernateException e){
                System.err.println("Erreur de création de la session");
                e.printStackTrace();
            }
        }
    }

    private static void ensureExistsSessionFactory(){
        if(sessionFactory == null || !sessionFactory.isOpen()){
            try {
                configuration = new Configuration().configure();
                sessionFactory = configuration.buildSessionFactory();
            } catch (Throwable ex) {
                // Log exception!
                throw new ExceptionInInitializerError(ex);
            }
        }
    }

    public static Configuration getConfiguration(){
        return configuration;
    }

    public static void start(){
        stop();
        session = getSession();
    }

    public static void stop(){
        if(session != null && session.isOpen()){
            session.close();
        }

        if(sessionFactory != null && sessionFactory.isOpen()){
            sessionFactory.close();
        }
    }
}
