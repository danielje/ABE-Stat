package com.diagenetix.abestat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.artfulbits.aiCharts.Base.ChartArea;
import com.artfulbits.aiCharts.Base.ChartSeries;
import com.artfulbits.aiCharts.ChartView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This activity parses saves data files, and builds an interactive interface with chart to display the
 * data.
 */

public class Data_View_Activity extends Activity {
	// for Debugging...
	private static final String TAG = "Data_View_Activity";
	private static final boolean D = true;

	public static final String FILEPATHSTRING = "filepathstring";
	
    // Note, FIRST is defined in menu class as the first integer value for group and item identifier integers
	// Context menu selections
    private static final int QUIT_DIALOG = 0;
	
	// Process name and parameters passed in intent from Main (ABEStatActivity
	private long Vertical_Scale = 100000;
	private long ChartFloor = -50000;
	private double Major_Divisions = 10000;

	private long logVertical_Scale = java.lang.Math.round(java.lang.Math.log10(Vertical_Scale) + 0.5);	// change vertical scales in linear and l
	private static double potentialScale = 2.0;
	private static double potentialFloor = 0.0;
	private static double potentialDivisions = 0.5;
	private static double temperatureScale = 100.0;
	private static double temperatureFloor = 0.0;
	private static double temperatureDivisions = 25.0;

	private static int THUMBSIZE = 200;
	
	private float touchY1, touchY2;
	private static int MIN_DISTANCE = 150;
	
	// Layout Views/ User Controls (recycling names frokm real_time_activity...
	private static TextView mTitle;
	private static Button mQButton;
	private static ImageView mThreshold_up;
	private static ImageView mThreshold_down;

	// ai ChartView components
	// ChartSeries are populated by data arrays representing time and RFU values
	private ChartView mChartView;
	private ChartArea mChArea;
	private ChartSeries series[] = new ChartSeries[2];

	private ArrayList<String> Frequency = new ArrayList<String>();
	private ArrayList<String> Z = new ArrayList<String>();
	private ArrayList<String> Phi = new ArrayList<String>();
	private ArrayList<String> Time = new ArrayList<String>();
	private ArrayList<String> Potential = new ArrayList<String>();
	private ArrayList<String> Current = new ArrayList<String>();
	private ArrayList<String> BoardTemp = new ArrayList<String>();
	private ArrayList<String> TCTemp = new ArrayList<String>();

	private int Method = Real_Time_Activity.CYCLIC_VOLTAMMETRY;	// this is used to control how interface displays data; defaults to CV

	// also recycle configuration data variables from Real_Time_Activity
	private static double StartFrequency;	// start frequency for electrochemical impedance spectroscopy
	private static double EndFrequency;	// end frequency for electrochemical impedance spectroscopy
	//private static long IntervalFrequency;	// frequency increment for analysis with electrochemical impedance spectroscopy
	//private static int SignalAmplitude;	// code for the signal amplitude applied to network (can be 3, 2, 1, 0 for 200 mV, 100mV, 40 mV, and 20mVp-p)
	//private static double BiasVoltage;	// bias voltage applied during electrochemical impedance spectroscopy
	private static double StartVoltage;	// low voltage end for cyclic voltammetry
	private static double EndVoltage;	// high voltage end for cyclic voltammetry
	//private static double ScanRate;	// scan rate in V/s, for cyclic voltammetry
	//private static String ScanNumber;	// # of scans to be executed (leave as string, because can be INF for continuous scanning)
	//private static int scansRemaining;
	//private static int StepFrequency;	// frequency (Hz) for steps in Differential Pulse Voltammetry
	//private static int StepPotential;	// step potential (mV) for Differential Pulse Voltammetry
	//private static int PulseAmplitude;	// pulse amplitude (mV) using Differential Pulse Voltammetry
	//private static int ElectrodeConfig;	// integer code for electrode configuration, 2 for 2 electrode, 3 for 3 electrode, 0 or any other for open circuit
	//private static int EquilibrationTime;	// time in seconds to equilibrate at starting potential

	private static boolean phaseDisplay;	// selects whether to display phase or impedance on EIS plots

	private static int EIS_plotType = Real_Time_Activity.LINEAR_PLOT;	// by default make it linear (otherwise it may plot non EIS data incorrectly on log scale)

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
        String FileName = getIntent().getStringExtra(FILEPATHSTRING);
        
        File dataFile = new File(FileName);

		UISetup();	// find the layout resource and populate headings...

        parseFileData(dataFile);// open the data file, parse info, and fill in basic textual info

		setupUserInterfaceWidgets();	// now based on what kind of analysis was done, can set the visibility/ behaviour of interface elements
    }
	
	/*
	 * Parse a string, return double if numeric, -1.0 if number format exception
	 */
	private long LongParse(String datastr)
	{
		long numbber = -1;
		try
    	{
    		numbber = Integer.parseInt(datastr);
    	}
    	catch (NumberFormatException nfe)
    	{
    		numbber = -1;
    	}
		return numbber;
	}
	
	private void parseFileData(File f) {
		try {
			String[] eof = {""};
            BufferedReader br = new BufferedReader(new FileReader(f));
			String methodStrCode = br.readLine();
			if (methodStrCode.equals(SettingsDbAdapter.CYCLIC_VOLTAMMETRY)) Method = Real_Time_Activity.CYCLIC_VOLTAMMETRY;
			else if (methodStrCode.equals(SettingsDbAdapter.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY)) Method = Real_Time_Activity.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY;
			else if (methodStrCode.equals(SettingsDbAdapter.DIFFERENTIAL_PULSE_VOLTAMMETRY)) Method = Real_Time_Activity.DIFFERENTIAL_PULSE_VOLTAMMETRY;
			else if (methodStrCode.equals(SettingsDbAdapter.ANODIC_STRIPPING_VOLTAMMETRY)) Method = Real_Time_Activity.ANODIC_STRIPPING_VOLTAMMETRY;
			else if (methodStrCode.equals(SettingsDbAdapter.STATIC_LOGGING)) Method = Real_Time_Activity.STATIC_LOGGING;

            String delims = "[ ]+";	// space is delimiter of descriptive text at start of file
            String[] tokens = br.readLine().split(delims);	// parse line of text using delimiters
            String Device = tokens[1];

			tokens = br.readLine().split(delims);	// next line has users method name...
			String methodString = tokens[1];

			TextView infoText = (TextView) findViewById(R.id.dataview_method_title);
			String DeviceStringFormat = getResources().getString(R.string.rt_device_method);
			String DeviceStringMsg = String.format(DeviceStringFormat, Device);
			infoText.setText(DeviceStringMsg);

			tokens = br.readLine().split(delims);	// next line has date and time of analysis...
			String dateString = tokens[2];
			String timeString = tokens[3];

            mTitle = (TextView) findViewById(R.id.title_right_text);
            String fString = getResources().getString(R.string.dataviewheading);
            String headingtext = String.format(fString, timeString, dateString);
            mTitle.setText(headingtext);

            tokens = br.readLine().split(delims); // next line has firmware version (or blank in early versions of app)
            if (tokens.length > 1) {
                String firmwareVersion = tokens[2];
                infoText.append("Firmware Version: " + firmwareVersion);
                br.readLine(); // read and discard next blank line (older versions the blank line would have been read in the .split(delims) line...
            }

			String lineString = br.readLine();
			String methodSummary = "";

			while (lineString.length() > 0) {
				methodSummary += lineString + "\n";
				lineString = br.readLine();
			}
			infoText = (TextView) findViewById(R.id.dataview_method_summary);
			infoText.setText(methodSummary);
            if (Method != Real_Time_Activity.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) infoText.setPadding(0,0,0,0);

			lineString = br.readLine();	// next lines have board and Thermocouple temperature data...
			String temperatureSummary = "";

			while (lineString.length() > 0) {
				temperatureSummary += lineString + "\n";
				lineString = br.readLine();
			}
			infoText = (TextView) findViewById(R.id.dataview_temp_report);
			infoText.setText(temperatureSummary);

			delims = "[,]";	// space is delimiter of descriptive text at start of file
			lineString = br.readLine();
			if (lineString != null) tokens = lineString.split(delims);
			else tokens = eof;
			// now the rest is just recurring data- first discard the data heading row, and systematically read the data
			while ((tokens.length < 2) && !tokens.equals(eof)) {
				lineString = br.readLine();
				if (lineString != null) tokens = lineString.split(delims);// next line should have data headings (already read blank line to exit previous while loop
				else tokens = eof;// if somehow we've gotten to end of file, make sure we exit this loop
			}
			/*
			 this previous loop should advance and read through the data headings- now actually read the data...
			 The data itself depends on the method that was used...
			 Start by clearing all of the array lists of data before populating them
			  */
			Potential.clear();
			Current.clear();
			BoardTemp.clear();
			TCTemp.clear();
			Frequency.clear();
			Z.clear();
			Phi.clear();
			lineString = br.readLine();
			if (lineString != null) tokens = lineString.split(delims);
			else tokens = eof;// if end of file somehow, make sure we don't go into the next loop...
			while (tokens.length > 1) {
				switch (Method) {
					case Real_Time_Activity.CYCLIC_VOLTAMMETRY:
						Potential.add(tokens[0]);
						Current.add(tokens[1]);
						break;
					case Real_Time_Activity.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY:
						Frequency.add(tokens[0]);
						Z.add(tokens[1]);
						Phi.add(tokens[2]);
						break;
					case Real_Time_Activity.DIFFERENTIAL_PULSE_VOLTAMMETRY:
						Potential.add(tokens[0]);
						Current.add(tokens[1]);
						break;
					case Real_Time_Activity.STATIC_LOGGING:
						Time.add(tokens[0]);
						Potential.add(tokens[1]);
						Current.add(tokens[2]);
						if (tokens.length > 3) {
							BoardTemp.add(tokens[3]);
							TCTemp.add(tokens[4]);
						}
						break;
					case Real_Time_Activity.ANODIC_STRIPPING_VOLTAMMETRY:
						break;
					default:
						break;
				}
				lineString = br.readLine();
				if (lineString != null) tokens = lineString.split(delims);
				else tokens = eof;	// if we've reached end of file somehow, force condition to exit this loop...
			}

            br.close();
		}
		catch (IOException e) {
			Log.e(TAG, "could not successfully read and close data file...");
			Toast.makeText(this, R.string.file_corrupted, Toast.LENGTH_LONG).show();
        }
	}

	private void UISetup() {
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.dataview);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
                 
        // Fill in title bar with app name and indicate that reaction is underway
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
	}
	
	private void setupUserInterfaceWidgets() {
		mChartView = (ChartView) findViewById(R.id.dataview_chart1);
		mChArea = mChartView.getAreas().get(0);
		mChArea.getDefaultXAxis().getScale().setRange(0, 100);
		mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, Vertical_Scale);

		mChartView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View mChartView, MotionEvent event) {
				switch (event.getAction())
				{
					case (MotionEvent.ACTION_DOWN):	// view was touched
						touchY1 = event.getY();
						break;
					case (MotionEvent.ACTION_UP):
						touchY2 = event.getY();	// getY returns Y position, starting at 0 from Top of View (and going down)
						if ((touchY2 - touchY1) > MIN_DISTANCE) {	// Swipe down (ZoomOut- increase vertical scale)
							int zoom_factor = (int) (touchY2 - touchY1) / MIN_DISTANCE;
							ZoomOut(zoom_factor);
						}
						else if ((touchY1 - touchY2) > MIN_DISTANCE) {
							int zoom_factor = (int) (touchY1 - touchY2) / MIN_DISTANCE;
							ZoomIn(zoom_factor);
						}
						break;
					default:
						break;
				}
				return true;
			}
		});

		if ((getResources().getConfiguration().screenLayout &
				Configuration.SCREENLAYOUT_SIZE_MASK) ==
				Configuration.SCREENLAYOUT_SIZE_XLARGE) {
			mChArea.getDefaultXAxis().getLabelPaint().setTextSize(35);
			mChArea.getDefaultXAxis().getTitlePaint().setTextSize(40);
			mChArea.getDefaultYAxis().getLabelPaint().setTextSize(35);
			mChArea.getDefaultYAxis().getTitlePaint().setTextSize(40);
			MIN_DISTANCE = 200;
			//Log.e(TAG, "This device is Extra Large!");
		}
		else if ((getResources().getConfiguration().screenLayout &
				Configuration.SCREENLAYOUT_SIZE_MASK) ==
				Configuration.SCREENLAYOUT_SIZE_LARGE) {
			mChArea.getDefaultXAxis().getLabelPaint().setTextSize(30);
			mChArea.getDefaultXAxis().getTitlePaint().setTextSize(35);
			mChArea.getDefaultYAxis().getLabelPaint().setTextSize(30);
			mChArea.getDefaultYAxis().getTitlePaint().setTextSize(35);
			MIN_DISTANCE = 150;
			//Log.e(TAG, "This device is Large!");
		}
		else if ((getResources().getConfiguration().screenLayout &
				Configuration.SCREENLAYOUT_SIZE_MASK) ==
				Configuration.SCREENLAYOUT_SIZE_NORMAL) {
			mChArea.getDefaultXAxis().getLabelPaint().setTextSize(25);
			mChArea.getDefaultXAxis().getTitlePaint().setTextSize(30);
			mChArea.getDefaultYAxis().getLabelPaint().setTextSize(25);
			mChArea.getDefaultYAxis().getTitlePaint().setTextSize(30);
			MIN_DISTANCE = 100;
			//Log.e(TAG, "This device is Normal!");
		}
		else if ((getResources().getConfiguration().screenLayout &
				Configuration.SCREENLAYOUT_SIZE_MASK) ==
				Configuration.SCREENLAYOUT_SIZE_SMALL) {
			mChArea.getDefaultXAxis().getLabelPaint().setTextSize(20);
			mChArea.getDefaultXAxis().getTitlePaint().setTextSize(25);
			mChArea.getDefaultYAxis().getLabelPaint().setTextSize(20);
			mChArea.getDefaultYAxis().getTitlePaint().setTextSize(25);
			MIN_DISTANCE = 50;
			//Log.e(TAG, "This device is Small!");
		}
		int lastIndexValue = 0;
		switch (Method) {
			case Real_Time_Activity.CYCLIC_VOLTAMMETRY:
				voltammetrySettings();
				break;
			case Real_Time_Activity.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY:
				lastIndexValue = Frequency.size() - 1;
				StartFrequency = Real_Time_Activity.safeParseDouble(Frequency.get(0));
				EndFrequency = Real_Time_Activity.safeParseDouble(Frequency.get(lastIndexValue));
				Log.e(TAG, "Start frequency: " + StartFrequency + " End Frequency: " + EndFrequency);
				mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.frequency));
				mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.impedance));
				RadioButton rb = (RadioButton) findViewById(R.id.dataview_z_magnitude);
				rb.setChecked(true);	// set up axes titles, and make sure radio button default set to impedance magnitude
				phaseDisplay = false;
				rb = (RadioButton) findViewById(R.id.dataview_logplot);
				rb.setChecked(true);
				EIS_plotType = Real_Time_Activity.LOG_PLOT;	// by default, show logarithmic impedance magnitude plot, and set corresponding radio buttons
				findViewById(R.id.dataview_threshold_controls).setVisibility(View.GONE);	// Don't allow change of origin on EIS plots
				findViewById(R.id.dataview_chart_controls).setVisibility(View.VISIBLE); // but show radio buttons to toggle Z and phi, and change axes/ plot type
				EIS_axisLabels();
				break;
			case Real_Time_Activity.ANODIC_STRIPPING_VOLTAMMETRY:
				EIS_plotType = Real_Time_Activity.LINEAR_PLOT;	// make sure we don't get stuck on "static" log plot type if were last looking at EIS data
				mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.potential));
				mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.current));
				break;
			case Real_Time_Activity.DIFFERENTIAL_PULSE_VOLTAMMETRY:
				voltammetrySettings();
				break;
			case Real_Time_Activity.STATIC_LOGGING:
				mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.time_heading));
				mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.current));
				findViewById(R.id.dataview_chart_controls).setVisibility(View.VISIBLE); // show radio buttons to toggle Z and phi, and change axes/ plot type
				findViewById(R.id.dataview_axes_select).setVisibility(View.VISIBLE);
				TextView yAxisSettingsLabel = (TextView) findViewById(R.id.dataview_axes_settings);
				yAxisSettingsLabel.setText(getResources().getString(R.string.data_select).toString());
				RadioButton radB = (RadioButton) findViewById(R.id.dataview_logplot);
				radB.setText(getResources().getString(R.string.current).toString());
				radB.setChecked(true); // plot current by default
				radB = (RadioButton) findViewById(R.id.dataview_linearplot);
				radB.setText(getResources().getString(R.string.potential).toString());
				radB = (RadioButton) findViewById(R.id.dataview_nyquist);
				radB.setText(getResources().getString(R.string.temperature).toString());
				phaseDisplay = false;
				EIS_plotType = Real_Time_Activity.LOG_PLOT;	// default setting is "LOG_PLOT", for Static logging this is current vs. time

				int lastTimeIndex = Time.size() - 1;
				double timeScale = Real_Time_Activity.safeParseDouble(Time.get(lastTimeIndex)) / 60.0;
				if (timeScale > 10.0) {
					int decimalDigits = (int) (timeScale / 10);
					if (timeScale % 10 != 0.0) decimalDigits++;	// increment 10 minutes on scale if last time value is not exact multiple of 10
					timeScale = decimalDigits * 10;
				}

				double xTicks = timeScale / 4.0;
				mChArea.getDefaultXAxis().getScale().setRange(0, timeScale);
				mChArea.getDefaultXAxis().getScale().setInterval(xTicks);
				findViewById(R.id.dataview_chart_data_select).setVisibility(View.GONE);
				findViewById(R.id.dataview_data_select_group).setVisibility(View.GONE);
				break;
			default:
				mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.potential));
				mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.current));
				break;
		}
		mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);

		// Assign ChartSeries parameters to series elements of chartview layout
		series[0] = mChartView.getSeries().get("data1");
		series[1] = mChartView.getSeries().get("data2");

		mQButton = (Button) findViewById(R.id.dataview_quit_button);
		mQButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View w) {
				showDialog(QUIT_DIALOG); // show the quit dialog
			}
		});

		mThreshold_up = (ImageView) findViewById(R.id.dataview_thresh_up);
		mThreshold_up.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {	// don't need to worry about EIS case, because these controls not available for EIS
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (Method == Real_Time_Activity.STATIC_LOGGING) {
						switch (EIS_plotType) {
							case Real_Time_Activity.LOG_PLOT:
								ChartFloor += Major_Divisions;	// LOG_PLOT is current vs time for STATIC_LOGGING
								mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);
								break;
							case Real_Time_Activity.LINEAR_PLOT:
								potentialFloor += potentialDivisions;	// LINEAR_PLOT is potential vs time for STATIC_LOGGING
								mChArea.getDefaultYAxis().getScale().setRange(potentialFloor, potentialFloor + potentialScale);
								break;
							case Real_Time_Activity.NYQUIST_PLOT:
								temperatureFloor += temperatureDivisions;	// NYQUIST_PLOT is temperature vs time for STATIC_LOGGING
								mChArea.getDefaultYAxis().getScale().setRange(temperatureFloor, temperatureFloor + temperatureScale);
								break;
							default:
								ChartFloor += Major_Divisions;	// LOG_PLOT is current vs time for STATIC_LOGGING
								break;
						}
					}
					else {
						ChartFloor += Major_Divisions;
						mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);
					}
				}
				return true;
			}
		});

		mThreshold_down = (ImageView) findViewById(R.id.dataview_thresh_down);
		mThreshold_down.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (Method == Real_Time_Activity.STATIC_LOGGING) {
						switch (EIS_plotType) {
							case Real_Time_Activity.LOG_PLOT:
								ChartFloor -= Major_Divisions;	// LOG_PLOT is current vs time for STATIC_LOGGING
								mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);
								break;
							case Real_Time_Activity.LINEAR_PLOT:
								potentialFloor -= potentialDivisions;	// LINEAR_PLOT is potential vs time for STATIC_LOGGING
								mChArea.getDefaultYAxis().getScale().setRange(potentialFloor, potentialFloor + potentialScale);
								break;
							case Real_Time_Activity.NYQUIST_PLOT:
								temperatureFloor -= temperatureDivisions;	// NYQUIST_PLOT is temperature vs time for STATIC_LOGGING
								mChArea.getDefaultYAxis().getScale().setRange(temperatureFloor, temperatureFloor + temperatureScale);
								break;
							default:
								ChartFloor += Major_Divisions;	// LOG_PLOT is current vs time for STATIC_LOGGING
								break;
						}
					}
					else {
						ChartFloor -= Major_Divisions;
						mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);
					}
				}
				return true;
			}
		});
		UpdateChart();
	}

	private void voltammetrySettings() {
		EIS_plotType = Real_Time_Activity.LINEAR_PLOT;	// make sure we don't get stuck on "static" log plot type if were last looking at EIS data
		//int lastIndexValue = Potential.size() - 1;
		StartVoltage = Real_Time_Activity.safeParseDouble(Potential.get(0));
		EndVoltage = StartVoltage;
		for (int a = 0; a < Potential.size(); a++) {
			if (Real_Time_Activity.safeParseDouble(Potential.get(a)) > EndVoltage) EndVoltage = Real_Time_Activity.safeParseDouble(Potential.get(a));
		}
		mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.potential));
		mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.current));
		mChArea.getDefaultXAxis().getScale().setRange(StartVoltage - 0.1, EndVoltage + 0.1);
		double Einterval = 0.1;
		if ((EndVoltage - StartVoltage) >= 2)  Einterval = 0.5;
		else if ((EndVoltage - StartVoltage) >= 1)  Einterval = 0.25;
		mChArea.getDefaultXAxis().getScale().setInterval(Einterval);
		mChArea.getDefaultXAxis().getScale().setInterval(0.1);
		mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);	// default range 100000 nA (100 uA)
		findViewById(R.id.dataview_chart_controls).setVisibility(View.GONE); // don't show radio buttons to toggle Z and phi
	}

	/*
	Label Axes of EIS analysis appropriately (based on selection of data being plotted, logarithmic transform, and/or "nyquist" plot
	 */
	private void EIS_axisLabels() {
		switch (EIS_plotType) {
			case Real_Time_Activity.LOG_PLOT:
				if (Method == Real_Time_Activity.STATIC_LOGGING) {	// for static logging method, "LOG_PLOT" button used to plot current vs. time
					mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);	// keep phase scale fixed...
					mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
					mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.current));
				}
				else if (Method == Real_Time_Activity.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) {
					double startLogf = java.lang.Math.round(java.lang.Math.log10(StartFrequency) - 0.5);	// low range of x scale truncates to nearest integer value of log
					if (StartFrequency < 2.0) startLogf = -1.5;
					double endLogf = java.lang.Math.round(java.lang.Math.log10(EndFrequency) + 0.5); // high end of range rounds up to nearest integer to log value
					Log.e(TAG, "Log scale frequency range: " + startLogf + " - " + endLogf);
					mChArea.getDefaultXAxis().getScale().setRange(startLogf, endLogf);
					mChArea.getDefaultXAxis().getScale().setInterval(1.0);
					mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.log_f));
					if (phaseDisplay) {
						mChArea.getDefaultYAxis().getScale().setRange(-180.0, 180.0);	// keep phase scale fixed...
						mChArea.getDefaultYAxis().getScale().setInterval(15.0);
						mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.phi));
					}
					else {
						mChArea.getDefaultYAxis().getScale().setRange(0, logVertical_Scale);	// keep phase scale fixed...
						mChArea.getDefaultYAxis().getScale().setInterval(1.0);
						mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.log_z));
					}

					// if nyquist plot is not chose, make sure to show radio buttons for Bode plot options / impedance and phase vs. f
					findViewById(R.id.dataview_chart_data_select).setVisibility(View.VISIBLE);
					findViewById(R.id.dataview_data_select_group).setVisibility(View.VISIBLE);
				}
				break;
			case Real_Time_Activity.LINEAR_PLOT:
				if (Method == Real_Time_Activity.STATIC_LOGGING) {	// for static logging method, "LINEAR_PLOT" button used to plot potential vs. time
					mChArea.getDefaultYAxis().getScale().setRange(potentialFloor, potentialScale);	// keep phase scale fixed...
					mChArea.getDefaultYAxis().getScale().setInterval(potentialDivisions);
					mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.potential));
				}
				else if (Method == Real_Time_Activity.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) {
					double EISXInterval = 1000;
					mChArea.getDefaultXAxis().getScale().setRange(StartFrequency, EndFrequency);
					if ((EndFrequency - StartFrequency) > 50000 ) EISXInterval = 20000.0;
					else if ((EndFrequency - StartFrequency) > 20000 ) EISXInterval = 10000.0;
					else if ((EndFrequency - StartFrequency) > 5000 ) EISXInterval = 1000.0;
					else if ((EndFrequency - StartFrequency) > 1000 ) EISXInterval = 500.0;
					else EISXInterval = 100.0;
					mChArea.getDefaultXAxis().getScale().setInterval(EISXInterval);
					mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.frequency));
					if (phaseDisplay) {
						mChArea.getDefaultYAxis().getScale().setRange(-180.0, 180.0);	// keep phase scale fixed...
						mChArea.getDefaultYAxis().getScale().setInterval(15.0);
						mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.phi));
					}
					else {
						mChArea.getDefaultYAxis().getScale().setRange(0, Vertical_Scale);	// keep phase scale fixed...
						mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
						mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.impedance));
					}

					// if nyquist plot is not chose, make sure to show radio buttons for Bode plot options / impedance and phase vs. f
					findViewById(R.id.dataview_chart_data_select).setVisibility(View.VISIBLE);
					findViewById(R.id.dataview_data_select_group).setVisibility(View.VISIBLE);
				}
				break;
			case Real_Time_Activity.NYQUIST_PLOT:
				if (Method == Real_Time_Activity.STATIC_LOGGING) {	// for static logging method, "LINEAR_PLOT" button used to plot potential vs. time
					mChArea.getDefaultYAxis().getScale().setRange(temperatureFloor, temperatureScale);	// keep phase scale fixed...
					mChArea.getDefaultYAxis().getScale().setInterval(temperatureDivisions);
					mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.temperature));
				}
				else if (Method == Real_Time_Activity.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) {
					mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.zreal));
					mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.zimag));
					mChArea.getDefaultYAxis().getScale().setRange(0, Vertical_Scale);	// link horizontal and vertical scales...
					mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
					mChArea.getDefaultXAxis().getScale().setRange(0, Vertical_Scale);
					mChArea.getDefaultXAxis().getScale().setInterval(Major_Divisions);

					// also if nyquist plot is chosen, need to remove radio buttons for Bode plot options / impedance and phase vs. f
					findViewById(R.id.dataview_chart_data_select).setVisibility(View.GONE);
					findViewById(R.id.dataview_data_select_group).setVisibility(View.GONE);
				}
				break;
			default:
				break;
		}
	}
	
	private void ZoomOut(int factor) {
		if (!phaseDisplay || (EIS_plotType == Real_Time_Activity.NYQUIST_PLOT)) {
			for (int a = 0; a < factor; a++) {
				if (EIS_plotType == Real_Time_Activity.LOG_PLOT) {
					logVertical_Scale++;	// for log plots, just adjust magnitudes one log unit at a time...
					Vertical_Scale = (long) java.lang.Math.pow(10, logVertical_Scale);
					ChartFloor = -Vertical_Scale / 2;
					Major_Divisions = Vertical_Scale / 5;	// adjust magnitudes of linear scales, so data is framed as desired in linear axes too
				}
				else if ((EIS_plotType == Real_Time_Activity.LINEAR_PLOT) && (Method == Real_Time_Activity.STATIC_LOGGING)) { // "LINEAR_PLOT" button used to plot voltage / potential in STATIC_LOGGING
					if (potentialScale < 2.0) {
						potentialScale += 0.1;    // only let user go up to 200 C...
						potentialDivisions = potentialScale / 4.0;
					}
				}
				else if ((EIS_plotType == Real_Time_Activity.NYQUIST_PLOT) && (Method == Real_Time_Activity.STATIC_LOGGING)) {	// "NYQUIST_PLOT" Button used to plot temperature
					if (temperatureScale < 200.0) {
						temperatureScale += 10.0;
						temperatureDivisions = temperatureScale / 4.0;
					}
				}
				else {
					if (Vertical_Scale < 5000) {
						Vertical_Scale += 1000;
						ChartFloor -= 500;
						Major_Divisions = 500;
					}
					else if (Vertical_Scale < 30000) {
						Vertical_Scale += 5000;
						ChartFloor -= 2500;
						Major_Divisions = 5000;
					}
					else if (Vertical_Scale < 100000) {
						Vertical_Scale += 10000;
						ChartFloor -= 5000;
						Major_Divisions = 10000;
					}
					else if (Vertical_Scale < 500000) {
						Vertical_Scale += 50000;
						ChartFloor -= 25000;
						Major_Divisions = 50000;
					}
					else if (Vertical_Scale < 1000000) {
						Vertical_Scale += 100000;
						ChartFloor -= 50000;
						Major_Divisions = 100000;
					}
					else if (Vertical_Scale < 3000000) {
						Vertical_Scale += 500000;
						ChartFloor -= 250000;
						Major_Divisions = 500000;
					}
					else {
						Vertical_Scale += 1000000;
						ChartFloor -= 500000;
						Major_Divisions = 1000000;
					}
					logVertical_Scale = java.lang.Math.round(java.lang.Math.log10(Vertical_Scale) + 0.5);	// keep log scale in tandem with linear scale changes...
				}
			}
			rescaleVerticalAxes();
		}
	}
	
	private void ZoomIn(int factor) {
		if (!phaseDisplay || (EIS_plotType == Real_Time_Activity.NYQUIST_PLOT)) {
			for (int a = 0; a < factor; a++) {
				if (EIS_plotType == Real_Time_Activity.LOG_PLOT) {
					logVertical_Scale--;	// for log plots, just adjust magnitudes one log unit at a time...
					Vertical_Scale = (long) java.lang.Math.pow(10, logVertical_Scale);
					ChartFloor = -Vertical_Scale / 2;
					Major_Divisions = Vertical_Scale / 5;	// adjust magnitudes of linear scales, so data is framed as desired in linear axes too
				}
				else if ((EIS_plotType == Real_Time_Activity.LINEAR_PLOT) && (Method == Real_Time_Activity.STATIC_LOGGING)) { // "LINEAR_PLOT" button used to plot voltage / potential in STATIC_LOGGING
					if (potentialScale > 0.1) {
						potentialScale -= 0.1;    // only let user go up to 200 C...
						potentialDivisions = potentialScale / 4.0;
					}
				}
				else if ((EIS_plotType == Real_Time_Activity.NYQUIST_PLOT) && (Method == Real_Time_Activity.STATIC_LOGGING)) {	// "NYQUIST_PLOT" Button used to plot temperature
					if (temperatureScale > 10.0) {
						temperatureScale -= 10.0;
						temperatureDivisions = temperatureScale / 4.0;
					}
				}
				else {	// otherwise it is a linear type plot- either z vs f, or nyquist -zimag vs zreal
					if (Vertical_Scale > 3000000) {
						Vertical_Scale -= 1000000;
						ChartFloor += 500000;
						Major_Divisions = 1000000;
					}
					else if(Vertical_Scale > 1000000) {
						Vertical_Scale -= 500000;
						ChartFloor += 250000;
						Major_Divisions = 500000;
					}
					else if(Vertical_Scale > 500000) {
						Vertical_Scale -= 100000;
						ChartFloor += 50000;
						Major_Divisions = 100000;
					}
					else if(Vertical_Scale > 100000) {
						Vertical_Scale -= 50000;
						ChartFloor += 25000;
						Major_Divisions = 50000;
					}
					else if (Vertical_Scale > 30000) {
						Vertical_Scale -= 10000;
						ChartFloor += 5000;
						Major_Divisions = 10000;
					}
					else if (Vertical_Scale > 5000) {
						Vertical_Scale -= 5000;
						ChartFloor += 2500;
						Major_Divisions = 5000;
					}
					else if (Vertical_Scale <= 5000) {
						if (Vertical_Scale > 1001) {
							Vertical_Scale -= 1000;
							ChartFloor += 500;
						}
						Major_Divisions = 500;
					}
					logVertical_Scale = java.lang.Math.round(java.lang.Math.log10(Vertical_Scale) + 0.5);	// keep log scale in tandem with linear scale changes...
				}
			}
			rescaleVerticalAxes();
		}
	}

	private void rescaleVerticalAxes() {
		if (Method == Real_Time_Activity.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) {	// EIS impedance always positive (start vertical scale at 0)
			switch(EIS_plotType) {
				case Real_Time_Activity.LOG_PLOT:
					mChArea.getDefaultYAxis().getScale().setRange(0, logVertical_Scale);
					break;
				case Real_Time_Activity.LINEAR_PLOT:
					mChArea.getDefaultYAxis().getScale().setRange(0, Vertical_Scale);
					mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
					break;
				case Real_Time_Activity.NYQUIST_PLOT:
					mChArea.getDefaultYAxis().getScale().setRange(0, Vertical_Scale);
					mChArea.getDefaultXAxis().getScale().setRange(0, Vertical_Scale);	// make nyquist plot square- only first quadrant, same axes scales
					mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
					mChArea.getDefaultXAxis().getScale().setInterval(Major_Divisions);
					break;
				default:
					break;
			}
		}
		else if (Method == Real_Time_Activity.STATIC_LOGGING) {
			switch(EIS_plotType) {
				case Real_Time_Activity.LOG_PLOT:	// "LOG_PLOT" for STATIC_LOGGING is just linear plot of current...
					mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);
					mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
					break;
				case Real_Time_Activity.LINEAR_PLOT:	// "LINEAR_PLOT" for STATIC_LOGGING is potential plot vs time
					mChArea.getDefaultYAxis().getScale().setRange(potentialFloor, potentialScale);
					mChArea.getDefaultYAxis().getScale().setInterval(potentialDivisions);
					break;
				case Real_Time_Activity.NYQUIST_PLOT: // "NYQUIST_PLOT" for STATIC_LOGGING is temperature plot vs. time
					mChArea.getDefaultYAxis().getScale().setRange(temperatureFloor, temperatureScale);
					mChArea.getDefaultYAxis().getScale().setInterval(temperatureDivisions);
					break;
				default:
					break;
			}
		}
		else {
			mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);
			mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
		}
	}
	
	/*
	 * Function for when radio buttons are toggled (either select optical ch1 
	 * (Fluor 1, first bank of 8 RFU values starting at index 0) or ch2 (second bank starting at index 8)
	 */
	public void onRadioButtonClicked(View v) {
		if(D) Log.e(TAG, "Radio Button Selected");
		RadioButton rb = (RadioButton) v;
		boolean update = false;
		switch(rb.getId()) {
			case(R.id.dataview_z_magnitude):// this is the impedance button for EIS
				if (phaseDisplay) {
					update = true;	// if not already on impedance setting, need to update chart
					phaseDisplay = false;
				}
				break;
			case(R.id.dataview_phi):	// phase select button for EIS
				if (!phaseDisplay) {
					update = true;
					phaseDisplay = true;
				}
				break;
			case(R.id.dataview_logplot):
				if (EIS_plotType != Real_Time_Activity.LOG_PLOT) {
					update = true;
					EIS_plotType = Real_Time_Activity.LOG_PLOT;
				}
				break;
			case(R.id.dataview_linearplot):
				if (EIS_plotType != Real_Time_Activity.LINEAR_PLOT) {
					update = true;
					EIS_plotType = Real_Time_Activity.LINEAR_PLOT;
				}
				break;
			case(R.id.dataview_nyquist):
				if (EIS_plotType != Real_Time_Activity.NYQUIST_PLOT) {
					update = true;
					EIS_plotType = Real_Time_Activity.NYQUIST_PLOT;
				}
				break;
			default:
				break;
		}
		if (update) {
			// first instruction unnecessary because buttons only active for EIS analysis, and EIS_axisLabels() called in UpdateChart for EIS
			if (Method == Real_Time_Activity.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) EIS_axisLabels();
			UpdateChart();
		}
	}

	/*
	EIS_chartDataSort- called when charting any EIS data values- decides which series to update with which data
	 */
	private void EIS_chartDataSort(double f, double impedanceMagnitude, double phase) {
		switch (EIS_plotType) {
			case Real_Time_Activity.LOG_PLOT:
				if (phaseDisplay)
					series[1].getPoints().addXY(java.lang.Math.log10(f), phase);
				else
					series[0].getPoints().addXY(java.lang.Math.log10(f), java.lang.Math.log10(impedanceMagnitude));
				break;
			case Real_Time_Activity.LINEAR_PLOT:
				if (phaseDisplay)
					series[1].getPoints().addXY(f, phase);
				else
					series[0].getPoints().addXY(f, impedanceMagnitude);
				break;
			case Real_Time_Activity.NYQUIST_PLOT:
				double realZ = impedanceMagnitude * java.lang.Math.cos(phase * java.lang.Math.PI / 180.0);
				double imagZ = -impedanceMagnitude * java.lang.Math.sin(phase * java.lang.Math.PI / 180.0);
				series[0].getPoints().addXY(realZ, imagZ);
				break;
		}
	}

	/*
	 *
	 */
	private void UpdateChart() {
		series[0].getPoints().clear();
		series[1].getPoints().clear();
		switch(Method) {
			case Real_Time_Activity.CYCLIC_VOLTAMMETRY:
				for (int j = 0; j < Potential.size(); j++) {
					double volt = Real_Time_Activity.safeParseDouble(Potential.get(j));    // convert to minutes before charting
					double currier = Real_Time_Activity.safeParseDouble(Current.get(j));
					series[0].getPoints().addXY(volt, currier);
				}
				break;
			case Real_Time_Activity.DIFFERENTIAL_PULSE_VOLTAMMETRY:
				for (int j = 0; j < Potential.size(); j++) {
					double volt = Real_Time_Activity.safeParseDouble(Potential.get(j));    // convert to minutes before charting
					double currier = Real_Time_Activity.safeParseDouble(Current.get(j));
					series[0].getPoints().addXY(volt, currier);
				}
				break;
			case Real_Time_Activity.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY:
				EIS_axisLabels();
				for (int j = 0; j < Frequency.size(); j++) {
					double freq = Real_Time_Activity.safeParseDouble(Frequency.get(j));
					double impMagnitude = Real_Time_Activity.safeParseDouble(Z.get(j));
					double phaseAngle = Real_Time_Activity.safeParseDouble(Phi.get(j));
					EIS_chartDataSort(freq, impMagnitude, phaseAngle);
				}
				break;
			case Real_Time_Activity.STATIC_LOGGING:
				EIS_axisLabels();	// function also handles changes in axis labels for STATIC_LOGGING method...
				staticLoggingDataSort();
				break;
			default:
				break;
		}
	}

	/*
	staticLoggingDataSort- called when new EIS data arrives- decides which series to update with which data (so entire chart
	doesn't need to be rebuilt every time EIS data arrives
	 */
	private void staticLoggingDataSort() {
		switch (EIS_plotType) {
			case Real_Time_Activity.LOG_PLOT:	// for STATIC_LOGGING, LOG_PLOT is setting to plot current vs. time
				for (int j = 0; j < Time.size(); j++) {
					double time = Real_Time_Activity.safeParseDouble(Time.get(j)) / 60.0;	// time in minutes
					double current = Real_Time_Activity.safeParseDouble(Current.get(j));
					series[0].getPoints().addXY(time, current);
				}
				break;
			case Real_Time_Activity.LINEAR_PLOT: // for STATIC_LOGGING, LINEAR_PLOT is setting to plot potential vs. time
				for (int j = 0; j < Time.size(); j++) {
					double time = Real_Time_Activity.safeParseDouble(Time.get(j)) / 60.0;
					double eVolts = Real_Time_Activity.safeParseDouble(Potential.get(j));
					series[0].getPoints().addXY(time, eVolts);
				}
				break;
			case Real_Time_Activity.NYQUIST_PLOT: // for STATIC_LOGGING, NYQUIST_PLOT is setting to plot temperatures vs. time
				for (int j = 0; j < Time.size(); j++) {
					double time = Integer.parseInt(Time.get(j)) / 60.0;
					if (BoardTemp.get(j) != null) {
						double boardTemp = Real_Time_Activity.safeParseDouble(BoardTemp.get(j));
						series[0].getPoints().addXY(time, boardTemp);
						if (!TCTemp.get(j).equals(ABEStatActivity.NO_THERMOCOUPLE_STRING)) {
							double tcTemperature = Real_Time_Activity.safeParseDouble(TCTemp.get(j));
							series[1].getPoints().addXY(time, tcTemperature);
						}
					}
				}
				break;
			default:
				break;
		}
	}
    
    @Override
    public void onBackPressed() 
    {
    	showDialog(QUIT_DIALOG); // show the quit dialog
    	return; // if user quits, dialog call will handle closing threads; otherwise return
    }

    @Override
    protected Dialog onCreateDialog(int id) {
    	switch(id) {
    	case QUIT_DIALOG:
    		return new AlertDialog.Builder(this)
    		.setTitle(R.string.exit_query)
    		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) {
    				Intent quitIntent = new Intent();
    				setResult(Activity.RESULT_OK, quitIntent);
    				finish();
    			}
    		})
    		.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) {
    				dismissDialog(QUIT_DIALOG);
    			}
    		})
    		.create();
  	}
  	return null;
  } 
}
