package main.java;

public class Unit {

    //Each player controls a list of units and has authority to change their data

    short positionX, positionY, velocityX, velocityY, healthValue, ownerPeerId, unitId;

    public Unit(short owner, short id){
        positionX = 0;
        positionY = 0;
        velocityX = 0;
        velocityY = 0;
        healthValue = 100;
        ownerPeerId = owner;
        unitId = id;
    }
}
