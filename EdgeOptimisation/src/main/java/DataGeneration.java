import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DataGeneration {

    private Random r;

    private PlayerDataObject[] dataObjects, clonedData;
    private NodeGeneration nodeGen;

    private NodeUpdateProcessing nodeUpdateProcessing;

    public DataGeneration(long seed, int numNodes, NodeGeneration nodeGen, NodeUpdateProcessing updating) {
        r = new Random(seed);
        dataObjects = new PlayerDataObject[numNodes*5];
        clonedData = new PlayerDataObject[numNodes*5];
        this.nodeUpdateProcessing = updating;

        this.nodeGen = nodeGen;

        for(int i = 0; i < dataObjects.length; i++) {
            dataObjects[i] = new PlayerDataObject(i, r.nextInt(numNodes), (short)0);
            clonedData[i] = new PlayerDataObject(i, r.nextInt(numNodes), (short)0);
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

            if((moveNode == 0 || moveNode == 1) && dataObjects[i].GetCurrentNodeId() == currentNode) {
                //one of the stored objects for this node is moving, check stored data for the new node to check consistency

                if(moveNode == 0) {
                    System.out.printf("==Player %d moving from node %d to %d%n", i, currentNode, nodeGen.GetNeighbour1(currentNode));
                    CompareLocalData(nodeGen.GetNeighbour1(currentNode));
                }
                else if(moveNode == 1) {
                    System.out.printf("==Player %d moving from node %d to %d%n", i, currentNode, nodeGen.GetNeighbour2(currentNode));
                    CompareLocalData(nodeGen.GetNeighbour2(currentNode));
                }
            }

            //generate neighbour 1 for given node

            if(moveNode == 0) {
                dataObjects[i].SetCurrentNodeId((short) nodeGen.GetNeighbour1(dataObjects[i].GetCurrentNodeId()));
            }
            else if(moveNode == 1) {
                dataObjects[i].SetCurrentNodeId((short) nodeGen.GetNeighbour2(dataObjects[i].GetCurrentNodeId()));
            }



            int numUpdates = 1 + r.nextInt(3);

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
        //Utils.PrintShortArray(finalPacketData, "Generated normal update");
        byte[] byteData = ConvertDataToBytes(finalPacketData);

        CloneData();

        return byteData;

    }

    public static byte[] ConvertDataToBytes(short[] shortArray) {

        ByteBuffer buffer = ByteBuffer.allocate(2*shortArray.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.asShortBuffer().put(shortArray);
        return buffer.array();
    }

    public short GetNodeIdAtIndex(int index) {
        return dataObjects[index].GetCurrentNodeId();
    }

    private void CompareLocalData(int newNode) {

        List<PlayerDataObject> copyOfData = new ArrayList<>();

        //make a list of all elements which are stored on the new node
        for(int i = 0; i < clonedData.length; i++) {
            if(clonedData[i].GetCurrentNodeId() == newNode) {
                copyOfData.add(clonedData[i]);
            }
        }

        nodeUpdateProcessing.CompareDataStored(copyOfData, newNode);
    }

    //Store a copy of data before changes are made to check consistency
    private void CloneData() {

        for(int i = 0; i < dataObjects.length; i++) {

            clonedData[i] = new PlayerDataObject(dataObjects[i]);
        }
    }


}
