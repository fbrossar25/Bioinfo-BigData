package fr.unistra.bioinfo.persistence.managers;

import fr.unistra.bioinfo.persistence.DBUtils;
import fr.unistra.bioinfo.persistence.entities.Replicon;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class RepliconManager extends AbstractEntityManager<Long, Replicon> {

    public RepliconManager(){
        super(Replicon.class);
    }

    @Override
    public void save(Replicon entity) {
        Session s = DBUtils.getSession();
        Transaction t = s.beginTransaction();
        if(entity.getHierarchy() != null){
            s.save(entity.getHierarchy());
        }
        s.save(entity);
        t.commit();
    }
}
