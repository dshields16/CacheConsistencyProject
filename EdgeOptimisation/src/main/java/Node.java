/*
    Base class for an edge node
 */
public class Node {

    private int posX, posY, latency, id;    //latency measured from the current node

    public Node neighbour1, neighbour2;

    public Node(int x, int y, int id) {

        posX = x;
        posY = y;
        this.id = id;
    }

    public int GetPositionX() {
        return posX;
    }

    public int GetPositionY() {
        return posY;
    }

    public void SetLatency(int value) {
        latency = value;
    }

    public int GetLatency() {
        return latency;
    }

    public String toString() {
        return String.format("Node at position %d, %d", posX, posY);
    }

    public int GetId() {
        return id;
    }

    public int GetLatencyForNeighbour(int index) {
        if(index == 0) {
            return neighbour1.GetLatency();
        } else {
            return neighbour2.GetLatency();
        }
    }
}
