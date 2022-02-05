import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class NodeGenerationTest {

    private static NodeGeneration nodeGen;
    private static long seed;

    @BeforeAll
    static void Setup() {
        Random r = new Random();
        //seed = r.nextLong();

        seed = 100;

        nodeGen = new NodeGeneration(seed, 5, 1);
    }

    @Test
    void GenerateNodesTest() {

        nodeGen.PrintNodeData();

        assertTrue(nodeGen.GetNodes().length == 5);
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

        System.out.printf("Neighbour 1 is: %d with latency %d%n", nodeGen.GetNeighbour1(), nodeGen.GetNeighbour1Latency());
        System.out.printf("Neighbour 2 is: %d with latency %d%n", nodeGen.GetNeighbour2(), nodeGen.GetNeighbour2Latency());

        assertTrue(nodeGen.GetNeighbour1() != -1);
        assertTrue(nodeGen.GetNeighbour2() != -1);
        assertTrue(nodeGen.GetNeighbour1() < nodeGen.GetNeighbour2());
    }

}