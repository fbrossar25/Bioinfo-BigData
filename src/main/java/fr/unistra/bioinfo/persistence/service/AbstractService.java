package fr.unistra.bioinfo.persistence.service;

import fr.unistra.bioinfo.persistence.entity.IEntity;
import fr.unistra.bioinfo.persistence.entity.members.EntityMembers;
import fr.unistra.bioinfo.persistence.manager.IManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.util.List;

public interface AbstractService<T extends IEntity<K>, K extends Serializable> {
    List<T> getAll();
    T getById(K id);
    boolean existsById(K id);
    T save(T entity);
    List<T> saveAll(List<T> entities);
    void delete(T entity);
    void deleteAll(List<T> entities);
    IManager<T, K> getManager();
    Long count();
    Page<T> getAll(Pageable p);

    /**
     * Retourne un resultat paginé
     * @param page numéro de la page (commence à 0)
     * @param sortDirection direction du tri
     * @param properties propriétés à trier
     * @return
     */
    Page<T> getAll(@NonNull Integer page, @NonNull Direction sortDirection, @NonNull EntityMembers<T>... properties);
}
