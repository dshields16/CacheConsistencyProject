import javax.xml.crypto.Data;
import java.sql.Timestamp;
import java.util.*;

public class NodeUpdateProcessing {

    //list of data stored
    private List<PlayerDataObject> dataList = new ArrayList<>();

    //store a map of player id to ttl values
    private HashMap<Integer, PlayerDataFrequency> ttlData = new HashMap<>();

    private short nodeId;
    public NodeUpdateProcessing(short nodeId) {
        this.nodeId = nodeId;
    }

    private static int finalStaleness = 0, totalMoves = 0;

    //measuring cache hits and misses
    private static int cacheHits = 0, cacheMisses = 0;

    //process a received update packet, isInternal = update generated locally
    public void ReceivePacket(short[] packetData, short senderId) {
        System.out.printf("Peer %d receiving data of length %d from node %d%n", nodeId, packetData.length, senderId);
        if(senderId == nodeId)
            Utils.PrintShortArray(packetData, "Locally generated packet");
        //else
        //    Utils.PrintShortArray(packetData, String.format("Remotely generated packet from peer %d", senderId));
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

            updatedObj = new PlayerDataObject(playerId, senderId, timestamp);
            dataList.add(updatedObj);
        }


        if(senderId == nodeId) {

            //server received data from client and doesnt have a ttl already
            if(!ttlData.containsKey((int)playerId))  {
                ttlData.put((int) playerId, new PlayerDataFrequency(200));
            }

            //check ownership and update if necessary (moved to this server)
            if(updatedObj.GetCurrentNodeId() != nodeId) {
                System.out.printf("Node %d moved to this node%n", playerId);
                updatedObj.SetCurrentNodeId(nodeId);

                //reset ttl values
                ttlData.remove(playerId);
                ttlData.put((int) playerId, new PlayerDataFrequency(200));
            }

            PlayerDataFrequency ttl = ttlData.get((int)playerId);
            ttl.UpdateMade(var);
        }

        //update the value
        updatedObj.SetVarFromIndex(var, value, timestamp);
        //update associated node
        updatedObj.SetCurrentNodeId(senderId);

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

            if(i >= correctDataList.size() || j >= copyOfLocalData.size()) {
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

    public byte[] GenerateDelayedUpdate(short timeSinceLastUpdate, boolean useTTL, int recipient, int otherNeighbour) {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long ts = timestamp.getTime();

        long timeSinceStart = ts-NodeConsistencyControlMain.SYSTEM_START;
        short tenthSeconds = (short) (timeSinceStart / 100);    //how many tenth seconds since system start, max 50 mins

        short[] packetData = new short[512];
        short currentPacketSize = 2;

        packetData[0] = tenthSeconds;

        short tsSinceLastUpdate = (short) (tenthSeconds-(timeSinceLastUpdate/100)); //add any data more recent than this

        //optimise by popularity of neighbours
        double ttlSkew = 1;
        if(GetNodePopularity(recipient) < 2*GetNodePopularity(otherNeighbour)) {
            System.out.printf("Node %d less popular%n", recipient);
            ttlSkew = 2;
        }


        //loop through stored data, add any changed vars to the packet
        for(int i = 0; i < dataList.size(); i++) {

            PlayerDataObject obj = dataList.get(i);
            if(obj.GetCurrentNodeId() != nodeId) {
                continue;
            }

            for(int j = 0; j < 5; j++) {

                boolean valueNeedsUpdated;
                long tsObjectUpdated = obj.GetTimestampFromIndex(j);

                if(useTTL) {
                    valueNeedsUpdated = tsObjectUpdated > tsSinceLastUpdate + (ttlSkew * GetTTLValueForVar(obj.GetPlayerId(), j));
                } else {
                    valueNeedsUpdated = tsObjectUpdated > tsSinceLastUpdate;
                }


                if(valueNeedsUpdated && !(obj.GetVarFromIndex(j) == 0 && obj.GetTimestampFromIndex(j) == 0)) {
                    packetData[currentPacketSize++] = obj.GetPlayerId();
                    packetData[currentPacketSize++] = (short) j;
                    packetData[currentPacketSize++] = obj.GetVarFromIndex(j);
                }
            }
        }

        //if(useTTL)
        //    OutputDataFrequencyValues();

        packetData[1] = currentPacketSize;
        short[] finalPacketData = Arrays.copyOfRange(packetData, 0, currentPacketSize);
        Utils.PrintShortArray(finalPacketData, "Generated delayed update");
        byte[] byteData = DataGeneration.ConvertDataToBytes(finalPacketData);

        return byteData;
    }

    public void OutputDataFrequencyValues() {

        for (Integer key : ttlData.keySet()) {
            ttlData.get(key).OutputFrequency(key);
        }
    }

    public int GetTTLValueForVar(int playerId, int var) {
        PlayerDataFrequency ttl = ttlData.get(playerId);
        if(ttl == null) {
            System.out.printf("Missing ttl for player %d%n", playerId);
            return 0;
        }
        return ttl.GetUpdateFrequencyForVar(var) / 100;   //divide by 100 for tenth seconds
    }

    public static void AddCacheHit() {
        cacheHits++;
    }

    public static void AddCacheMiss() {
        cacheMisses++;
    }

    public void OutputCacheRatioData() {
        System.out.printf("Cache hit rate: %.2f%n", (float)cacheHits/(cacheHits+cacheMisses));
    }

    /*
        More popular nodes should have data sent at a lower TTL
     */
    public double GetNodePopularity(int nodeId) {

        double sum = 0;

        for(PlayerDataObject obj : dataList) {

            if(obj.GetCurrentNodeId() == nodeId) {
                sum++;
            }
        }

        return sum / dataList.size();
    }
}




















