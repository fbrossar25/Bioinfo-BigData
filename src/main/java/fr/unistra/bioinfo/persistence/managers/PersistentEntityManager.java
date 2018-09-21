package fr.unistra.bioinfo.persistence.managers;

import com.sun.istack.internal.NotNull;
import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.entities.PersistentEntity;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

public class PersistentEntityManager<T extends PersistentEntity> {
    private Class<T> clazz;

    public PersistentEntityManager(@NotNull Class<T> clazz){
        this.clazz = clazz;
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

    public List<T> getAll(){
        Session s = DBUtils.getSession();
        CriteriaQuery<T> cq = s.getCriteriaBuilder().createQuery(clazz);
        cq.from(clazz);
        return s.createQuery(cq).getResultList();
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

    public static <T extends PersistentEntity> PersistentEntityManager<T> create(Class<T> clazz){
        return new PersistentEntityManager<>(clazz);
    }
}
