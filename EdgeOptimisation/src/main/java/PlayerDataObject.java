import javax.xml.crypto.Data;
import java.sql.Timestamp;

public class PlayerDataObject {

    private short[] vars;
    private short playerId = -1, currentNodeId = -1;
    private long[] varsTs;

    public PlayerDataObject(int playerId, int currentNodeId, short timestamp) {

        this.playerId = (short) playerId;
        this.currentNodeId = (short) currentNodeId;

        vars = new short[DataGeneration.numberOfVariables];
        varsTs = new long[DataGeneration.numberOfVariables];

        for(int i = 0; i < DataGeneration.numberOfVariables; i++) {
            varsTs[i] = timestamp;
        }

    }

    public PlayerDataObject(PlayerDataObject obj) {

        this.playerId = obj.GetPlayerId();
        this.currentNodeId = obj.GetCurrentNodeId();

        vars = new short[DataGeneration.numberOfVariables];
        varsTs = new long[DataGeneration.numberOfVariables];

        for(int i = 0; i < DataGeneration.numberOfVariables; i++) {
            SetVarFromIndex(i, obj.GetVarFromIndex(i), obj.GetTimestampFromIndex(i));
        }
    }

    public long GetTimestampFromIndex(int index) {

        return varsTs[index];
    }

    public short GetVarFromIndex(int index) {

        return vars[index];
    }

    public void SetVarFromIndex(int varIndex, short newValue, long timestamp) {

        vars[varIndex] = newValue;
        varsTs[varIndex] = timestamp;
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

        String output = String.format("%d, %d, ",
                currentNodeId, playerId);

        for(int i = 0; i < DataGeneration.numberOfVariables; i++) {

            output += String.format("%d: %d, ", vars[i], varsTs[i]);
        }

        return output;
    }

    //comparing against local data
    public int CompareWithOtherObject(PlayerDataObject obj) {

        int totalStaleness = 0;

        for(int i = 0; i < DataGeneration.numberOfVariables; i++) {
            if(obj.GetVarFromIndex(i) != GetVarFromIndex(i) &&
                    obj.GetTimestampFromIndex(i) <= GetTimestampFromIndex(i)) {     //if the local data is more up-to-date, then ignore
                System.out.printf("Var%d is different for Player %d%n", i+1, obj.GetPlayerId());
                NodeUpdateProcessing.AddCacheMiss();
                totalStaleness += GetTimestampFromIndex(i) - obj.GetTimestampFromIndex(i);  //perfect - local staleness
                System.out.printf("Adding %d staleness%n", GetTimestampFromIndex(i) - obj.GetTimestampFromIndex(i));
            }
            else {
                NodeUpdateProcessing.AddCacheHit();
            }
        }

        return totalStaleness;
    }
}
