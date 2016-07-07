package abr.main;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

//============================================================================
// SDPPacket
//============================================================================
// SpiNNaker data packet
public class SDPPacket
{
    //============================================================================
    // Constants
    //============================================================================
    private static final int FLAG_REPLY = 0x87;
    private static final int FLAG_NO_REPLY = 0x07;

    public SDPPacket(byte[] data, int length)
    {
        // Wrap header bytes from incoming data into byte buffer
        // **NOTE** ignore first two bytes
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(data, 2, 8);
        headerByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Read header words
        int flags = readU8(headerByteBuffer);
        m_Tag = readU8(headerByteBuffer);
        int destCPUPort = readU8(headerByteBuffer);
        int srcCPUPort = readU8(headerByteBuffer);
        m_DestY = readU8(headerByteBuffer);
        m_DestX = readU8(headerByteBuffer);
        m_SrcY = readU8(headerByteBuffer);
        m_SrcX = readU8(headerByteBuffer);

        // Unpack packed header words
        m_DestCPU = destCPUPort & 0x1F;
        m_DestPort = destCPUPort >> 5;
        m_SrcCPU = srcCPUPort & 0x1F;
        m_SrcPort = srcCPUPort >> 5;

        m_ReplyExpected = (flags == FLAG_REPLY);

        // Wrap payload in second byte buffer
        m_Data = ByteBuffer.wrap(data, 10, length - 10);
        m_Data.order(ByteOrder.LITTLE_ENDIAN);
    }

    //============================================================================
    // Getters
    //============================================================================
    public int get_Tag() {
        return m_Tag;
    }

    public int get_DestCPU() {
        return m_DestCPU;
    }

    public int get_DestPort() {
        return m_DestPort;
    }

    public int get_DestX() {
        return m_DestX;
    }

    public int get_DestY() {
        return m_DestY;
    }

    public int get_SrcCPU() {
        return m_SrcCPU;
    }

    public int get_SrcPort() {
        return m_SrcPort;
    }

    public int get_SrcX() {
        return m_SrcX;
    }

    public int get_SrcY() {
        return m_SrcY;
    }

    public boolean is_ReplyExpected() {
        return m_ReplyExpected;
    }

    public ByteBuffer get_Data() {
        return m_Data;
    }

    //============================================================================
    // Protected static methods
    //============================================================================
    protected static short readU8(ByteBuffer buffer)
    {
        return ((short) (buffer.get() & 0xFF));
    }

    protected static int readU16(ByteBuffer buffer)
    {
        return ((int) (buffer.getShort() & 0xFFFF));
    }

    protected static long readU32(ByteBuffer buffer)
    {
        return ((long) (buffer.getInt() & 0xFFFFFFFFL));
    }
    //============================================================================
    // Private members
    //============================================================================
    private int m_Tag;
    private int m_DestCPU;
    private int m_DestPort;
    private int m_DestX;
    private int m_DestY;
    private int m_SrcCPU;
    private int m_SrcPort;
    private int m_SrcX;
    private int m_SrcY;

    private boolean m_ReplyExpected;

    private ByteBuffer m_Data;
}
