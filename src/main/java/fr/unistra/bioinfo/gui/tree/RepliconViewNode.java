package fr.unistra.bioinfo.gui.tree;

import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import javafx.scene.image.Image;

/**
 * Vue d'un replicon dans un RepliconView
 * @see RepliconView
 */
public class RepliconViewNode {
    /**
     * Énumeration des types d'item
     */
    enum RepliconViewNodeType{
        ROOT, REPLICON, KINGDOM, GROUP, SUBGROUP, ORGANISM, UNKNOWN
    }

    /**
     * Enumeration des états d'un item
     */

    enum RepliconViewNodeState{
        OK("images/vert.png"),
        NOK("images/rouge.png"),
        INTERMEDIARY("images/jaune.png");

        private String value;
        private Image img;

        RepliconViewNodeState(String value){
            this.value = value;
            this.img = new Image(value);
        }

        public String getValue(){
            return value;
        }
        public Image getImage(){
            return img;
        }

    }

    /** Élément affiché par ce noeud */
    private RepliconViewNodeType type = RepliconViewNodeType.UNKNOWN;
    /** Replicon lié au noeud, ou null */
    private RepliconEntity replicon;
    /** État du noeud */
    private RepliconViewNodeState state = RepliconViewNodeState.NOK;
    /** Valeur affichée dans l'arbre */
    private String displayValue = "No display value supplied";

    RepliconViewNode(){}

    public boolean isReplicon(){
        return type == RepliconViewNodeType.REPLICON;
    }

    public RepliconViewNodeType getType() {
        return type;
    }

    public void setType(RepliconViewNodeType type) {
        this.type = type;
    }

    public RepliconEntity getReplicon() {
        return replicon;
    }

    public void setReplicon(RepliconEntity replicon) {
        this.replicon = replicon;
        if(replicon != null){
            setDisplayValue(replicon.getGenbankName());
            setState(RepliconViewNodeState.NOK);
            setType(RepliconViewNodeType.REPLICON);
        }else{
            setDisplayValue("NULL Replicon");
            setState(RepliconViewNodeState.NOK);
            setType(RepliconViewNodeType.UNKNOWN);
        }
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public RepliconViewNodeState getState() {
        return state;
    }

    public void setState(RepliconViewNodeState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return getDisplayValue();
    }
}
