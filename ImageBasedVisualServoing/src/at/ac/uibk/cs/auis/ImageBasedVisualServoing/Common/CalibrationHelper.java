package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import android.os.Parcel;
import android.os.Parcelable;

public class CalibrationHelper implements Parcelable, Serializable {
	private static final long serialVersionUID = 4529667799595429110L;

	private ArrayList<ParcelablePoint> _imagePlaneCoordinates = new ArrayList<ParcelablePoint>();
	private MatOfPoint2f _worldCoordinates;

	private Mat _cachedImagePlane2WorldCoordinates;

	public CalibrationHelper(MatOfPoint2f worldCoordinates) {
		_worldCoordinates = worldCoordinates;
		
//		worldCoordinates.add(new ParcelablePoint(175.0, 125.0));
//		worldCoordinates.add(new ParcelablePoint(175.0, 25.0));
//		worldCoordinates.add(new ParcelablePoint(275.0, 25.0));
//		worldCoordinates.add(new ParcelablePoint(275.0, 125.0));
	}
	
	public CalibrationHelper(List<Point> worldCoordinates) {
		_worldCoordinates = convertToMatOfPoints(worldCoordinates);
	}

	public void addImagePlaneCoordinates(int pointNumber, Point point) {
		if (pointNumber < 0)
			throw new IllegalArgumentException();
		if (point == null)
			throw new IllegalArgumentException("point must not be null");
		
		_imagePlaneCoordinates.ensureCapacity((int) _worldCoordinates.size().height);
		_imagePlaneCoordinates.add(pointNumber, new ParcelablePoint(point));
	}
	
	public void clearImagePlaneCoordinates() {
		_imagePlaneCoordinates.clear();
	}

	public Point getWorldCoordinates(int pointNumber) {
		if (pointNumber <= 0)
			throw new IllegalArgumentException();

		return new Point(_worldCoordinates.get(pointNumber - 1, 0)[0], _worldCoordinates.get(pointNumber - 1, 0)[1]);
	}

	public List<String> getSummary() {
		List<String> strings = new ArrayList<String>();
		strings.add("Image-Plane -> Ground-Plane");

		for (int i = 0; i < _imagePlaneCoordinates.size(); i++)
			strings.add(PointToString(_imagePlaneCoordinates.get(i)) + " -> "
					+ PointToString(new Point(_worldCoordinates.get(i, 0)[0], _worldCoordinates.get(i, 0)[1])));

		return strings;
	}

	public String PointToString(Point point) {
		return "(" + point.x + ", " + point.y + ")";
	}
	
	public Mat getHomogenousMat() {
		if(_cachedImagePlane2WorldCoordinates!=null)
			return _cachedImagePlane2WorldCoordinates;
		
		calculateHomogenousMat();
		return _cachedImagePlane2WorldCoordinates;
	}

	private void calculateHomogenousMat() {
//		Mat src = new Mat(4, 1, CvType.CV_32FC2);
//		src.put(0, 0, new double[] {imagePlaneCoordinates.get(0).x, imagePlaneCoordinates.get(0).y});
//		src.put(1, 0, new double[] {imagePlaneCoordinates.get(1).x, imagePlaneCoordinates.get(1).y});
//		src.put(2, 0, new double[] {imagePlaneCoordinates.get(2).x, imagePlaneCoordinates.get(2).y});
//		src.put(3, 0, new double[] {imagePlaneCoordinates.get(3).x, imagePlaneCoordinates.get(3).y});
//		
//		Mat dest = new Mat(4, 1, CvType.CV_32FC2);
//		dest.put(0, 0, new double[] {worldCoordinates.get(0).x, worldCoordinates.get(0).y});
//		dest.put(1, 0, new double[] {worldCoordinates.get(1).x, worldCoordinates.get(1).y});
//		dest.put(2, 0, new double[] {worldCoordinates.get(2).x, worldCoordinates.get(2).y});
//		dest.put(3, 0, new double[] {worldCoordinates.get(3).x, worldCoordinates.get(3).y});
//		
//		Mat worldCorrdinates2imagePlaneCoordinates = Imgproc.getPerspectiveTransform(src, dest);
//		
//		//_cachedImagePlane2WorldCoordinates = worldCorrdinates2imagePlaneCoordinates.inv();
//		_cachedImagePlane2WorldCoordinates = worldCorrdinates2imagePlaneCoordinates;
		
		MatOfPoint2f src = convertToMatOfPoints_2(_imagePlaneCoordinates);
		
		Mat worldCorrdinates2imagePlaneCoordinates = Calib3d.findHomography(src, _worldCoordinates, Calib3d.RANSAC, 3);
		
		//_cachedImagePlane2WorldCoordinates = worldCorrdinates2imagePlaneCoordinates.inv();
		_cachedImagePlane2WorldCoordinates = worldCorrdinates2imagePlaneCoordinates;
	}
	
	public Point calculateGroundPlaneCoordinates(Point imagePlaneCoordinates) {

		Mat imagePlane2WorldCoordinates = getHomogenousMat();

		// get homogeneous coordinates out of supplied imagePlaneCoordinates
		Mat mat3 = new Mat(3, 1, CvType.CV_64FC1);
		mat3.put(0, 0, new double[] { imagePlaneCoordinates.x, imagePlaneCoordinates.y, 1.0f });

		Mat dest = new Mat(3, 1, CvType.CV_64FC1);
		// 3*3*CV_64FC1 x 3*1*CV_64FC1 -> 3*1*CV_64FC1

		// see
		// http://stackoverflow.com/questions/10168058/basic-matrix-multiplication-in-opencv-for-android
		// Core.multiply(imagePlane2WorldCoordinates, mat3, dest);
		Core.gemm(imagePlane2WorldCoordinates, mat3, 1, new Mat(), 0, dest, 0);
		return new Point(dest.get(0, 0)[0] / dest.get(2, 0)[0],
				dest.get(1, 0)[0] / dest.get(2, 0)[0]); // convert into
														// homogeneous
														// coordinate with form
														// (x,y,1)
	}

	/********************** Parceling **********************/
	// see
	// http://stackoverflow.com/questions/7042272/how-to-properly-implement-parcelable-with-an-arraylistparcelable
	// and
	// http://stackoverflow.com/questions/2139134/how-to-send-an-object-from-one-android-activity-to-another-using-intents
	@Override
	public int describeContents() {
		return 0;
	}

	// write your object's data to the passed-in Parcel
	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeTypedList(_imagePlaneCoordinates);
		out.writeTypedList(convertToArrayList(_worldCoordinates));
	}

	// this is used to regenerate your object. All Parcelables must have a
	// CREATOR that implements these two methods
	public static final Parcelable.Creator<CalibrationHelper> CREATOR = new Parcelable.Creator<CalibrationHelper>() {
		public CalibrationHelper createFromParcel(Parcel in) {
			return new CalibrationHelper(in);
		}

		public CalibrationHelper[] newArray(int size) {
			return new CalibrationHelper[size];
		}
	};

	// example constructor that takes a Parcel and gives you an object populated
	// with it's values
	private CalibrationHelper(Parcel in) {
		in.readTypedList(_imagePlaneCoordinates, ParcelablePoint.CREATOR);
		ArrayList<ParcelablePoint> temp = new ArrayList<ParcelablePoint>();
		in.readTypedList(temp, ParcelablePoint.CREATOR);
		_worldCoordinates = convertToMatOfPoints_2(temp);
	}
	/********************** end-Parceling **********************/

	/********************** Serializing **********************/
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(_imagePlaneCoordinates);
		out.writeObject(convertToArrayList(_worldCoordinates));
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		_imagePlaneCoordinates = (ArrayList<ParcelablePoint>) in.readObject();
		_worldCoordinates = convertToMatOfPoints((List<Point>) in.readObject());
		_cachedImagePlane2WorldCoordinates = null;
	}
	/********************** end-Serializing **********************/

	/********************** Helpers (conversion) **********************/
	private ArrayList<ParcelablePoint> convertToArrayList(MatOfPoint2f matOfPoints) {
		ArrayList<ParcelablePoint> list = new ArrayList<ParcelablePoint>();
		
		int size = (int) matOfPoints.size().height;
		for (int i = 0; i < size; i++)
			list.add(new ParcelablePoint(new Point(_worldCoordinates.get(i, 0)[0], _worldCoordinates.get(i, 0)[1])));
		
		return list;
	}
	
	private MatOfPoint2f convertToMatOfPoints(List<Point> worldCoordinates) {
		Point[] array = new Point[worldCoordinates.size()];
		for(int i=0;i<array.length;i++) {
			array[i] = worldCoordinates.get(i);
		}
		return new MatOfPoint2f(array);
	}
	
	private MatOfPoint2f convertToMatOfPoints_2(List<ParcelablePoint> worldCoordinates) {
		Point[] array = new Point[worldCoordinates.size()];
		for(int i=0;i<array.length;i++) {
			array[i] = worldCoordinates.get(i);
		}
		return new MatOfPoint2f(array);
	}
	/********************** end-Helpers (conversion) **********************/

}

