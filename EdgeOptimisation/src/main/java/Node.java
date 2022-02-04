public class Node {

    private int posX, posY, latency;    //latency measured from the current node

    public Node(int x, int y) {

        posX = x;
        posY = y;
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
}
