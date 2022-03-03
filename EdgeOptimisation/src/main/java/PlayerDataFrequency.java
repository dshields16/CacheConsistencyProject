public class PlayerDataFrequency {

    private int var1Total = 0, var2Total = 0, var3Total = 0, var4Total = 0, var5Total = 0, updatesMade = 0;

    private int timeBetweenUpdates = 200;

    public PlayerDataFrequency(int timeBetweenUpdates) {
        this.timeBetweenUpdates = timeBetweenUpdates;
    }

    //Store total updates made
    public void UpdateMade(int varId) {
        switch(varId) {
            case 0:
                var1Total++;
                break;
            case 1:
                var2Total++;
                break;
            case 2:
                var3Total++;
                break;
            case 3:
                var4Total++;
                break;
            case 4:
                var5Total++;
                break;
        }

        updatesMade++;
    }

    //Vars with a low frequency value should be updated more than values with a high frequency value
    public int GetUpdateFrequencyForVar(int varId) {
        float freq = GetFrequencyFromId(varId);

        return GetTimeFromFrequency(freq);
    }

    private int GetTimeFromFrequency(float freq)
    {
        if(freq >= 0.4)
            return 2*timeBetweenUpdates;
        else if(freq >= 0.1)
            return timeBetweenUpdates;

        return 0;
    }

    private float GetFrequencyFromId(int varId) {
        switch(varId) {
            case 0:
                return (float)var1Total / updatesMade;
            case 1:
                return (float)var2Total / updatesMade;
            case 2:
                return (float)var3Total / updatesMade;
            case 3:
                return (float)var4Total / updatesMade;
            case 4:
                return (float)var5Total / updatesMade;
        }
        return 0;
    }

    public void OutputFrequency(int id) {


        System.out.printf("Player %d with %d Updates: %.2f/%d %.2f/%d %.2f/%d %.2f/%d %.2f/%d%n", id, updatesMade,
                GetFrequencyFromId(0), var1Total, GetFrequencyFromId(1), var2Total, GetFrequencyFromId(2),
                var3Total, GetFrequencyFromId(3), var4Total, GetFrequencyFromId(4), var5Total);
    }


}
