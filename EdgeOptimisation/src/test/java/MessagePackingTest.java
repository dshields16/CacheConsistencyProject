import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessagePackingTest {

    static MessagePacking mp;
    static List<Unit> units;
    static short currentSequence = 1;

    @BeforeAll
    static void Setup() {

        mp = new MessagePacking(3, 1);

        //generate a list of units
        units = new ArrayList<>();
        units.add(new Unit((short)0, (short)0, currentSequence));
        units.add(new Unit((short)1, (short)0, currentSequence));
        units.add(new Unit((short)2, (short)0, currentSequence));

        //called when adding a new unit in PeerService, generated TTL arrays for each of these units
        mp.AddUnit((short)0, (short)0);
        mp.AddUnit((short)1, (short)0);
        mp.AddUnit((short)2, (short)0);
    }

    /*
        Add a new unit, generate an update packet that shows this
     */
    @Test
    void GenerateNewObjectPacket() {

        short[] newPacket = mp.GenerateUpdatePacket(0, units, currentSequence);

        Utils.PrintShortArray(newPacket);

        /*
                packet should have updates for:
                client 1 unit 0: created
                client 2 unit 0: created
         */

        short[] expectedResults = new short[] {
                1, 0, -1, 0,
                2, 0, -1, 0
        };

        assertArrayEquals(newPacket, expectedResults);
    }

    /*
        Test server generating a simple update for client 0
     */
    @Test
    void GenerateUpdatePacket() {

        mp.GenerateUpdatePacket(2, units, currentSequence); //set creation flag to false for both new objects
        currentSequence++;


        //apply some updates to units owned by clients 1 and 2
        Unit unit = units.get(1);
        unit.SetVarFromIndex(0, (short) 10, currentSequence);
        unit.SetVarFromIndex(4, (short) 5, currentSequence);

        unit = units.get(2);
        unit.SetVarFromIndex(0, (short) 2, currentSequence);
        unit.SetVarFromIndex(1, (short) 3, currentSequence);

        short[] newPacket = mp.GenerateUpdatePacket(0, units, currentSequence);

        Utils.PrintShortArray(newPacket);

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