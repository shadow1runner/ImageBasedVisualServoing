package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Robot.SubsumptionArchiteture;

import ioio.lib.api.exception.ConnectionLostException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.opencv.core.Point;

import android.os.Environment;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Robot.Robot;

public class Level1 {
	private static final String TAG = "Level1";
	private int DEGREE_GRANULARITY = 4;
	private int ROUTE_GRANULARITY = 10;

	private static boolean append2File = false;

	private static void log2File(String message) {
		File root = Environment.getExternalStorageDirectory();
		File file = new File(root, "level1.txt");
		FileWriter filewriter;
		try {
			filewriter = new FileWriter(file, append2File);
			if (append2File == false)
				append2File = true;
			BufferedWriter bufferedWriter = new BufferedWriter(filewriter);
			DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");

			Date date = new Date();
			bufferedWriter.write(dateFormat.format(date) + " by #"
					+ Thread.currentThread().getId() + ": " + message + "\r\n");
			bufferedWriter.close();
			filewriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Level1() {
		log2File("In Level1-ctor");
	}

	/*
	 * the following state transitions are valid: IDLE [setPoint set] -> TURN
	 * [angle reached] -> MOVE i.e. the robot first of all turns to the
	 * specified setPoint than moves to it
	 */
	private enum FsmState {
		IDLE, TURNING, MOVING, GRABBING, DONE
	};

	private FsmState _fsmState = FsmState.IDLE;
	private Robot _robot;
	private Point _setPoint;
	private boolean _grabFinished;

	public void setup(Robot robot) throws ConnectionLostException {
		log2File("in setup()");
		_robot = robot;
	}

	public void loop() throws ConnectionLostException, InterruptedException {
		log2File("in loop()");
		
		doStateTransitionIfNecessary();
		
		switch (_fsmState) {
		case IDLE:
			log2File("Current state: IDLE");
			// _robot.grab(0);
			break;
		case MOVING:
			log2File("Current state: MOVING");
			int amount = getRoute();
			log2File("Scheduling new move-amount for robot: " + amount + "cm");
			_robot.move(amount);
			break;
		case TURNING:
			log2File("Current state: TURNING");
			int degree = getAngle();
			log2File("Scheduling new rotate-amount for robot: " + degree + "°");
			if(degree==0) {
				log2File("MOVING=> IDLE ignoring scheduled degrees as it is 0, changing to IDLE");
				_fsmState=FsmState.IDLE;
			} else {
				_robot.rotate(degree);
			}
			break;
		case GRABBING:
			log2File("Current state: GRABBING");
			log2File("Scheduling new grab-command for robot");
			_robot.grab(100);
			_grabFinished = true;
			_fsmState = FsmState.DONE;
			break;
		case DONE:
			log2File("Current state: DONE");
			break;
		default:
			break;
		}
	}

	private void doStateTransitionIfNecessary() {
		log2File("in doStateTransitionIfNecessary()");
		
		if(_fsmState==FsmState.TURNING) {
			log2File("checking, whether the robot still has to turn");
			if(isTurnFinished()) {
				log2File("Turning is finished");
				log2File("DOING STATE TRANSITION TURNING => MOVING");
				_fsmState=FsmState.MOVING;
			}
		} else if(_fsmState==FsmState.MOVING) {
			log2File("checking, whether the robot still has to turn");
			if(isRouteFinished()) {
				log2File("Route is finished");
				log2File("DOING STATE TRANSITION MOVING => GRABBING");
				_fsmState=FsmState.GRABBING;
			}
		}
		
		log2File("in doStateTransitionIfNecessary()-end");
	}

	private boolean isTurnFinished() {
		log2File("in isTurnFinished()");
		
		log2File("robots current setpoint is: " + _setPoint);
		if(Math.abs(_setPoint.y) < 1.0) {
			log2File("|_setPoint.y| < 1.0 is true => RETURNING true");
			return true;
		} else {
			log2File("|_setPoint.y| < 1.0 is false => RETURNING false");
			return false;
		}
	}
	
	private boolean isRouteFinished() {
		log2File("in isRouteFinished()");

		log2File("robots current setpoint is: " + _setPoint);
		if(_setPoint.x < 15.0) {
			log2File("_setPoint.x < 15.0 is true => RETURNING true");
			return true;
		} else {
			log2File("_setPoint.x < 15.0  is false => RETURNING false");
			return false;
		}
	}
	
	private boolean isGrabbingFinished() {
		log2File("in isGrabbingFinished()");

		log2File("_grabFinished: " + _grabFinished);
		return _grabFinished == true;
	}

	private int getAngle() {
		log2File("in getAngle()");
		
		int angleDeg = (int) (Math.atan(Math.abs(_setPoint.y) / Math.abs(_setPoint.x)) * 180.0 / Math.PI);
		if(_setPoint.y>0) {
			angleDeg *= -1;
		}
		
		log2File("calculated angle: " + angleDeg + "°");
		return angleDeg;
	}

	private int getRoute() {
		log2File("in getRoute()");
		return (int) _setPoint.x;
	}

	

	/**
	 * sets the setPoint to the current value - this does not change the robot
	 * behavior when he is already moving/turning; but when a new schedule has
	 * to be planned, the current {@code setPoint} gonna be used for it
	 */
	public void setSetPoint(Point setPoint) {
		log2File("in setSetPoint() with point: (" + setPoint.x + ", "
				+ setPoint.y + ")");
		_setPoint = setPoint;
		if (_fsmState == FsmState.IDLE) {
			log2File("Doing FSM state transition: IDLE=>TURNING");
			_fsmState = FsmState.TURNING;
		}
	}

	/**
	 * resets the FSMs of this AND all lower layers
	 */
	public void reset() {
		log2File("in reset()");

		log2File("opening grabber");
		try {
			_robot.grab(0);
		} catch (ConnectionLostException e1) {
		}
		
		synchronized (_fsmState) {
			log2File("resetting _fsmState to IDLE");
			_fsmState = FsmState.IDLE;
		}

		if (_robot != null) {
			log2File("Calling _robot.reset()");
			try {
				_robot.grab(0);
			} catch (ConnectionLostException e) {
			}
			_robot.reset();
		} else {
			log2File("Skipped call to _robot.reset() as _robot is null");
		}

		log2File("reset() done");
	}

}
