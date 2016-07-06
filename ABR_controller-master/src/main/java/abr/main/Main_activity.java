package abr.main;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import abr.main.R;

public class Main_activity extends Activity implements IOIOLooperProvider 		// implements IOIOLooperProvider: from IOIOActivity
{
	private final IOIOAndroidApplicationHelper helper_ = new IOIOAndroidApplicationHelper(this, this);			// from IOIOActivity
	private static final String LogTag = "Main_activity";

	IOIO_thread m_ioio_thread;

	SpiNNakerReceiver_thread m_SpiNNakerReceiverThread;

	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		helper_.create();		// from IOIOActivity

		m_ActuatorData = new HashMap<Integer, TextView[]>();

		m_ActuatorTable = (TableLayout)findViewById(R.id.actuator_table); //inverted for some reason
		//layout_right_joystick = (RelativeLayout)findViewById(R.id.layout_right_joystick ); //inverted for some reason

		AddActuator("remote", 2);

		// Create handler to handle handle messages
		m_SpiNNakerReceiverHandler =
			new Handler()
			{
				@Override
				public void handleMessage(Message msg) {
					// Extract message fields
					int sourcePopulation = msg.getData().getInt("sourcePopulation");
					float[] payload = msg.getData().getFloatArray("payload");

					TextView[] view = m_ActuatorData.get(Integer.valueOf(sourcePopulation));
					if(view == null)
					{
						Log.e(LogTag, String.format("Cannot find view corresponding to source population hash %u",
								sourcePopulation));
					}
					else if(view.length != payload.length)
					{
						Log.e(LogTag, String.format("View corresponding to source population hash %u has length %u rather than %u",
								sourcePopulation, view.length, payload.length));
					}
					else
					{
						for(int i = 0; i < view.length; i++)
						{
							view[i].setText(String.format("%f", payload[i]));
						}
					}
					// Superclass
					super.handleMessage(msg);
				}
			};

		// Create a thread to receive UDP from SpiNNaker
		m_SpiNNakerReceiverThread = new SpiNNakerReceiver_thread(50007, 15, m_SpiNNakerReceiverHandler);

	} 	

	/****************************************************** functions from IOIOActivity *********************************************************************************/

	/**
	 * Create the  {@link IOIO_thread}. Called by the {@link IOIOAndroidApplicationHelper}. <br>
	 * Function copied from original IOIOActivity.
	 * @see {@link #get_ioio_data()} {@link #start_IOIO()} 
	 * */
	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) 
	{
		if(m_ioio_thread == null && connectionType.matches("ioio.lib.android.bluetooth.BluetoothIOIOConnection"))
		{
			m_ioio_thread = new IOIO_thread(this);
			return m_ioio_thread;
		}
		else return null;
	}

	@Override
	protected void onDestroy() 
	{
		helper_.destroy();
		super.onDestroy();
	}

	@Override
	protected void onStart() 
	{
		super.onStart();
		helper_.start();
	}

	@Override
	protected void onStop() 
	{
		Log.e("abr controller", "stopping...");
		helper_.stop();
		super.onStop();
	}

	@Override
	protected void onNewIntent(Intent intent) 
	{
		super.onNewIntent(intent);
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) 
		{
			helper_.restart();
		}
	}

	private void AddActuator(String name, int numDimensions)
	{
		MessageDigest md5Encoder = null;
		try
		{
			md5Encoder = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e)
		{
			System.out.println("Exception while encrypting to md5");
			e.printStackTrace();
		} // Encryption algorithm

		// Hash name bytes
		md5Encoder.update(name.getBytes());
		ByteBuffer nameHash = ByteBuffer.wrap(md5Encoder.digest());
		int nameHashInt = nameHash.getInt();

		// Create row
		TableRow row = new TableRow(this);
		TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
		row.setLayoutParams(lp);

		// Create label
		TextView label = new TextView(this);
		label.setText(name);
		row.addView(label);

		// Add a text view to array for each dimension
		TextView[] values = new TextView[numDimensions];
		for(int i = 0; i < numDimensions; i++)
		{
			values[i] = new TextView(this);
			row.addView(values[i]);
		}

		// Add row to table
		m_ActuatorTable.addView(row);
		m_ActuatorData.put(nameHashInt, values);
	}

	// Members
	private Handler m_SpiNNakerReceiverHandler;
	private TableLayout m_ActuatorTable;
	private HashMap<Integer, TextView[]> m_ActuatorData;
}
