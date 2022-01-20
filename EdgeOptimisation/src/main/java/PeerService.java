package main.java;

import java.util.*;

public class PeerService
{

    private static final int MAX_PACKET_SIZE = 1400;

    short peerId = -1;
    short newUnitId = 0;

    short currentUnit = 0, maxUnits = 1;
    short currentVar = 0, maxVars = 5;
    short currentValue = 10, maxValue = 100;

    //list of all units for every other peer
    List<Unit> unitsList = new ArrayList<>();

    public PeerService(short peerId) {
        this.peerId = peerId;
    }

    //add a new unit and return packet string
    private short[] AddNewUnit() {

        unitsList.add(new Unit(peerId, newUnitId));

        short data[] = { peerId, newUnitId, -1, 0 };
        newUnitId++;
        return data;
    }

    /*

    Send update packet, packets consist of a list unit state updates of the form:
    [ ownerID, unitID, variable to update, new value ]
    where each value is a short of size 2 bytes

    variable no: var
    -1: new unit
    0:  posX
    1:  posY
    2:  velX
    3:  velY
    4:  health

     */
    public short[] GenerateUpdatePacket(short sequenceNo) {


        short packetData[];
        int updatesToSend = 4, currentPacketSize = 0;
        short[] newData;

        if(sequenceNo != -1) {
            packetData = new short[MAX_PACKET_SIZE + 1];
            packetData[0] = sequenceNo;
            currentPacketSize = 1;
        }
        else {
            packetData = new short[MAX_PACKET_SIZE];
        }


        for (int i = 0; i < updatesToSend; i++){
            if(newUnitId < maxUnits){
                newData = AddNewUnit();
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
        }

        short[] finalPacketData = Arrays.copyOfRange(packetData, 0, currentPacketSize);

        return finalPacketData;
    }

    //receive update packet
    public void ReceivePacket(short[] packetData) {
        System.out.printf("Peer %d receiving data of length %d%n", peerId, packetData.length);
        int length = packetData.length;
        int startValue = 0;
        short sequenceNo = -1;
        if(length % 4 != 0) {
            sequenceNo = packetData[0];
            startValue = 1;
        }



        for(int i = startValue; i < length; i += 4){
            short ownerId = packetData[i];
            short unitId = packetData[i+1];
            short varToUpdate = packetData[i+2];
            short newValue = packetData[i+3];

            //System.out.printf("Peer %d received value %d, %d, %d, %d%n", peerId, ownerId, unitId, varToUpdate, newValue);

            CompleteParsedCommand(ownerId, unitId, varToUpdate, newValue, sequenceNo);
        }


    }

    //Use parsed data to complete the sent command i.e. modify some stored data
    boolean CompleteParsedCommand(short peer, short unitId, short var, short value, short sequenceNumber) {

        if(var == -1) {
            unitsList.add(new Unit(peer, unitId));
            return true;
        }

        //check sequence value and return if stored value is higher ------------

        Unit updatedUnit = unitsList.stream()
                .filter(unit -> peer == unit.ownerPeerId && unitId == unit.unitId)
                .findAny()
                .orElse(null);
        if(updatedUnit != null) {
            switch(var){
                case 0:
                    if(updatedUnit.posXSeq < sequenceNumber) {
                        updatedUnit.positionX = value;
                        updatedUnit.posXSeq = sequenceNumber;
                    }
                    break;
                case 1:
                    if(updatedUnit.posYSeq < sequenceNumber) {
                        updatedUnit.positionY = value;
                        updatedUnit.posYSeq = sequenceNumber;
                    }
                    break;
                case 2:
                    if(updatedUnit.velXSeq < sequenceNumber) {
                        updatedUnit.velocityX = value;
                        updatedUnit.velXSeq = sequenceNumber;
                    }
                    break;
                case 3:
                    if(updatedUnit.velYSeq < sequenceNumber) {
                        updatedUnit.velocityY = value;
                        updatedUnit.velYSeq = sequenceNumber;
                    }
                    break;
                case 4:
                    if(updatedUnit.healthValueSeq < sequenceNumber) {
                        updatedUnit.healthValue = value;
                        updatedUnit.healthValueSeq = sequenceNumber;
                    }
                    break;
            }
            return true;
        }

        return false;
    }

    //Print the final stored data to check correctness
    public void OutputStoredData(){

        System.out.printf("=====Data stored for Peer %d=====%n", peerId);

        for (Unit unit: unitsList) {
            System.out.printf("%d:%d, %d:%d, %d:%d, %d:%d, %d:%d, %d:%d, %d:%d%n",
                    unit.ownerPeerId, unit.ownerPeerIdSeq, unit.unitId, unit.unitIdSeq,
                    unit.positionX, unit.posXSeq, unit.positionY, unit.posYSeq,
                    unit.velocityX, unit.velXSeq, unit.velocityY, unit.velYSeq,
                    unit.healthValue, unit.healthValueSeq);
        }

    }

}
