import java.sql.Timestamp;
import java.util.*;

/*
    Receive and process cache update messages into local cache changes
    Monitor cache updates to use PATA and optimise update generation
    Track performance of algorithm
 */
public class NodeUpdateProcessing {

    //list of data stored
    private List<PlayerDataObject> dataList = new ArrayList<>();

    //store a map of player id to ttl values
    private HashMap<Integer, PlayerDataFrequency> ttlData = new HashMap<>();

    private short nodeId;

    /*
        nodeId - node id of this process
     */
    public NodeUpdateProcessing(short nodeId) {
        this.nodeId = nodeId;
    }

    private static int finalStaleness = 0, totalMoves = 0;

    //measuring cache hits and misses
    private static int cacheHits = 0, cacheMisses = 0;

    /*
        packetData - received cache update message
        senderId - the id of the node which sent the packet

        process a received update packet and apply changes to local cache
     */
    public void ReceivePacket(short[] packetData, short senderId) {

        short timestamp = packetData[0], length = packetData[1];

        for(int i = 2; i < length; i += 3){
            short playerId = packetData[i];
            short varToUpdate = packetData[i+1];
            short newValue = packetData[i+2];

            if(playerId == 0 && varToUpdate == 0 && newValue == 0) {
                break;
            }

            CompleteParsedCommand(playerId, varToUpdate, newValue, timestamp, senderId);
        }
    }

    /*
        playerId - id of the player object being updated
        var - id of the variable being updated
        value - new value assigned to the variable
        timestamp - time that the cache update was sent
        senderId - process which sent the cache update

        Use parsed data to complete the sent command i.e. modify some stored cache data
     */
    private void CompleteParsedCommand(short playerId, short var, short value, short timestamp, short senderId) {

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

            //server received data from client and doesn't have a ttl already
            if(!ttlData.containsKey((int)playerId))  {
                ttlData.put((int) playerId, new PlayerDataFrequency(200));
            }

            //check ownership and update if necessary (moved to this server)
            if(updatedObj.GetCurrentNodeId() != nodeId) {
                System.out.printf("Player %d moved to this node%n", playerId);
                updatedObj.SetCurrentNodeId(nodeId);
            }

            PlayerDataFrequency ttl = ttlData.get((int)playerId);
            ttl.UpdateMade(var);
        }

        //update the value
        updatedObj.SetVarFromIndex(var, value, timestamp);
        //update associated node
        updatedObj.SetCurrentNodeId(senderId);

    }

    /*
        Print the final stored data to check correctness
     */
    public void OutputStoredData(){

        //sort data first
        dataList.sort(Comparator.comparing(PlayerDataObject::GetCurrentNodeId)
                .thenComparing(PlayerDataObject::GetPlayerId));

        System.out.printf("=====Data stored for Peer %d=====%n", nodeId);

        OutputDataList(dataList);

    }

    /*
        list - list of player cache data

        Prints the cache data
     */
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
                continue;
            }

            totalStaleness += perfectData.CompareWithOtherObject(localData);

        }

        finalStaleness += totalStaleness;
        totalMoves++;
    }

    /*
        Prints the staleness data gathered during execution
     */
    public void OutputStalenessData() {
        System.out.printf("Total staleness of data: %d%n", finalStaleness);
        float avg = finalStaleness/totalMoves;
        System.out.printf("Average over %d runs is %.2f%n", totalMoves, avg);
    }

    /*
        timeSinceLastUpdate - the time elapsed since the previous update generation
        useTTL - whether or not adaptive TTL optimisation should be used
        recipient - the node id of the process receiving the update

        Generate a cache update packet for the specified node using the specified optimisation method
     */
    public byte[] GenerateDelayedUpdate(short timeSinceLastUpdate, boolean useTTL, int recipient) {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long ts = timestamp.getTime();

        long timeSinceStart = ts-NodeConsistencyControlMain.SYSTEM_START;
        short tenthSeconds = (short) (timeSinceStart / 100);    //how many tenth seconds since system start, max 50 mins
        tenthSeconds = (short) (Math.round(tenthSeconds / 2) * 2);

        short[] packetData = new short[512];
        short currentPacketSize = 2;

        packetData[0] = tenthSeconds;

        short tsSinceLastUpdate = (short) (tenthSeconds-(timeSinceLastUpdate/100)); //add any data more recent than this

        //optimise by popularity of neighbours
        double ttlSkew = 1;
        if(dataList.size() > 0) {
            if(3*GetNodePopularity(recipient) < (5/dataList.size())) {
                System.out.printf("**********Node %d less popular****************%n", recipient);
                ttlSkew = 1.5;
            }
        }



        //loop through stored data, add any changed vars to the packet
        for(int i = 0; i < dataList.size(); i++) {

            PlayerDataObject obj = dataList.get(i);
            if(obj.GetCurrentNodeId() != nodeId) {
                continue;
            }

            for(int j = 0; j < DataGeneration.numberOfVariables; j++) {

                long tsObjectUpdated = obj.GetTimestampFromIndex(j);
                double ttl = ttlSkew * GetTTLValueForVar(obj.GetPlayerId(), j);

                boolean valueNeedsUpdated = tsObjectUpdated > tsSinceLastUpdate;


                if(useTTL && valueNeedsUpdated) {
                    //valueNeedsUpdated = tsObjectUpdated > tsSinceLastUpdate + (ttlSkew * GetTTLValueForVar(obj.GetPlayerId(), j));

                    //check time divisible by ttl value, if not then don't update
                    if (tenthSeconds % ttl != 0) {

                        valueNeedsUpdated = false;
                        ResetUpdateTimeForVar(i, j, tenthSeconds);
                    }
                }



                //add update to packet
                if(valueNeedsUpdated && !(obj.GetVarFromIndex(j) == 0 && obj.GetTimestampFromIndex(j) == 0)) {

                    packetData[currentPacketSize++] = obj.GetPlayerId();
                    packetData[currentPacketSize++] = (short) j;
                    packetData[currentPacketSize++] = obj.GetVarFromIndex(j);
                }
            }
        }

        packetData[1] = currentPacketSize;

        //convert to a byte array
        short[] finalPacketData = Arrays.copyOfRange(packetData, 0, currentPacketSize);
        byte[] byteData = DataGeneration.ConvertDataToBytes(finalPacketData);

        return byteData;
    }

    /*
        Print the frequency data for each object
     */
    public void OutputDataFrequencyValues() {

        for (Integer key : ttlData.keySet()) {
            ttlData.get(key).OutputFrequency(key);
        }
    }

    /*
        playerId - the id of the player object to be searched
        var - the id of the var to check

        returns the TTL value for the player id and var passed in
     */
    public int GetTTLValueForVar(int playerId, int var) {
        PlayerDataFrequency ttl = ttlData.get(playerId);
        if(ttl == null) {
            System.out.printf("Missing ttl for player %d%n", playerId);
            return 0;
        }
        return ttl.GetUpdateFrequencyForVar(var) / 100;   //divide by 100 for tenth seconds
    }

    /*
        playerId - the id of the player object to be searched
        var - the id of the var to check

        returns the variable value for the player id and var passed in
     */
    public short GetPlayerVar(int playerId, int var) {
        PlayerDataObject playerObj;
        try {
            playerObj = dataList.stream()
                    .filter(obj -> playerId == obj.GetPlayerId())
                    .findAny()
                    .orElse(null);
        } catch(ConcurrentModificationException e) {
            e.printStackTrace();
            System.out.printf("Error with player index %d%n", playerId);
            return -1;
        }

        return playerObj.GetVarFromIndex(var);
    }

    /*
        objId - the id of the object
        var - the id of the var to reset
        ts - the current time set

        If the update is being delayed, then reset the update time so it can be picked up later
     */
    public void ResetUpdateTimeForVar(int objId, int var, long ts) {
        PlayerDataObject obj = dataList.get(objId);
        obj.SetVarFromIndex(var, obj.GetVarFromIndex(var), ts+2);
    }

    /*
        Add a cache hit to the total
     */
    public static void AddCacheHit() {
        cacheHits++;
    }

    /*
        Add a cache miss to the total
     */
    public static void AddCacheMiss() {
        cacheMisses++;
    }

    /*
        Print the cache hit data
     */
    public void OutputCacheRatioData() {
        System.out.printf("Cache hit rate: %.2f%n", (float)cacheHits/(cacheHits+cacheMisses));
    }

    /*
        nodeId - the id of the node being checked

        Returns a popularity value for the node, comparing sizes of the data list
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




















