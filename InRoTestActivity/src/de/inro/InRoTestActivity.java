package de.inro;

import java.text.DecimalFormat;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class InRoTestActivity extends IOIOActivity implements SensorEventListener
{

	private ToggleButton button_;
	ProgressBar bar[] = new ProgressBar[9];
	SeekBar sbar[] = new SeekBar[5];
	CheckBox box[] = new CheckBox[6];
	int chkState;
	String analog[] = new String[9];
	TextView tv[] = new TextView[12];
	short xPos;
	short yPos;
	short anglePos;
	TextView debugView;
	SensorManager sensorManager;
	boolean lint = false;
	boolean acc = false;
	int velo0, velo1;
	boolean enabledInt = false;
	boolean enabledMoff = false;

	byte[] test = new byte[8];
	
	Object sync = new Object();
	
	private DigitalOutput led_;
	private PwmOutput servo_;
	private DigitalInput lint_;
	private TwiMaster twi;
	
	Handler mHandler = new Handler()
	{
		int i=12;
		
		@Override
		public void handleMessage(Message msg)
		{
			for (int l = 0; l < 5; l++)
			{
				if ((chkState & (1 << l)) != 0)
					box[l].setChecked(true);
				else
					box[l].setChecked(false);
			}
			box[5].setChecked(lint);
			for (int l = 0; l < 7; l++)
			{
				tv[l].setText(analog[l]);
			}
			tv[7].setText(velo0 + "/" + analog[7]);
			tv[8].setText(velo1 + "/" + analog[8]);
			
			tv[9].setText(xPos + "cm");
			tv[10].setText(yPos + "cm");
			tv[11].setText(anglePos + "°");
			
			//debugView.setText("");
			//for(int i=0; i<8 ;i++)
			//	debugView.append(((int)test[i] & 0x00FF) + " "); 
			
			if((chkState & (1<<5)) != 0)
			{
				//badhack
				if(i++ > 12)
				{
					Toast.makeText(getApplicationContext(), "Overheat", Toast.LENGTH_SHORT).show();
					i = 0;
				}
			}
			else
			{
				i = 12;
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		byte[] request;
		byte[] response;
		
		// Handle item selection
		switch (item.getItemId())
		{
		case R.id.quit:
			finish();
			return true;
		case R.id.acc:
			if(acc)
			{
				acc = false;
				sbar[0].setProgress(80);
				sbar[1].setProgress(80);
			}
			else
				acc = true;
			Toast.makeText(this, "ACC mode: " + acc, Toast.LENGTH_LONG).show();
			return true;
		case R.id.tint:

			/* WRITE Int line */
			request = new byte[9];
			response = new byte[0];
			request[0] = 0x11;	// int line
			
			if(enabledInt == false)
			{
				request[1] = 0x06;	//sensible to bumper 1 and 2
				request[2] = 0;		//voltage irrelevant
				request[3] = 0;		//sensor 1 irrelevant
				request[4] = 0;		//sensor 2 irrelevant
				request[5] = 20;	//sensor 3 < 20cm
				request[6] = 20;	//sensor 4 < 20cm
				request[7] = 0;		//sensor 5 irrelevant
				request[8] = 0;		//sensor 6 irrelevant
				enabledInt = true;
			}
			else
			{
				request[1] = 0x00;	//sensible to bumper 1 and 2
				request[2] = 0;		//voltage irrelevant
				request[3] = 0;		//sensor 1 irrelevant
				request[4] = 0;		//sensor 2 irrelevant
				request[5] = 0;		//sensor 3 irrelevant
				request[6] = 0;		//sensor 4 irrelevant
				request[7] = 0;		//sensor 5 irrelevant
				request[8] = 0;		//sensor 6 irrelevant
				enabledInt = false;
			}

			Toast.makeText(this, "INT Line: " + enabledInt, Toast.LENGTH_LONG).show();
			
			try
			{
				synchronized(twi)
				{
					if(twi.writeRead(0x69, false, request, request.length, response, response.length) == false)
					{
						Toast.makeText(this, "Hups. Failed.", Toast.LENGTH_LONG).show();
						enabledInt = !enabledInt;
					}
				}
			}
			catch (ConnectionLostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/* VERIFY stuff*/
			request = new byte[1];
			response = new byte[8];
			request[0] = 0x11;	// int line

			try
			{
				debugView.setText("");
				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
					for(int i=0; i<response.length; i++)
						debugView.append(response[i] + " ");
				}
			}
			catch (ConnectionLostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		case R.id.tmoff:
			/* WRITE motor turn off boundaries */
			request = new byte[9];
			response = new byte[0];
			request[0] = 0x12;	// int line
			
			if(enabledMoff == false)
			{
				request[1] = 0x18;	//sensible to bumper 3 and 4
				request[2] = 0;		//voltage irrelevant
				request[3] = 15;	//sensor 1 < 15cm
				request[4] = 15;	//sensor 2 < 15cm
				request[5] = 0;		//sensor 3 irrelevant
				request[6] = 0;		//sensor 4 irrelevant
				request[7] = 30;	//sensor 5 < 30cm
				request[8] = 0;		//sensor 6 irrelevant
				enabledMoff = true;
			}
			else
			{
				request[1] = 0x00;	//sensible to bumper 1 and 2
				request[2] = 0;		//voltage irrelevant
				request[3] = 0;		//sensor 1 irrelevant
				request[4] = 0;		//sensor 2 irrelevant
				request[5] = 0;		//sensor 3 irrelevant
				request[6] = 0;		//sensor 4 irrelevant
				request[7] = 0;		//sensor 5 irrelevant
				request[8] = 0;		//sensor 6 irrelevant
				enabledMoff = false;
			}

			Toast.makeText(this, "Motor Off : " + enabledMoff, Toast.LENGTH_LONG).show();
			
			try
			{
				synchronized(twi)
				{
					if(twi.writeRead(0x69, false, request, request.length, response, response.length) == false)
					{
						Toast.makeText(this, "Hups. Failed.", Toast.LENGTH_LONG).show();
						enabledMoff = !enabledMoff;
					}
				}
			}
			catch (ConnectionLostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/* VERIFY Stuff */
			request = new byte[1];
			response = new byte[8];
			request[0] = 0x12;	// int line

			try
			{
				debugView.setText("");
				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
					for(int i=0; i<response.length; i++)
						debugView.append(response[i] + " ");
				}
			}
			catch (ConnectionLostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		case R.id.dbg:
			/* just for PID debug... ignore */
			request = new byte[] { 0x70 };
			response = new byte[128];
			try
			{
				debugView.setText("");
				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
				}
				for(int i=0; i<128; i++)
					debugView.append(response[i] + " ");
				
			}
			catch (ConnectionLostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		case R.id.zero:
			/* Determin odometry position */
			request = new byte[7];
			response = new byte[0];
			try
			{
				request[0] = 0x1B;		//command
				request[1] = 0; //x low
				request[2] = 0; //high
				request[3] = 0;	//y low
				request[4] = 0; //high
				request[5] = 0;	//alpha low
				request[6] = 0; //high

				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
				}
			}
			catch (ConnectionLostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
/*		case R.id.turn180:
			request = new byte[3];
			response = new byte[0];
			try
			{
				request[0] = 0x1A;
				request[1] = 10; //left
				request[2] = -10; //right

				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
				}
				Thread.sleep(2123);
				request[0] = 0x1A;
				request[1] = 0; //left
				request[2] = 0; //right

				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
				}
			}
			catch (ConnectionLostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			return true;
*/			
		case R.id.forward1:
			/* drive request[1] (in cm) forward */
			request = new byte[2];
			response = new byte[1];
			try
			{
				request[0] = 0x1C;	//cmd
				request[1] = 100;

				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, 0));
				}
				/*debugView.setText("");
				for(int i=0; i<60; i++)
				{
					synchronized(twi)
					{
						Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, 1, response, response.length));
					}
					debugView.append(response[0] + " ");
					Thread.sleep(100);
				}*/
			}
			catch (ConnectionLostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			return true;
		case R.id.back1:
			/* drive request[1] (in cm) forward */
			request = new byte[2];
			response = new byte[0];
			try
			{
				request[0] = 0x1C;	//cmd
				request[1] = -100; 

				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
				}
			}
			catch (ConnectionLostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			return true;
		case R.id.left45:
			/* turn request[1] (in degree) */
			request = new byte[2];
			response = new byte[0];
			try
			{
				request[0] = 0x1D;	//cmd
				request[1] = 45; 

				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
				}
			}
			catch (ConnectionLostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			return true;
		case R.id.right45:
			/* turn request[1] (in degree) */
			request = new byte[2];
			response = new byte[0];
			try
			{
				request[0] = 0x1D;	//cmd
				request[1] = -45; 

				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
				}
			}
			catch (ConnectionLostException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Called when the activity is first created. Here we normally
	 * initialize our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// button_ = (ToggleButton) findViewById(R.id.button);
		bar[0] = (ProgressBar) findViewById(R.id.ProgressBar01);
		bar[1] = (ProgressBar) findViewById(R.id.ProgressBar02);
		bar[2] = (ProgressBar) findViewById(R.id.ProgressBar03);
		bar[3] = (ProgressBar) findViewById(R.id.ProgressBar04);
		bar[4] = (ProgressBar) findViewById(R.id.ProgressBar05);
		bar[5] = (ProgressBar) findViewById(R.id.ProgressBar06);
		bar[6] = (ProgressBar) findViewById(R.id.ProgressBar07);
		sbar[0] = (SeekBar) findViewById(R.id.seekBar1);
		sbar[1] = (SeekBar) findViewById(R.id.SeekBar01);
		sbar[2] = (SeekBar) findViewById(R.id.seekBar2);
		sbar[3] = (SeekBar) findViewById(R.id.seekBar3);
		sbar[4] = (SeekBar) findViewById(R.id.seekBar4);
		tv[0] = (TextView) findViewById(R.id.TextView01);
		tv[1] = (TextView) findViewById(R.id.TextView02);
		tv[2] = (TextView) findViewById(R.id.TextView03);
		tv[3] = (TextView) findViewById(R.id.TextView04);
		tv[4] = (TextView) findViewById(R.id.TextView05);
		tv[5] = (TextView) findViewById(R.id.TextView06);
		tv[6] = (TextView) findViewById(R.id.TextView07);
		tv[7] = (TextView) findViewById(R.id.TextView08);
		tv[8] = (TextView) findViewById(R.id.TextView09);
		tv[9] = (TextView) findViewById(R.id.textView8);
		tv[10] = (TextView) findViewById(R.id.textView9);
		tv[11] = (TextView) findViewById(R.id.textView10);
		box[0] = (CheckBox) findViewById(R.id.checkBox1);
		box[1] = (CheckBox) findViewById(R.id.checkBox2);
		box[2] = (CheckBox) findViewById(R.id.checkBox3);
		box[3] = (CheckBox) findViewById(R.id.checkBox4);
		box[4] = (CheckBox) findViewById(R.id.checkBox5);
		box[5] = (CheckBox) findViewById(R.id.checkBox6);
		// tv = (TextView) findViewById(R.id.textView1);
		// tv.setText("val = ");
		debugView = (TextView) findViewById(R.id.textView12);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be
	 * run every time the application is resumed and aborted when it is
	 * paused. The method setup() will be called right after a connection
	 * with the IOIO has been established (which might happen several
	 * times!). Then, loop() will be called repetitively until the IOIO gets
	 * disconnected.
	 */
	class Looper extends BaseIOIOLooper implements OnSeekBarChangeListener
	{

		/**
		 * Called every time a connection with IOIO has been
		 * established. Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *                 When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException
		{
			led_ = ioio_.openDigitalOutput(0, true);
			lint_ = ioio_.openDigitalInput(1, DigitalInput.Spec.Mode.PULL_UP); 
			twi = ioio_.openTwiMaster(1, TwiMaster.Rate.RATE_100KHz, false);
			servo_ = ioio_.openPwmOutput(10, 50);
			servo_.setDutyCycle(0.0528f);
			
		        sbar[0].setOnSeekBarChangeListener(this);
		        sbar[1].setOnSeekBarChangeListener(this);
		        sbar[2].setOnSeekBarChangeListener(this);
		        sbar[3].setOnSeekBarChangeListener(this);
		        sbar[4].setOnSeekBarChangeListener(this);
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *                 When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException
		{
			led_.write(true);
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
			}

			try
			{
				/* get sensor readings */
				byte[] request = new byte[] { 0x10 };	//get sensors
				byte[] response = new byte[8];				
				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
				}
				chkState = response[0];
				for (int l = 0; l < 7; l++)
				{
					int i = 0xFF & (int) response[l + 1];
					bar[l].setProgress(i);
					if (l != 0)
						analog[l] = i + "cm";
					else
						analog[l] = new DecimalFormat("#.#").format(i / 10.0) + "V";
				}

				/* get velocity */
				request[0] = 0x1A;		//get velocity
				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, 2));
				}
				analog[7] = response[0] + "";
				analog[8] = response[1] + "";
				
				/* get position */
				request[0] = 0x1B;		//get position
				synchronized(twi)
				{
					Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, 6));
				}
				
				xPos = (short) (((response[1] & 0xFF) << 8) | (response[0] & 0xFF));
				yPos = (short) (((response[3] & 0xFF) << 8) | (response[2] & 0xFF));
				anglePos = (short) (((response[5] & 0xFF) << 8) | (response[4] & 0xFF));
				
				
				lint = !lint_.read();
					

				mHandler.sendMessage(new Message());
			}
			catch (InterruptedException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			led_.write(false);
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
			}
		}

		@Override
		public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2)
		{
			
			
			if(arg0.equals(sbar[0]) || arg0.equals(sbar[1]))	//v0+1
			{
				velo0 = sbar[0].getProgress() - 80;	
				velo1 = sbar[1].getProgress() - 80;
				mHandler.sendMessage(new Message());
				byte[] request = new byte[3];
				byte[] response = new byte[0];

				/* set velocity */
				request[0] = 0x1A;		//cmd
				request[1] = (byte) velo0;
				request[2] = (byte) velo1;
				try
				{
					synchronized(twi)
					{
						Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
					}
				}
				catch (ConnectionLostException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(arg0.equals(sbar[2]) || arg0.equals(sbar[3]))
			{
				byte[] request = new byte[3];
				byte[] response = new byte[0];
				/* SET Leds */
				request[0] = 0x20;
				request[1] = (byte) sbar[2].getProgress();
				request[2] = (byte) sbar[3].getProgress();
				try
				{
					synchronized(twi)
					{
						Log.v("I2C", "ret = " + twi.writeRead(0x69, false, request, request.length, response, response.length));
					}
				}
				catch (ConnectionLostException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(arg0.equals(sbar[4]))
			{
				try
				{
					servo_.setDutyCycle(0.0528f + sbar[4].getProgress() * 0.0005f);
				}
				catch (ConnectionLostException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar arg0)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStopTrackingTouch(SeekBar arg0)
		{
			// TODO Auto-generated method stub
			
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper()
	{
		return new Looper();
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && acc)
		{
			int v0, v1;
			
			debugView.setText("x=" + event.values[0] + "g\ty=" + event.values[1] + "g\tz=" + event.values[2] + "g\n");
			
			event.values[0] = Math.min(Math.abs(event.values[0]), 5.0f) * Math.signum(event.values[0]);
			event.values[1] = Math.min(Math.abs(event.values[1]), 5.0f) * Math.signum(event.values[1]);
			event.values[2] = Math.min(Math.abs(event.values[2]), 5.0f) * Math.signum(event.values[2]);
			
			v0 = Math.round(event.values[1] * (-8.0f)) + Math.round(event.values[0] * (4.0f));
			v1 = Math.round(event.values[1] * (-8.0f)) + Math.round(event.values[0] * (-4.0f));
			
			v0 = (int) (Math.min(Math.abs(v0), 80) * Math.signum(v0));
			v1 = (int) (Math.min(Math.abs(v1), 80) * Math.signum(v1));

			
			debugView.append("v0=" + v0 + "\tv1=" + v1);
			
			sbar[0].setProgress(v0+80);
			sbar[1].setProgress(v1+80);
		}

	}

}
