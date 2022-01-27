import java.util.Arrays;
import java.util.List;

public class MessagePacking {

    /*
            get a list of vars with 0 TTL values
            check the sequence value is higher than RM
            for each client compile the updates which they should receive
            pack and send message
     */

    private RelevanceMetric rm;
    private int numClients = 0;

    private static int MESSAGE_DELAY = 1;   //allow for delay in sending messages

    public MessagePacking(int numClients, int serverId) {

        this.rm = new RelevanceMetric(numClients, serverId);
        this.numClients = numClients;
    }

    public void AddUnit(short ownerId, short unitId) {
        rm.AddUnit(ownerId, unitId);
    }


    public short[] GenerateUpdatePacket(int clientId, List<Unit> units, int currentSeq) {

        if(clientId == 0)
            rm.ProgressTTL();   //increment ttl values for all stored values

        short[] packet = new short[PeerService.MAX_PACKET_SIZE];
        packet[0] = (short) currentSeq;
        int currentPacketSize = 2;
        short[] newUpdate;

        List<UnitTTL> ttlValues = rm.GetTTLValues(clientId);    //one unit maps to a ttl for this client

        //for each unit
        for (UnitTTL ttl:ttlValues) {

            int[] varsToSend = ttl.GetVarsToSend(); //which vars should be checked

            //System.out.printf("Found %d vars to send%n", varsToSend.length);

            //get the unit related to this ttl
            Unit updatedUnit = units.stream()
                    .filter(unit -> ttl.clientId == unit.GetOwnerId() && ttl.unitId == unit.GetUnitId())
                    .findAny()
                    .orElse(null);

            //check if the unit has been recently created
            if(updatedUnit.IsRecentlyCreated()) {

                newUpdate = new short[]{ ttl.clientId, updatedUnit.GetUnitId(), (short)-1, (short)0};
                for(int j = 0; j < 4; j++) {

                    packet[currentPacketSize++] = newUpdate[j];
                }

                //if this is the last client to be updated, disable creation flag
                if(clientId == numClients-1 || (updatedUnit.GetOwnerId() == numClients-1 && clientId == GetPenultimateClient())) {
                    updatedUnit.SetIsNotRecentlyCreated();
                }
            }

            //for each 0 ttl value, check its sequence number to ensure it needs to be sent
            for(int i = 0; i < varsToSend.length; i++) {

                //check currentSeq - varSeq > RM, skip (value has not been updated recently)
                int varUpdated = varsToSend[i];

                //System.out.printf("%d %d %d%n", currentSeq, updatedUnit.GetSeqFromIndex(varUpdated), rm.GetTTLForVar(varsToSend[i], updatedUnit.GetOwnerId()));

                if(currentSeq - updatedUnit.GetSeqFromIndex(varUpdated) > rm.GetTTLForVar(varsToSend[i], updatedUnit.GetOwnerId()) + MESSAGE_DELAY) {
                    continue;
                }


                //add this update to the packet
                newUpdate = new short[]{ ttl.clientId, updatedUnit.GetUnitId(), (short)varUpdated, updatedUnit.GetVarFromIndex(varUpdated)};
                for(int j = 0; j < 4; j++) {

                    packet[currentPacketSize++] = newUpdate[j];
                }
            }
        }

        packet[1] = (short) (currentPacketSize-2);

        //return final packet
        short[] finalPacketData = Arrays.copyOfRange(packet, 0, currentPacketSize);

        return finalPacketData;
    }

    //for the edge case of creating a client and ending before the final client
    private int GetPenultimateClient() {
        int id = numClients-2;
        if(id == numClients/2)
            return numClients-3;
        return id;
    }
}
