package main.java;

import java.util.Arrays;

public class UnitTTL {

    /*
            Each Unit maps to a UnitTTL for each client which controls when each value should be sent
     */

    short positionX, positionY, velocityX, velocityY, healthValue, clientId, unitId;

    RelevanceMetric rm;

    public UnitTTL(short owner, short id){
        positionX = 0;
        positionY = 0;
        velocityX = 0;
        velocityY = 0;
        healthValue = 0;
        clientId = owner;
        unitId = id;
    }

    //init the TTL values
    public void SetTTL(short positionTTL, short velocityTTL, short healthTTL) {

        positionX = positionTTL;
        positionY = positionTTL;
        velocityX = velocityTTL;
        velocityY = velocityTTL;
        healthValue = healthTTL;
    }

    //TTL values ticked down every 200ms
    public void TickTTL() {

        positionX--;
        positionY--;
        velocityX--;
        velocityY--;
        healthValue--;

        //send owner id, var and get base value
    }

    //get a list of values to update for this unit using TTL values
    public int[] GetVarsToSend() {

        int[] vars = new int[5];
        int size = 0;

        for(int i = 0; i < 5; i++) {

            if(GetVarFromIndex(i) <= 0){
                vars[size++] = i;                                   //add var to list to update
                SetVarFromIndex(i, rm.GetTTLForVar(i, clientId));       //reset TTL
            }
        }

        return Arrays.copyOfRange(vars, 0, size);
    }

    private short GetVarFromIndex(int index) {
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

    private void SetVarFromIndex(int index, short value) {
        switch (index) {
            case 0:
                positionX = value;
            case 1:
                positionY = value;
            case 2:
                velocityX = value;
            case 3:
                velocityY = value;
            case 4:
                healthValue = value;

        }
    }
}
