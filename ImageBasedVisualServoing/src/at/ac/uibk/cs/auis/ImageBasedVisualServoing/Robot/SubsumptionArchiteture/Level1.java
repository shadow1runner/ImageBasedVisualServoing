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
			// TODO Auto-generated catch block
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
		IDLE, TURNING, MOVING, GRABBING
	};

	private FsmState _fsmState = FsmState.IDLE;
	private Robot _robot;
	private Point _setPoint;
	private int[] _degreesCached;
	private int _degreeCachePointer;
	private int[] _routesCached;
	private int _routeCachePointer;
	private boolean _grabFinished;

	public void setup(Robot robot) throws ConnectionLostException {
		log2File("in setup()");
		_robot = robot;
	}

	public void loop() throws ConnectionLostException, InterruptedException {
		log2File("in loop()");
		if (_robot.workInProgress()) {
			log2File("Robot workInProgress: true");
			switch (_fsmState) {
			case IDLE:
				log2File("Staying in IDLE-mode");
				break;
			case MOVING:
				if (isRouteFinished()) {
					log2File("MOVING -> GRABBING");
					_fsmState = FsmState.GRABBING;
					
					log2File("_routesCached = null;");
					_routesCached = null;
					
					log2File("setting _grabFinished to false");
					_grabFinished = false;
				} else {
					log2File("Staying in MOVING-mode");
				}
				break;
			case TURNING:
				if (isTurnFinished()) {
					log2File("TURNING -> MOVING");
					_fsmState = FsmState.MOVING;
					
					log2File("_degreesCached = null;");
					_degreesCached = null;
				} else {
					log2File("Staying in TURNING-mode");
				}
				break;
			case GRABBING:
				if (isGrabbingFinished()) {
					log2File("GRABBING -> IDLE");
					_fsmState = FsmState.MOVING;
				} else {
					log2File("Staying in GRABBING-mode");
				}
				break;
			}
		} else {
			log2File("Robot workInProgress: false");
			// robot is waiting for new commands
			switch (_fsmState) {
			case IDLE:
				_robot.grab(0);
				break;
			case MOVING:
				int amount = getNextScheduledRoute();
				log2File("Scheduling new move-amount for robot: " + amount + "cm");
				_robot.move(amount);
				break;
			case TURNING:
				int degree = getNextScheduledAngle();
				log2File("Scheduling new rotate-amount for robot: " + degree
						+ "°");
				_robot.rotate(degree);
				break;
			case GRABBING:
				log2File("Scheduling new grab-command for robot");
				_robot.grab(100);
				_grabFinished = true;
				_fsmState = FsmState.IDLE;
				break;
			default:
				break;
			}
		}
	}

	private boolean isGrabbingFinished() {
		log2File("in isGrabbingFinished()");
		
		log2File("_grabFinished: " + _grabFinished);
		return _grabFinished == true;
	}

	private boolean isRouteFinished() {
		log2File("in isRouteFinished()");
		log2File("_routeCachePointer == ROUTE_GRANULARITY: "
				+ _routeCachePointer + " == " + ROUTE_GRANULARITY);
		return _routeCachePointer == ROUTE_GRANULARITY;
	}

	private boolean isTurnFinished() {
		return _degreeCachePointer == DEGREE_GRANULARITY;
	}

	/**
	 * calculates and returns a given amount in [cm] using an multiplicative
	 * increase method
	 * 
	 * @return amount which should be scheduled to the robot to be moved
	 */
	public int getNextScheduledRoute() {
		log2File("In getNextScheduledRoute()");
		if (_routesCached == null) {
			calculateRouteSchedule();
		}
		if(isRouteFinished()) {
			log2File("Route is finished, returning nothing");
			return 0;
		}
		
		int _return = _routesCached[_routeCachePointer++];
		log2File("Return new scheduled route: " + _return + "cm");
		return _return;
	}

	private void calculateRouteSchedule() {
		log2File("In calculateRouteSchedule()");
		_routesCached = new int[ROUTE_GRANULARITY];
		int temp = (int) (_setPoint.x / 2);
		int sum = 0;
		int i;
		for (i = 0; i < _routesCached.length - 1; i++) {
			log2File("in calculateRouteSchedule(), iteration #" + i);
			sum += temp;

			log2File("#" + i + ": _routesCached[" + i + "] = " + temp);
			_routesCached[i] = temp;
			temp /= 2;
			if (temp == 0) {
				log2File("#" + i + "; breaking because temp=0");
				break;
			}
		}
		_routesCached[i] = (int) (_setPoint.x - sum);
		log2File("_routesCached[" + i + "] = " + _routesCached[i]);

		ROUTE_GRANULARITY = i + 1;
		log2File("calculated Route-Schedule[" + ROUTE_GRANULARITY + "] to: "
				+ _routesCached);
		_routeCachePointer = 0;
	}

	/**
	 * calculates and returns a given amount in [°] using an multiplicative
	 * increase method
	 * 
	 * @return degrees which should be scheduled to the robot to be turned
	 */
	private int getNextScheduledAngle() {
		log2File("in getNextScheduledAngle()");
		if (_degreesCached == null) {
			calculateDegreeSchedule();
		}

		int _return = _degreesCached[_degreeCachePointer++];
		log2File("Return new scheduled route: " + _return + "°");
		return _return;
	}

	/**
	 * calculates a degree schedule
	 */
	private void calculateDegreeSchedule() {
		log2File("in calculateDegreeSchedule()");
		_degreesCached = new int[DEGREE_GRANULARITY];
		int temp = (int) (_setPoint.y / 2);
		int sum = 0;
		int i;
		for (i = 0; i < _degreesCached.length - 1; i++) {
			log2File("in calculateDegreeSchedule(), iteration #" + i);
			sum += temp;

			log2File("#" + i + ": _degreesCached[" + i + "] = " + temp);
			_degreesCached[i] = temp;
			temp /= 2;
			if (temp == 0) {
				log2File("#" + i + "; breaking because temp=0");
				break;
			}
		}

		_degreesCached[i] = (int) (_setPoint.y - sum);
		log2File("_degreesCached[" + i + "] = " + _degreesCached[i]);
		DEGREE_GRANULARITY = i + 1;
		log2File("calculated Turn-Schedule[" + DEGREE_GRANULARITY + "] to: "
				+ _degreesCached);
		_degreeCachePointer = 0;
	}

	/**
	 * sets the setPoint to the current value - this does not change the robot
	 * behavior when he is already moving/turning; but when a new schedule has
	 * to be planned, the current {@code setPoint} gonna be used for it
	 */
	public void setSetPoint(Point setPoint) {
		log2File("in setSetPoint()");
		_setPoint = setPoint;
		if (_fsmState == FsmState.IDLE) {
			log2File("Doing FSM state transition: IDLE=>TURNING");
			_fsmState = FsmState.TURNING;
		} else {
			log2File("Robot not in IDLE ignoring setSetPoint");
		}
	}

	/**
	 * resets the FSMs of this AND all lower layers
	 */
	public void reset() {
		log2File("in reset()");
		
		synchronized (_fsmState) {
			log2File("resetting _fsmState to IDLE");
			_fsmState = FsmState.IDLE;
			
			log2File("setting _degreesCached and _routesCached to null");
			_degreesCached = null;
			_routesCached = null;
		}
		
		if(_robot!=null) {
			log2File("Calling _robot.reset()");		
			_robot.reset();
		} else {
			log2File("Skipped call to _robot.reset() as _robot is null");
		}
		
		log2File("reset() done");		
	}

}
