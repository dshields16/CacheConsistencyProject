/*
    Store frequency data associated with a player data object
 */
public class PlayerDataFrequency {

    private int[] totals;
    private int updatesMade = 0;

    private int timeBetweenUpdates = 200;

    /*
        timeBetweenUpdates - the interval between data generation

        creates a data frequency object
     */
    public PlayerDataFrequency(int timeBetweenUpdates) {
        this.timeBetweenUpdates = timeBetweenUpdates;
        totals = new int[DataGeneration.numberOfVariables];
    }

    /*
        varId - the id of the var which was updated

        Increment update total for the specified variable
     */
    public void UpdateMade(int varId) {
        totals[varId]++;

        updatesMade++;
    }

    /*
        varId - the id of the var to get frequency value

        Returns the base TTL value for the specified variable
     */
    public int GetUpdateFrequencyForVar(int varId) {
        float freq = GetFrequencyFromId(varId);

        return GetTimeFromFrequency(freq);
    }

    /*
        freq - frequency value of a variable

        Returns an adaptive TTL value using the specified frequency
     */
    private int GetTimeFromFrequency(float freq)
    {
        if(updatesMade < 5){
            return timeBetweenUpdates;  //not enough sample size
        }

        if(freq >= 0.4)
            return 3*timeBetweenUpdates;
        else if(freq >= 0.1)
            return 2*timeBetweenUpdates;

        return 1*timeBetweenUpdates;
    }

    /*
        varId - the variable id to get the frequency value of

        return the frequency value of the specified variable
     */
    private float GetFrequencyFromId(int varId) {

        return (float)totals[varId] / updatesMade;
    }

    /*
        id - the variable id to print

        Print the frequency details of the specified variable
     */
    public void OutputFrequency(int id) {

        String output = String.format("Player %d with %d Updates: ",
                id, updatesMade);

        for(int i = 0; i < DataGeneration.numberOfVariables; i++) {

            output += String.format("%.2f/%d ", GetFrequencyFromId(i), totals[i]);
        }

        System.out.println(output);
    }


}
