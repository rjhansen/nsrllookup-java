package engineering.hansen.nsrllookup;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestHits
{
    private static String[] known;

    static {
        known = new String[]{
                "000000A603173B73A381F93C70AF9330",
                "000000A603173B73A381F93C70AF9330",
                "000000E2E580978FA6E621B7842D8FBF",
                "000000E2E580978FA6E621B7842D8FBF",
                "000001A29434521CF0C4D4FC143E8792",
                "DECAFBADDECAFBADDECADBADDECAFBAD",
                "DECAFBADDECAFBADDECADBADDECAFBAD",
                "DEADBEEFDEADBEEFDEADBEEFDEADBEEF",
                "DEADBEEFDEADBEEFDEADBEEFDEADBEEF",
                "BADD00D5BADD00D5BADD00D5BADD00D5"
        };
    }

    @Test
    public void testHits() {
        String[] hits = new Connection().hits(Arrays.stream(known)).toArray(String[]::new);
        assertTrue(hits.length == 3);
        assertTrue(hits[0].compareTo(hits[1]) < 0);
        assertTrue(hits[1].compareTo(hits[2]) < 0);
        assertTrue(hits[0].charAt(0) == hits[1].charAt(0) &&
                hits[1].charAt(0) == hits[2].charAt(0));
    }

    @Test
    public void testMisses() {
        String[] misses = new Connection().misses(Arrays.stream(known)).toArray(String[]::new);
        assertTrue(misses.length == 3);
        assertTrue(misses[0].compareTo(misses[1]) < 0);
        assertTrue(misses[1].compareTo(misses[2]) < 0);
        assertTrue(misses[0].charAt(0) != misses[1].charAt(0) &&
                misses[1].charAt(0) == misses[2].charAt(0));
    }
}
