package fr.unistra.bioinfo.persistence.entity;

public enum Phase {
    PHASE_0(0), PHASE_1(1), PHASE_2(2);

    private int idx;

    private Phase(int idx){
        this.idx = idx;
    }

    public int getIdx() {
        return idx;
    }
}
