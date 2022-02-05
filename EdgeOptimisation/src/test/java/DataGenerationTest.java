import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DataGenerationTest {

    private static DataGeneration dataGen;
    private static NodeGeneration nodeGen;

    @BeforeAll
    static void Setup() {

        long seed = new Random().nextLong();
        nodeGen = new NodeGeneration(seed, 5, 0);
        dataGen = new DataGeneration(seed, 5, nodeGen);

        NodeConsistencyControlMain.SYSTEM_START = new Timestamp(System.currentTimeMillis()).getTime();

    }

    @Test
    void GenerateUpdateAndCheckObjectOwner() {

        byte[] packet = dataGen.GenerateUpdate(0);

        //convert back to short array
        short[] sendPacket = new short[packet.length / 2];
        ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sendPacket);

        short messageLength = sendPacket[1];

        for(int i = 2; i < messageLength; i=i+3) {

            short playerId = sendPacket[i];
            System.out.printf("Player at index %d updated var%n", playerId);
            assertTrue(dataGen.GetNodeIdAtIndex(playerId) == 0);
        }


    }

}