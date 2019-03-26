package fr.unistra.bioinfo.gui.tree;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Arbre de visualisation des replicons
 * TODO : Lazy loading pour chaque branches
 */
public class RepliconView extends TreeView<RepliconViewNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepliconView.class);

    private Map<String, TreeItem<RepliconViewNode>> kingdoms = new HashMap<>();
    private Map<String, TreeItem<RepliconViewNode>> groups = new HashMap<>();
    private Map<String, TreeItem<RepliconViewNode>> subgroups = new HashMap<>();
    private Map<String, TreeItem<RepliconViewNode>> organisms = new HashMap<>();
    private Map<String, TreeItem<RepliconViewNode>> replicons = new HashMap<>();

    private class NonBlockingAddNode implements Runnable{
        private final TreeItem<RepliconViewNode> son;
        private final TreeItem<RepliconViewNode> father;
        NonBlockingAddNode(TreeItem<RepliconViewNode> son, TreeItem<RepliconViewNode> father){
            this.son = son;
            this.father = father;
            Image i1 = new Image(this.son.getValue().getState().getValue());
            ImageView iv1 = new ImageView();
            iv1.setImage(i1);
            this.son.setGraphic(iv1);
        }

        @Override
        public void run() {
            father.getChildren().add(son);
        }
    }

    /**
     * Créer un arbre de visualisation des replicons, avec un noeud racine.
     */
    public RepliconView(){
        super();
        setRoot(new TreeItem<>(RepliconViewNodeFactory.createRootNode()));
        getRoot().setExpanded(true);
    }

    /**
     * Créé l'item royaume s'il n'existe pas, ou retourne l'item du royaume du même nom.
     * @param kingdom Le nom du royaume
     * @return L'item du royaume ou null
     */
    private synchronized TreeItem<RepliconViewNode> addKingdomNode(String kingdom){
        TreeItem<RepliconViewNode> node = getKingdomNode(kingdom);
        if(node != null){
            return node;
        }
        RepliconViewNode kingdomView = RepliconViewNodeFactory.createKingdomNode(kingdom);
        node = new TreeItem<>(kingdomView);
        Platform.runLater(new NonBlockingAddNode(node, getRoot()));
        kingdoms.put(kingdom, node);
        return node;
    }

    /**
     * Créé l'item groupe s'il n'existe pas, ou retourne l'item du groupe du même nom.
     * @param kingdom Le nom du royaume
     * @param group Le nom du groupe
     * @return L'item du groupe ou null
     */
    private synchronized TreeItem<RepliconViewNode> addGroupNode(String kingdom, String group){
        TreeItem<RepliconViewNode> node = getGroupNode(group);
        if(node != null){
            return node;
        }
        TreeItem<RepliconViewNode> kingdomItem = addKingdomNode(kingdom);
        RepliconViewNode groupView = RepliconViewNodeFactory.createGroupNode(group);
        node = new TreeItem<>(groupView);
        Platform.runLater(new NonBlockingAddNode(node, kingdomItem));
        groups.put(group, node);
        return node;
    }

    /**
     * Créé l'item sous-groupe s'il n'existe pas, ou retourne l'item du sous-groupe du même nom.
     * @param kingdom Le nom du royaume
     * @param group Le nom du groupe
     * @param subgroup Le nom du sous-groupe
     * @return L'item du sous-groupe ou null
     */
    private synchronized TreeItem<RepliconViewNode> addSubgroupNode(String kingdom, String group, String subgroup){
        TreeItem<RepliconViewNode> node = getSubgroupNode(subgroup);
        if(node != null){
            return node;
        }
        TreeItem<RepliconViewNode> groupItem = addGroupNode(kingdom, group);
        RepliconViewNode subgroupView = RepliconViewNodeFactory.createSubgroupNode(subgroup);
        node = new TreeItem<>(subgroupView);
        Platform.runLater(new NonBlockingAddNode(node, groupItem));
        subgroups.put(subgroup, node);
        return node;
    }

    /**
     * Créé l'item organisme s'il n'existe pas, ou retourne l'item d'organisme du même nom.
     * @param kingdom Le nom du royaume
     * @param group Le nom du groupe
     * @param subgroup Le nom du sous-groupe
     * @param organism Le nom de l'organisme
     * @return L'item de l'organisme créé, ou l'existant
     */
    private synchronized TreeItem<RepliconViewNode> addOrganismNode(String kingdom, String group, String subgroup, String organism){
        TreeItem<RepliconViewNode> node = getOrganismNode(organism);
        if(node != null){
            return node;
        }
        TreeItem<RepliconViewNode> subgroupItem = addSubgroupNode(kingdom, group, subgroup);
        RepliconViewNode organismView = RepliconViewNodeFactory.createOrganismNode(organism);
        node = new TreeItem<>(organismView);
        Platform.runLater(new NonBlockingAddNode(node, subgroupItem));
        organisms.put(organism, node);
        return node;
    }

    /**
     * Ajoute un replicon à l'arbre des replicon, ou retourne le replicon de même nom existant.</br>
     * Si le replicon n'as pas de hierarchy, ne créé pas d'item et retourne null.
     * @param replicon Le replicon
     * @return L'item du replicon créé ou existant. Retourne null si le replicon n'as pas de hierarchy
     */
    public synchronized TreeItem<RepliconViewNode> addReplicon(@NonNull RepliconEntity replicon){
        TreeItem<RepliconViewNode> node = getRepliconNode(replicon);
        if(node != null){
            return node;
        }
        HierarchyEntity hierarchy = replicon.getHierarchyEntity();
        if(hierarchy == null){
            LOGGER.error("Le replicon '{}' n'as pas de hierarchy", replicon);
            return null;
        }
        TreeItem<RepliconViewNode> organismItem = addOrganismNode(hierarchy.getKingdom(), hierarchy.getGroup(), hierarchy.getSubgroup(), hierarchy.getOrganism());
        RepliconViewNode repliconView = RepliconViewNodeFactory.createRepliconNode(replicon);
        node = new TreeItem<>(repliconView);
        Platform.runLater(new NonBlockingAddNode(node, organismItem));
        replicons.put(replicon.getName(), node);
        return node;
    }

    /**
     *
     * @param kingdom Le nom du royaume
     * @return L'item du royaume correspondant ou null
     */
    private TreeItem<RepliconViewNode> getKingdomNode(String kingdom){
        return kingdoms.get(kingdom);
    }

    /**
     *
     * @param group Le nom du groupe
     * @return L'item du groupe correspondant ou null
     */
    private TreeItem<RepliconViewNode> getGroupNode(String group){
        return groups.get(group);
    }

    /**
     *
     * @param subgroup Le nom du sous-groupe
     * @return L'item du sous-groupe correspondant ou null
     */
    private TreeItem<RepliconViewNode> getSubgroupNode(String subgroup){
        return subgroups.get(subgroup);
    }

    /**
     *
     * @param organism nom de l'organisme
     * @return L'item de l'organisme correspondant ou null
     */
    private TreeItem<RepliconViewNode> getOrganismNode(String organism){
        return organisms.get(organism);
    }

    /**
     *
     * @param replicon Le nom du replicon (ex : NC_012345)
     * @return Le noeud du replicon, ou null s'il n'existe pas
     */
    public TreeItem<RepliconViewNode> getRepliconNode(RepliconEntity replicon){
        return replicons.get(replicon.getName());
    }

    /**
     * Supprime tous les noeuds est replicons de la vues sauf le noeud racine
     */
    public void clear(){
        //Suppression des liens parents/enfant
        organisms.values().forEach(o -> o.getChildren().clear());
        subgroups.values().forEach(s -> s.getChildren().clear());
        groups.values().forEach(g -> g.getChildren().clear());
        kingdoms.values().forEach(k -> k.getChildren().clear());
        getRoot().getChildren().clear();

        //Suppression des lien maps / noeuds
        kingdoms.clear();
        groups.clear();
        subgroups.clear();
        organisms.clear();
        replicons.clear();
    }
}
