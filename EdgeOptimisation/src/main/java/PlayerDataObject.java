import java.sql.Timestamp;

public class PlayerDataObject {

    private short var1 = 0, var2 = 0, var3 = 0, var4 = 0, var5 = 0,
            playerId = -1, currentNodeId = -1;
    private long var1Ts, var2Ts, var3Ts, var4Ts, var5Ts;

    public PlayerDataObject(int playerId, int currentNodeId, short timestamp) {

        this.playerId = (short) playerId;
        this.currentNodeId = (short) currentNodeId;


        var1Ts = timestamp;
        var2Ts = timestamp;
        var3Ts = timestamp;
        var4Ts = timestamp;
        var5Ts = timestamp;
    }

    public long GetTimestampFromIndex(int index) {
        switch (index) {
            case 0:
                return var1Ts;
            case 1:
                return var2Ts;
            case 2:
                return var3Ts;
            case 3:
                return var4Ts;
            case 4:
                return var5Ts;

        }

        return -1;
    }

    public short GetVarFromIndex(int index) {
        switch (index) {
            case 0:
                return var1;
            case 1:
                return var2;
            case 2:
                return var3;
            case 3:
                return var4;
            case 4:
                return var5;

        }

        return -1;
    }

    public void SetVarFromIndex(int varIndex, short newValue, long timestamp) {

        switch (varIndex) {
            case 0:
                var1 = newValue;
                var1Ts = timestamp;
                break;
            case 1:
                var2 = newValue;
                var2Ts = timestamp;
                break;
            case 2:
                var3 = newValue;
                var3Ts = timestamp;
                break;
            case 3:
                var4 = newValue;
                var4Ts = timestamp;
                break;
            case 4:
                var5 = newValue;
                var5Ts = timestamp;
                break;

        }
    }

    public short GetPlayerId() {
        return playerId;
    }

    public short GetCurrentNodeId() {
        return currentNodeId;
    }

    public void SetCurrentNodeId(short newNode) {
        currentNodeId = newNode;
    }

    public String toString() {

        return String.format("%d, %d, %d:%d, %d:%d, %d:%d, %d:%d, %d:%d",
                currentNodeId, playerId,
                var1, var1Ts, var2, var2Ts, var3, var3Ts, var4, var4Ts, var5, var5Ts);
    }
}
