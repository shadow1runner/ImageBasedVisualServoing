package at.ac.uibk.cs.auis.ImageBasedVisualServoing;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

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

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

import at.ac.uibk.cs.auis.ImageBasedVisualServoing.R;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Calibration.CalibrationActivity;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common.CalibrationHelper;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common.DrawHelper;
import at.ac.uibk.cs.auis.Tracker.ColorBasedTracker;
import at.ac.uibk.cs.auis.Tracker.TrackerHelper;

/**
 * Activity is used for implementing ImageBasedVisualServoing
 * This activity gets a matrix which is used for calculating
 * pixel coordinates to world-coordinates
 * @author Helmut Wolf
 *
 */
public class ImageBasedVisualServoingActivity extends Activity implements
		CvCameraViewListener2, OnTouchListener  {
	private static final String TAG = "Auis::ImageBasedVisualServoingActivity";

	private CameraBridgeViewBase mOpenCvCameraView;

	private Mat hsv;

	private CalibrationHelper calibrationHelper;

	private MenuItem CreateCalibrationMenuItem;
	private MenuItem SerializeCalibrationMenuItem;
	private MenuItem LoadCalibrationMenuItem;
	
	private ColorBasedTracker colorBasedTracker = new ColorBasedTracker();
	private TrackerHelper trackerHelper = new TrackerHelper();

	private static final Scalar INDICATING_COLOR = new Scalar(0xbf, 0xfe, 0x00, 0x00);
	
	private boolean isTrackingColorSet = false;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				mOpenCvCameraView.setOnTouchListener(ImageBasedVisualServoingActivity.this);
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

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
		
		
		Bundle b = getIntent().getExtras();
		if(b==null) {
			Log.e(TAG, "unable to get CalibrationHelper from intent-invocation, exiting");
//			Intent intent = new Intent(Intent.ACTION_MAIN);
//			intent.addCategory(Intent.CATEGORY_HOME);
//			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			startActivity(intent);
//			finish();
		}
		
		Log.i(TAG, "trying to get a CalibrationHelper passed as argument on intent-invocation");
		Intent i = getIntent();
		calibrationHelper = (CalibrationHelper) i.getParcelableExtra("calibrationHelper");
		if(calibrationHelper==null)
			Log.e(TAG, "unable to get calibrationHelper");
		else
			Log.e(TAG, "Got calibrationHelper successfully");

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

	public void onCameraViewStarted(int width, int height) {
	}

	public void onCameraViewStopped() {
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		
		Mat rgba = inputFrame.rgba();
		
		if(isTrackingColorSet) {
			hsv = new Mat();
			Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV_FULL);
			
			Point[] lowestPoints = null;
			try {
				//lowestPoints = colorBasedTracker.getLowestBoundOfContours(hsv, 2); // 2 beacons SHOULD be in view
				lowestPoints = colorBasedTracker.getLowestBoundOfContours(hsv, 1); // 2 beacons SHOULD be in view
			} catch (IllegalArgumentException e) {
			}
			
			for(Point point : lowestPoints) {
				rgba = DrawHelper.drawPoint(rgba, point, new Scalar(0xFF, 0x00, 0x00, 0x00));
			}
			
			for(Rect rect : colorBasedTracker.getBoundingRects())
				rgba = DrawHelper.drawRectangle(rgba, rect, INDICATING_COLOR);
		} else {
			hsv=rgba;
		}
		
		return rgba;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		CreateCalibrationMenuItem = menu.add("Calibrate");
		SerializeCalibrationMenuItem = menu.add("Serialize current calibration");
		LoadCalibrationMenuItem = menu.add("Load current calibration");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		if(item==CreateCalibrationMenuItem) {
			Log.i(TAG, "CreateCalibrationMenuItem has been clicked, invoking calibration intent");
			Intent calibrateIntent = new Intent(ImageBasedVisualServoingActivity.this, CalibrationActivity.class);
	        startActivity(calibrateIntent);
		} else if(item==SerializeCalibrationMenuItem) {
			Log.i(TAG, "SerializeCalibrationMenuItem has been clicked");
			SerializeCalibration();
		} else if(item==LoadCalibrationMenuItem) {
			Log.i(TAG, "LoadCalibrationMenuItem has been clicked");
			DeSerializeCalibration();
		} else {
			Log.e(TAG, "Invalid MenuItem has been clicked, quitting to home screen");
			Intent startMain = new Intent(Intent.ACTION_MAIN);
	        startMain.addCategory(Intent.CATEGORY_HOME);
	        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        startActivity(startMain);
		}
		return true;
	}

	private void SerializeCalibration() {
		if(calibrationHelper==null) {
			Log.e(TAG, "No calibration data to be serialized");
			Toast.makeText(getApplicationContext(), "Camera has not yet been calibrated.", Toast.LENGTH_LONG).show();
		}
		
		try {
			FileOutputStream fos = getApplicationContext().openFileOutput("calibrationHelper.bndl", getApplicationContext().MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(calibrationHelper);
			oos.flush();
			oos.close();
			Toast.makeText(getApplicationContext(), "Calibration-data saved successfully", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e(TAG, "IOExcpetion occurded during saving the calibration-data", e);
			Toast.makeText(getApplicationContext(), "Unable to save data", Toast.LENGTH_LONG).show();
		}
	}
	
	private void DeSerializeCalibration() {
		try {
			FileInputStream fis = getApplicationContext().openFileInput("calibrationHelper.bndl");
			ObjectInputStream ois = new ObjectInputStream(fis);
			calibrationHelper = (CalibrationHelper) ois.readObject();
			ois.close();
			Toast.makeText(getApplicationContext(), "Calibration-data loaded successfully", Toast.LENGTH_LONG).show();
		} catch (ClassNotFoundException cnf) {
			Log.e(TAG, "ClassNotFoundException occured during saving the calibration-data", cnf);
			Toast.makeText(getApplicationContext(), "Dev-error: Unable to load data - contact developer", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e(TAG, "IOExcpetion occured during saving the calibration-data", e);
			Toast.makeText(getApplicationContext(), "Unable to load data", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
	    int cols = hsv.cols();
        int rows = hsv.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows))
        	return false;
        
        // todo
        Point groundPlane = calculateGroundPlaneCoordinates(new Point(x, y));
        
        Log.i(TAG, "Ground plane coordinates: (" + groundPlane.x + ", " + groundPlane.y + ")");
        
        colorBasedTracker.setColorForTrackingHSV(trackerHelper.calcColorForTracking(hsv, new Point(x,y)));
        isTrackingColorSet = true;
        
        return true	;
	}

	private Point calculateGroundPlaneCoordinates(Point imagePlaneCoordinates) {
//		Point groundPlane = new Point();
//		Mat imagePlane = new Mat();
//		imagePlane.
//		groundPlane = projectiveMatrix.mul();
		
		Mat imagePlane2WorldCoordinates = calibrationHelper.getHomogenousMat();
		
		// get homogeneous coordinates out of supplied imagePlaneCoordinates
		Mat mat3 = new Mat(3, 1, CvType.CV_64FC1);
		mat3.put(0,0, new double[] {imagePlaneCoordinates.x, imagePlaneCoordinates.y, 1.0f});
		
		
		Mat dest = new Mat(3, 1, CvType.CV_64FC1);
		// 3*3*CV_64FC1 x 3*1*CV_64FC1 -> 3*1*CV_64FC1
		
		// see http://stackoverflow.com/questions/10168058/basic-matrix-multiplication-in-opencv-for-android
		//Core.multiply(imagePlane2WorldCoordinates, mat3, dest);
		Core.gemm(imagePlane2WorldCoordinates, mat3, 1, new Mat(), 0, dest, 0);
		return new Point(dest.get(0, 0)[0]/dest.get(2, 0)[0], dest.get(1, 0)[0]/dest.get(2, 0)[0]); // convert into homogeneous coordinate with form (x,y,1)
	}

}