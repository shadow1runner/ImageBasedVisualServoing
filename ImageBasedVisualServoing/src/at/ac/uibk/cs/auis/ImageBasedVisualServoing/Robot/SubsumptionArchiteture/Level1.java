package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Robot.SubsumptionArchiteture;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;

import org.opencv.core.Point;

import android.util.Log;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Robot.Robot;

public class Level1 extends BaseIOIOLooper {
	private static final String TAG = "Level1";
	private int DEGREE_GRANULARITY = 4;
	private int ROUTE_GRANULARITY = 10;

	public Level1(Robot robot) {
		Log.v(TAG, "In Level1-ctor");
		_robot = robot;
	}
	
	/*
	 * the following state transitions are valid:
	 * IDLE
	 *   [setPoint set]
	 *  -> TURN 
	 *   [angle reached]
	 *  -> MOVE
	 * i.e. the robot first of all turns to the specified setPoint than moves to it
	 */
	private enum FsmState {
		IDLE,
		TURNING,
		MOVING,
		GRABBING
	};
	
	private FsmState _fsmState = FsmState.IDLE;
	private Robot _robot;
	private Point _setPoint;
	private int[] _degreesCached;
	private int _degreeCachePointer;
	private int[] _routesCached;
	private int _routeCachePointer;
	
	
	@Override
	public void loop() throws ConnectionLostException, InterruptedException {
		Log.v(TAG, "In loop()");
		if(_robot.workInProgress()) {
			Log.d(TAG, "Robot workInProgress: true");
			switch(_fsmState) {
			case IDLE:
				Log.d(TAG, "Staying in IDLE-mode");
				break;
			case MOVING:
				if(isRouteFinished()) {
					Log.d(TAG, "MOVING -> GRABBING");
					_fsmState = FsmState.GRABBING;
				} else {
					Log.d(TAG, "Staying in MOVING-mode");
				}
				break;
			case TURNING:
				if(isTurnFinished()) {
					Log.d(TAG, "TURNING -> MOVING");
					_fsmState = FsmState.MOVING;
				} else {
					Log.d(TAG, "Staying in TURNING-mode");
				}
				break;
			case GRABBING:
				if(isGrabbingFinished()) {
					Log.d(TAG, "GRABBING -> IDLE");
					_fsmState = FsmState.MOVING;
				} else {
					Log.d(TAG, "Staying in GRABBING-mode");
				}
				break;
			}
		} else {
			Log.d(TAG, "Robot workInProgress: false");
			// robot is waiting for new commands
			switch(_fsmState) {
			case IDLE:
				break;
			case MOVING:
				int amount = getNextScheduledRoute();
				Log.i(TAG, "Scheduling new move-amount for robot: " + amount + "cm");
				_robot.move(amount);
				break;			
			case TURNING:
				int degree = getNextScheduledAngle();
				Log.i(TAG, "Scheduling new rotate-amount for robot: " + degree + "°");
				_robot.rotate(degree);
				break;
			case GRABBING:
				// TODO
				break;
			default:
				break;
			
			}
		}
		
		Log.v(TAG, "calling _robot.loop");
		_robot.loop(); // always call base looper
	}
	
	private boolean isGrabbingFinished() {
		// TODO
		throw new RuntimeException();
	}

	private boolean isRouteFinished() {
		return _routeCachePointer==ROUTE_GRANULARITY;
	}

	private boolean isTurnFinished() {
		return _degreeCachePointer==DEGREE_GRANULARITY;
	}

	/**
	 * calculates and returns a given amount in [cm] using an multiplicative increase method
	 * @return
	 *  amount which should be scheduled to the robot to be moved
	 */
	public int getNextScheduledRoute() {
		Log.v(TAG, "In calculateRoute()");
		if(_routesCached==null) {
			calculateRouteSchedule();
		}
		
		int _return = _routesCached[_routeCachePointer++];
		Log.d(TAG, "Return new scheduled route: " + _return + "cm");
		return _return;
	}

	private void calculateRouteSchedule() {
		Log.v(TAG, "In calculateRouteSchedule()");
		_routesCached = new int[ROUTE_GRANULARITY];
		int temp = (int) (_setPoint.x / 2);
		int sum = 0;
		int i;
		for(i=0;i<_routesCached.length-1;i++) {
			sum += temp;
			_routesCached[i] = temp;
			temp /= 2;
			if(temp==0)
				break;
		}
		_routesCached[++i] = (int) (_setPoint.x - sum);
		ROUTE_GRANULARITY = i+1;
		Log.d(TAG, "calculated Route-Schedule[" + ROUTE_GRANULARITY + "] to: " + _routesCached);
		_routeCachePointer=0;
	}

	/**
	 * calculates and returns a given amount in [°] using an multiplicative increase method
	 * @return
	 *  degrees which should be scheduled to the robot to be turned
	 */
	private int getNextScheduledAngle() {
		if(_degreesCached==null) {
			calculateDegreeSchedule();
		}
		
		int _return = _degreesCached[_degreeCachePointer++];
		Log.d(TAG, "Return new scheduled route: " + _return + "°");
		return _return;
	}

	/**
	 * calculates a degree schedule
	 */
	private void calculateDegreeSchedule() {
		Log.v(TAG, "In calculateDegreeSchedule()");
		_degreesCached = new int[DEGREE_GRANULARITY];
		int temp = (int) (_setPoint.y / 2);
		int sum = 0;
		int i;
		for(i=0;i<_degreesCached.length-1;i++) {
			sum += temp;
			_degreesCached[i] = temp;
			temp /= 2;
			if(temp==0)
				break;
		}
		_degreesCached[++i] = (int) (_setPoint.y - sum);
		DEGREE_GRANULARITY = i+1;
		Log.d(TAG, "calculated Turn-Schedule[" + DEGREE_GRANULARITY + "] to: " + _degreesCached);
		_degreeCachePointer=0;
	}

	/**
	 * sets the setPoint to the current value
	 * - this does not change the robot behavior when he is already moving/turning;
	 *   but when a new schedule has to be planned, the current {@code setPoint} gonna be used for it
	 */
	public void setSetPoint(Point setPoint) {
		_setPoint = setPoint;
		if(_fsmState==FsmState.IDLE)
			_fsmState = FsmState.TURNING;
	}

}
