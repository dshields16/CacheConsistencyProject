/*
    Used to store and manage a user's cache data
 */
public class PlayerDataObject {

    private short[] vars;   //variable values
    private short playerId = -1, currentNodeId = -1;
    private long[] varsTs;  //variable timestamps

    /*
        playerId - id for this object
        currentNodeId - the current node being used by this user
        timestamp - the time at which this object was created

        Creates a new object instance
     */
    public PlayerDataObject(int playerId, int currentNodeId, short timestamp) {

        this.playerId = (short) playerId;
        this.currentNodeId = (short) currentNodeId;

        vars = new short[DataGeneration.numberOfVariables];
        varsTs = new long[DataGeneration.numberOfVariables];

        //set all timestamps to a default when the user is created
        for(int i = 0; i < DataGeneration.numberOfVariables; i++) {
            varsTs[i] = timestamp;
        }

    }

    /*
        obj - an existing object instance

        Creates a copy of an existing data object
     */
    public PlayerDataObject(PlayerDataObject obj) {

        this.playerId = obj.GetPlayerId();
        this.currentNodeId = obj.GetCurrentNodeId();

        vars = new short[DataGeneration.numberOfVariables];
        varsTs = new long[DataGeneration.numberOfVariables];

        for(int i = 0; i < DataGeneration.numberOfVariables; i++) {
            SetVarFromIndex(i, obj.GetVarFromIndex(i), obj.GetTimestampFromIndex(i));
        }
    }

    /*
        index - index of the variable timestamp

        returns the timestamp of the variable with the specified index
     */
    public long GetTimestampFromIndex(int index) {

        return varsTs[index];
    }

    /*
        index - index of the variable timestamp

        returns the value of the variable with the specified index
     */
    public short GetVarFromIndex(int index) {

        return vars[index];
    }

    /*
        varIndex - index of te variable to update
        newValue - the new value to be assigned to the variable
        timestamp - the time which the change was made

        Sets the variable and timestamp to the new value and timestamp
     */
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

    /*
        compare this object with another, sum the timestamp difference and return
     */
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
