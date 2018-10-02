package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.entities.AbstractEntity;
import fr.unistra.bioinfo.persistence.entities.Hierarchy;
import fr.unistra.bioinfo.persistence.entities.Replicon;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class AbstractEntityManager<K extends Serializable,T extends AbstractEntity<K>> {
    /** Taille pour les traitements par lots */
    public static final long BATCH_SIZE = 1000;

    private static final Logger LOGGER = LogManager.getLogger();

    /** Classe de l'entité */
    protected Class<T> clazz;

    protected AbstractEntityManager(Class<T> clazz){
        this.clazz = clazz;
    }

    public T getById(K id){
        return DBUtils.getSession().get(clazz, id);
    }

    public Long count(){
        Session s = DBUtils.getSession();
        CriteriaBuilder qb = s.getCriteriaBuilder();
        CriteriaQuery<Long> cq = qb.createQuery(Long.class);
        cq.select(qb.count(cq.from(clazz)));
        return s.createQuery(cq).getSingleResult();
    }

    public void save(T entity){
        Session s = DBUtils.getSession();
        Transaction t = s.beginTransaction();
        s.save(entity);
        t.commit();
    }

    public void delete(T entity){
        Session s = DBUtils.getSession();
        Transaction t = s.beginTransaction();
        s.delete(entity);
        t.commit();
    }

    private void handleBatchFlush(int count, Session s){
        if(count % BATCH_SIZE == 0){
            s.flush();
            s.clear();
        }
    }

    public void delete(Collection<T> entities){
        Session s = DBUtils.getSession();
        Transaction t = s.beginTransaction();
        int i = 0;
        for(T entity : entities){
            s.delete(entity);
            handleBatchFlush(i, s);
            i++;
        }
        t.commit();
    }

    public int delete(String where){
        if(StringUtils.isBlank(where)){
            return 0;
        }
        Session s = DBUtils.getSession();
        Transaction t = s.beginTransaction();
        int n = s.createQuery("delete from "+getTableName()+" where "+where).executeUpdate();
        t.commit();
        return n;
    }

    /**
     * Retourne la table entière
     * @return entités de la table
     */
    public List<T> getAll(){
        return DBUtils.getSession()
                .createQuery("from "+getTableName(),clazz)
                .getResultList();
    }

    /**
     * Retourne la page donnée de la taille donnée.
     * @return entités de la page
     */
    public List<T> getAll(int pagesize, int page){
        return DBUtils.getSession()
                .createQuery("from "+getTableName(),clazz)
                .getResultList();
    }

    /**
     * @param id l'id à rechercher
     * @return true si l'id existe dans la table
     */
    public boolean idExists(K id){
        return CollectionUtils.isNotEmpty(DBUtils.getSession()
                .createQuery("select * from "+getTableName()+" where id = :id", clazz)
                .setParameter("id", id)
                .getResultList());
    }

    /**
     * Vide la table
     * @return nombre de lignes supprimées
     */
    public int deleteAll(){
        Session s = DBUtils.getSession();
        Transaction t = s.beginTransaction();
        int deleted = s.createQuery("delete from "+getTableName()).executeUpdate();
        t.commit();
        return deleted;
    }

    /**
     * Sauvegarde avec traitement par lot
     * @param entities entités à sauvegarder
     */
    public void save(Collection<T> entities) {
        if(entities == null || entities.isEmpty()){
            return;
        }
        Session s = DBUtils.getSession();
        Transaction t = s.beginTransaction();
        int i = 0;
        for(T entity : entities){
            s.saveOrUpdate(entity);
            handleBatchFlush(i, s);
            i++;
        }
        t.commit();
    }

    public String getTableName(){
        return clazz.getSimpleName();
    }
}
