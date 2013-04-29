package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Calibration;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.ImageBasedVisualServoingActivity;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.R;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common.CalibrationHelper;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.R.id;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.R.layout;
import at.ac.uibk.cs.auis.Tracker.ColorBasedTracker;
import at.ac.uibk.cs.auis.Tracker.TrackerHelper;

/**
 * @author Helmut Wolf
 */
public class CalibrationSummaryActivity extends Activity {
	private static final String TAG = "Auis::CalibrationSummaryActivity";
	
	// UI elements
	//  --  calibration_summary_view.xml	

	// controller elements
	private CalibrationHelper calibrationHelper;

	private TextView summaryPoint1;

	private TextView summaryPoint2;

	private TextView summaryPoint3;

	private TextView summaryPoint4;

	private TextView summaryHeader;	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.calibration_summary_view);
		
		Log.i(TAG, "trying to get a CalibrationHelper passed as argument on intent-invocation");
		Bundle b = getIntent().getExtras();
		if(b==null) {
			Log.e(TAG, "unable to get CalibrationHelper from intent-invocation, exiting");
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();
		}
		
		Intent i = getIntent();
		calibrationHelper = (CalibrationHelper) i.getParcelableExtra("calibrationHelper");
		
		summaryHeader = (TextView) findViewById(R.id.summaryHeader);
		summaryPoint1 = (TextView) findViewById(R.id.summaryPoint1);
		summaryPoint2 = (TextView) findViewById(R.id.summaryPoint2);
		summaryPoint3 = (TextView) findViewById(R.id.summaryPoint3);
		summaryPoint4 = (TextView) findViewById(R.id.summaryPoint4);
		
		List<String> summary = calibrationHelper.getSummary();
		summaryHeader.setText(summary.get(0));
		summaryPoint1.setText(summary.get(1));
		summaryPoint2.setText(summary.get(2));
		summaryPoint3.setText(summary.get(3));
		summaryPoint4.setText(summary.get(4));
		
		Button restartCalibration = (Button) findViewById(R.id.restartCalibration);
		restartCalibration.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(TAG, "restarting calibration - sending intent.");
				Intent intent = new Intent(CalibrationSummaryActivity.this, CalibrationActivity.class);
				startActivity(intent);
				finish();
			}
		});
		Button acceptCalibration = (Button) findViewById(R.id.acceptCalibration);
		acceptCalibration.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.i(TAG, "calibration done - starting main-activity");
				Intent intent = new Intent(CalibrationSummaryActivity.this, ImageBasedVisualServoingActivity.class);
				Bundle b = new Bundle();
				b.putParcelable("calibrationHelper", calibrationHelper);
				intent.putExtras(b); //Put your id to your next Intent
				startActivity(intent);
				finish();
			}
		});
	}

}
