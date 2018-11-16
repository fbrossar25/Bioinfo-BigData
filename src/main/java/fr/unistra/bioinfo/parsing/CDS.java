package fr.unistra.bioinfo.parsing;

public final class CDS {
    private long begin = 0;
    private long end = 0;

    public CDS(){}

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public boolean isValid(long originLength){
        return begin < end && begin < originLength && end < originLength;
    }
}
