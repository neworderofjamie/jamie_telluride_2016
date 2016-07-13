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

    public SDPPacket(boolean replyExpected, int tag, int destPort, int destCPU,
                     int srcPort, int srcCPU, int destX, int destY, int srcX, int srcY,
                     ByteBuffer data)
    {
        m_ReplyExpected = replyExpected;
        m_Tag = tag;
        m_DestPort = destPort;
        m_DestCPU = destCPU;
        m_SrcPort = srcPort;
        m_SrcCPU = srcCPU;
        m_DestX = destX;
        m_DestY = destY;
        m_SrcX = srcX;
        m_SrcY = srcY;
        m_Data = data;
    }

    //============================================================================
    // Public methods
    //============================================================================
    public ByteBuffer writeByteBuffer()
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(10 + m_Data.position());
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Padding
        byteBuffer.put((byte)0);
        byteBuffer.put((byte)0);

        // Write SDP header fields
        writeU8(byteBuffer, m_ReplyExpected ? FLAG_REPLY : FLAG_NO_REPLY);
        writeU8(byteBuffer, m_Tag);
        writeU8(byteBuffer, ((m_DestPort & 0x7) << 5) | (m_DestCPU & 0x1F));
        writeU8(byteBuffer, ((m_SrcPort & 0x7) << 5) | (m_SrcCPU & 0x1F));
        writeU8(byteBuffer, m_DestY);
        writeU8(byteBuffer, m_DestX);
        writeU8(byteBuffer, m_SrcY);
        writeU8(byteBuffer, m_SrcX);

        // Write data
        byteBuffer.put(m_Data);

        // Return new byte buffer
        return byteBuffer;
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

    protected static void writeU8(ByteBuffer buffer, int value) {
        buffer.put((byte)(value & 0xFF));
    }

    protected static void writeU16(ByteBuffer buffer, int value) {
        buffer.putShort((short)(value & 0xFFFF));
    }

    protected static void writeU32(ByteBuffer buffer, long value) {
        buffer.putInt((int)(value & 0xFFFFFFFFL));
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
