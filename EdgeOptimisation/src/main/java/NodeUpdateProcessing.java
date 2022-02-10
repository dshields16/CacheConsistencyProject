import java.sql.Timestamp;
import java.util.*;

public class NodeUpdateProcessing {

    //list of data stored
    private List<PlayerDataObject> dataList = new ArrayList<>();

    private short nodeId;
    public NodeUpdateProcessing(short nodeId) {
        this.nodeId = nodeId;
    }

    private static int finalStaleness = 0, totalMoves = 0;

    //process a received update packet, isInternal = update generated locally
    public void ReceivePacket(short[] packetData, short senderId) {
        //System.out.printf("Peer %d receiving data of length %d%n", peerId, packetData.length);
        if(senderId == nodeId)
            Utils.PrintShortArray(packetData, "Locally generated packet");
        short timestamp = packetData[0], length = packetData[1];

        for(int i = 2; i < length; i += 3){
            int currentIndex = i;
            short playerId = packetData[currentIndex];
            short varToUpdate = packetData[currentIndex+1];
            short newValue = packetData[currentIndex+2];

            if(playerId == 0 && varToUpdate == 0 && newValue == 0) {
                break;
            }

            CompleteParsedCommand(playerId, varToUpdate, newValue, timestamp, senderId);
        }
    }

    //Use parsed data to complete the sent command i.e. modify some stored data
    private void CompleteParsedCommand(short playerId, short var, short value, short timestamp, short senderId) {

        //System.out.printf("Completing command: player: %d, var: %d, new value: %d, timestamp: %d%n", playerId, var, value, timestamp);
        PlayerDataObject updatedObj;
        try {
            updatedObj = dataList.stream()
                    .filter(obj -> playerId == obj.GetPlayerId())
                    .findAny()
                    .orElse(null);
        } catch(ConcurrentModificationException e) {
            e.printStackTrace();
            System.out.printf("Error with player index %d%n", playerId);
            return;
        }

        //new data object
        if(updatedObj == null) {

            if(nodeId == senderId)
                updatedObj = new PlayerDataObject(playerId, nodeId, timestamp);
            else
                updatedObj = new PlayerDataObject(playerId, senderId, timestamp);
            dataList.add(updatedObj);
        }

        //check ownership and update if necessary
        if(updatedObj.GetCurrentNodeId() != nodeId && nodeId == senderId) {
            updatedObj.SetCurrentNodeId(nodeId);
        }

        updatedObj.SetVarFromIndex(var, value, timestamp);
    }

    //Print the final stored data to check correctness
    public void OutputStoredData(){

        //sort data first
        dataList.sort(Comparator.comparing(PlayerDataObject::GetCurrentNodeId)
                .thenComparing(PlayerDataObject::GetPlayerId));

        System.out.printf("=====Data stored for Peer %d=====%n", nodeId);

        OutputDataList(dataList);

    }

    private void OutputDataList(List<PlayerDataObject> list) {

        String output = "";
        for (PlayerDataObject obj: list) {
            output += String.format("%s%n", obj.toString());
        }
        System.out.println(output);
    }

    /*
        When a player leaves this node, compare the locally stored data for that node with the
        perfect data to check the consistency of data stored about the node
     */
    public void CompareDataStored(List<PlayerDataObject> correctDataList, int otherNodeId) {

        List<PlayerDataObject> copyOfLocalData = new ArrayList<>();

        //make a list of all elements which are stored on the new node
        for(int i = 0; i < dataList.size(); i++) {
            if(dataList.get(i).GetCurrentNodeId() == otherNodeId) {
                copyOfLocalData.add(dataList.get(i));
            }
        }

        System.out.printf("Comparing data stored for node %d%n", otherNodeId);

        copyOfLocalData.sort(Comparator.comparing(PlayerDataObject::GetCurrentNodeId)
                .thenComparing(PlayerDataObject::GetPlayerId));

        correctDataList.sort(Comparator.comparing(PlayerDataObject::GetCurrentNodeId)
                .thenComparing(PlayerDataObject::GetPlayerId));

        OutputDataList(correctDataList);
        OutputDataList(copyOfLocalData);

        int totalStaleness = 0;

        //compare this list of data with the perfect list
        int j = 0;  //index for local data
        for(int i = 0; i < copyOfLocalData.size(); i++, j++) {

            if(i >= correctDataList.size()) {
                continue;
            }

            PlayerDataObject localData = copyOfLocalData.get(j);
            PlayerDataObject perfectData = correctDataList.get(i);

            //if the player data doesn't match
            if(localData.GetPlayerId() != perfectData.GetPlayerId()) {

                //extra local data that has not been updated
                if(localData.GetPlayerId() < perfectData.GetPlayerId()) {

                    i--;
                    continue;
                }
                j--;
                //comparison += String.format("Missing player with id %d%n", perfectData.GetPlayerId());
                continue;
            }

            totalStaleness += perfectData.CompareWithOtherObject(localData);
        }

        //System.out.printf("Total staleness: %d%n", totalStaleness);
        finalStaleness += totalStaleness;
        totalMoves++;
    }

    public void OutputStalenessData() {
        System.out.printf("Total staleness of data: %d%n", finalStaleness);
        float avg = finalStaleness/totalMoves;
        System.out.printf("Average over %d runs is %.2f%n", totalMoves, avg);
    }

    public byte[] GenerateDelayedUpdate(short timeSinceLastUpdate) {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long ts = timestamp.getTime();

        long timeSinceStart = ts-NodeConsistencyControlMain.SYSTEM_START;
        short tenthSeconds = (short) (timeSinceStart / 100);    //how many tenth seconds since system start, max 50 mins

        short[] packetData = new short[512];
        short currentPacketSize = 2;

        packetData[0] = tenthSeconds;

        short tsSinceLastUpdate = (short) (tenthSeconds-(timeSinceLastUpdate/100)); //add any data more recent than this

        //System.out.printf("Checking for updates since %d%n", tsSinceLastUpdate);

        //loop through stored data, add any changed vars to the packet
        for(int i = 0; i < dataList.size(); i++) {

            PlayerDataObject obj = dataList.get(i);
            if(obj.GetCurrentNodeId() != nodeId) {
                continue;
            }

            for(int j = 0; j < 5; j++) {
                if(obj.GetTimestampFromIndex(j) > tsSinceLastUpdate
                        && !(obj.GetVarFromIndex(j) == 0 && obj.GetTimestampFromIndex(j) == 0)) {
                    packetData[currentPacketSize++] = obj.GetPlayerId();
                    packetData[currentPacketSize++] = (short) j;
                    packetData[currentPacketSize++] = obj.GetVarFromIndex(j);
                }
            }
        }

        packetData[1] = currentPacketSize;
        short[] finalPacketData = Arrays.copyOfRange(packetData, 0, currentPacketSize);
        //Utils.PrintShortArray(finalPacketData, "Generated delayed update");
        byte[] byteData = DataGeneration.ConvertDataToBytes(finalPacketData);

        return byteData;
    }
}




















