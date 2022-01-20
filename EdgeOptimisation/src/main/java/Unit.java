package main.java;

public class Unit {

    //Each player controls a list of units and has authority to change their data

    short positionX, positionY, velocityX, velocityY, healthValue, ownerPeerId, unitId;

    //store a sequence for each to manage consistency
    short posXSeq, posYSeq, velXSeq, velYSeq, healthValueSeq, ownerPeerIdSeq, unitIdSeq;

    public Unit(short owner, short id){
        positionX = 0;
        positionY = 0;
        velocityX = 0;
        velocityY = 0;
        healthValue = 100;
        ownerPeerId = owner;
        unitId = id;

        posXSeq = -1;
        posYSeq = -1;
        velXSeq = -1;
        velYSeq = -1;
        healthValueSeq = -1;
        ownerPeerIdSeq = -1;
        unitIdSeq = -1;
    }
}
