package at.ac.uibk.cs.auis.ImageBasedVisualServoing;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Calibration.CalibrationActivity;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Calibration.CalibrationChessboardActivity;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common.CalibrationHelper;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common.DrawHelper;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common.NavigationCalibrationHelper;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Navigation.NavigationCalibrationActivity;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Navigation.NavigationConstants;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Robot.Robot;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Robot.SubsumptionArchiteture.Level1;
import at.ac.uibk.cs.auis.Tracker.ColorBasedTracker;
import at.ac.uibk.cs.auis.Tracker.TrackerHelper;

/**
 * Activity is used for implementing ImageBasedVisualServoing This activity gets
 * a matrix which is used for calculating pixel coordinates to world-coordinates
 * 
 * @author Helmut Wolf
 * 
 */
public class ImageBasedVisualServoingActivity extends IOIOActivity implements
		CvCameraViewListener2, OnTouchListener {
	private static final String TAG = "Auis::ImageBasedVisualServoingActivity";

	private CameraBridgeViewBase mOpenCvCameraView;
	private Button _resetFsmsButton;
	private TextView _textView;

	private Mat hsv;

	private MenuItem CreateCalibrationMenuItem;
	private MenuItem CreateCalibrationChessboardMenuItem;
	private MenuItem SerializeCalibrationMenuItem;
	private MenuItem LoadCalibrationMenuItem;
	private MenuItem CalibrteWorldMenuItem;

	private ColorBasedTracker colorBasedTracker = new ColorBasedTracker();
	private TrackerHelper trackerHelper = new TrackerHelper();

	private CalibrationHelper calibrationHelper;
	private NavigationCalibrationHelper navigationCalibrationHelper;

	private static final Scalar INDICATING_COLOR = new Scalar(0xbf, 0xfe, 0x00,
			0x00);
	private boolean isTrackingColorSet = false;

	
	private static boolean append2File = false;

	private static void log2File(String message) {
		File root = Environment.getExternalStorageDirectory();
		File file = new File(root, "ImageBasedVisualServoing.txt");
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
	
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				mOpenCvCameraView
						.setOnTouchListener(ImageBasedVisualServoingActivity.this);
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	private NavigationCalibrationHelper navigationCalibrationActivity;

	// ------------------------------------------ ANDROID LIFECYLCE
	// ------------------------------------------

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.image_based_visual_servoing_view);
		
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mOpenCvCameraView.setClickable(false);
			}
		});
		
		_resetFsmsButton = (Button)findViewById(R.id.ResetFsmsButton);
		_resetFsmsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(TAG, "Resetting FSMs on user-request");
				isTrackingColorSet = false;
				_level1.reset();
				Toast.makeText(getApplicationContext(), "FSMs resetted successfully", Toast.LENGTH_LONG).show();
			}
		});
		
		_textView = (TextView)findViewById(R.id.TextView);
		

		Bundle b = getIntent().getExtras();
		if (b == null) {
			Log.e(TAG, "unable to get CalibrationHelper from intent-invocation, exiting");
		}

		Log.i(TAG,
				"trying to get a CalibrationHelper passed as argument on intent-invocation");
		Intent i = getIntent();
		calibrationHelper = (CalibrationHelper) i.getParcelableExtra("calibrationHelper");
		if (calibrationHelper == null) {
			Log.i(TAG, "unable to get calibrationHelper by unparceling");
		}
		else
			Log.i(TAG, "Got calibrationHelper successfully by unparceling");
		
		navigationCalibrationHelper = (NavigationCalibrationHelper) i.getParcelableExtra("navigationCalibrationHelper");
		if (navigationCalibrationHelper == null) {
			Log.i(TAG, "unable to get navigationCalibrationHelper by unparceling");
		}
		else {
			Log.i(TAG, "Got navigationCalibrationHelper successfully by unparceling");
			Toast.makeText(this.getApplicationContext(), "got navigationCalibrationHelper", Toast.LENGTH_LONG);
		}

	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	// ------------------------------------------ /ANDROID LIFECYLCE
	// ------------------------------------------

	// ------------------------------------------ MENU-MANAGEMENT & -HANDLING
	// ------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		CreateCalibrationMenuItem = menu.add("Calibrate");
		CreateCalibrationChessboardMenuItem = menu.add("Calibrate with chessboard");
		SerializeCalibrationMenuItem = menu.add("Serialize current calibration");
		LoadCalibrationMenuItem = menu.add("Load current calibration");
		CalibrteWorldMenuItem = menu.add("Calibrate World");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		if (item == CreateCalibrationMenuItem) {
			Log.i(TAG,
					"CreateCalibrationMenuItem has been clicked, invoking calibration intent");
			Intent calibrateIntent = new Intent(
					ImageBasedVisualServoingActivity.this,
					CalibrationActivity.class);
			startActivity(calibrateIntent);
		} else if (item == CreateCalibrationChessboardMenuItem) {
			Log.i(TAG,
					"CreateCalibrationChessboardMenuItem has been clicked, invoking calibration via chessboard intent");
			Intent calibrateIntent = new Intent(
					ImageBasedVisualServoingActivity.this,
					CalibrationChessboardActivity.class);
			startActivity(calibrateIntent);
		} else if (item == SerializeCalibrationMenuItem) {
			Log.i(TAG, "SerializeCalibrationMenuItem has been clicked");
			SerializeCalibration();
		} else if (item == LoadCalibrationMenuItem) {
			Log.i(TAG, "LoadCalibrationMenuItem has been clicked");
			DeSerializeCalibration();
		} else if (item == CalibrteWorldMenuItem) {
			Log.i(TAG, "CalibrateWorldMenuItem has been clicked, invoking calibrate world intent");
			Intent calibrateIntent = new Intent(
					ImageBasedVisualServoingActivity.this,
					NavigationCalibrationActivity.class);
			startActivity(calibrateIntent);
		} else {
			Log.e(TAG,
					"Invalid MenuItem has been clicked, quitting to home screen");
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
		}
		return true;
	}

	// ------------------------------------------ /MENU MANAGEMENT & HANDLING
	// ------------------------------------------

	// ------------------------------------------ HANDLER
	// ------------------------------------------
	Handler _handler = new Handler() {
		int i = 12;

		@Override
		public void handleMessage(Message msg) {
			int chkState = msg.arg1;

			if ((chkState & (1 << 5)) != 0) {
				// badhack
				if (i++ > 12) {
					Toast.makeText(getApplicationContext(), "Overheat",
							Toast.LENGTH_SHORT).show();
					i = 0;
				}
			} else {
				i = 12;
			}
		}
	};

	// ------------------------------------------ /HANDLER
	// ------------------------------------------

	// ------------------------------------------ IOIO MANAGEMENT
	// ------------------------------------------
	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		Log.d(TAG, "in createIOIOLooper()");
		return _robot;
	}

	// ------------------------------------------ /IOIO MANAGEMENT
	// ------------------------------------------

	// ------------------------------------------ CAMERA MANAGEMENT
	// ------------------------------------------
	public void onCameraViewStarted(int width, int height) {
	}

	public void onCameraViewStopped() {
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		Mat rgba = inputFrame.rgba();

		if (isTrackingColorSet) {
			hsv = new Mat();
			Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV_FULL);

			Point[] lowestPoints = null;
			try {
				// lowestPoints =
				// colorBasedTracker.getLowestBoundOfContours(hsv, 2); // 2
				// beacons SHOULD be in view
				lowestPoints = colorBasedTracker.getLowestBoundOfContours(hsv, 1); // 2 beacons SHOULD be in view

			} catch (IllegalArgumentException e) {
			}

			try {
				for (Point point : lowestPoints) {
					rgba = DrawHelper.drawPoint(rgba, point, new Scalar(0xFF, 0x00,0x00, 0x00));
					if(calibrationHelper!=null) {
						Point groundPlane = calibrationHelper.calculateGroundPlaneCoordinates(point);
						
						Log.i(TAG, "Ground plane coordinates: (" + groundPlane.x + ", " + groundPlane.y + ")");
						
						_level1.setSetPoint(groundPlane);
					}
				}
	
				for (Rect rect : colorBasedTracker.getBoundingRects())
					rgba = DrawHelper.drawRectangle(rgba, rect, INDICATING_COLOR);
			
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		} else {
			hsv = rgba;
		}

		return rgba;
	}

	// ------------------------------------------ /CAMERA MANAGEMENT
	// ------------------------------------------

	// ------------------------------------------ ROBOT MANAGEMENT
	// ------------------------------------------
	private Level1 _level1 = new Level1();
	private Robot _robot = new Robot(_handler, _level1);
	//private Robot _robot = new Robot(_handler);

	// ------------------------------------------ /ROBOT MANAGEMENT
	// ------------------------------------------

	// ------------------------------------------ SERIALIZATION
	// ------------------------------------------
	private void SerializeCalibration() {
		if (calibrationHelper == null) {
			Log.e(TAG, "No calibration data to be serialized");
			Toast.makeText(getApplicationContext(),
					"Camera has not yet been calibrated.", Toast.LENGTH_LONG)
					.show();
		}

		try {
			FileOutputStream fos = getApplicationContext().openFileOutput(
					"calibrationHelper.bndl",
					getApplicationContext().MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(calibrationHelper);
			oos.flush();
			oos.close();
			Toast.makeText(getApplicationContext(),
					"Calibration-data saved successfully", Toast.LENGTH_LONG)
					.show();
		} catch (IOException e) {
			Log.e(TAG,
					"IOExcpetion occurded during saving the calibration-data",
					e);
			Toast.makeText(getApplicationContext(), "Unable to save data",
					Toast.LENGTH_LONG).show();
		}
	}

	private void DeSerializeCalibration() {
		try {
			FileInputStream fis = getApplicationContext().openFileInput(
					"calibrationHelper.bndl");
			ObjectInputStream ois = new ObjectInputStream(fis);
			calibrationHelper = (CalibrationHelper) ois.readObject();
			ois.close();
			Toast.makeText(getApplicationContext(),
					"Calibration-data loaded successfully", Toast.LENGTH_LONG)
					.show();
		} catch (ClassNotFoundException cnf) {
			Log.e(TAG,
					"ClassNotFoundException occured during saving the calibration-data",
					cnf);
			Toast.makeText(getApplicationContext(),
					"Dev-error: Unable to load data - contact developer",
					Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e(TAG,
					"IOExcpetion occured during saving the calibration-data", e);
			Toast.makeText(getApplicationContext(), "Unable to load data",
					Toast.LENGTH_LONG).show();
		}
	}

	// ------------------------------------------ SERIALIZATION
	// ------------------------------------------

	private boolean targetReached=false;
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(navigationCalibrationHelper!=null) {
			if(calibrationHelper==null) {
				Log.d(TAG, "trying to get CalibrationHelper by deserializing");
				DeSerializeCalibration();
			}
				
			navigationCalibrationHelper.setCalibrationHelper(calibrationHelper);
			Point setPoint = navigationCalibrationHelper.getWorldGroundPlaneCoordinates();
			
			if(targetReached==false) {
				moveToGoal(setPoint);
				targetReached=true;
			}
			Log.d(TAG, "robot is at " + setPoint);
		}
		else {
			if(calibrationHelper==null) {
				Log.i(TAG, "calibrationhelper is null, user has to calibrate first, cancelling onTouch");
				//_textView.setText("calibrationhelper is null, plz calibrate first");
				Toast.makeText(this.getApplicationContext(), "calibrationhelper is null, plz calibrate first", Toast.LENGTH_LONG).show();
				return false;
			} else {
				//_textView.setText("Reset FSMs");
			}
			
			int cols = hsv.cols();
			int rows = hsv.rows();
	
			int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
			int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
	
			int x = (int) event.getX() - xOffset;
			int y = (int) event.getY() - yOffset;
	
			Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");
			
			Log.i(TAG, "Egocentric world coordinates: " + calibrationHelper.calculateGroundPlaneCoordinates(new Point(x,y)));
	
			if ((x < 0) || (y < 0) || (x > cols) || (y > rows))
				return false;
	
			colorBasedTracker.setColorForTrackingHSV(trackerHelper
					.calcColorForTracking(hsv, new Point(x, y)));
			isTrackingColorSet = true;
		}

		return true;
	}

	private void moveToGoal(Point setPoint) {
		log2File("in moveToGoal with setPoint: " + setPoint);
		
		double dx = NavigationConstants.GoalLocationWorldCoordinates.x - setPoint.x;
		double dy = NavigationConstants.GoalLocationWorldCoordinates.y - setPoint.y;
		
		double length = Math.sqrt(dx*dx + dy*dy);
		double angle = Math.atan(Math.abs(dy)/Math.abs(dx)); // notice inversed x and y
		
		if(dy>0 && dx<0) { // 1st quadrant
			angle *= -1;
		} else if(dy<0 && dx<0) { // 2nd quadrant
			// passt
		} else if(dy<0 && dx>0) { // 3rd quadrant
			angle = Math.PI - angle;
		} else if(dy>0 && dx>0) { //4th quadrant
			angle = angle - Math.PI;
		}
		
		
		log2File("moveToGoal: angle=" + angle + "°, length=" + length);
		
		//_level1.s
		try {
			_robot.rotate((int)(angle*180/Math.PI));

			Thread.currentThread().sleep(3000); // sleep 10 seconds

			_robot.move((int)length);
		} catch (ConnectionLostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
