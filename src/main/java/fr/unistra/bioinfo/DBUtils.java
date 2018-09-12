package fr.unistra.bioinfo;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class DBUtils {
    private static SessionFactory sessionFactory;
    private static Session session;

    public static Session getSession(){
        ensureExistsSessionFactory();
        if(session != null && session.isOpen()){
            return session;
        }
        try{
            session = sessionFactory.openSession();
        }catch (HibernateException e){
            System.err.println("Erreur de création de la session");
            e.printStackTrace();
        }
        return session;
    }

    private static void ensureExistsSessionFactory(){
        if(sessionFactory == null || !sessionFactory.isOpen()){
            try {
                sessionFactory = new Configuration().configure().buildSessionFactory();
            } catch (Throwable ex) {
                // Log exception!
                throw new ExceptionInInitializerError(ex);
            }
        }
    }

    public static void close(){
        if(session != null && session.isOpen()){
            session.close();
        }

        if(sessionFactory != null && sessionFactory.isOpen()){
            sessionFactory.close();
        }
    }
}
