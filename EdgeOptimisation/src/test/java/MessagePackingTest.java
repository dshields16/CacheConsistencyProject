import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessagePackingTest {

    static MessagePacking mp;
    static RelevanceMetric rm;

    @BeforeAll
    static void Setup() {

        mp = new MessagePacking();
        rm = new RelevanceMetric(3, 1);

        //generate a list of units
        List<Unit> units = new ArrayList<>();
        units.add(new Unit((short)0, (short)0));
        units.add(new Unit((short)1, (short)0));
        units.add(new Unit((short)2, (short)0));

        //relevance metric should generate TTL arrays for each of these units
        rm.AddUnit((short)0, (short)0);
        rm.AddUnit((short)1, (short)0);
        rm.AddUnit((short)2, (short)0);

        //apply some updates to units owned by clients 1 and 2
        short currentSequence = 1;
        Unit unit = units.get(1);
        unit.positionX = 10;
        unit.posXSeq = currentSequence;
        unit.velocityY = 5;
        unit.velYSeq = currentSequence;

        unit = units.get(2);
        unit.positionX = 2;
        unit.posXSeq = currentSequence;
        unit.positionY = 3;
        unit.posYSeq = currentSequence;

        rm.ProgressTTL();

        mp.rm = rm;
        mp.units = units;
        mp.currentSeq = currentSequence;
    }

    /*
        Test server generating a simple update for client 0
     */
    @Test
    void GenerateUpdatePacket() {

        short[] newPacket = mp.GenerateUpdatePacket(0);

        /*
                packet should have updates for:
                client 1 unit 0: posx=10
                client 2 unit 0: posx=2, posy=3
         */

        short[] expectedResults = new short[] {
                1, 0, 0, 10,
                2, 0, 0, 2,
                2, 0, 1, 3
        };

        assertArrayEquals(newPacket, expectedResults);
    }
}