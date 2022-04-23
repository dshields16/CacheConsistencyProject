public class PlayerDataFrequency {

    private int[] totals;
    private int updatesMade = 0;

    private int timeBetweenUpdates = 200;

    public PlayerDataFrequency(int timeBetweenUpdates) {
        this.timeBetweenUpdates = timeBetweenUpdates;
        totals = new int[DataGeneration.numberOfVariables];
    }

    //Store total updates made
    public void UpdateMade(int varId) {
        totals[varId]++;

        updatesMade++;
    }

    //Vars with a low frequency value should be updated more than values with a high frequency value
    public int GetUpdateFrequencyForVar(int varId) {
        float freq = GetFrequencyFromId(varId);

        return GetTimeFromFrequency(freq);
    }

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

    private float GetFrequencyFromId(int varId) {

        return (float)totals[varId] / updatesMade;
    }

    public void OutputFrequency(int id) {

        String output = String.format("Player %d with %d Updates: ",
                id, updatesMade);

        for(int i = 0; i < DataGeneration.numberOfVariables; i++) {

            output += String.format("%.2f/%d ", GetFrequencyFromId(i), totals[i]);
        }

        System.out.println(output);
    }


}
