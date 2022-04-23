import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CacheStorageTest {

    //generate data
    //read local update
    //apply to local cache


    private static DataGeneration dataGen;
    private static NodeGeneration nodeGen;
    private static NodeUpdateProcessing updateProcessing;

    private static short currentNode = 0;

    @BeforeAll
    static void Setup() {

        long seed = new Random().nextLong();
        updateProcessing = new NodeUpdateProcessing(currentNode);
        nodeGen = new NodeGeneration(seed, 5, currentNode);
        dataGen = new DataGeneration(seed, 5, nodeGen, updateProcessing);

        NodeConsistencyControlMain.SYSTEM_START = new Timestamp(System.currentTimeMillis()).getTime();
    }

    @Test
    void CheckLocalUpdateInCache() {

        byte[] packetData = dataGen.GenerateUpdate(currentNode);
        short[] sendPacket = new short[packetData.length / 2];
        ByteBuffer.wrap(packetData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sendPacket);

        updateProcessing.ReceivePacket(sendPacket, currentNode);

        //print data generated
        Utils.PrintShortArray(sendPacket, "Data gen");

        //print cache state
        updateProcessing.OutputStoredData();

        //check each update and make sure it matches the local cache
        for(int i = 2; i < sendPacket.length; i+=3) {

            assertTrue(updateProcessing.GetPlayerVar(sendPacket[i], sendPacket[i+1]) == sendPacket[i+2]);
        }


    }
}
