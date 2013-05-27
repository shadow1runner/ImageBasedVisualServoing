package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Navigation;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.R;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Calibration.CalibrationSummaryActivity;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common.CalibrationHelper;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common.DrawHelper;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common.NavigationCalibrationHelper;
import at.ac.uibk.cs.auis.Tracker.ColorBasedTracker;
import at.ac.uibk.cs.auis.Tracker.TrackerHelper;

public class NavigationCalibrationActivity extends Activity implements
		CvCameraViewListener2,
		OnTouchListener,
		View.OnClickListener {
	private static final String TAG = "Auis::NavigationCalibrationActivity";

	private static final int SIZE_OF_CENTER_OF_MASS = 6;

	private static final Scalar INDICATING_COLOR = new Scalar(0xbf, 0xfe, 0x00, 0x00);

	// UI elements
	//  --  calibration_wizard_view.xml
	private CameraBridgeViewBase mOpenCvCameraView;
	private Button mCalibrationButtonNext;
	private Button mCalibrationButtonBack;
	private TextView mCalibrationLabel;
	//  --  calibration_summary_view.xml	

	// controller elements
	private ColorBasedTracker colorBasedTracker = new ColorBasedTracker();
	private TrackerHelper trackerHelper = new TrackerHelper();
	private NavigationCalibrationHelper calibrationHelper;
	private States state = States.GatherFirstBeacon;
	private Point beaconLowestPoint;
	private boolean isTrackingColorSet = false;

	private enum States {
		GatherFirstBeacon,
		GatherSecondBeacon,
		GatherThirdBeacon,
		GatherFourthBeacon,
		DisplaySummary
	}
	
	private Mat mHsv;
	
	public NavigationCalibrationActivity() {
		// started from (0,0) each beacon is enumerated counterclockwise (CCW)
		List<Point> worldCoordinates = new ArrayList<Point>();
		worldCoordinates.add(new Point(0, 0));
		//worldCoordinates.add(new Point(150, 0));
		//worldCoordinates.add(new Point(100, 0));
		worldCoordinates.add(new Point(0, 100));
		worldCoordinates.add(new Point(0, 300));
		worldCoordinates.add(new Point(150, 300));
		worldCoordinates.add(new Point(150, 150));
		worldCoordinates.add(new Point(150, 0));
		
		calibrationHelper = new NavigationCalibrationHelper(worldCoordinates);
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				mOpenCvCameraView.setOnTouchListener(NavigationCalibrationActivity.this);
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
		
		setContentView(R.layout.calibration_wizard_view);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_based_tracking_surface_view);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mOpenCvCameraView.setClickable(false);
			}
		});
		
		mCalibrationButtonNext = (Button)findViewById(R.id.CalibrationNextButton);
		mCalibrationButtonNext.setOnClickListener(this);
		
		mCalibrationButtonBack = (Button)findViewById(R.id.CalibrationBackButton);
		mCalibrationButtonBack.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				reverseStateTransition();				
			}
		});
		
		mCalibrationLabel = (TextView)findViewById(R.id.CalibrationLabel);
		
		updateControls();
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

	public synchronized Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		
		Mat rgba = inputFrame.rgba();
		
		if(isTrackingColorSet) {
			mHsv = new Mat();
			Imgproc.cvtColor(rgba, mHsv, Imgproc.COLOR_RGB2HSV_FULL);
			
			beaconLowestPoint = null;
			
			try {
				beaconLowestPoint = colorBasedTracker.getLowestBoundOfContours(mHsv, 1)[0];
			} catch (IllegalArgumentException e) {
				Log.e(TAG, e.getMessage());
			}

			if(beaconLowestPoint==null) {
				NavigationCalibrationActivity.this.runOnUiThread(new Runnable() {			
					@Override
					public void run() {
						mCalibrationButtonNext.setEnabled(false);
						mCalibrationButtonNext.setText(getString(R.string.CalibrationButtonNextText));
					}
				});
			} else {
				NavigationCalibrationActivity.this.runOnUiThread(new Runnable() {			
					@Override
					public void run() {
						mCalibrationButtonNext.setEnabled(true);
						mCalibrationButtonNext.setText(getString(R.string.CalibrationButtonNextText) + " (" + beaconLowestPoint.x + ", " + beaconLowestPoint.y + ")");
					}
				});
				DrawHelper.drawPoint(rgba, beaconLowestPoint, new Scalar(0xFF, 0x00, 0x00, 0x00));
			}
			
			
			if (colorBasedTracker.getBoundingRects().size()>0)
				rgba = DrawHelper.drawRectangle(rgba, colorBasedTracker.getBoundingRects().get(0), INDICATING_COLOR);
		} else {
			NavigationCalibrationActivity.this.runOnUiThread(new Runnable() {			
				@Override
				public void run() {
					mCalibrationButtonNext.setEnabled(false);
					mCalibrationButtonNext.setText(getString(R.string.CalibrationButtonNextText));
				}
			});
			mHsv=rgba;
		}
		
		return rgba;
	}


	/**
	 * does a state transition of the FSM depending on the actual state;
	 * the current centerOfMass is added to the list in {@code pointHelper}
	 * is the target state of the fsm reached, a new activity is launched
	 */
	protected synchronized void doStateTransition() {
		if(beaconLowestPoint==null) {
			Log.i(TAG, "centerOfMass must not be null, no state-transition is done.");
			return;
		}
		
		int pointNumber = -1;
		switch(state) {
		case GatherFirstBeacon:
			pointNumber = 0;
			state = States.GatherSecondBeacon;
			break;
		case GatherSecondBeacon:
			pointNumber = 1;
			state = States.DisplaySummary;
			break;
		default:
			state=States.DisplaySummary;
			break;
		}
		
		if(beaconLowestPoint==null) {
			Log.i(TAG, "centerOfMass must not be null, no state-transition is done.");
			return;
		}
		calibrationHelper.addImagePlaneCoordinates(pointNumber, beaconLowestPoint);
		
		updateControls();
	}
	
	/**
	 * happens when user clicks the back button
	 */
	protected synchronized void reverseStateTransition() {
		switch(state) {
		case GatherSecondBeacon:
			state = States.GatherFirstBeacon;
			break;
		case GatherFirstBeacon:
			throw new IllegalStateException(); 
		}
		
		updateControls();
	}

	private void updateControls() {
		int whichPoint = -1;
		switch(state) {
		case GatherFirstBeacon:
			whichPoint = 1;
			mCalibrationButtonBack.setEnabled(false);
			mCalibrationButtonNext.setEnabled(true);
			break;
		case GatherSecondBeacon:
			whichPoint = 2;
			mCalibrationButtonBack.setEnabled(true);
			mCalibrationButtonNext.setEnabled(true);
			break;
		case DisplaySummary:			
			// http://stackoverflow.com/questions/3913592/start-an-activity-with-a-parameter
			Intent intent = new Intent(this, NavigationCalibrationSummaryActivity.class);
			Bundle b = new Bundle();
			b.putParcelable("calibrationNavigationHelper", calibrationHelper);
			intent.putExtras(b); //Put your id to your next Intent
			startActivity(intent);
			finish();
		}
		if(state!=States.DisplaySummary)
			mCalibrationLabel.setText(String.format(getString(R.string.CalibrationLabelPoint), whichPoint, (int)calibrationHelper.getWorldCoordinates(whichPoint).x, (int)calibrationHelper.getWorldCoordinates(whichPoint).y));
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int cols = mHsv.cols();
        int rows = mHsv.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows))
        	return false;
        
        Point point = new Point(x, y);
        colorBasedTracker.setColorForTrackingHSV(trackerHelper.calcColorForTracking(mHsv, point));
        
        isTrackingColorSet = true;
        
        return true;
	}

	@Override
	public void onClick(View view) {
		doStateTransition();		
	}
}
