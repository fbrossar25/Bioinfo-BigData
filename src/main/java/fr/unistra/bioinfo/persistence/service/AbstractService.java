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
    T getById(@NonNull K id);
    boolean existsById(@NonNull K id);
    T save(@NonNull T entity);
    List<T> saveAll(@NonNull List<T> entities);
    void delete(@NonNull T entity);
    void deleteAll(@NonNull List<T> entities);
    /** Supprime toutes les entités */
    void deleteAll();
    IManager<T, K> getManager();
    Long count();
    Page<T> getAll(@NonNull Pageable p);

    /**
     * Retourne un resultat paginé
     * @param page numéro de la page (commence à 0)
     * @param sortDirection direction du tri
     * @param properties propriétés à trier
     * @return
     */
    Page<T> getAll(int page, @NonNull Direction sortDirection, @NonNull EntityMembers<T>... properties);
}
