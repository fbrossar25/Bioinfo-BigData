package fr.unistra.bioinfo.persistence.service.impl;

import fr.unistra.bioinfo.persistence.entity.IEntity;
import fr.unistra.bioinfo.persistence.entity.members.EntityMembers;
import fr.unistra.bioinfo.persistence.manager.IManager;
import fr.unistra.bioinfo.persistence.service.AbstractService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

public abstract class AbstractServiceImpl<T extends IEntity<K>, K extends Serializable> implements AbstractService<T, K> {
    private static final Integer PAGE_SIZE = 50;

    AbstractServiceImpl(){ }

    @Override
    @Transactional
    public List<T> getAll() {
        return getManager().getAll();
    }

    @Override
    @Transactional
    public T getById(@NonNull K id) {
        return getManager().findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(@NonNull K id) {
        return getManager().existsById(id);
    }

    @Override
    @Transactional
    public T save(@NonNull T entity) {
        return getManager().save(entity);
    }

    @Override
    @Transactional
    public List<T> saveAll(@NonNull List<T> entities) {
        return getManager().saveAll(entities);
    }

    @Override
    @Transactional
    public void delete(@NonNull T entity) {
        getManager().delete(entity);
        entity.setId(null);
    }

    @Override
    @Transactional
    public void deleteAll(@NonNull List<T> entities) {
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
    public Page<T> getAll(@NonNull Pageable p) {
        return getManager().findAll(p);
    }

    @Override
    public Page<T> getAll(int pageNumber, @NonNull Direction sortDirection, @NonNull EntityMembers... properties) {
        String[] propertiesNames = new String[properties.length];
        for(int i=0; i<properties.length; i++){
            propertiesNames[i] = properties[i].getName();
        }
        return getManager().findAll(PageRequest.of(pageNumber, PAGE_SIZE, sortDirection, propertiesNames));
    }

    @Override
    public void deleteAll() {
        getManager().deleteAll();
    }

    public abstract IManager<T, K> getManager();
}
