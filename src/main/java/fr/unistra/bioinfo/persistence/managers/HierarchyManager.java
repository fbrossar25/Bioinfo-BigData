package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.QueryUtils;
import fr.unistra.bioinfo.persistence.entities.Hierarchy;
import fr.unistra.bioinfo.persistence.entities.Replicon;
import fr.unistra.bioinfo.persistence.properties.RepliconProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import java.util.List;

public class HierarchyManager extends AbstractEntityManager<Long, Hierarchy> {

    private static final Logger LOGGER  = LogManager.getLogger();

    public HierarchyManager(){
        super(Hierarchy.class);
    }

    @Override
    public void delete(Hierarchy entity) {
        Session s = DBUtils.getSession();
        //Suppression des replicons liés à l'entité
        List<Replicon> replicons = getReplicons(entity);
        PersistentEntityManagerFactory.getRepliconManager().delete(replicons);
        LOGGER.debug(replicons.size() + " replicons liés à la hierarchy d'id '"+entity.getId()+"' supprimés");
        super.delete(entity);
    }


    @Override
    public int delete(String where){
        List<Long> ids = DBUtils.getSession()
                .createQuery("select id from Hierarchy where "+where, Long.class).getResultList();
        if(CollectionUtils.isNotEmpty(ids)){
            PersistentEntityManagerFactory.getRepliconManager().delete(QueryUtils.in(RepliconProperties.HIERARCHY, ids));
        }
        return super.delete(where);
    }

    public List<Replicon> getReplicons(Hierarchy h1) {
        Session session = DBUtils.getSession();
        return session
                .createQuery("from Replicon where hierarchy.id = :id", Replicon.class)
                .setParameter("id", h1 == null ? null : h1.getId())
                .getResultList();
    }
}
