package main.java;

public class Unit {

    //Each player controls a list of units and has authority to change their data

    short positionX, positionY, velocityX, velocityY, healthValue, ownerPeerId, unitId;

    //store a sequence for each to manage consistency
    short posXSeq, posYSeq, velXSeq, velYSeq, healthValueSeq;



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
    }

    public short GetSeqFromIndex(int index) {
        switch (index) {
            case 0:
                return posXSeq;
            case 1:
                return posYSeq;
            case 2:
                return velXSeq;
            case 3:
                return velYSeq;
            case 4:
                return healthValueSeq;

        }

        return -1;
    }

    public short GetVarFromIndex(int index) {
        switch (index) {
            case 0:
                return positionX;
            case 1:
                return positionY;
            case 2:
                return velocityX;
            case 3:
                return velocityY;
            case 4:
                return healthValue;

        }

        return -1;
    }
}
