package at.ac.uibk.cs.auis.ImageBasedVisualServoing.Robot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.ImageBasedVisualServoingActivity;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.R;
import at.ac.uibk.cs.auis.ImageBasedVisualServoing.Common.CalibrationHelper;

/**
 * @author Helmut Wolf
 */
public class LoggingActivity extends Activity {
	private static final String TAG = "Auis::LoggingActivity";
	
	public class LogTest extends Activity {
		  @Override
		  public void onCreate(Bundle savedInstanceState) {
		    super.onCreate(savedInstanceState);
		    //setContentView(R.layout.main);
		    
		    moveTaskToBack(true);
		    
		    try {
		      Process process = Runtime.getRuntime().exec("logcat -d");
		      BufferedReader bufferedReader = new BufferedReader(
		      new InputStreamReader(process.getInputStream()));

		      StringBuilder log=new StringBuilder();
		      String line;
		      while ((line = bufferedReader.readLine()) != null) {
		        log.append(line);
		      }
		      File root = Environment.getExternalStorageDirectory();
		      File file = new File(root, "tomato50.txt");
		      
		      FileWriter filewriter = new FileWriter(file);
		      BufferedWriter bufferedWriter = new BufferedWriter(filewriter);
		      
		      bufferedWriter.write(line);
		      bufferedWriter.close();
		    } catch (IOException e) {
		    }
		  }
		}

}
