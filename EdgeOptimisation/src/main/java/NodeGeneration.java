import java.util.Random;

/*
    Generates a network environment on which to base socket connections and latency
 */
public class NodeGeneration {

    private final int MIN_DIST = 15;

    private Random r;
    private Node[] nodes;

    /*
        seed - random seed used
        numNodes - number of nodes in the network
        currentNode - the node id of this process

        Calls the network layout generation method
     */
    public NodeGeneration(long seed, int numNodes, int currentNode) {
        r = new Random(seed);
        nodes = new Node[numNodes];

        GenerateNodes(numNodes, currentNode);
    }

    /*
        numNodes - number of nodes in the network
        currentNode - the node id of this process

        Generates the neighbours for each node and the latency of each connection
     */
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

            nodes[i] = new Node(x, y, i);
        }

        int x = nodes[currentNode].GetPositionX();
        int y = nodes[currentNode].GetPositionY();

        //generate latency from current node
        for(int i = 0; i < nodes.length; i++) {
            if(i == currentNode) {
                continue;
            }

            double distance = GetEuclideanDistance(x, nodes[i].GetPositionX(), y, nodes[i].GetPositionY());
            nodes[i].SetLatency((int)distance);
        }

        //ensure each node has 2 neighbours
        int[] neighbourCount = new int[nodes.length];

        //generate neighbours for each node
        for(int i = 0; i < nodes.length; i++) {

            //control incrementing of num neighbours
            boolean oneStartsNull = nodes[i].neighbour1 == null;

            if(nodes[i].neighbour2 != null){
                continue;
            }

            int startingIndex = 0;
            if(!oneStartsNull) {
                startingIndex = nodes[i].neighbour1.GetId() + 1;
            }

            //loop and assign base neighbours
            for(int minNodes = 1; minNodes < 3; minNodes++) {
                for (int j = startingIndex; j < nodes.length; j++) {

                    if (j == i)
                        continue;

                    if (nodes[i].neighbour1 == null && neighbourCount[j] < minNodes) {
                        nodes[i].neighbour1 = nodes[j];
                        continue;
                    }

                    if (nodes[i].neighbour2 == null && neighbourCount[j] < minNodes) {
                        nodes[i].neighbour2 = nodes[j];
                        continue;
                    }
                }
            }

            //loop through each other available node index and assign a new neighbour if it has a lower latency
            for(int j = nodes[i].neighbour2.GetId() + 1; j < nodes.length; j++) {
                if(j == i) {
                    continue;
                }

                //if the latency value is lower than either currently
                if(GetDistanceBetweenNodes(nodes[i], nodes[j]) < GetDistanceBetweenNodes(nodes[i], nodes[i].neighbour1) ||
                        GetDistanceBetweenNodes(nodes[i], nodes[j]) < GetDistanceBetweenNodes(nodes[i], nodes[i].neighbour2)) {

                    //if 1 is higher than 2 then replace 1, else replace 2
                    if(GetDistanceBetweenNodes(nodes[i], nodes[i].neighbour1) > GetDistanceBetweenNodes(nodes[i], nodes[i].neighbour2) ) {

                        if(neighbourCount[j] > neighbourCount[nodes[i].neighbour1.GetId()] || !oneStartsNull){
                            continue;
                        }
                        nodes[i].neighbour1 = nodes[j];
                    }
                    else {
                        if(neighbourCount[j] > neighbourCount[nodes[i].neighbour2.GetId()]){
                            continue;
                        }
                        nodes[i].neighbour2 = nodes[j];
                    }
                }
            }

            //set this node as neighbour for new neighbours
            int neighbour1Id = nodes[i].neighbour1.GetId();
            if(neighbour1Id > i) {
                if(neighbourCount[neighbour1Id] == 0)
                    nodes[neighbour1Id].neighbour1 = nodes[i];
                else
                    nodes[neighbour1Id].neighbour2 = nodes[i];
            }

            int neighbour2Id = nodes[i].neighbour2.GetId();
            if(neighbourCount[neighbour2Id] == 0)
                nodes[neighbour2Id].neighbour1 = nodes[i];
            else
                nodes[neighbour2Id].neighbour2 = nodes[i];

            //increment neighbour total
            if(oneStartsNull) {
                neighbourCount[i]++;
                neighbourCount[nodes[i].neighbour1.GetId()]++;
            }

            neighbourCount[i]++;
            neighbourCount[nodes[i].neighbour2.GetId()]++;

            //order neighbours so 1 has a lower index
            if(nodes[i].neighbour2.GetId() < nodes[i].neighbour1.GetId()) {
                int temp = nodes[i].neighbour1.GetId();
                nodes[i].neighbour1 = nodes[i].neighbour2;
                nodes[i].neighbour2 = nodes[temp];
            }
        }


    }

    /*
        x - x position of node
        y - y position of node
        placedNodes - the number of nodes in the network

        return true if the generated position value be placed in the space and respect min distance
     */
    private boolean NodePlacementValid(int x, int y, int placedNodes) {

        for(int i = 0; i <= placedNodes; i++) {

            if(GetEuclideanDistance(x, nodes[i].GetPositionX(), y, nodes[i].GetPositionY()) < MIN_DIST) {
                return false;
            }
        }

        return true;
    }

    /*
        n1 - first node
        n2 - second node

        returns the euclidean distance between two nodes
     */
    public double GetDistanceBetweenNodes(Node n1, Node n2) {
        return GetEuclideanDistance(n1.GetPositionX(), n1.GetPositionY(), n2.GetPositionX(), n2.GetPositionY());
    }

    /*
        x1 - first x pos
        y1 - first y pos
        x2 - second x pos
        y2 - second y pos

        returns the euclidean distance between two sets of coordinates
     */
    public static double GetEuclideanDistance(int x1, int y1, int x2, int y2) {

        return Math.sqrt(((x2-x1)*(x2-x1)) + ((y2-y1)*(y2-y1)));
    }

    /*
        return the nodes array
     */
    public Node[] GetNodes() {
        return nodes;
    }

    /*
        Print the details of the nodes array
     */
    public void PrintNodeData() {
        System.out.println("Generated Nodes: ");

        for(int i = 0; i < nodes.length; i++) {
            System.out.println(nodes[i]);
        }
    }

    /*
        nodeId - node being searched

        Get the id of the first neighbour for the node
     */
    public int GetNeighbour1(int nodeId) {
        return nodes[nodeId].neighbour1.GetId();
    }

    /*
        nodeId - node being searched

        Get the id of the second neighbour for the node
     */
    public int GetNeighbour2(int nodeId) {
        return nodes[nodeId].neighbour2.GetId();
    }

    /*
        nodeId - node being searched

        Get the latency of the first neighbour for the node
     */
    public int GetNeighbour1Latency(int nodeId) {
        return nodes[nodeId].GetLatencyForNeighbour(0);
    }

    /*
        nodeId - node being searched

        Get the latency of the second neighbour for the node
     */
    public int GetNeighbour2Latency(int nodeId) {
        return nodes[nodeId].GetLatencyForNeighbour(1);
    }


}
