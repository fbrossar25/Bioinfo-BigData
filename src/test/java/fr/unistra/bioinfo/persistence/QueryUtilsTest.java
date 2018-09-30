package fr.unistra.bioinfo.persistence;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryUtilsTest {

    @Test
    void equals() {
        assertEquals(" a = 1 ", QueryUtils.equals("a", 1));
        assertEquals(" a = 'foo' ", QueryUtils.equals("a", "foo"));
    }

    @Test
    void in() {
        assertEquals(" a IN () ",QueryUtils.in("a", null));
        assertEquals(" a IN () ",QueryUtils.in("a", Collections.emptyList()));
        assertEquals(" a IN ( 1, 2, 3 ) ", QueryUtils.in("a", Arrays.asList(1,2,3)));
        assertEquals(" a IN ( 1 ) ", QueryUtils.in("a", Collections.singleton(1)));
        assertEquals(" a IN ( 'foo', 'bar', 'baz' ) ", QueryUtils.in("a", Arrays.asList("foo","bar","baz")));
        assertEquals(" a IN ( 'foo' ) ", QueryUtils.in("a", Collections.singleton("foo")));
    }
}