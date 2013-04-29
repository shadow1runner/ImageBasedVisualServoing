package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelablePoint extends org.opencv.core.Point implements Parcelable, Serializable {
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(x);
        out.writeDouble(y);
    }

    public static final Parcelable.Creator<ParcelablePoint> CREATOR
            = new Parcelable.Creator<ParcelablePoint>() {
        public ParcelablePoint createFromParcel(Parcel in) {
            return new ParcelablePoint(in);
        }

        public ParcelablePoint[] newArray(int size) {
            return new ParcelablePoint[size];
        }
    };
    
    private ParcelablePoint(Parcel in) {
        x = in.readDouble();
        y = in.readDouble();
    }

	public ParcelablePoint(org.opencv.core.Point point) {
		super(point.x, point.y);
	}

	public ParcelablePoint(double x, double y) {
		super(x,y);
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeDouble(x);
		out.writeDouble(y);
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		x = in.readDouble();
		y = in.readDouble();
	}
}
