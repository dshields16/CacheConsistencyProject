import java.util.Arrays;
import java.util.List;

public class MessagePacking {

    /*
            get a list of vars with 0 TTL values
            check the sequence value is higher than RM
            for each client compile the updates which they should receive
            pack and send message
     */

    RelevanceMetric rm;

    List<Unit> units;   //get this from PeerService
    int currentSeq = 0;

    public short[] GenerateUpdatePacket(int clientId) {

        short[] packet = new short[PeerService.MAX_PACKET_SIZE];
        int currentPacketSize = 0;
        short[] newUpdate;

        List<UnitTTL> ttlValues = rm.GetTTLValues(clientId);    //one unit maps to a ttl for this client

        //for each unit
        for (UnitTTL ttl:ttlValues) {

            int[] varsToSend = ttl.GetVarsToSend(); //which vars should be checked

            //get the unit related to this ttl
            Unit updatedUnit = units.stream()
                    .filter(unit -> ttl.clientId == unit.ownerPeerId && ttl.unitId == unit.unitId)
                    .findAny()
                    .orElse(null);

            //for each 0 ttl value, check its sequence number to ensure it needs to be sent
            for(int i = 0; i < varsToSend.length; i++) {

                //check currentSeq - varSeq > RM, skip (value has not been updated recently)
                int varUpdated = varsToSend[i];
                if(currentSeq - updatedUnit.GetSeqFromIndex(varUpdated) > rm.GetTTLForVar(varsToSend[i], updatedUnit.ownerPeerId))
                    continue;

                //add this update to the packet
                newUpdate = new short[]{ ttl.clientId, updatedUnit.unitId, (short)varUpdated, updatedUnit.GetVarFromIndex(varUpdated)};
                for(int j = 0; j < 4; j++) {

                    packet[currentPacketSize++] = newUpdate[j];
                }
            }
        }

        //return final packet
        short[] finalPacketData = Arrays.copyOfRange(packet, 0, currentPacketSize);

        return finalPacketData;
    }
}
