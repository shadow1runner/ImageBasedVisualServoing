package at.ac.uibk.cs.auis.Tracker;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

public class TrackerHelper {

	private int pixels_of_detection;

	public TrackerHelper() {
		super();
		this.pixels_of_detection = 32;
	}

	public TrackerHelper(int pixels_of_detection) {
		super();
		this.pixels_of_detection = pixels_of_detection;
	}

//	private static class ScalarComp implements Comparator<Scalar> {
//
//		@Override
//		public int compare(Scalar lhs, Scalar rhs) {
//			double sumLhs = 0;
//			double sumRhs = 0;
//			for (int i = 0; i < lhs.val.length; i++) {
//				sumLhs += lhs.val[i] * lhs.val[i];
//				sumRhs += rhs.val[i] * rhs.val[i];
//			}
//			return (int) Math.sqrt(sumRhs - sumLhs);
//		}
//
//	}
	
	public Scalar calcColorForTracking(Mat hsv, Point centerDetect) {
		
		// Scalar[] allColors = new Scalar[pixels_of_detection *
		// pixels_of_detection];
		// int scalarCounter = 0;
		// for(int i = 0; i < pixels_of_detection; i++){
		// for( int j = 0; j< pixels_of_detection; j++){
		// allColors[scalarCounter] = new Scalar(selectedRegionHsv.get(i, j));
		// scalarCounter ++;
		// }
		// }
		// Arrays.sort(allColors, new ScalarComp());

		// colorForTrackingHSV = allColors[allColors.length / 2];

		int maxX = (int) (centerDetect.x + pixels_of_detection / 2);
		int minX = (int) (centerDetect.x - pixels_of_detection / 2);
		int maxY = (int) (centerDetect.y + pixels_of_detection / 2);
		int minY = (int) (centerDetect.y - pixels_of_detection / 2);

		if (maxX > hsv.width() - 1)
			maxX = hsv.width() - 1;

		if (maxY > hsv.height() - 1)
			maxY = hsv.height() - 1;

		if (minX < 0)
			minX = 0;

		if (minY < 0)
			minY = 0;

		Mat selectedRegion = hsv.submat(minY, maxY, minX, maxX);

		Scalar colorForTrackingHSV = Core.sumElems(selectedRegion);
		int pointCount = selectedRegion.width() * selectedRegion.height();
		for (int i = 0; i < colorForTrackingHSV.val.length; i++)
			colorForTrackingHSV.val[i] /= pointCount;

		return colorForTrackingHSV;
	}

}
