import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class ByteArrayPackingTest {

    /*
        Test a short array can be converted to byte array and back
     */
    @Test
    void Pack() {

        //generate a message packet
        PeerService peerService = new PeerService((short) 0);
        short[] packetData = peerService.GenerateUpdatePacket((short) 0);

        //convert short array to byte array which can be sent over a socket
        ByteBuffer buffer = ByteBuffer.allocate(2*packetData.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.asShortBuffer().put(packetData);
        byte[] bytes = buffer.array();

        //convert packet back into a short array
        short[] sendPacket = new short[packetData.length];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sendPacket);

        assertArrayEquals(packetData, sendPacket);
    }

}