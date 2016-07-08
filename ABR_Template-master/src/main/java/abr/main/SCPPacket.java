package abr.main;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

//============================================================================
// SCPPacket
//============================================================================
// SpiNNaker control packet
public class SCPPacket extends SDPPacket
{
    public SCPPacket(byte[] data, int length)
    {
        this(data, length, 3);
    }

    /*public SCPPacket(boolean replyExpected, int tag, int destPort, int destCPU,
                     int srcPort, int srcCPU, int destX, int destY, int srcX, int srcY,
                     int cmdRC, int seq, int arg1, int arg2, int arg3, ByteBuffer data)
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate((4 * 3) + data.position());
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        super(replyExpected, tag, destPort, destCPU, srcPort, srcCPU,
                destX, destY, srcX, srcY, byteBuffer);
    }*/

    public SCPPacket(byte[] data, int length, int numArguments)
    {
        super(data, length);

        // Read command
        m_CmdRC = readU16(get_Data());
        m_Seq = readU16(get_Data());

        // Read argument words
        int dataLen = get_Data().remaining();
        if(numArguments >= 1 && dataLen >= 4)
        {
            m_Arg1 = get_Data().getInt();

            if(numArguments >= 2 && dataLen >= 8)
            {
                m_Arg2 = get_Data().getInt();

                if(numArguments >= 3 && dataLen >= 12)
                {
                    m_Arg3 = get_Data().getInt();
                }
            }
        }
    }

    //============================================================================
    // Getters
    //============================================================================
    public int get_Arg1() {
        return m_Arg1;
    }

    public int get_Arg2() {
        return m_Arg2;
    }

    public int get_Arg3() {
        return m_Arg3;
    }

    public int get_CmdRC() {
        return m_CmdRC;
    }

    public int get_Seq() {
        return m_Seq;
    }

    //============================================================================
    // Members
    //============================================================================
    private int m_Arg1;
    private int m_Arg2;
    private int m_Arg3;
    private int m_CmdRC;
    private int m_Seq;
}
