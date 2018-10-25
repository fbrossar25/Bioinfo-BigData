package fr.unistra.bioinfo.persistence.service.impl;

import fr.unistra.bioinfo.persistence.entity.IEntity;
import fr.unistra.bioinfo.persistence.entity.members.EntityMembers;
import fr.unistra.bioinfo.persistence.manager.IManager;
import fr.unistra.bioinfo.persistence.service.AbstractService;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.SessionFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManagerFactory;
import java.io.Serializable;
import java.util.List;

public abstract class AbstractServiceImpl<T extends IEntity<K>, K extends Serializable> implements AbstractService<T, K> {
    protected EntityManagerFactory entityManagerFactory;
    private Class<T> clazz;
    protected SessionFactory sessionFactory;
    public static final Integer PAGE_SIZE = 50;

    public AbstractServiceImpl(EntityManagerFactory entityManagerFactory, Class<T> clazz){
        this.entityManagerFactory = entityManagerFactory;
        this.clazz = clazz;
        this.sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
    }

    @Override
    @Transactional
    public List<T> getAll() {
        return getManager().getAll();
    }

    @Override
    @Transactional
    public T getById(K id) {
        if(id == null){
            return null;
        }
        return getManager().findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(K id) {
        if(id == null){
            return false;
        }
        return getManager().existsById(id);
    }

    @Override
    @Transactional
    public T save(T entity) {
        return getManager().save(entity);
    }

    @Override
    @Transactional
    public List<T> saveAll(List<T> entities) {
        return getManager().saveAll(entities);
    }

    @Override
    @Transactional
    public void delete(T entity) {
        getManager().delete(entity);
        if(entity != null){
            entity.setId(null);
        }
    }

    @Override
    @Transactional
    public void deleteAll(List<T> entities) {
        getManager().deleteInBatch(entities);
        if(CollectionUtils.isNotEmpty(entities)){
            entities.forEach(e -> e.setId(null));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long count(){
        return getManager().count();
    }

    @Override
    public Page<T> getAll(Pageable p) {
        return getManager().findAll(p);
    }

    @Override
    public Page<T> getAll(Integer pageNumber, Direction sortDirection, EntityMembers... properties) {
        String[] propertiesNames = new String[properties.length];
        for(int i=0; i<properties.length; i++){
            propertiesNames[i] = properties[i].getName();
        }
        return getManager().findAll(PageRequest.of(pageNumber, PAGE_SIZE, sortDirection, propertiesNames));
    }

    public abstract IManager<T, K> getManager();
}
