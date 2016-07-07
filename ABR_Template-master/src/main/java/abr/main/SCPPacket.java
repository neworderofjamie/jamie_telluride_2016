package abr.main;
//============================================================================
// SCPPacket
//============================================================================
// SpiNNaker control packet
public class SCPPacket extends SDPPacket
{
    public SCPPacket(byte[] data)
    {
        this(data, 3);
    }

    public SCPPacket(byte[] data, int numArguments)
    {
        super(data);

        // Read command
        m_CmdRC = (int)get_Data().getShort();
        m_Seq = (int)get_Data().getShort();

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
