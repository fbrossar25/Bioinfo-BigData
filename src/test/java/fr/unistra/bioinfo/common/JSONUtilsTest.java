package fr.unistra.bioinfo.common;

import fr.unistra.bioinfo.model.Replicon;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class JSONUtilsTest {
    @Test
    public void repliconJSON(){
        JSONObject json = new JSONObject("{\"isDownloaded\":false,\"replicon\":\"NC_013023\",\"isComputed\":true,\"dinucleotides\":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15],\"trinucleotides\":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63],\"version\":2}");
        Replicon r = new Replicon(json, null);
        assertTrue(r.isComputed());
        assertFalse(r.isDownloaded());
        assertEquals("NC_013023", r.getReplicon());
        assertEquals(2, r.getVersion().intValue());
        int i = 0;
        for(String di : CommonUtils.DINUCLEOTIDES){
            assertEquals(i, r.getDinucleotide(di).intValue());
            i++;
        }
        i = 0;
        for(String tri : CommonUtils.TRINUCLEOTIDES){
            assertEquals(i, r.getTrinucleotide(tri).intValue());
            i++;
        }
    }
}