package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Robot;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;

public class Robot extends BaseIOIOLooper 
{
	private DigitalOutput _led;
	private PwmOutput _servo;
	private DigitalInput _lint;
	private TwiMaster _twi;
	private Handler _handler;	
	
	private static final String TAG = "Robot";
	
	private enum FsmState {
		IDLE,
		TURN_IN_PROGRESS,
		MOVE_IN_PROGRESS
	};
	
	private FsmState _fsmState = FsmState.IDLE;
	private RobotPosition _origin;
	private RobotPosition _prevCurrentPos;
	private RobotPosition _currentPos;
	private RobotPosition _setPoint;
	
	public Robot(Handler handler) {
		_handler = handler;
	}
	
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
		_led = ioio_.openDigitalOutput(0, true);
		_lint = ioio_.openDigitalInput(1, DigitalInput.Spec.Mode.PULL_UP); 
		_twi = ioio_.openTwiMaster(1, TwiMaster.Rate.RATE_100KHz, false);
		_servo = ioio_.openPwmOutput(10, 50);
		_servo.setDutyCycle(0.0528f);
	}

	/**
	 * Called repetitively while the IOIO is connected.
	 * 
	 * @throws ConnectionLostException
	 *                 When IOIO connection is lost.
	 * @throws InterruptedException 
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
	 */
	@Override
	public void loop() throws ConnectionLostException, InterruptedException
	{
		_led.write(true);
		Thread.sleep(100);

		int chkState = getSensorReadings();
		_prevCurrentPos = _currentPos;
		_currentPos = getRobotPosition();
		Log.d(TAG, "Robot position: (" + _currentPos.xPos + ", " + _currentPos.yPos + "), " + _currentPos.anglePos + "°");
		
		Message message = new Message();
		message.arg1 = chkState;
		Log.d(TAG, "Sending message to handler with chkState=" + chkState);
		_handler.sendMessage(message);
		
		if(setPointReached()) {
			Log.i(TAG, "Robot reached setpoint: " + _currentPos);
			_fsmState = FsmState.IDLE;
		}
		else if(_prevCurrentPos.equals(_currentPos)) {
			Log.e(TAG, "Robot has not moved in this iteration: " + _currentPos);
		}
		
		synchronized(_fsmState)
		{
			switch(_fsmState) {
			case IDLE: break;
			case MOVE_IN_PROGRESS: 
				Log.d(TAG, "Robot still moving: " + _currentPos + ", setPoint is: " + _setPoint);
				break;
			case TURN_IN_PROGRESS: 
				Log.d(TAG, "Robot still turning: " + _currentPos + ", setPoint is: " + _setPoint);
				break;
			}
		}

		_led.write(false);
		Thread.sleep(100);
	}

	private boolean setPointReached() {
		return _setPoint.equals(_currentPos);
	}

	private int getSensorReadings() throws ConnectionLostException, InterruptedException {
		/* get sensor readings */
		byte[] request = new byte[] { 0x10 };	//get sensors
		byte[] response = new byte[8];				
		synchronized(_twi)
		{
			Log.v("I2C", "ret = " + _twi.writeRead(0x69, false, request, request.length, response, response.length));
		}
		return response[0];
	}

	private RobotPosition getRobotPosition() throws ConnectionLostException, InterruptedException {
		byte[] request = new byte[] { 0x1A };	//get velocity
		byte[] response = new byte[8];
		
		/* get velocity */
		request[0] = 0x1A;		//get velocity
		synchronized(_twi)
		{
			Log.v("I2C", "ret = " + _twi.writeRead(0x69, false, request, request.length, response, 2));
		}
		
		/* get position */
		request[0] = 0x1B;		//get position
		synchronized(_twi)
		{
			Log.v("I2C", "ret = " + _twi.writeRead(0x69, false, request, request.length, response, 6));
		}
		RobotPosition robotPosition = new RobotPosition();
		robotPosition.xPos = (short) (((response[1] & 0xFF) << 8) | (response[0] & 0xFF));
		robotPosition.yPos = (short) (((response[3] & 0xFF) << 8) | (response[2] & 0xFF));
		robotPosition.anglePos = (short) (((response[5] & 0xFF) << 8) | (response[4] & 0xFF));
		return robotPosition;
	}
	
	public void move(int cm) throws ConnectionLostException, InterruptedException {
		synchronized(_fsmState)
		{
			if(_fsmState!=FsmState.IDLE) {
				throw new IllegalStateException();
			}
		}
		
		_origin = getRobotPosition();
		RobotPosition offset = new RobotPosition();
		offset.xPos = cm;
		_setPoint = _origin.Add(offset);
		
		move_Internal(offset);
		synchronized(_fsmState)
		{
			_fsmState = FsmState.MOVE_IN_PROGRESS;
		}
	}
	
	private void move_Internal(RobotPosition offset) throws ConnectionLostException, InterruptedException {
		/* drive request[1] (in cm) forward */
		byte[] request = new byte[2];
		byte[] response = new byte[1];
		
		request[0] = 0x1C;	//cmd
		request[1] = (byte) offset.xPos;

		synchronized(_twi)
		{
			Log.v("I2C", "ret = " + _twi.writeRead(0x69, false, request, request.length, response, 0));
		}		
	}
	
	public void rotate(int degree) throws ConnectionLostException, InterruptedException {
		synchronized(_fsmState)
		{
			if(_fsmState!=FsmState.IDLE) {
				throw new IllegalStateException();
			}
		}
		
		_origin = getRobotPosition();
		RobotPosition offset = new RobotPosition();
		offset.anglePos = degree;
		_setPoint = _origin.Add(offset);
		
		rotate_Internal(offset);
		synchronized(_fsmState)
		{
			_fsmState = FsmState.TURN_IN_PROGRESS;
		}
	}
	
	public void rotate_Internal(RobotPosition offset) throws ConnectionLostException, InterruptedException {
		/* turn request[1] (in degree) */
		byte[] request = new byte[2];
		byte[] response = new byte[0];
		
		request[0] = 0x1D;	//cmd
		request[1] = (byte) offset.anglePos; 

		synchronized(_twi)
		{
			Log.v("I2C", "ret = " + _twi.writeRead(0x69, false, request, request.length, response, response.length));
		}
	}

	public boolean workInProgress(){
		return _fsmState!=FsmState.IDLE;
	}
}
