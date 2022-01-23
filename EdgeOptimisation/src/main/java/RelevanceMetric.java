import org.junit.jupiter.api.parallel.Resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RelevanceMetric {

    /*
            Relevance Metric, used in server only
            Each client has an RM value for each var, e.g. pos_RM index 0 is RM value
            for position values being sent to client id 0

            TTL counts down until message send, then reset to a value defined by the RM
            0 RM = ASAP
            x+   = Send every x "ticks" tick=200ms

            Each client has a TTL value for each var for each unit, when TTL reaches 0
            the value should be sent.

     */

    private static final int BASE_POS_RM = 0, BASE_VEL_RM = 3, BASE_HEALTH_RM = 5;

    private int numClients, serverId = -1;
    private short[] pos_RM, vel_RM, healthValue_RM;

    //each client has a list of unit ttl which map to their list of units stored
    private List<UnitTTL>[] timeValues;

    //index 0 represents state of network between server and client 0,
    // 1 is normal and scales up as it gets worse
    int[] networkConditions;

    public RelevanceMetric(int noClients, int serverId) {

        this.numClients = noClients;
        this.serverId = serverId;

        timeValues = new ArrayList[numClients];
        for(int i = 0; i < numClients; i++)
            timeValues[i] = new ArrayList<>();

        pos_RM = new short[numClients];
        Arrays.fill(pos_RM, (short)BASE_POS_RM);
        vel_RM = new short[numClients];
        Arrays.fill(vel_RM, (short)BASE_VEL_RM);
        healthValue_RM = new short[numClients];
        Arrays.fill(healthValue_RM, (short)BASE_HEALTH_RM);

        networkConditions = new int[numClients];
        Arrays.fill(networkConditions, 1);
    }

    //create a new unit ttl and add to all client lists except owner and server
    public void AddUnit(short ownerId, short unitId) {

        for(int i = 0; i < numClients; i++) {

            if(i == ownerId || i == serverId)
                continue;

            UnitTTL ttl = new UnitTTL(ownerId, unitId, this);
            ttl.SetTTL(pos_RM[ownerId], vel_RM[ownerId], healthValue_RM[ownerId]);

            timeValues[i].add(ttl);
        }
    }

    //get the base ttl value for a variable being sent to a client
    public short GetTTLForVar(int varId, short clientId) {

        switch (varId) {
            case 0:
            case 1:
                return pos_RM[clientId];
            case 2:
            case 3:
                return vel_RM[clientId];
            case 4:
                return healthValue_RM[clientId];

        }
        return 0;
    }

    //update the RM values after network conditions are changed
    public void UpdateRM() {

        for(int i = 0; i < numClients; i++) {
            if(i == serverId)
                continue;

            pos_RM[i] = (short)         (BASE_POS_RM * networkConditions[i]);
            vel_RM[i] = (short)         (BASE_VEL_RM * networkConditions[i]);
            healthValue_RM[i] = (short) (BASE_HEALTH_RM * networkConditions[i]);
        }
    }

    public List<UnitTTL> GetTTLValues(int clientId) {
        return timeValues[clientId];
    }

    /*
        Tick down the TTL values for stored values
     */
    public void ProgressTTL() {

        for(int i = 0; i < numClients; i++) {

            for (UnitTTL ttl: timeValues[i]) {
                ttl.TickTTL();
            }
        }
    }

}
