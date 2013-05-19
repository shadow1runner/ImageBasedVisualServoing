package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.os.Parcel;
import android.os.Parcelable;

public class NavigationCalibrationHelper implements Parcelable, Serializable {
	private static final long serialVersionUID = 4529667799485429110L;

	private ArrayList<ParcelablePoint> _imagePlaneCoordinates = new ArrayList<ParcelablePoint>();
	private ArrayList<ParcelableScalar> _beaconColors = new ArrayList<ParcelableScalar>();
	
	private MatOfPoint2f _worldCoordinates;
	
	private CalibrationHelper _calibrationHelper;


	public NavigationCalibrationHelper(MatOfPoint2f worldCoordinates) {
		_worldCoordinates = worldCoordinates;
	}
	
	public NavigationCalibrationHelper(List<Point> worldCoordinates) {
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
	
	public void addBeaconColor(int pointNumber, Scalar beaconColor) {
		if (pointNumber < 0)
			throw new IllegalArgumentException();
		if (beaconColor == null)
			throw new IllegalArgumentException("point must not be null");
		
		_beaconColors.ensureCapacity((int) _worldCoordinates.size().height);
		_beaconColors.add(pointNumber, new ParcelableScalar(beaconColor));
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
	
	public void setCalibrationHelper(CalibrationHelper calibrationHelper) {
		_calibrationHelper = calibrationHelper;
	}
	
	public Point getWorldGroundPlaneCoordinates() {
		Point zeroWorld = getWorldCoordinates(1);
		Point oneWorld = getWorldCoordinates(2);
		assert(zeroWorld.x==0.0 && zeroWorld.y==0.0); // the very first point should always be at (0,0)
		
		Point zeroImage = _imagePlaneCoordinates.get(0);
		Point oneImage = _imagePlaneCoordinates.get(1);
		
		if(_calibrationHelper==null)
			throw new RuntimeException("_calibrationhelper must not be null");
		
		Point zeroEgocentricWorld = _calibrationHelper.calculateGroundPlaneCoordinates(zeroImage);
		Point oneEgocentricWorld = _calibrationHelper.calculateGroundPlaneCoordinates(oneImage);
		
		double r0 = pythagoras(zeroEgocentricWorld.x, zeroEgocentricWorld.y);
		double r1 = pythagoras(oneEgocentricWorld.x, oneEgocentricWorld.y);
		
		double d01 = pythagoras((oneWorld.x-zeroWorld.x), (oneWorld.y-zeroWorld.y));
		
		double y = (r0*r0 - r1*r1 + d01*d01)/(2*d01);
		
		double x = Math.sqrt(r0*r0-y*y);
		
		return new Point(x,y);
	}

	private double pythagoras(double x, double y) {
		return Math.sqrt(x*x + y*y);
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
		out.writeTypedList(_beaconColors);
	}

	// this is used to regenerate your object. All Parcelables must have a
	// CREATOR that implements these two methods
	public static final Parcelable.Creator<NavigationCalibrationHelper> CREATOR = new Parcelable.Creator<NavigationCalibrationHelper>() {
		public NavigationCalibrationHelper createFromParcel(Parcel in) {
			return new NavigationCalibrationHelper(in);
		}

		public NavigationCalibrationHelper[] newArray(int size) {
			return new NavigationCalibrationHelper[size];
		}
	};

	// example constructor that takes a Parcel and gives you an object populated
	// with it's values
	private NavigationCalibrationHelper(Parcel in) {
		in.readTypedList(_imagePlaneCoordinates, ParcelablePoint.CREATOR);
		ArrayList<ParcelablePoint> temp = new ArrayList<ParcelablePoint>();
		in.readTypedList(temp, ParcelablePoint.CREATOR);
		_worldCoordinates = convertToMatOfPoints_2(temp);
		in.readTypedList(_beaconColors, ParcelableScalar.CREATOR);
	}
	/********************** end-Parceling **********************/

	/********************** Serializing **********************/
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(_imagePlaneCoordinates);
		out.writeObject(convertToArrayList(_worldCoordinates));
		out.writeObject(_beaconColors);
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		_imagePlaneCoordinates = (ArrayList<ParcelablePoint>) in.readObject();
		_worldCoordinates = convertToMatOfPoints((List<Point>) in.readObject());
		_beaconColors = (ArrayList<ParcelableScalar>) in.readObject();
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

