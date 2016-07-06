package abr.main;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.IntBuffer;
import java.util.Dictionary;

//============================================================================
// SpiNNakerReceiver_thread
//============================================================================
public class SpiNNakerReceiver_thread implements Runnable {
    private static final String LogTag = "SpiNNakerReceiver_thread";

    public SpiNNakerReceiver_thread(int port, int fixedPointPosition, Handler handler)
    {
        m_Port = port;
        m_Running = false;
        m_Handler = handler;

        // Calculate fixed-point scale
        m_FixedPointScale = 1.0f / (float)(1 << fixedPointPosition);
    }

    @Override
    public void run()
    {
        try {
            // Create socket
            DatagramSocket socket = new DatagramSocket(m_Port);

            // Create SpiNNaker-sized buffer
            byte[] buffer = new byte[512];

            // Read data
            while (true) {
                // Create datagram using buffer for storage
                DatagramPacket receivedDatagram = new DatagramPacket(buffer, buffer.length);

                // Attempt to read datagram
                socket.receive(receivedDatagram);

                // Parse SCP packet
                SCPPacket packet = new SCPPacket(receivedDatagram.getData());

                // If there is further data in packet
                if(packet.get_Data().hasRemaining())
                {
                    // Get hash of source population
                    int sourcePopulation = packet.get_Arg1();

                    // Convert payload into floating point
                    int[] fixedPointPayload = packet.get_Data().asIntBuffer().array();
                    float[] floatingPointPayload = new float[fixedPointPayload.length];
                    for(int i = 0; i < fixedPointPayload.length; i++)
                    {
                        floatingPointPayload[i] = m_FixedPointScale * (float)fixedPointPayload[i];
                    }

                    // Build bundle containing source population
                    Bundle bundle = new Bundle();
                    bundle.putInt("sourcePopulation", sourcePopulation);
                    bundle.putFloatArray("payload", floatingPointPayload);

                    // Attach bundle to message and send to handler
                    Message msg = Message.obtain();
                    msg.setData(bundle);
                    m_Handler.sendMessage(msg);
                }
            }

            // Close socket
            //socket.close();
        }
        catch(SocketException e) {
            // TODO intelligent error handling
        }
        catch(IOException e) {
            // TODO intelligent error handling
        }
    }

    //============================================================================
    // Memnbers
    //============================================================================
    private int m_Port;
    private float m_FixedPointScale;
    private boolean m_Running;
    private Handler m_Handler;
}
