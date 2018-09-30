package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.entities.AbstractEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public abstract class AbstractEntityManager<K extends Serializable,T extends AbstractEntity<K>> {
    /** Taille pour les traitements par lots */
    public static final long BATCH_SIZE = 1000;

    private static final Logger LOGGER = LogManager.getLogger();

    /** Classe de l'entité */
    protected Class<T> clazz;
    /** Nom de la table */
    protected String TABLE_NAME;
    /** Nom utilisé par Hibernate */
    protected String HIBERNATE_NAME;
    /** Nom de la colonne servant d'id */
    protected String ID_COLUMN;
    /** Clause de type ' from TABLE_NAME ' */
    protected String FROM_TABLE;

    protected AbstractEntityManager(Class<T> clazz){
        this.clazz = clazz;
        TABLE_NAME = clazz.getAnnotation(Table.class).name();
        HIBERNATE_NAME = clazz.getSimpleName();
        try {
            ID_COLUMN = clazz.getMethod("getId").getAnnotation(Column.class).name();
        } catch (NoSuchMethodException e) {
            LOGGER.warn("pas de méthode getId() pour la classe "+clazz.getSimpleName()+" : le nom de la colonne id seras supposé être ID");
            ID_COLUMN = "ID";
        }
        FROM_TABLE = " from "+HIBERNATE_NAME+" ";
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
        int n = s.createQuery("delete "+FROM_TABLE+" where "+where).executeUpdate();
        t.commit();
        return n;
    }

    /**
     * Retourne la table entière
     * @return entités de la table
     */
    public List<T> getAll(){
        return DBUtils.getSession()
                .createQuery(FROM_TABLE,clazz)
                .getResultList();
    }

    /**
     * Retourne la page donnée de la taille donnée.
     * @return entités de la page
     */
    public List<T> getAll(int pagesize, int page){
        return DBUtils.getSession()
                .createQuery(FROM_TABLE,clazz)
                .getResultList();
    }

    /**
     * @param id l'id à rechercher
     * @return true si l'id existe dans la table
     */
    public boolean idExists(K id){
        return CollectionUtils.isNotEmpty(DBUtils.getSession()
                .createQuery("select :idcolumn "+FROM_TABLE+" where :idcolumn = :id", clazz)
                .setParameter("idcolumn", ID_COLUMN)
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
        int deleted = s.createQuery("delete "+FROM_TABLE).executeUpdate();
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

    public String getIdColumn(){
        return ID_COLUMN;
    }

    public String getTableName(){
        return TABLE_NAME;
    }

    public String getHibernateName(){
        return HIBERNATE_NAME;
    }
}
