import java.util.*;

public class PeerService
{

    public static final int MAX_PACKET_SIZE = 1400;

    short peerId = -1;
    short newUnitId = 0;

    short currentUnit = 0, maxUnits = 1;
    short currentVar = 0, maxVars = 5;
    short currentValue = 10, maxValue = 100;

    //list of all units for every other peer
    private List<Unit> unitsList = new ArrayList<>();

    //private RelevanceMetric relevanceMetric;
    private MessagePacking messagePacking;

    public PeerService(short peerId, int numClients, int serverId) {
        this.peerId = peerId;
        messagePacking = new MessagePacking(numClients, serverId);
    }

    //add a new unit and return packet string
    private short[] AddNewUnit(short sequenceNo) {

        unitsList.add(new Unit(peerId, newUnitId, sequenceNo));
        messagePacking.AddUnit(peerId, newUnitId);

        short data[] = { peerId, newUnitId, -1, 0 };
        newUnitId++;
        return data;
    }

    /*

    Generate a random client update packet,
    packets consist of a list unit state updates of the form:
    [ ownerID, unitID, variable to update, new value ]
    where each value is a short of size 2 bytes

    If sequence number is <0 then this is the client connection handshake packet, otherwise:
     the first byte is the sequence number of the packet used for synchronisation
     the second byte is the length of packet, number of short values excluding "header" values

     */
    public short[] GenerateRandomClientUpdatePacket(short sequenceNo) {


        short packetData[];
        int updatesToSend = 4, currentPacketSize = 0;
        short[] newData;

        packetData = new short[MAX_PACKET_SIZE];
        packetData[0] = sequenceNo;
        packetData[1] = 0;
        currentPacketSize = 2;


        for (int i = 0; i < updatesToSend; i++){
            if(newUnitId < maxUnits){
                newData = AddNewUnit(sequenceNo);
            }
            else {
                newData = new short[]{ peerId, currentUnit, currentVar, currentValue};

                //modify stored data to reflect this
                if(!CompleteParsedCommand(peerId, currentUnit, currentVar, currentValue, sequenceNo))
                    continue;

                //increment values
                currentUnit = (short) ((currentUnit + 1) % maxUnits);
                currentVar = (short) ((currentVar + 1) % maxVars);
                currentValue = (short) ((currentValue + 1) % maxValue);
            }

            //add data to packet
            for(int j = 0; j < 4; j++) {
                packetData[currentPacketSize + j] = newData[j];
            }
            currentPacketSize += 4;

            //System.out.printf("New command: %d, %d, %d, %d%n", newData[0], newData[1], newData[2], newData[3]);
        }

        //update packet length
        packetData[1] = (short) (currentPacketSize-2);

        short[] finalPacketData = Arrays.copyOfRange(packetData, 0, currentPacketSize);

        Utils.PrintShortArray(finalPacketData, "Old random");

        return finalPacketData;
    }

    /*

    The server generates a packet of updates from all clients for a specific client.
    The updates uses a combination of storing TTL values and relevance metrics to only
    send the values which are necessary to minimise the size of packets being sent

     */
    public short[] GenerateServerUpdatePacket(short sequenceNo, int receivingClientId) {

        return messagePacking.GenerateUpdatePacket(receivingClientId, unitsList, sequenceNo);
    }

    //receive update packet
    public void ReceivePacket(short[] packetData) {
        //System.out.printf("Peer %d receiving data of length %d%n", peerId, packetData.length);
        Utils.PrintShortArray(packetData, "Old receive");
        short sequenceNo = packetData[0], length = packetData[1], startValue = 2;

        for(int i = 0; i < length; i += 4){
            int currentIndex = startValue+i;
            short ownerId = packetData[currentIndex];
            short unitId = packetData[currentIndex+1];
            short varToUpdate = packetData[currentIndex+2];
            short newValue = packetData[currentIndex+3];

            CompleteParsedCommand(ownerId, unitId, varToUpdate, newValue, sequenceNo);
        }


    }

    //Use parsed data to complete the sent command i.e. modify some stored data
    boolean CompleteParsedCommand(short peer, short unitId, short var, short value, short sequenceNumber) {

        Unit updatedUnit = unitsList.stream()
                .filter(unit -> peer == unit.GetOwnerId() && unitId == unit.GetUnitId())
                .findAny()
                .orElse(null);


        if(var == -1) {
            //unit has already been added
            if(updatedUnit != null) {
                return false;
            }

            unitsList.add(new Unit(peer, unitId, sequenceNumber));
            messagePacking.AddUnit(peer, unitId);
            return true;
        }

        //check sequence value and return if stored value is higher ------------


        if(updatedUnit != null) {

            if(updatedUnit.GetSeqFromIndex(var) < sequenceNumber) {

                updatedUnit.SetVarFromIndex(var, value, sequenceNumber);
            }
            return true;
        }

        return false;
    }

    //Print the final stored data to check correctness
    public void OutputStoredData(){

        System.out.printf("=====Data stored for Peer %d=====%n", peerId);

        for (Unit unit: unitsList) {
            System.out.println(unit.toString());
        }

    }

}
