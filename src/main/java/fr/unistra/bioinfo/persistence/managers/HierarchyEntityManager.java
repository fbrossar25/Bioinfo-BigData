package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.QueryUtils;
import fr.unistra.bioinfo.persistence.entities.HierarchyEntity;
import fr.unistra.bioinfo.persistence.properties.RepliconProperties;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class HierarchyEntityManager extends AbstractEntityManager<Long, HierarchyEntity> {

    public HierarchyEntityManager(){
        super(HierarchyEntity.class);
    }

    @Override
    public void delete(HierarchyEntity entity) {
        PersistentEntityManagerFactory.getRepliconManager().delete(QueryUtils.equals(RepliconProperties.HIERARCHY, entity.getId()));
        super.delete(entity);
    }


    @Override
    public int delete(String where){
        List<Long> ids = DBUtils.getSession().createQuery("select id "+FROM_TABLE+" where "+where, Long.class).getResultList();
        if(CollectionUtils.isNotEmpty(ids)){
            PersistentEntityManagerFactory.getRepliconManager().delete(QueryUtils.in(RepliconProperties.HIERARCHY, ids));
        }
        return super.delete(where);
    }
}
