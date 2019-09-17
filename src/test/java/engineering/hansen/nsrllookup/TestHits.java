package engineering.hansen.nsrllookup;

import engineering.hansen.nsrllookup.Connection;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestHits
{
    private static String[] known;

    static {
        known = new String[]{
                "000000A603173B73A381F93C70AF9330",
                "000000E2E580978FA6E621B7842D8FBF",
                "000001A29434521CF0C4D4FC143E8792",
                "000002244FF823527944C84BB1C3E33C",
                "000003A20F4478A192448F93094B1984",
                "000003DFB762671613F823C93EC6EF1D",
                "0000041A0ACED2A48B335C92E9800E10",
                "00000422D47441DBEF718394532CDD7A",
                "00000448C638A2AE7C4CB08960A627D8",
                "0000045A85E07F080BD13ACC24EDF8B8"
        };
    }

    @Test
    public void testHits() {
        Stream<String> knownStream = Arrays.stream(known);
        String[] hits = new Connection().hits(knownStream).toArray(String[]::new);
        assertTrue(hits.length == known.length);
    }
}
