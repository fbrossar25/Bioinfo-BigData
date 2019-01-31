package fr.unistra.bioinfo.gui.tree;

import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.springframework.lang.NonNull;

final class RepliconViewNodeFactory {

    static RepliconViewNode createRootNode(){
        RepliconViewNode root = new RepliconViewNode();
        root.setDisplayValue("Racine");
        root.setType(RepliconViewNode.RepliconViewNodeType.ROOT);
        return root;
    }

    static RepliconViewNode createRepliconNode(@NonNull RepliconEntity replicon){
        RepliconViewNode node = new RepliconViewNode();
        node.setReplicon(replicon);
        return node;
    }

    static RepliconViewNode createKingdomNode(@NonNull String kingdom){
        RepliconViewNode node = new RepliconViewNode();
        node.setDisplayValue(kingdom);
        node.setType(RepliconViewNode.RepliconViewNodeType.KINGDOM);
        return node;
    }

    static RepliconViewNode createGroupNode(@NonNull String group){
        RepliconViewNode node = new RepliconViewNode();
        node.setDisplayValue(group);
        node.setType(RepliconViewNode.RepliconViewNodeType.GROUP);
        return node;
    }

    static RepliconViewNode createSubgroupNode(@NonNull String subgroup){
        RepliconViewNode node = new RepliconViewNode();
        node.setDisplayValue(subgroup);
        node.setType(RepliconViewNode.RepliconViewNodeType.SUBGROUP);
        return node;
    }

    static RepliconViewNode createOrganismNode(@NonNull String organism){
        RepliconViewNode node = new RepliconViewNode();
        node.setDisplayValue(organism);
        node.setType(RepliconViewNode.RepliconViewNodeType.ORGANISM);
        return node;
    }
}
