import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Random;

public class DataGeneration {

    private Random r;

    private PlayerDataObject[] dataObjects;
    private NodeGeneration nodeGen;

    public DataGeneration(long seed, int numNodes, NodeGeneration nodeGen) {
        r = new Random(seed);
        dataObjects = new PlayerDataObject[numNodes*5];

        this.nodeGen = nodeGen;

        for(int i = 0; i < dataObjects.length; i++) {
            dataObjects[i] = new PlayerDataObject(i, r.nextInt(numNodes), (short)0);
        }
    }

    /*
            generate updates for all data objects, return updates relevant for this node

            packets will now be 2 bytes for timestamp, then 2 for packet length
            then each update will be 6 bytes of the form:
            playerId, varId, value
     */
    public byte[] GenerateUpdate(int currentNode) {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long ts = timestamp.getTime();

        long timeSinceStart = ts-NodeConsistencyControlMain.SYSTEM_START;
        short tenthSeconds = (short) (timeSinceStart / 100);    //how many tenth seconds since system start, max 50 mins

        short[] packetData = new short[512];
        short currentPacketSize = 2;

        packetData[0] = tenthSeconds;

        for(int i = 0; i < dataObjects.length; i++) {

            //chance to move the object to a neighbouring object
            int moveNode = r.nextInt(20);
            if(moveNode == 0) {
                dataObjects[i].SetCurrentNodeId((short) nodeGen.GetNeighbour1());
            }
            else if(moveNode == 1) {
                dataObjects[i].SetCurrentNodeId((short) nodeGen.GetNeighbour2());
            }

            int numUpdates = r.nextInt(4);

            for (int j = 0; j < numUpdates; j++) {
                int varToUpdate = r.nextInt(5);
                int newValue = r.nextInt(100);

                dataObjects[i].SetVarFromIndex(varToUpdate, (short) newValue, tenthSeconds);

                //if the data objects node is this one then add the update to the packet
                if(dataObjects[i].GetCurrentNodeId() == currentNode) {

                    packetData[currentPacketSize++] = (short) i;
                    packetData[currentPacketSize++] = (short) varToUpdate;
                    packetData[currentPacketSize++] = (short) newValue;
                }
            }


        }

        packetData[1] = currentPacketSize;
        short[] finalPacketData = Arrays.copyOfRange(packetData, 0, currentPacketSize);

        Utils.PrintShortArray(finalPacketData);

        byte[] byteData = ConvertDataToBytes(finalPacketData);

        return byteData;

    }

    private byte[] ConvertDataToBytes(short[] shortArray) {

        ByteBuffer buffer = ByteBuffer.allocate(2*shortArray.length + Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.asShortBuffer().put(shortArray);
        return buffer.array();
    }

    public short GetNodeIdAtIndex(int index) {
        return dataObjects[index].GetCurrentNodeId();
    }


}
