package abr.main;


import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import abr.main.R;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Main_activity extends Activity	// implements IOIOLooperProvider: from IOIOActivity
{
	private static final String LogTag = "Main_activity";

	RelativeLayout layout_left_joystick;
	ImageView image_joystick, image_border;

	JoyStickClass js_left;

	// Socket for sending datagrams back to SpiNNaker
	DatagramSocket m_SpiNNakerTransmitterSocket;

	private SpiNNakerReceiver_thread m_SpiNNakerReceiverThread;
	private Handler m_SpiNNakerReceiverHandler;

	private TextView m_SpiNNakerAddressText;

	private InetAddress m_SpiNNakerAddress;

	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		layout_left_joystick = (RelativeLayout)findViewById(R.id.layout_left_joystick); //inverted for some reason

		js_left = new JoyStickClass(getApplicationContext(), layout_left_joystick, R.drawable.image_button);
		js_left.setStickSize(150, 150);
//		js_left.setLayoutSize(300, 300);
		js_left.setLayoutAlpha(150);
		js_left.setStickAlpha(100);
		js_left.setOffset(90);
		js_left.setMinimumDistance(50);

		layout_left_joystick.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View arg0, MotionEvent arg1) 
			{
//				Log.e("abr controller", "left joystick");
				
				js_left.drawStick(arg1);
				if(arg1.getAction() == MotionEvent.ACTION_DOWN	|| arg1.getAction() == MotionEvent.ACTION_MOVE) 
				{
					setJoystick(50012, js_left.getX(), js_left.getY(), false);
				}
				else if(arg1.getAction() == MotionEvent.ACTION_UP) //user stopped touching screen on layout
				{
					setJoystick(50012, 0.0f, 0.0f, true);
				}
				return true;
			}
		});

		// Initially invalidate SpiNNaker address
		m_SpiNNakerAddress = null;
		// Create
		try
		{
			m_SpiNNakerTransmitterSocket = new DatagramSocket();
		}
		catch(SocketException e)
		{
			Log.e(LogTag, String.format("Socket exception %s", e.toString()));
		}

		m_SpiNNakerAddressText = (TextView) findViewById(R.id.spinnaker_address);

		// Create SpiNNaker event handler
		m_SpiNNakerReceiverHandler =
				new Handler()
				{
					@Override
					public void handleMessage(Message msg) {
						m_SpiNNakerAddress = (InetAddress)msg.getData().getSerializable("address");

						// Stick SpiNNaker address in UI
						if(m_SpiNNakerAddressText != null)
						{
							m_SpiNNakerAddressText.setText(m_SpiNNakerAddress.toString());
						}

						// Superclass
						super.handleMessage(msg);
					}
				};

		// Create a thread to receive UDP from SpiNNaker
		m_SpiNNakerReceiverThread = new SpiNNakerReceiver_thread(50007, 15, m_SpiNNakerReceiverHandler);
		m_SpiNNakerReceiverThread.start();
	}

	private void setJoystick(int port, float x, float y, boolean released)
	{
		// If we have a SpiNNaker transmitter socket and address
		if(m_SpiNNakerTransmitterSocket != null && m_SpiNNakerAddress != null)
		{
			// Create a byte buffer for payload
			ByteBuffer payload = ByteBuffer.allocate((2 * 2) + (3 * 4) + (3 * 4));
			payload.order(ByteOrder.LITTLE_ENDIAN);

			// Write SCP header
			payload.putShort((short)0);	// CmdRC
			payload.putShort((short)0);	// Seq
			payload.putInt(0);			// Arg1
			payload.putInt(0);			// Arg2
			payload.putInt(0);			// Arg3

			// Loop through sensor values
			final float fixedPointScale =  (float)(1 << 15);
			payload.putInt((int)Math.round(x * fixedPointScale));
			payload.putInt((int)Math.round(y * fixedPointScale));
			payload.putInt(released ? (int)fixedPointScale : 0);

			// Create packet
			DatagramPacket packet = new DatagramPacket(payload.array(), payload.position());
			packet.setPort(port);
			packet.setAddress(m_SpiNNakerAddress);

			// Send packet asynchronously
			new AsyncTask<DatagramPacket, Void, Void>()
			{
				@Override
				protected Void doInBackground(DatagramPacket... packets)
				{
					// Send over socket
					try
					{
						m_SpiNNakerTransmitterSocket.send(packets[0]);
					}
					catch(IOException e)
					{
						Log.e(LogTag, String.format("IO exception %s", e.toString()));
					}
					return null;
				}
			}.execute(packet);

		}
	}

}
