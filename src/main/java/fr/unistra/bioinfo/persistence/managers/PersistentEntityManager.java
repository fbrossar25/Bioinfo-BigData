package fr.unistra.bioinfo.persistence.managers;

import com.sun.istack.internal.NotNull;
import fr.unistra.bioinfo.genbank.GenbankUtils;
import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.entities.PersistentEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import javax.persistence.Table;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistentEntityManager<K extends Serializable,T extends PersistentEntity<K>> {
    private Class<T> clazz;
    private static final Map<Class, PersistentEntityManager> SINGLETONS = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger();

    public PersistentEntityManager(@NotNull Class<T> clazz){
        this.clazz = clazz;
    }

    public BigInteger count(){
        Query query = DBUtils.getSession().createNativeQuery("select count(*) from "+clazz.getAnnotation(Table.class).name());
        return (BigInteger)query.getSingleResult();
    }

    public void save(T entity){
        if(entity == null){
            return;
        }
        Session s = DBUtils.getSession();
        Transaction t = s.beginTransaction();
        s.save(entity);
        t.commit();
    }

    public void delete(T entity){
        if(entity == null){
            return;
        }
        Session s = DBUtils.getSession();
        Transaction t = s.beginTransaction();
        s.delete(entity);
        t.commit();
    }

    public int delete(CriteriaDelete<T> query){
        Session s = DBUtils.getSession();
        Transaction t = s.beginTransaction();
        int deleted = s.createQuery(query).executeUpdate();
        t.commit();
        return deleted;
    }

    public List<T> getAll(){
        Session s = DBUtils.getSession();
        CriteriaQuery<T> cq = s.getCriteriaBuilder().createQuery(clazz);
        cq.from(clazz);
        return s.createQuery(cq).getResultList();
    }

    public boolean idExists(@NotNull K key){
        return DBUtils.getSession().get(clazz, key) != null;
    }

    public int deleteAll(){
        Session s = DBUtils.getSession();
        CriteriaDelete<T> cq = s.getCriteriaBuilder().createCriteriaDelete(clazz);
        cq.from(clazz);
        Transaction t = s.beginTransaction();
        int deleted = s.createQuery(cq).executeUpdate();
        t.commit();
        return deleted;
    }

    @SuppressWarnings("unchecked")
    public static <K extends Serializable,T extends PersistentEntity<K>> PersistentEntityManager<K,T> create(Class<T> clazz){
        if(SINGLETONS.containsKey(clazz)){
            return SINGLETONS.get(clazz);
        }
        SINGLETONS.put(clazz, new PersistentEntityManager<>(clazz));
        return SINGLETONS.get(clazz);
    }

    public void save(List<T> entities) {
        if(entities == null || entities.isEmpty()){
            return;
        }
        Session s = DBUtils.getSession();
        Transaction t = s.beginTransaction();
        int i = 0;
        for(T entity : entities){
            if(!idExists(entity.getId())){
                s.save(entity);
                if(i % GenbankUtils.BATCH_INSERT_SIZE == 0){
                    s.flush();
                    s.clear();
                }
                i++;
            }else{
                LOGGER.warn("Id '"+entity.getId()+"' deja existant dans la table '"+clazz.getSimpleName()+"'");
            }
        }
        t.commit();
    }
}
