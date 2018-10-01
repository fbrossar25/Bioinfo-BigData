package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.QueryUtils;
import fr.unistra.bioinfo.persistence.entities.HierarchyEntity;
import fr.unistra.bioinfo.persistence.properties.RepliconProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class HierarchyEntityManager extends AbstractEntityManager<Long, HierarchyEntity> {

    private static final Logger LOGGER  = LogManager.getLogger();

    public HierarchyEntityManager(){
        super(HierarchyEntity.class);
    }

    @Override
    public void delete(HierarchyEntity entity) {
        Session s = DBUtils.getSession();
        //Suppression des replicons liés à l'entité
        String query = "delete from "
                        + PersistentEntityManagerFactory.getRepliconManager().getHibernateName()
                        + " where " + QueryUtils.equals(RepliconProperties.HIERARCHY, entity.getId());
        Transaction t = s.beginTransaction();
        int n = s.createQuery(query).executeUpdate();
        LOGGER.debug(n + " replicons liés à la hierarchy d'id '"+entity.getId()+"' supprimés");
        s.delete(entity);
        t.commit();
    }


    @Override
    public int delete(String where){
        List<Long> ids = DBUtils.getSession()
                .createQuery("select id "+FROM_TABLE+" where "+where, Long.class).getResultList();
        if(CollectionUtils.isNotEmpty(ids)){
            PersistentEntityManagerFactory.getRepliconManager().delete(QueryUtils.in(RepliconProperties.HIERARCHY, ids));
        }
        return super.delete(where);
    }
}
