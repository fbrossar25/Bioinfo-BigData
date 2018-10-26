package fr.unistra.bioinfo.persistence.manager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

@NoRepositoryBean
public interface IManager<T, K extends Serializable> extends JpaRepository<T, K> {
    List<T>  getAll();
}
