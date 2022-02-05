import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NodeGeneration {

    private final int MIN_DIST = 15;

    private Random r;
    private Node[] nodes;

    private int neighbour1 = -1, neighbour2 = -1;

    public NodeGeneration(long seed, int numNodes, int currentNode) {
        r = new Random(seed);
        nodes = new Node[numNodes];

        GenerateNodes(numNodes, currentNode);
    }

    private void GenerateNodes(int numNodes, int currentNode) {

        //create nodes with random position
        int areaSize = (int) (Math.sqrt(numNodes) * 30);

        //place nodes
        for(int i = 0; i < numNodes; i++) {

            int x = r.nextInt(areaSize + 1);
            int y = r.nextInt(areaSize + 1);

            while(!NodePlacementValid(x, y, i-1)) {

                x = r.nextInt(areaSize + 1);
                y = r.nextInt(areaSize + 1);
            }

            nodes[i] = new Node(x, y);
            //System.out.printf("Created a new node %d%n", i);
        }

        int x = nodes[currentNode].GetPositionX();
        int y = nodes[currentNode].GetPositionY();

        //generate latency from current node
        for(int i = 0; i < nodes.length; i++) {
            if(i == currentNode) {
                continue;
            }

            double distance = GetEuclideanDistance(x, nodes[i].GetPositionX(), y, nodes[i].GetPositionY());
            //System.out.printf("Distance is %d%n", (int)distance);
            nodes[i].SetLatency((int)distance);
        }

        //find the closest 2 nodes to set as neighbours
        if(currentNode > 1) {
            neighbour1 = 0;
            neighbour2 = 1;
        }
        else if(currentNode == 0) {
            neighbour1 = 1;
            neighbour2 = 2;
        }
        else {
            neighbour1 = 0;
            neighbour2 = 2;
        }

        for(int i = neighbour2+1; i < nodes.length; i++) {
            if(i == currentNode) {
                continue;
            }

            //if the latency value is lower than either currently
            if(nodes[i].GetLatency() < nodes[neighbour1].GetLatency() ||
                    nodes[i].GetLatency() < nodes[neighbour2].GetLatency()) {

                //if 1 is higher than 2 then replace 1, else replace 2
                if(nodes[neighbour1].GetLatency() > nodes[neighbour2].GetLatency()) {
                    neighbour1 = i;
                }
                else {
                    neighbour2 = i;
                }
            }
        }

        //order neighbours so 1 has a lower index
        if(neighbour2 < neighbour1) {
            int temp = neighbour1;
            neighbour1 = neighbour2;
            neighbour2 = temp;
        }
    }

    //Can the generated position value be placed in the space and respect min distance
    private boolean NodePlacementValid(int x, int y, int placedNodes) {

        for(int i = 0; i <= placedNodes; i++) {

            if(GetEuclideanDistance(x, nodes[i].GetPositionX(), y, nodes[i].GetPositionY()) < MIN_DIST) {
                return false;
            }
        }

        return true;
    }

    private boolean NodePlacementValid(int x, int y) {
        return NodePlacementValid(x, y, nodes.length);
    }

    public static double GetEuclideanDistance(int x1, int y1, int x2, int y2) {

        return Math.sqrt(((x2-x1)*(x2-x1)) + ((y2-y1)*(y2-y1)));
    }

    public Node[] GetNodes() {
        return nodes;
    }

    public void PrintNodeData() {
        System.out.println("Generated Nodes: ");

        for(int i = 0; i < nodes.length; i++) {
            System.out.println(nodes[i]);
        }
    }

    public int GetNeighbour1() {
        return neighbour1;
    }

    public int GetNeighbour2() {
        return neighbour2;
    }

    public int GetNeighbour1Latency() {
        return nodes[neighbour1].GetLatency();
    }

    public int GetNeighbour2Latency() {
        return nodes[neighbour2].GetLatency();
    }


}
