package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Robot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Robot.SubsumptionArchiteture.Level1;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;

public class Robot extends BaseIOIOLooper {
	//private DigitalOutput _led;
	private PwmOutput _servo;
	//private DigitalInput _lint;
	private TwiMaster _twi;
	private Handler _handler;

	private static final String TAG = "Auis::Robot";

	private static boolean append2File = false;
	private static void log2File(String message) {
		File root = Environment.getExternalStorageDirectory();
		File file = new File(root, "robot.txt");
		FileWriter filewriter;
		try {
			filewriter = new FileWriter(file, append2File);
			if(append2File==false)
				append2File = true;
			BufferedWriter bufferedWriter = new BufferedWriter(filewriter);
			DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");

			Date date = new Date();
			bufferedWriter.write(dateFormat.format(date) + " by #" + Thread.currentThread().getId() + ": " + message
					+ "\r\n");
			bufferedWriter.close();
			filewriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private enum FsmState {
		IDLE, TURN_IN_PROGRESS, MOVE_IN_PROGRESS, GRABBING
	};

	private FsmState _fsmState = FsmState.IDLE;
	private Level1 _level1;

	public Robot(Handler handler, Level1 level1) {
		log2File("In Robot-ctor");
		_handler = handler;
		_level1 = level1;
	}

	/**
	 * Called every time a connection with IOIO has been established. Typically
	 * used to open pins.
	 * 
	 * @throws ConnectionLostException
	 *             When IOIO connection is lost.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
	 */
	@Override
	public void setup() throws ConnectionLostException {
		try {
			log2File("in setup()");
			Log.i(TAG, "in setup");
			
			log2File("calling _level1.setup()");
			_level1.setup(this);

			//log2File("opening leds");
			//_led = ioio_.openDigitalOutput(0, true);

			//log2File("opening lints");
			//_lint = ioio_.openDigitalInput(1, DigitalInput.Spec.Mode.PULL_UP);

			log2File("opening TwiMaster");
			_twi = ioio_.openTwiMaster(1, TwiMaster.Rate.RATE_100KHz, false);

			log2File("opening PWM-Output and setting duty cylce");
			_servo = ioio_.openPwmOutput(10, 50);
			_servo.setDutyCycle(0.0528f);
		} catch (ConnectionLostException connectionLostException) {
			log2File("connectionLostException caught in setup()");
			log2File(connectionLostException.toString());
		}
	}

	/**
	 * Called repetitively while the IOIO is connected.
	 * 
	 * @throws ConnectionLostException
	 *             When IOIO connection is lost.
	 * @throws InterruptedException
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
	 */
	@Override
	public void loop() throws ConnectionLostException, InterruptedException {
	
		log2File("in loop()");
		
		try {
			Thread.sleep(100);
			
			log2File("calling _level1.loop()");
			_level1.loop();
			log2File("called _level1.loop()");

			int chkState = getSensorReadings();
			
			Message message = new Message();
			message.arg1 = chkState;
			log2File("Sending message to handler with chkState=" + chkState);
			Log.d(TAG, "Sending message to handler with chkState=" + chkState);
			_handler.sendMessage(message);

			synchronized (_fsmState) {
				switch (_fsmState) {
				case IDLE:
					log2File("robot in IDLE-mode");
					break;
				case MOVE_IN_PROGRESS:
					log2File("Robot in MOVE_IN_PROGRESS-mode");
					break;
				case TURN_IN_PROGRESS:
					log2File("robot in TURN_IN_PROGRESS-mode");
					break;
				case GRABBING:
					log2File("robot in GRABBING-mode");
					break;
				}
			}

			Thread.sleep(100);
		} catch (ConnectionLostException connectionLostException) {
			log2File("connectionLostException caught in loop()");
			log2File(connectionLostException.getStackTrace().toString());
		} catch (InterruptedException interruptedException) {
			log2File("interruptedException caught in loop()");
			log2File(interruptedException.getStackTrace().toString());
		}
	}

//	static int counter = 0;
//	private boolean setPointReached() {
//		log2File("in setPointReached()");
//		if(_setPoint.equals(_currentPos))
//			counter ++;
//		
//		if(counter >= 10) {
//			counter =0;
//			return true;
//		}
//		return false;
//	}

	private int getSensorReadings() throws ConnectionLostException,
			InterruptedException {
		try {
			log2File("in getSensorReadings()");

			/* get sensor readings */
			byte[] request = new byte[] { 0x10 }; // get sensors
			byte[] response = new byte[8];

			log2File("reading sensor-values");
			synchronized (_twi) {
				if(_twi.writeRead(0x69, false, request, request.length, response, response.length)) {
					log2File("I2C: read sensor-values successfully:" + response[0]);
				} else {
					log2File("I2C: read sensor-values NOT successfully!");
				}
			}
			return response[0];
		} catch (ConnectionLostException connectionLostException) {
			log2File("connectionLostException caught in getSensorReadings()");
			log2File(connectionLostException.getStackTrace().toString());
		} catch (InterruptedException interruptedException) {
			log2File("interruptedException caught in getSensorReadings()");
			log2File(interruptedException.getStackTrace().toString());
		}
		return -1;
	}

//	private RobotPosition getRobotPosition() throws ConnectionLostException,
//			InterruptedException {
//		try {
//			log2File("in getRobotPosition()");
//
//			byte[] request = new byte[] { 0x1A }; // get velocity
//			byte[] response = new byte[8];
//
//			log2File("reading velocity from twi");
//			/* get velocity */
//			request[0] = 0x1A; // get velocity
//			synchronized (_twi) {				
//				if(_twi.writeRead(0x69, false, request, request.length, response, 2)) {
//					log2File("I2C: read velocity successfully:" + response[0]);
//				} else {
//					log2File("I2C: read velocity NOT successfully!");
//				}
//			}
//
//			log2File("reading position from twi");
//			/* get position */
//			request[0] = 0x1B; // get position
//			synchronized (_twi) {
//				
//				if(_twi.writeRead(0x69, false, request, request.length, response, 6)) {
//					log2File("I2C: read position successfully:" + response[0]);
//				} else {
//					log2File("I2C: read position NOT successfully!");
//				}
//			}
//			RobotPosition robotPosition = new RobotPosition();
//			robotPosition.xPos = (short) (((response[1] & 0xFF) << 8) | (response[0] & 0xFF));
//			robotPosition.yPos = (short) (((response[3] & 0xFF) << 8) | (response[2] & 0xFF));
//			robotPosition.anglePos = (short) (((response[5] & 0xFF) << 8) | (response[4] & 0xFF));
//
//			log2File("Current robotPosition is: " + robotPosition);
//			return robotPosition;
//		} catch (ConnectionLostException connectionLostException) {
//			log2File("connectionLostException caught in getRobotPosition()");
//			log2File(connectionLostException.getStackTrace().toString());
//		} catch (InterruptedException interruptedException) {
//			log2File("interruptedException caught in getRobotPosition()");
//			log2File(interruptedException.getStackTrace().toString());
//		}
//		return new RobotPosition();
//	}

	public void move(int cm) throws ConnectionLostException,
			InterruptedException {
		
		try {
			log2File("in move(" + cm + "cm");

			synchronized (_fsmState) {
				if (_fsmState != FsmState.IDLE) {
					log2File("robot not in idle mode => ignored");
					// throw new IllegalStateException(); // ignored, as user e.g. can touch more than once
				}
			}

			RobotPosition offset = new RobotPosition();
			offset.xPos = cm;
			log2File("Calling move_internal with " + offset);
			move_Internal(offset);
//			synchronized (_fsmState) {
//				_fsmState = FsmState.MOVE_IN_PROGRESS;
//				log2File("doing FSM-state transition: MOVE_IN_PROGRESS");
//			}

		} catch (ConnectionLostException connectionLostException) {
			log2File("connectionLostException caught in move()");
			log2File(connectionLostException.getStackTrace().toString());
		} catch (InterruptedException interruptedException) {
			log2File("interruptedException caught in move()");
			log2File(interruptedException.getStackTrace().toString());
		}
	}

	private void move_Internal(RobotPosition offset)
			throws ConnectionLostException, InterruptedException {
		log2File("in move_Internal with offset: " + offset);

		/* drive request[1] (in cm) forward */
		byte[] request = new byte[2];
		byte[] response = new byte[1];

		request[0] = 0x1C; // cmd
		request[1] = (byte) offset.xPos;

		synchronized (_twi) {
			Log.v("I2C",
					"ret = "
							+ _twi.writeRead(0x69, false, request,
									request.length, response, 0));
		}
		log2File("move_Internal finished successfully");
	}

	public void rotate(int degree) throws ConnectionLostException,
			InterruptedException {
		log2File("in rotate, with degrees: " + degree);

		synchronized (_fsmState) {
			if (_fsmState != FsmState.IDLE) {
				log2File("robot not in idle mode => ignored");
				//throw new IllegalStateException(); // ignored, as user e.g. can touch more than once
			}
		}

		RobotPosition offset = new RobotPosition();
		offset.anglePos = degree;
		
		log2File("Calling rotate_Internal with " + offset);
		rotate_Internal(offset);
//		synchronized (_fsmState) {
//			_fsmState = FsmState.TURN_IN_PROGRESS;
//			log2File("doing FSM-state transition: TURN_IN_PROGRESS");
//		}
	}

	public void rotate_Internal(RobotPosition offset)
			throws ConnectionLostException, InterruptedException {
		log2File("in rotate_Internal with offset: " + offset);

		/* turn request[1] (in degree) */
		byte[] request = new byte[2];
		byte[] response = new byte[0];

		request[0] = 0x1D; // cmd
		request[1] = (byte) offset.anglePos;

		synchronized (_twi) {
			Log.v("I2C",
					"ret = "
							+ _twi.writeRead(0x69, false, request,
									request.length, response, response.length));
		}
		log2File("rotate_Internal finished successfully");
	}

	public boolean workInProgress() {
		log2File("in workInProgress");
		return _fsmState != FsmState.IDLE;
	}

	/**
	 * sets the grabber to the given percentage
	 * a percent-value of 0 means the grabber is 'open'
	 * a percent-value of 100 means the grabber is 'down' (i.e. is grabbing)
	 * @param percent
	 * 	should have a range between [0, 100]
	 * @throws ConnectionLostException 
	 */
	public void grab(int percent) throws ConnectionLostException {
		log2File("in grab, with percent: " + percent);

		synchronized (_fsmState) {
			if (_fsmState != FsmState.IDLE) {
				log2File("robot not in idle mode => ignored");
				//throw new IllegalStateException(); // ignored, as user e.g. can touch more than once
			} else {
				log2File("doing FSM-state transition: IDLE => GRABBING"); // should already be, but just to be sure
				_fsmState=FsmState.GRABBING;
			}
		}
		
		grab_Internal(percent);
		
		synchronized (_fsmState) {
			log2File("doing FSM-state transition: GRABBING => IDLE"); // should already be, but just to be sure
			_fsmState = FsmState.IDLE;
		}
	}

	private void grab_Internal(int percent) throws ConnectionLostException {
		log2File("in grab_Internal with percent: " + percent);
		_servo.setDutyCycle(0.0528f + percent * 0.0005f);
		log2File("grab_Internal finished successfully");
	}

	public void reset() {
		log2File("in reset()");
		synchronized (_fsmState) {
			log2File("resetting _fsmState to IDLE");
			_fsmState = FsmState.IDLE;
		}
		
		log2File("reset() done");
	}
}
