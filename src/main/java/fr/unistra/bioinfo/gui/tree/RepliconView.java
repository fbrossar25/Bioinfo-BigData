package fr.unistra.bioinfo.gui.tree;

import fr.unistra.bioinfo.gui.MainWindowController;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.util.Comparator;
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
        }

        private void updateNodeState(TreeItem<RepliconViewNode> node){
            TreeItem<RepliconViewNode> parent = node;
            do{
                RepliconViewNode.RepliconViewNodeState nextState = RepliconViewNode.RepliconViewNodeState.OK;
                for(TreeItem<RepliconViewNode> child : parent.getChildren()){
                    if(child.getValue().getState() == RepliconViewNode.RepliconViewNodeState.INTERMEDIARY){
                        nextState = RepliconViewNode.RepliconViewNodeState.INTERMEDIARY;
                    }else if(child.getValue().getState() == RepliconViewNode.RepliconViewNodeState.NOK){
                        nextState = RepliconViewNode.RepliconViewNodeState.INTERMEDIARY;
                        break;
                    }
                }
                parent.getValue().setState(nextState);
                ImageView iv1 = new ImageView();
                iv1.setImage(parent.getValue().getState().getImage());
                parent.setGraphic(iv1);
            }while((parent = parent.getParent()) != null);
        }

        @Override
        public void run() {
            if(this.son.getValue().getType() == RepliconViewNode.RepliconViewNodeType.REPLICON){
                RepliconEntity r = this.son.getValue().getReplicon();
                if(r.isComputed()){
                    this.son.getValue().setState(RepliconViewNode.RepliconViewNodeState.OK);
                }else if(!r.isParsed()){
                    this.son.getValue().setState(RepliconViewNode.RepliconViewNodeState.NOK);
                }else{
                    this.son.getValue().setState(RepliconViewNode.RepliconViewNodeState.INTERMEDIARY);
                }
            }
            updateNodeState(this.father);
            ImageView iv1 = new ImageView();
            iv1.setImage(this.son.getValue().getState().getImage());
            this.son.setGraphic(iv1);
            father.getChildren().add(son);
            father.getChildren().sort(Comparator.comparing(n->n.getValue().getDisplayValue()));
        }
    }

    /**
     * Créer un arbre de visualisation des replicons, avec un noeud racine.
     */
    public RepliconView(){
        super();
        setRoot(RepliconViewNodeFactory.createRootNode().getNode());
        setShowRoot(false);
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
        node = kingdomView.getNode();
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
        TreeItem<RepliconViewNode> node = getGroupNode(kingdom, group);
        if(node != null){
            return node;
        }
        TreeItem<RepliconViewNode> kingdomItem = addKingdomNode(kingdom);
        RepliconViewNode groupView = RepliconViewNodeFactory.createGroupNode(group);
        node = groupView.getNode();
        Platform.runLater(new NonBlockingAddNode(node, kingdomItem));
        groups.put(kingdom + "-" + group, node);
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
        TreeItem<RepliconViewNode> node = getSubgroupNode(kingdom, group, subgroup);
        if(node != null){
            return node;
        }
        TreeItem<RepliconViewNode> groupItem = addGroupNode(kingdom, group);
        RepliconViewNode subgroupView = RepliconViewNodeFactory.createSubgroupNode(subgroup);
        node = subgroupView.getNode();
        Platform.runLater(new NonBlockingAddNode(node, groupItem));
        subgroups.put(kingdom + "-" + group + "-" + subgroup, node);
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
        node = organismView.getNode();
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
        node = repliconView.getNode();
        Platform.runLater(new NonBlockingAddNode(node, organismItem));
        replicons.put(replicon.getName(), node);
        return node;
    }

    /**
     *
     * @param kingdom Le nom du royaume
     * @return L'item du royaume correspondant ou null
     */
    public TreeItem<RepliconViewNode> getKingdomNode(String kingdom){
        return kingdoms.get(kingdom);
    }

    /**
     *
     * @param group Le nom du groupe
     * @return L'item du groupe correspondant ou null
     */
    public TreeItem<RepliconViewNode> getGroupNode(String kingdom, String group){
        return groups.get(kingdom+"-"+group);
    }

    /**
     *
     * @param subgroup Le nom du sous-groupe
     * @return L'item du sous-groupe correspondant ou null
     */
    public TreeItem<RepliconViewNode> getSubgroupNode(String kingdom, String group, String subgroup){
        return subgroups.get(kingdom+"-"+group+"-"+subgroup);
    }

    /**
     *
     * @param organism nom de l'organisme
     * @return L'item de l'organisme correspondant ou null
     */
    public TreeItem<RepliconViewNode> getOrganismNode(String organism){
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
