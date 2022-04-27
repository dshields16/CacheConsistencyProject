import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/*
    Test the node generation module
 */
class NodeGenerationTest {

    private static NodeGeneration nodeGen;
    private static long seed;
    private static int numNodes = 4;

    @BeforeAll
    static void Setup() {
        Random r = new Random();
        seed = r.nextLong();

        nodeGen = new NodeGeneration(seed, numNodes, 0);
    }

    @Test
    void GenerateNodesTest() {

        nodeGen.PrintNodeData();

        assertTrue(nodeGen.GetNodes().length == numNodes);
    }

    @Test
    void EuclideanDistanceCalculation() {
        int x1 = 4, x2 = 31, y1 = 57, y2 = 62;

        double distance = NodeGeneration.GetEuclideanDistance(x1, y1, x2, y2);
        System.out.println(distance);

        assertTrue((int)distance == 27);
    }

    @Test
    void CalculateNeighboursTest() {

        System.out.printf("Neighbour 1 is: %d with latency %d%n", nodeGen.GetNeighbour1(0), nodeGen.GetNeighbour1Latency(0));
        System.out.printf("Neighbour 2 is: %d with latency %d%n", nodeGen.GetNeighbour2(0), nodeGen.GetNeighbour2Latency(0));

        assertTrue(nodeGen.GetNeighbour1(0) != -1 && nodeGen.GetNeighbour1(0) != 0);
        assertTrue(nodeGen.GetNeighbour2(0) != -1 && nodeGen.GetNeighbour2(0) != 0);
        assertTrue(nodeGen.GetNeighbour1(0) < nodeGen.GetNeighbour2(0));
    }

    @Test
    void GenerateNeighboursGreaterThanThree() {

        int[] count = new int[numNodes];

        for(int i = 0; i < numNodes; i++) {
            System.out.printf("Node %d has neighbours: %d and %d%n", i, nodeGen.GetNeighbour1(i), nodeGen.GetNeighbour2(i));

            count[nodeGen.GetNeighbour1(i)]++;
            count[nodeGen.GetNeighbour2(i)]++;
        }

        for(int i = 0; i < numNodes; i++) {
            assertTrue(count[i] == 2);
        }

    }

}