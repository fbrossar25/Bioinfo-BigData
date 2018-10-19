package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.persistence.manager.IManager;

import java.io.Serializable;
import java.util.List;

public interface AbstractService<T, K extends Serializable> {
    List<T> getAll();
    T getById(K id);
    boolean existsById(K id);
    T save(T entity);
    List<T> saveAll(List<T> entities);
    void delete(T entity);
    void deleteAll(List<T> entities);
    IManager<T, K> getManager();
    Long count();
}
