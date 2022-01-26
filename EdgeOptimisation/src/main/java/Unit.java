public class Unit {

    //Each player controls a list of units and has authority to change their data

    private short positionX, positionY, velocityX, velocityY, healthValue, ownerPeerId, unitId;

    //store a sequence for each to manage consistency
    private short posXSeq, posYSeq, velXSeq, velYSeq, healthValueSeq;

    //this unit has been recently created, after it has been sent to clients it is set to false
    private boolean creationFlag = true;



    public Unit(short owner, short id, short seqNo){
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

    public void SetVarFromIndex(int varIndex, short newValue, short sequenceNo) {

        switch (varIndex) {
            case 0:
                positionX = newValue;
                posXSeq = sequenceNo;
                break;
            case 1:
                positionY = newValue;
                posYSeq = sequenceNo;
                break;
            case 2:
                velocityX = newValue;
                velXSeq = sequenceNo;
                break;
            case 3:
                velocityY = newValue;
                velYSeq = sequenceNo;
                break;
            case 4:
                healthValue = newValue;
                healthValueSeq = sequenceNo;
                break;

        }
    }

    public short GetUnitId() {
        return unitId;
    }

    public short GetOwnerId() {
        return ownerPeerId;
    }

    public String toString() {

        return String.format("%d, %d, %d:%d, %d:%d, %d:%d, %d:%d, %d:%d",
                ownerPeerId, unitId,
                positionX, posXSeq, positionY, posYSeq,
                velocityX, velXSeq, velocityY, velYSeq,
                healthValue, healthValueSeq);
    }

    public boolean IsRecentlyCreated() {
        return creationFlag;
    }

    public void SetIsNotRecentlyCreated() {
        creationFlag = false;
    }
}


















