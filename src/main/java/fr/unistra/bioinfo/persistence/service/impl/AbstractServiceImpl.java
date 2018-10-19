package fr.unistra.bioinfo.persistence.service.impl;

import fr.unistra.bioinfo.persistence.entity.IEntity;
import fr.unistra.bioinfo.persistence.manager.IManager;
import fr.unistra.bioinfo.persistence.service.AbstractService;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.EntityManagerFactory;
import java.io.Serializable;
import java.util.List;

public abstract class AbstractServiceImpl<T extends IEntity<K>, K extends Serializable> implements AbstractService<T, K> {
    protected EntityManagerFactory entityManagerFactory;
    protected static final Integer BATCH_SIZE = 1000;
    private Class<T> clazz;
    protected SessionFactory sessionFactory;

    public AbstractServiceImpl(EntityManagerFactory entityManagerFactory, Class<T> clazz){
        this.entityManagerFactory = entityManagerFactory;
        this.clazz = clazz;
        this.sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    }

    @Override
    public List<T> getAll() {
        return getManager().getAll();
    }

    @Override
    public T getById(K id) {
        return getManager().findById(id).orElse(null);
    }

    @Override
    public boolean existsById(K id) {
        return getManager().existsById(id);
    }

    @Override
    public T save(T entity) {
        return getManager().save(entity);
    }

    @Override
    public List<T> saveAll(List<T> entities) {
        return getManager().saveAll(entities);
    }

    @Override
    public void delete(T entity) {
        getManager().delete(entity);
        if(entity != null){
            entity.setId(null);
        }
    }

    @Override
    public void deleteAll(List<T> entities) {
        getManager().deleteInBatch(entities);
        if(CollectionUtils.isNotEmpty(entities)){
            entities.forEach(e -> e.setId(null));
        }
    }

    @Override
    public Long count(){
        return getManager().count();
    }

    public abstract IManager<T, K> getManager();
}
