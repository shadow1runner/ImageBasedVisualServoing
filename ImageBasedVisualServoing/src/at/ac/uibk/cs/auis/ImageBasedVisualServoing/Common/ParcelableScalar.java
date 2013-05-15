package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common;

import java.io.IOException;
import java.io.Serializable;

import org.opencv.core.Scalar;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableScalar extends org.opencv.core.Scalar implements Parcelable, Serializable {
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(val.length);
        for(double value : val)
        	out.writeDouble(value);
    }

    public static final Parcelable.Creator<ParcelableScalar> CREATOR = new Parcelable.Creator<ParcelableScalar>() {
        public ParcelableScalar createFromParcel(Parcel in) {
            return new ParcelableScalar(in);
        }

        public ParcelableScalar[] newArray(int size) {
            return new ParcelableScalar[size];
        }
    };
    
    private ParcelableScalar(Parcel in) {
        super(0);
    	int length = in.readInt();
        val = new double[length];
        for(int i=0;i<val.length;i++)
        	val[i] = in.readDouble();
    }
	
	public ParcelableScalar(Scalar beaconColor) {
		super(beaconColor.val);
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		 out.writeInt(val.length);
	        for(double value : val)
	        	out.writeDouble(value);
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		int length = in.readInt();
        val = new double[length];
        for(int i=0;i<val.length;i++)
        	val[i] = in.readDouble();
	}
}
