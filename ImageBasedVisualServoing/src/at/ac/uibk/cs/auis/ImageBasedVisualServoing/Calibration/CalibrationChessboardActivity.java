package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Calibration;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.R;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common.CalibrationHelper;

/**
 * Activity is used for calibrating camera:
 *  a workflow is implemented that forces the user to touch the 4 points of interest (which world's coordinates
 *  need to be known);
 * Using these 4 points we can calculate a projection matrix (the transformation matrix from world to image-plane
 * coordinates and vice verca)
 * @author Helmut Wolf
 *
 */
public class CalibrationChessboardActivity extends Activity implements
		CvCameraViewListener2 {
	private static final String TAG = "Auis::CalibrationChessboardActivity";

	// UI elements
	//  --  calibration_chessboard_view.xml
	private CameraBridgeViewBase mOpenCvCameraView;
	private Button mCalibrateUsingChessBoard;
	private TextView mCalibrationLabel;
	//  --  calibration_chessboard_view.xml	

	// controller elements
	private CalibrationHelper calibrationHelper;
	
	public CalibrationChessboardActivity() {
//		List<Point> worldCoordinates = new ArrayList<Point>();
//		// TODO calculate right points
//		for(int i=0;i<8;i++) {
//			worldCoordinates.add(new Point(i, i));
//		}
		List<Point> worldCoordinates = new ArrayList<Point>();
		// TODO give correct dimensions [1]
		int dimX = 7;
		int dimY = 7;
		// TODO give correct offset [mm] (relative to robot position, which is at (0,0))
		int dx = 150;
		int dy = 150;
		// TODO give correct dimensions of 1 square [mm] (measured on printed A4-sheet)
		int lx = 50;
		int ly = 50;

		/*
		(0,0) (0,1) .... (0,6)
		   .
		   .
		   .
		(6,0) (6,1) .... (6,6)
		*/
		for(int i=0;i<dimY;i++) {
		      for(int j=0;j<dimX;j++) {
		            worldCoordinates.add(new Point(dx-i*lx, dy+j*ly));         
		      }
		}

		
		calibrationHelper = new CalibrationHelper(worldCoordinates);
	}
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				//mOpenCvCameraView.setOnTouchListener(CalibrationChessboardActivity.this);
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
		
		setContentView(R.layout.calibration_chessboard_view);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.JavaCameraView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mOpenCvCameraView.setClickable(false);
			}
		});
		
		mCalibrateUsingChessBoard = (Button)findViewById(R.id.CalibrateChessboardCorners);
		mCalibrateUsingChessBoard.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(CalibrationChessboardActivity.this, CalibrationSummaryActivity.class);
				Bundle b = new Bundle();
				b.putParcelable("calibrationHelper", calibrationHelper);
				intent.putExtras(b); //Put your id to your next Intent
				startActivity(intent);
				finish();				
			}
		});
		
		mCalibrationLabel = (TextView)findViewById(R.id.CalibrationLabel);
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
		Mat grey_8uc1 = new Mat();
		Imgproc.cvtColor(rgba, grey_8uc1, Imgproc.COLOR_RGB2GRAY);
		
		Size patternSize= new org.opencv.core.Size(7,7);
		MatOfPoint2f corners = new MatOfPoint2f();
		
		if(Calib3d.findChessboardCorners(grey_8uc1, patternSize, corners, Calib3d.CALIB_CB_ADAPTIVE_THRESH + Calib3d.CALIB_CB_NORMALIZE_IMAGE + Calib3d.CALIB_CB_FAST_CHECK)) {
			Log.i(TAG, "found Chessboard corners");
			calibrationHelper.clearImagePlaneCoordinates();
			for(int p=0;p<corners.size().height;p++) {
				double[] point = corners.get(p, 0);
				Log.d(TAG, "#" + p + ": (" + point[0] + ", " + point[1] + ")");
				calibrationHelper.addImagePlaneCoordinates(p, new Point(point[0],point[1]));
			}
			
			CalibrationChessboardActivity.this.runOnUiThread(new Runnable() {			
				@Override
				public void run() {
					mCalibrateUsingChessBoard.setEnabled(true);
					mCalibrateUsingChessBoard.setText("Calibrate");
				}
			});
			//Imgproc.cornerSubPix(rgba, corners, new Size(11, 11), new Size(-1, -1), new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 30, 0.1));
			Calib3d.drawChessboardCorners(rgba, patternSize, corners, true);
		} else {
			Log.i(TAG, "no Chessboard corners found");
			CalibrationChessboardActivity.this.runOnUiThread(new Runnable() {			
				@Override
				public void run() {
					mCalibrateUsingChessBoard.setEnabled(false);
					mCalibrateUsingChessBoard.setText("Find chessboard first...");
				}
			});
			Calib3d.drawChessboardCorners(rgba, patternSize, corners, false);
		}
		
		return rgba;
	}
}
