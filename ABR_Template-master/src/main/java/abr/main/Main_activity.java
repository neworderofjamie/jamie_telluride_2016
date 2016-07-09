/**
 * Rescue Robotics 2016 App
 * Developed by Cognitive Anteater Robotics Laboratory at University of California, Irvine
 * Controls wheeled robot through IOIO
 * Parts of code adapted from OpenCV blob follow
 * Before running, connect phone to IOIO with a bluetooth connection
 * If you would like to uncomment sections for message passing, first connect peer phones using wifi direct
 */
package abr.main;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

//============================================================================
// IActuator
//============================================================================
interface IActuator
{
	void actuate(float... values);
}

//============================================================================
// Main_activity
//============================================================================
public class Main_activity extends Activity implements IOIOLooperProvider, SensorEventListener
		 // implements IOIOLooperProvider: from IOIOActivity
{
	private static final String LogTag = "Main_activity";

	//============================================================================
	// Private members
	//============================================================================
	private final IOIOAndroidApplicationHelper helper_ = new IOIOAndroidApplicationHelper(this, this); // from IOIOActivity
	
	// ioio variables
	IOIO_thread m_ioio_thread;

	// Socket for sending datagrams back to SpiNNaker
	DatagramSocket m_SpiNNakerTransmitterSocket;

	private SpiNNakerReceiver_thread m_SpiNNakerReceiverThread;
	private Handler m_SpiNNakerReceiverHandler;

	private TextView m_SpiNNakerAddressText;

	private TableLayout m_ActuatorTable;
	private HashMap<Integer, Pair<TextView[], IActuator>> m_Actuators;

	private TableLayout m_SensorTable;
	private HashMap<String, Pair<TextView[], Integer>> m_Sensors;

	private InetAddress m_SpiNNakerAddress;

	//variables for compass
	private SensorManager mSensorManager;
	private Sensor mCompass, mAccelerometer;
    float[] mGravity;
	float[] mGeomagnetic;

	// called whenever the activity is created
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
 
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.main);

		helper_.create(); // from IOIOActivity

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

		// Create actuator and sensor data structures
		m_Actuators = new HashMap<Integer, Pair<TextView[], IActuator>>();;
		m_Sensors = new HashMap<String, Pair<TextView[], Integer>>();

		// Find tables for displaying current actuator and sensor state
		m_ActuatorTable = (TableLayout) findViewById(R.id.actuator_table);
		m_SensorTable = (TableLayout) findViewById(R.id.sensor_table);

		m_SpiNNakerAddressText = (TextView) findViewById(R.id.spinnaker_address);

		//set up compass
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    mCompass= mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    mAccelerometer= mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
	    //mGravityS = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

		// Add supported actuators
		AddActuator("speed", 1,
				new IActuator()
				{
					@Override
					public void actuate(float... values)
					{
						if(m_ioio_thread != null) {
							m_ioio_thread.set_speed(
									IOIO_thread.DEFAULT_PWM + (int) (values[0] * (float)(IOIO_thread.MAX_PWM - IOIO_thread.DEFAULT_PWM))
							);
						}
					}
				});
		AddActuator("steer", 1,
				new IActuator()
				{
					@Override
					public void actuate(float... values)
					{
						if(m_ioio_thread != null) {
							m_ioio_thread.set_steering(
									IOIO_thread.DEFAULT_PWM + (int) (values[0] * (float)(IOIO_thread.MAX_PWM - IOIO_thread.DEFAULT_PWM))
							);
						}
					}
				});

		// Add supported sensors
		AddSensor("orientation", 3, 50008);
		AddSensor("sonar", 3, 50009);

		// Create SpiNNaker event handler
		m_SpiNNakerReceiverHandler =
				new Handler()
				{
					@Override
					public void handleMessage(Message msg) {
						// Extract message fields
						int sourcePopulation = msg.getData().getInt("sourcePopulation");
						float[] payload = msg.getData().getFloatArray("payload");
						m_SpiNNakerAddress = (InetAddress)msg.getData().getSerializable("address");

						// Stick SpiNNaker address in UI
						if(m_SpiNNakerAddressText != null)
						{
							m_SpiNNakerAddressText.setText(m_SpiNNakerAddress.toString());
						}

						// Find hashed population
						Pair<TextView[], IActuator> actuator = m_Actuators.get(Integer.valueOf(sourcePopulation));

						// If it's not found
						if(actuator == null)
						{
							Log.e(LogTag, String.format("Cannot find actuator corresponding to source population hash %u",
									sourcePopulation));
						}
						else
						{
							// If the number of text fields doesn't match the length of the payload vector
							if (actuator.first.length != payload.length)
							{
								Log.e(LogTag, String.format("View corresponding to source population hash %u has length %u rather than %u",
										sourcePopulation, actuator.first.length, payload.length));
							}
							// Otherwise loop through each text view and set to floating point value
							else
							{
								for (int i = 0; i < actuator.first.length; i++)
								{
									actuator.first[i].setText(String.format("%f", payload[i]));
								}
							}

							// Call actuate method
							actuator.second.actuate(payload);
						}
						// Superclass
						super.handleMessage(msg);
					}
				};

		// Create a thread to receive UDP from SpiNNaker
		m_SpiNNakerReceiverThread = new SpiNNakerReceiver_thread(50007, 15, m_SpiNNakerReceiverHandler);
		m_SpiNNakerReceiverThread.start();
	}
	
    @Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}
    
    //Called whenever the value of a sensor changes
	@Override
	public final void onSensorChanged(SensorEvent event)
	{
		// If we have an ioio thread, update sonar
		if(m_ioio_thread != null)
		{
			UpdateSensor("sonar",
					ConvertSonar(m_ioio_thread.get_sonar1_reading()),
					ConvertSonar(m_ioio_thread.get_sonar2_reading()),
					ConvertSonar(m_ioio_thread.get_sonar3_reading()));
		}

		//if (event.sensor.getType() == Sensor.TYPE_GRAVITY)
		//mGravityV = event.values;
		//if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
		//	mGyro = event.values;
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;

		if (mGravity != null && mGeomagnetic != null) {
			float[] temp = new float[9];
			float[] R = new float[9];
			//Load rotation matrix into R
			SensorManager.getRotationMatrix(temp, null, mGravity, mGeomagnetic);
			//Remap to camera's point-of-view
			SensorManager.remapCoordinateSystem(temp, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);

			//Return the orientation values
			float[] values = new float[3];
			SensorManager.getOrientation(R, values);

			// Convert numbers from +- PI to +- 1.0
			for (int i=0; i < values.length; i++)
			{
				values[i] /= (float)Math.PI;
			}
			//Convert to degrees
			/*for (int i=0; i < values.length; i++) {
				Double degrees = (values[i] * 180) / Math.PI;
				values[i] = degrees.floatValue();
			}
			//Update the compass direction
			float heading = values[0]+12;
			heading = (heading*5 + fixWraparound(values[0]+12))/6; //add 12 to make up for declination in Irvine, average out from previous 2 for smoothness(*/

			UpdateSensor("orientation",
					values[0], values[1], values[2]);
		}
	}

	
	//Called whenever activity resumes from pause
	@Override
	public void onResume() {
		super.onResume();

	    mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
	    mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	    //mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
	    //mSensorManager.registerListener(this, mGravityS, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	//Called when activity pauses
	@Override
	public void onPause() {
		super.onPause();

		mSensorManager.unregisterListener(this);
	}
	
	//Called when activity restarts. onCreate() will then be called
	@Override
	public void onRestart() {
		super.onRestart();
		Log.i("activity cycle","main activity restarting");
	}

	/****************************************************** functions from IOIOActivity *********************************************************************************/

	/**
	 * Create the {@link IOIO_thread}. Called by the
	 * {@link IOIOAndroidApplicationHelper}. <br>
	 * Function copied from original IOIOActivity.
	 * 
	 * @see {@link #get_ioio_data()} {@link #start_IOIO()}
	 * */
	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
		if (m_ioio_thread == null
				&& connectionType
						.matches("ioio.lib.android.bluetooth.BluetoothIOIOConnection")) {
			m_ioio_thread = new IOIO_thread(this);
			return m_ioio_thread;
		} else
			return null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i("activity cycle","main activity being destroyed");
		helper_.destroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i("activity cycle","main activity starting");
		helper_.start();
	}

	@Override
	protected void onStop() {
		Log.i("activity cycle","main activity stopping");
		super.onStop();
		helper_.stop();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
			if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
			helper_.restart();
		}
	}

	static private float ConvertSonar(int sonar)
	{
		// Give up representing beyond 12" (about 1m)
		final float maxSonar = 12.0f;

		// Clamp sonar
		return (float)sonar / maxSonar;
	}
	//============================================================================
	// Private methods
	//============================================================================
	private void AddSensor(String name, int numDimensions, int port)
	{
		// Create row
		TableRow row = new TableRow(this);
		TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
		row.setLayoutParams(lp);

		// Create label
		TextView label = new TextView(this);
		label.setText(name);
		row.addView(label);

		TableRow.LayoutParams labelParams = (TableRow.LayoutParams)label.getLayoutParams();
		labelParams.setMargins(2,2,2,2);
		labelParams.column = 0;
		labelParams.width = TableRow.LayoutParams.FILL_PARENT;
		labelParams.height = TableRow.LayoutParams.WRAP_CONTENT;
		label.setLayoutParams(labelParams);

		// Add a text view to array for each dimension
		TextView[] values = new TextView[numDimensions];
		for(int i = 0; i < numDimensions; i++)
		{
			values[i] = new TextView(this);
			values[i].setText("?");
			row.addView(values[i]);

			TableRow.LayoutParams valueParams = (TableRow.LayoutParams)values[i].getLayoutParams();
			valueParams.setMargins(2,2,2,2); //To "draw" margins
			valueParams.column = 1 + i;
			valueParams.width = TableRow.LayoutParams.FILL_PARENT;
			valueParams.height = TableRow.LayoutParams.WRAP_CONTENT;
			values[i].setLayoutParams(valueParams);
			values[i].setPadding(2, 2, 2, 2);
		}

		// Add row to table
		m_SensorTable.addView(row, new TableLayout.LayoutParams(
				TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));

		// Add to data structure
		m_Sensors.put(name, new Pair<TextView[], Integer>(values, port));
	}

	private void AddActuator(String name, int numDimensions, IActuator actuator)
	{
		// Create MD5 encoder
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

		TableRow.LayoutParams labelParams = (TableRow.LayoutParams)label.getLayoutParams();
		labelParams.setMargins(2,2,2,2);
		labelParams.column = 0;
		labelParams.width = TableRow.LayoutParams.FILL_PARENT;
		labelParams.height = TableRow.LayoutParams.WRAP_CONTENT;
		label.setLayoutParams(labelParams);

		// Add a text view to array for each dimension
		TextView[] values = new TextView[numDimensions];
		for(int i = 0; i < numDimensions; i++)
		{
			values[i] = new TextView(this);
			values[i].setText("?");
			row.addView(values[i]);

			TableRow.LayoutParams valueParams = (TableRow.LayoutParams)values[i].getLayoutParams();
			valueParams.setMargins(2,2,2,2); //To "draw" margins
			valueParams.column = 1 + i;
			valueParams.width = TableRow.LayoutParams.FILL_PARENT;
			valueParams.height = TableRow.LayoutParams.WRAP_CONTENT;
			values[i].setLayoutParams(valueParams);
			values[i].setPadding(2, 2, 2, 2);
		}

		// Add row to table
		m_ActuatorTable.addView(row, new TableLayout.LayoutParams(
				TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));

		// Add array of views and actuator to data structures
		m_Actuators.put(nameHashInt, new Pair<TextView[], IActuator>(values, actuator));
	}

	private void UpdateSensor(String name, Float... values)
	{
		// Find named sensor
		Pair<TextView[], Integer> sensor = m_Sensors.get(name);

		// If it's not found
		if(sensor == null)
		{
			Log.e(LogTag, String.format("Cannot find sensor named %s", name));
		}
		else
		{
			// If the number of text fields doesn't match the length of the sensor data
			if (sensor.first.length != values.length)
			{
				Log.e(LogTag, String.format("View corresponding to sensor name %s has length %u rather than %u",
						name, sensor.first.length, values.length));
			}
			// Otherwise loop through each text view and set to floating point value
			else
			{
				for (int i = 0; i < sensor.first.length; i++)
				{
					sensor.first[i].setText(String.format("%f", values[i]));
				}
			}

			// If we have a SpiNNaker transmitter socket and address
			if(m_SpiNNakerTransmitterSocket != null && m_SpiNNakerAddress != null)
			{
				// Create a byte buffer for payload
				ByteBuffer payload = ByteBuffer.allocate((2 * 2) + (3 * 4) + (values.length * 4));
				payload.order(ByteOrder.LITTLE_ENDIAN);

				// Write SCP header
				payload.putShort((short)0);	// CmdRC
				payload.putShort((short)0);	// Seq
				payload.putInt(0);			// Arg1
				payload.putInt(0);			// Arg2
				payload.putInt(0);			// Arg3

				// Loop through sensor values
				final float fixedPointScale =  (float)(1 << 15);
				for(int i = 0 ; i < values.length; i++)
				{
					// Convert to fixed-point and stick in payload
					int fixedPointValue = (int)Math.round(values[i] * fixedPointScale);
					payload.putInt(fixedPointValue);
				}
				// Create packet
				DatagramPacket packet = new DatagramPacket(payload.array(), payload.position());
				packet.setPort(sensor.second);
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
}
