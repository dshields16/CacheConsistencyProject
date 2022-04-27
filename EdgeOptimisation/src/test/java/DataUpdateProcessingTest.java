import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/*
    Test the update processing module
 */
public class DataUpdateProcessingTest {

    private static DataGeneration dataGen;
    private static NodeGeneration nodeGen;
    private static NodeUpdateProcessing updateProcessing;

    private static short currentNode = 0;

    @BeforeAll
    static void Setup() {

        long seed = new Random().nextLong();
        updateProcessing = new NodeUpdateProcessing(currentNode);
        nodeGen = new NodeGeneration(seed, 3, currentNode);
        dataGen = new DataGeneration(seed, 3, nodeGen, updateProcessing);

        NodeConsistencyControlMain.SYSTEM_START = new Timestamp(System.currentTimeMillis()).getTime();
    }

    @Test
    void CheckGeneratedPacketEqualToDataGen() {

        byte[] packetData = dataGen.GenerateUpdate(currentNode);
        short[] sendPacket = new short[packetData.length / 2];
        ByteBuffer.wrap(packetData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sendPacket);

        updateProcessing.ReceivePacket(sendPacket, currentNode);


        //updateProcessing.OutputStoredData();

        byte[] updateData = updateProcessing.GenerateDelayedUpdate((short) 200, false, 1);
        short[] updatePacket = new short[updateData.length / 2];
        ByteBuffer.wrap(updateData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(updatePacket);

        Utils.PrintShortArray(sendPacket, "Data gen");
        Utils.PrintShortArray(updatePacket, "Update packet");

        for(int i = 2; i < sendPacket.length; i+=3) {

            boolean foundSubString = false;

            for(int j = 2; j < updatePacket.length; j+=3) {

                if(sendPacket[i] == updatePacket[j] && sendPacket[i+1] == updatePacket[j+1] && sendPacket[i+2] == updatePacket[j+2]) {
                    foundSubString = true;
                    break;
                }
            }

            assertTrue(foundSubString);
        }



    }
}
