import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NodeUpdateProcessing {

    //list of data stored
    private List<PlayerDataObject> dataList = new ArrayList<>();

    private short nodeId;

    public NodeUpdateProcessing(short nodeId) {
        this.nodeId = nodeId;
    }

    //process a received update packet, isInternal = update generated locally
    public void ReceivePacket(short[] packetData, short senderId) {
        //System.out.printf("Peer %d receiving data of length %d%n", peerId, packetData.length);
        //if(!isInternal)
        //    Utils.PrintShortArray(packetData);
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
    void CompleteParsedCommand(short playerId, short var, short value, short timestamp, short senderId) {

        //System.out.printf("Completing command: player: %d, var: %d, new value: %d, timestamp: %d%n", playerId, var, value, timestamp);

        PlayerDataObject updatedObj = dataList.stream()
                .filter(obj -> playerId == obj.GetPlayerId())
                .findAny()
                .orElse(null);

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

        for (PlayerDataObject obj: dataList) {
            System.out.println(obj.toString());
        }

    }
}
