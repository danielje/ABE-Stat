package com.diagenetix.abestat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ParseException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.artfulbits.aiCharts.Base.ChartArea;
import com.artfulbits.aiCharts.Base.ChartSeries;
import com.artfulbits.aiCharts.ChartView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This is the activity that actually communicates with device and manages data during real time analysis
 * Tasks include, prompting device to observe and report back fluorescences at predefined intervals, managing time,
 * managing real time canvases displaying data graphically, maintain two dimensional array of data (time, temperature, and
 * fluorescence values), and at the end of the analysis allowing user to store the recorded data in a comma delimited file.
 */

public class Real_Time_Activity extends Activity {
	// for Debugging...
	private static final String TAG = "Real_Time_Analysis";
	private static final boolean D = true;
	
	public static final String FILE_SAVED = "File_Saved";
	public static final String SAVE_FILE_CHECK = "Save_File_Check";

	// Data keys for data returned from mBTService:
	public static final String POTENTIAL_KEY = "voltage";
	public static final String CURRENT_KEY = "current";
	public static final String FREQUENCY_KEY = "frequency";
	public static final String IMPEDANCE_MAGNITUDE_KEY = "zmag";
	public static final String IMPEDANCE_PHASE_KEY = "zphase";

	//codes for currently running method
	public static final int CYCLIC_VOLTAMMETRY = 0;
	public static final int ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY = 1;
	public static final int DIFFERENTIAL_PULSE_VOLTAMMETRY = 2;
	public static final int ANODIC_STRIPPING_VOLTAMMETRY = 3;
	public static final int STATIC_LOGGING = 4;

	// codes for electrode configuration
	private static final int OPEN_CIRCUIT_POTENTIAL = 0;
	private static final int TWO_ELECTRODE_CONFIGURATION = 2;
	private static final int THREE_ELECTRODE_CONFIGURATION = 3;

	// codes for chart EIS chart type
	public static final int LOG_PLOT = 0;
	public static final int LINEAR_PLOT = 1;
	public static final int NYQUIST_PLOT = 2;

	private static final int PERMISSIONS_REQUEST_CODE = 1;

	// Handler Request Codes for mBTService
	public static final int CV_DATA = 30;
	public static final int EQ_TIME_LEFT = 31;
	public static final int CYCLE_COMPLETE = 32;
	public static final int EIS_DATA = 33;
	
	// Constants- minimum change in temperature HEAT_THRESHOLD after initial warmup period PREHEAT_TIME;
	// Maximum safe operating temperature MAX_TEMP
	private static final String EXIT_STRING = "xxx";
	private static final char tab = '\t';
	
	// Process name and parameters passed in intent from Main (ABEStatActivity)
	private static String Title;
	private static String Device;
	private static String Firmware;

	private static int Method;
	private static double StartFrequency;	// start frequency for electrochemical impedance spectroscopy
	private static double EndFrequency;	// end frequency for electrochemical impedance spectroscopy
	private static int IntervalFrequency;	// frequency increment for analysis with electrochemical impedance spectroscopy
	private static int SignalAmplitude;	// code for the signal amplitude applied to network (can be 3, 2, 1, 0 for 200 mV, 100mV, 40 mV, and 20mVp-p)
	private static double BiasVoltage;	// bias voltage applied during electrochemical impedance spectroscopy
	private static double StartVoltage;	// low voltage end for cyclic voltammetry
	private static double EndVoltage;	// high voltage end for cyclic voltammetry
	private static double ScanRate;	// scan rate in V/s, for cyclic voltammetry
	private static String ScanNumber;	// # of scans to be executed (leave as string, because can be INF for continuous scanning)
	private static int scansRemaining;
	private static int scanDuration = 30;	// # of minutes to record data in STATIC_LOGGING method
	private static long staticLoggingIndex = 0;
	private static int StepFrequency;	// frequency (Hz) for steps in Differential Pulse Voltammetry
	private static int StepPotential;	// step potential (mV) for Differential Pulse Voltammetry
	private static int PulseAmplitude;	// pulse amplitude (mV) using Differential Pulse Voltammetry
	private static int ElectrodeConfig;	// integer code for electrode configuration, 2 for 2 electrode, 3 for 3 electrode, 0 or any other for open circuit
	private static int EquilibrationTime;	// time in seconds to equilibrate at starting potential

	private static double boardTemperature = 20.0;
	private static String tcTemperature = ABEStatActivity.NO_THERMOCOUPLE_STRING;

	private static double TempVal = 20.0;

	private static boolean phaseDisplay;	// selects whether to display phase or impedance on EIS plots

	private static int EIS_plotType = LINEAR_PLOT;	// by default make it linear (otherwise it may plot non EIS data incorrectly on log scale)

	public static boolean BT_Break = false;	// variable to see if Bluetooth connection has already 
								// been lost at least once during reaction; if so next connection
								// starts remote reset..
	
	private static int Battery;

	private static long Vertical_Scale = 200000;
	private static long ChartFloor = -100000;
	private static double Major_Divisions = 50000;

	private static long logVertical_Scale = java.lang.Math.round(java.lang.Math.log10(Vertical_Scale) + 0.5);	// change vertical scales in linear and log plots in concert...
	private static double potentialScale = 2.0;
	private static double potentialFloor = 0.0;
	private static double potentialDivisions = 0.5;
	private static double temperatureScale = 100.0;
	private static double temperatureFloor = 0.0;
	private static double temperatureDivisions = 25.0;

	private static float touchY1, touchY2;
	private static int MIN_DISTANCE = 150;
	
	// Layout Views/ User Controls
	private static TextView mProcessParameters;
	private static TextView mReturnedData;
	private static TextView mMethod;
	private static TextView mTitle;
	private static TextView mTemperature;
	private static TextView mBattery;
	private static Button mQButton;
	private static ImageView mThreshold_up;
	private static ImageView mThreshold_down;
	private static CheckBox Save_Data_Check;

	private static boolean File_Saved = false;
	//private static boolean Thread_Interrupted = false;
	
	// ai ChartView components
	// ChartSeries are populated by data arrays representing time and RFU values
	private ChartView mChartView;
	private ChartArea mChArea;
	
	//private ChartGestureListener mChartPanner;
	
	private ChartSeries series[] = new ChartSeries[2];
	private static int ChannelBank = 0; // offset for selecting which optical channel to display (0 for Fluor 1, 8 for Fluor 2)

	private String DateString = "";
	private String FileName = "";
	
	private static PowerManager pm;
	private static PowerManager.WakeLock wl;

	private Timer loggingTimer = new Timer();
	private TimerTask dataRequest;
	
	public static boolean BTComm = true;	// start with it true (for checking BT connection status by listening for incoming characters)
	// in this app, start it as true assume connection is made, so don't try to reconnect prematurely before we've even asked for communication

	private static int LostCommsCount = 0;

	private ArrayList<String> Frequency = new ArrayList<String>();
	private ArrayList<String> Z = new ArrayList<String>();
	private ArrayList<String> Phi = new ArrayList<String>();
	private ArrayList<String> Potential = new ArrayList<String>();
	private ArrayList<String> Current = new ArrayList<String>();
	private ArrayList<String> BoardT = new ArrayList<String>();
	private ArrayList<String> TCTemp = new ArrayList<String>();
	private ArrayList<String> Time = new ArrayList<String>();
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        // collect reaction parameter data passed in intent
        Firmware = getIntent().getStringExtra(ABEStatActivity.FIRMWARE_VERSION_NAME);
        Battery = getIntent().getIntExtra(ABEStatActivity.BATTERY, 100);
        
        //TempVal = getIntent().getDoubleExtra(ABEStatActivity.TEMPERATURE, -300.0);
        
        Title = getIntent().getStringExtra(SettingsDbAdapter.KEY_TITLE);
        Device = getIntent().getStringExtra(ABEStatActivity.DEVICE_NAME);
		String startf = getIntent().getStringExtra(SettingsDbAdapter.KEY_START_FREQUENCY);
		String endf = getIntent().getStringExtra(SettingsDbAdapter.KEY_END_FREQUENCY);
		String intervalf = getIntent().getStringExtra(SettingsDbAdapter.KEY_INTERVAL_FREQUENCY);
		String signalamp = getIntent().getStringExtra(SettingsDbAdapter.KEY_SIGNAL_AMPLITUDE);
		String biasvoltage = getIntent().getStringExtra(SettingsDbAdapter.KEY_BIAS_VOLTAGE);
		String startvoltage = getIntent().getStringExtra(SettingsDbAdapter.KEY_START_VOLTAGE);
		String endvoltage = getIntent().getStringExtra(SettingsDbAdapter.KEY_END_VOLTAGE);
		String scanrate = getIntent().getStringExtra(SettingsDbAdapter.KEY_SCAN_RATE);

		String stepf = getIntent().getStringExtra(SettingsDbAdapter.KEY_STEP_FREQUENCY);
		String stepe = getIntent().getStringExtra(SettingsDbAdapter.KEY_STEP_E);
		String pulseamp = getIntent().getStringExtra(SettingsDbAdapter.KEY_PULSE_AMPLITUDE);
		String equilt = getIntent().getStringExtra(SettingsDbAdapter.KEY_EQUILIBRIUM_TIME);

		Firmware = getIntent().getStringExtra(ABEStatActivity.FIRMWARE_VERSION_NAME);


		StartFrequency = SettingsEdit.localeSpecificParsedDoubleString(startf);
		EndFrequency = SettingsEdit.localeSpecificParsedDoubleString(endf);
		IntervalFrequency = SettingsEdit.localeSpecificParsedIntString(intervalf);
		SignalAmplitude = SettingsEdit.localeSpecificParsedIntString(signalamp);
		BiasVoltage = SettingsEdit.localeSpecificParsedDoubleString(biasvoltage);
		StartVoltage = SettingsEdit.localeSpecificParsedDoubleString(startvoltage);
		EndVoltage = SettingsEdit.localeSpecificParsedDoubleString(endvoltage);
		ScanRate = SettingsEdit.localeSpecificParsedDoubleString(scanrate);
		ScanNumber = getIntent().getStringExtra(SettingsDbAdapter.KEY_SCAN_NUMBER);
		StepFrequency = SettingsEdit.localeSpecificParsedIntString(stepf);
		StepPotential = SettingsEdit.localeSpecificParsedIntString(stepe);
		PulseAmplitude = SettingsEdit.localeSpecificParsedIntString(pulseamp);
		EquilibrationTime = SettingsEdit.localeSpecificParsedIntString(equilt);

		String config = getIntent().getStringExtra(SettingsDbAdapter.KEY_ELECTRODE_CONFIGURATION);
		if (config.equals(SettingsDbAdapter.OPEN_CIRCUIT_CONFIGURATION)) ElectrodeConfig = OPEN_CIRCUIT_POTENTIAL;
		else if (config.equals(SettingsDbAdapter.TWO_ELECTRODE_CONFIGURATION)) ElectrodeConfig = TWO_ELECTRODE_CONFIGURATION;
		else if (config.equals(SettingsDbAdapter.THREE_ELECTRODE_CONFIGURATION)) ElectrodeConfig = THREE_ELECTRODE_CONFIGURATION;

		config = getIntent().getStringExtra(SettingsDbAdapter.KEY_METHOD);
		if (config.equals(SettingsDbAdapter.CYCLIC_VOLTAMMETRY)) Method = CYCLIC_VOLTAMMETRY;
		else if (config.equals(SettingsDbAdapter.DIFFERENTIAL_PULSE_VOLTAMMETRY)) Method = DIFFERENTIAL_PULSE_VOLTAMMETRY;
		else if (config.equals(SettingsDbAdapter.ANODIC_STRIPPING_VOLTAMMETRY)) Method = ANODIC_STRIPPING_VOLTAMMETRY;
		else if (config.equals(SettingsDbAdapter.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY)) Method = ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY;
		else if (config.equals(SettingsDbAdapter.STATIC_LOGGING)) Method = STATIC_LOGGING;

		int noPermissionCount = 0;	// count up the number app permissions not granted (used to request all remaining permissions at once)

		ArrayList<String> PermissionsList = new ArrayList<String>();
		if (!ABEStatActivity.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getApplicationContext(), this)) {
			noPermissionCount++;	// increment the count of no permissions
			PermissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}

		String[] PermissionsString = new String[noPermissionCount];
		PermissionsList.toArray(PermissionsString);

		if (noPermissionCount > 0) {
			Toast.makeText(this, "You must give access external storage or your data will be lost!", Toast.LENGTH_LONG).show();
			ActivityCompat.requestPermissions(this, PermissionsString, PERMISSIONS_REQUEST_CODE);
		}

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.realtime);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

		// Fill in title bar with app name and indicate that reaction is underway
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);
		mTitle.setText(R.string.title_connected_to);
		mTitle.append(Device);

		mMethod = (TextView) findViewById(R.id.method_title);
		String DeviceStringFormat = getResources().getString(R.string.rt_device_method);
		String DeviceStringMsg = String.format(DeviceStringFormat, Device);
		mMethod.setText(DeviceStringMsg);

		mProcessParameters = (TextView) findViewById(R.id.method_summary);
		String MethodSummary = "";
		String ParameterStringFormat = "";
		String electrodeConfigString = "";
		switch (ElectrodeConfig) {
			case OPEN_CIRCUIT_POTENTIAL:
				electrodeConfigString = getResources().getString(R.string.open_circuit_config).toString();
				break;
			case TWO_ELECTRODE_CONFIGURATION:
				electrodeConfigString = getResources().getString(R.string.two_electrode_configuration).toString();
				break;
			case THREE_ELECTRODE_CONFIGURATION:
				electrodeConfigString = getResources().getString(R.string.three_electrode_configuration).toString();
				break;
			default:
				break;
		}

		String scanNumberString = ScanNumber;
		if (ScanNumber.equals(SettingsDbAdapter.INFINITE)) scanNumberString = getResources().getString(R.string.infinite).toString();
		switch (Method) {
			case CYCLIC_VOLTAMMETRY:
				ParameterStringFormat = getResources().getString(R.string.cv_method_string);
				MethodSummary = String.format(ParameterStringFormat, Title,
						startvoltage, endvoltage, scanrate,
						equilt, scanNumberString, electrodeConfigString);
				break;
			case DIFFERENTIAL_PULSE_VOLTAMMETRY:
				ParameterStringFormat = getResources().getString(R.string.dpv_method_string);
				MethodSummary = String.format(ParameterStringFormat, Title,
						startvoltage, endvoltage, stepf, stepe, pulseamp,
						equilt, scanNumberString, electrodeConfigString);
				break;
			case ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY:
				ParameterStringFormat = getResources().getString(R.string.eis_method_string);
				String amplitude = "";
				switch (SignalAmplitude) {
					case 0x00:
						amplitude = getResources().getString(R.string.amplitude0);
						break;
					case 0x01:
						amplitude = getResources().getString(R.string.amplitude1);
						break;
					case 0x02:
						amplitude = getResources().getString(R.string.amplitude2);
						break;
					case 0x03:
						amplitude = getResources().getString(R.string.amplitude3);
					break;
					default:
					break;
				}
				MethodSummary = String.format(ParameterStringFormat, Title,
						startf, endf, intervalf, amplitude,
						equilt, biasvoltage, electrodeConfigString);
				break;
			case STATIC_LOGGING:
				String potentialString = "";
				String durationString = "";
				ParameterStringFormat = getResources().getString(R.string.applied_potential_string);
				if (!(ElectrodeConfig == OPEN_CIRCUIT_POTENTIAL))
					potentialString = String.format(ParameterStringFormat, startvoltage); // don't report applied potential unless not open circuit potential
				ParameterStringFormat = getResources().getString(R.string.record_duration_string);
				if (ScanNumber.equals(SettingsDbAdapter.INFINITE))
					durationString = getResources().getString(R.string.open_ended_duration).toString();
				else {
					durationString = String.format(ParameterStringFormat, ScanNumber);
					scanDuration = Integer.parseInt(ScanNumber) * 60; // convert scan duration in min to scan duration in seconds...
					if (D) Log.e(TAG, "scan duration " + scanDuration + "s");
				}
				ParameterStringFormat = getResources().getString(R.string.static_record_string);
				MethodSummary = String.format(ParameterStringFormat, Title,
						equilt, durationString, electrodeConfigString, potentialString);
				break;
			default:
				break;
		}
		//CharSequence csParameters = MethodSummary;

		mProcessParameters.setText(MethodSummary);//(Html.fromHtml(MethodSummary, Html.FROM_HTML_MODE_LEGACY));
		if (Method != ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) mProcessParameters.setPadding(0,0,0,0);

		mReturnedData = (TextView) findViewById(R.id.returned_data);

		mTemperature = (TextView) findViewById(R.id.temp_report);

		mBattery = (TextView) findViewById(R.id.batt_report);
		Log.e(TAG, "battery charge: " + Battery);
		String battFormat = getResources().getString(R.string.battery_report);
		String battText = String.format(battFormat, Battery);
		mBattery.setText(battText + "%");

		if (!ScanNumber.equals(SettingsDbAdapter.INFINITE)) scansRemaining = (SettingsEdit.localeSpecificParsedIntString(ScanNumber)) - 1;
		else scansRemaining = 30;	// make sure we populate this with a number, just in case...; note that for STATIC_LOGGING, scansRemaining is time in min for data record
		SetUpUserInterfaceWidgets();
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        BT_Break = false;
        
        Battery = 100;	// start battery at 100 (make sure routine doesn't exit automatically at start due to low battery reading, before battery read)

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        start_wakelock();
    	
    	Calendar calendar = new GregorianCalendar();
        
        String StringFormat = getResources().getString(R.string.file_name_string);
        String Year = calendar.get(Calendar.YEAR) + "";
        String Month = padCalendarItems(calendar.get(Calendar.MONTH)+1);	// months are indexed starting at 0 in Calendar
        String Day = padCalendarItems(calendar.get(Calendar.DAY_OF_MONTH));
        String Hour = padCalendarItems(calendar.get(Calendar.HOUR_OF_DAY));
        String Minute = padCalendarItems(calendar.get(Calendar.MINUTE));
        String Second = padCalendarItems(calendar.get(Calendar.SECOND));
        //   	 String MonthString = Month + "";
        FileName = String.format(StringFormat, Title, Year, Month, Day, Hour, Minute, Second);
        //   	 String FileName = (calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-" + calendar.get(Calendar.DAY_OF_MONTH) + " " + calendar.get(Calendar.HOUR_OF_DAY) + "_" + calendar.get(Calendar.MINUTE) + ".csv");
        Log.e(TAG, "File name string: " + FileName); 
        
        StringFormat = getResources().getString(R.string.date_string);
		DateString = String.format(StringFormat, Year, Month, Day, Hour, Minute, Second);
        
        ABEStatActivity.mBTService.newHandler(rtHandler);
        if(D) Log.e(TAG, "Reassigned bluetooth service handler to rtHandler...");
        
        File_Saved = false;
        
        Save_Data_Check = (CheckBox) findViewById(R.id.save_data);

		Save_Data_Check.setChecked(true);
		Save_Data_Check.setVisibility(View.VISIBLE);
        
        String Storage_State = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(Storage_State))
        	Toast.makeText(this, R.string.ext_not_available, Toast.LENGTH_LONG).show();

        UpdateChart();	// make sure chart is updated to reflect settings for the given method

		String commandString = "";	// build the string with commands to set up analysis...

		switch (ElectrodeConfig) {
			case OPEN_CIRCUIT_POTENTIAL:
				commandString += "f0";
				break;
			case TWO_ELECTRODE_CONFIGURATION:
				commandString += "f2";
				break;
			case THREE_ELECTRODE_CONFIGURATION:
				commandString += "f3";
				break;
			default:
				break;
		}
		commandString += "a";// COMMAND TO ENTER ANALYTICAL SELECTION FUNCTION
		//NumberFormat nFormatter = NumberFormat.getInstance();
		switch (Method) {
			case CYCLIC_VOLTAMMETRY:
				/*commandString += "c" + Double.toString(StartVoltage).replace(",", "") + tab + Double.toString(EndVoltage).replace(",", "") +
						+ tab + Double.toString(ScanRate).replace(",", "") + tab + Integer.toString(EquilibrationTime).replace(",", "") + tab;*/
				commandString += "c" + Double.toString(StartVoltage) + tab + Double.toString(EndVoltage) + tab + Double.toString(ScanRate) + tab + Integer.toString(EquilibrationTime) + tab;
				break;
			case DIFFERENTIAL_PULSE_VOLTAMMETRY:
				commandString += "d" + Double.toString(StartVoltage) + tab + Double.toString(EndVoltage) + tab + Integer.toString(StepFrequency) + tab + Integer.toString(StepPotential) + tab + Integer.toString(PulseAmplitude) + tab + Integer.toString(EquilibrationTime) + tab;
				break;
			case ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY:
				/*commandString += "e" + Double.toString(StartFrequency).replace(",", "") + tab + Double.toString(EndFrequency).replace(",", "") +
						+ tab + Integer.toString(IntervalFrequency).replace(",", "") + tab + Double.toString(BiasVoltage).replace(",", "") +
						+ tab + signalamp + tab + Integer.toString(EquilibrationTime).replace(",", "") + tab;*/
				commandString += "e" + Double.toString(StartFrequency) + tab + Double.toString(EndFrequency) + tab + Integer.toString(IntervalFrequency) + tab + Double.toString(BiasVoltage) + tab + signalamp + tab + Integer.toString(EquilibrationTime) + tab;
				break;
			case STATIC_LOGGING:
				commandString += "l" + startvoltage + tab + Integer.toString(EquilibrationTime) + tab;	// send the potential for the analysis (electrode configuration already sent, and "equillibration time"
				break;
			case ANODIC_STRIPPING_VOLTAMMETRY:
				break;
			default:
				break;
		}
		sendMessage(commandString);
	}

	private void Post_Temperature() {
		String TempReportFormat = getResources().getString(R.string.temperature_report);
		String thermoString = tcTemperature;
		if (!tcTemperature.equals(ABEStatActivity.NO_THERMOCOUPLE_STRING)) thermoString += getResources().getString(R.string.degrees_c).toString();
		String TempReportMsg = String.format(TempReportFormat, boardTemperature, thermoString);
		mTemperature.setText(TempReportMsg);
	}

	private void SetUpUserInterfaceWidgets() {
        // Fill out textviews summarizing the method name and parameters
		mChartView = (ChartView) findViewById(R.id.chart1);
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
					Log.e(TAG, "Chart was touched at Y position " + touchY1);
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
					Log.e(TAG, "Chart was released at Y position " + touchY2);
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

        switch (Method) {
			case CYCLIC_VOLTAMMETRY:
				voltammetryChartSetup();
				break;
			case DIFFERENTIAL_PULSE_VOLTAMMETRY:
				voltammetryChartSetup();
				break;
			case ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY:
				mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.frequency));
				mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.impedance));
				RadioButton rb = (RadioButton) findViewById(R.id.z_magnitude);
				rb.setChecked(true);	// set up axes titles, and make sure radio button default set to impedance magnitude
				phaseDisplay = false;
				rb = (RadioButton) findViewById(R.id.logplot);
				rb.setChecked(true);
				EIS_plotType = LOG_PLOT;	// by default, show logarithmic impedance magnitude plot, and set corresponding radio buttons
				findViewById(R.id.threshold_controls).setVisibility(View.GONE);	// Don't allow change of origin on EIS plots
				findViewById(R.id.chart_controls).setVisibility(View.VISIBLE); // but show radio buttons to toggle Z and phi, and change axes/ plot type
				EIS_axisLabels();
				break;
			case ANODIC_STRIPPING_VOLTAMMETRY:
				mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.potential));
				mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.current));
				findViewById(R.id.chart_controls).setVisibility(View.VISIBLE); // show radio buttons to toggle Z and phi, and change axes/ plot type
				break;
			case STATIC_LOGGING:	// by default show current vs. time...
				mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.time_heading));
				mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.current));
				findViewById(R.id.chart_controls).setVisibility(View.VISIBLE); // show radio buttons to toggle Z and phi, and change axes/ plot type
				findViewById(R.id.axes_select).setVisibility(View.VISIBLE);
				TextView yAxisSettingsLabel = (TextView) findViewById(R.id.axes_settings);
				yAxisSettingsLabel.setText(getResources().getString(R.string.data_select).toString());
				RadioButton radB = (RadioButton) findViewById(R.id.logplot);
				radB.setText(getResources().getString(R.string.current).toString());
				radB.setChecked(true); // plot current by default
				radB = (RadioButton) findViewById(R.id.linearplot);
				radB.setText(getResources().getString(R.string.potential).toString());
				radB = (RadioButton) findViewById(R.id.nyquist);
				radB.setText(getResources().getString(R.string.temperature).toString());
				phaseDisplay = false;
				EIS_plotType = LOG_PLOT;	// default setting is "LOG_PLOT", for Static logging this is current vs. time
				double xTicks = 1.0;
				scansRemaining = 30;	// default time range for data record in minutes
				if (!ScanNumber.equals(SettingsDbAdapter.INFINITE))
					scansRemaining = Integer.parseInt(ScanNumber);
				xTicks = scansRemaining / 4.0;
				mChArea.getDefaultXAxis().getScale().setRange(0, scansRemaining);
				mChArea.getDefaultXAxis().getScale().setInterval(xTicks);
				findViewById(R.id.chart_data_select).setVisibility(View.GONE);
				findViewById(R.id.data_select_group).setVisibility(View.GONE);
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

		mQButton = (Button) findViewById(R.id.quit_button);
        mQButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View w) {
        		ExitDialog();
        	}
        });

        mThreshold_up = (ImageView) findViewById(R.id.thresh_up);
        mThreshold_up.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {	// don't need to worry about EIS case, because these controls not available for EIS
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (Method == STATIC_LOGGING) {
						switch (EIS_plotType) {
							case LOG_PLOT:
								ChartFloor += Major_Divisions;	// LOG_PLOT is current vs time for STATIC_LOGGING
								mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);
								break;
							case LINEAR_PLOT:
								potentialFloor += potentialDivisions;	// LINEAR_PLOT is potential vs time for STATIC_LOGGING
								mChArea.getDefaultYAxis().getScale().setRange(potentialFloor, potentialFloor + potentialScale);
								break;
							case NYQUIST_PLOT:
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

        mThreshold_down = (ImageView) findViewById(R.id.thresh_down);
        mThreshold_down.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (Method == STATIC_LOGGING) {
						switch (EIS_plotType) {
							case LOG_PLOT:
								ChartFloor -= Major_Divisions;	// LOG_PLOT is current vs time for STATIC_LOGGING
								mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);
								break;
							case LINEAR_PLOT:
								potentialFloor -= potentialDivisions;	// LINEAR_PLOT is potential vs time for STATIC_LOGGING
								mChArea.getDefaultYAxis().getScale().setRange(potentialFloor, potentialFloor + potentialScale);
								break;
							case NYQUIST_PLOT:
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
	}

	private void voltammetryChartSetup() {
		mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.potential));
		mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.current));
		mChArea.getDefaultXAxis().getScale().setRange(StartVoltage - 0.1, EndVoltage + 0.1);
		double Einterval = 0.1;
		if ((EndVoltage - StartVoltage) >= 2)  Einterval = 0.5;
		else if ((EndVoltage - StartVoltage) >= 1)  Einterval = 0.25;
		mChArea.getDefaultXAxis().getScale().setInterval(Einterval);
		mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);	// default range 100000 nA (100 uA)
		findViewById(R.id.chart_controls).setVisibility(View.GONE); // don't show radio buttons to toggle Z and phi
	}

	private void ZoomOut(int factor) {
		if (!phaseDisplay || (EIS_plotType == NYQUIST_PLOT)) {	// EIS_plotType == LINEAR for voltammetry methods...
			for (int a = 0; a < factor; a++) {
				if ((EIS_plotType == LOG_PLOT) && (Method == ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY)) {
					logVertical_Scale++;	// for log plots, just adjust magnitudes one log unit at a time...
					Vertical_Scale = (long) java.lang.Math.pow(10, logVertical_Scale);
					ChartFloor = -Vertical_Scale / 2;
					Major_Divisions = Vertical_Scale / 5;	// adjust magnitudes of linear scales, so data is framed as desired in linear axes too
				}
				else if ((EIS_plotType == LINEAR_PLOT) && (Method == STATIC_LOGGING)) { // "LINEAR_PLOT" button used to plot voltage / potential in STATIC_LOGGING
					if (potentialScale < 2.0) {
						potentialScale += 0.1;    // only let user go up to 200 C...
						potentialDivisions = potentialScale / 4.0;
					}
				}
				else if ((EIS_plotType == NYQUIST_PLOT) && (Method == STATIC_LOGGING)) {	// "NYQUIST_PLOT" Button used to plot temperature
					if (temperatureScale < 200.0) {
						temperatureScale += 10.0;
						temperatureDivisions = temperatureScale / 4.0;
					}
				}
				else {	// all other cases, including STATIC_LOGGING with "LOG_PLOT setting, is setup to chart current in nA on linear scale
					if (Vertical_Scale < 5000) {
						Vertical_Scale += 1000;
						ChartFloor -= 500;
						Major_Divisions = 1000;
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
		if (!phaseDisplay || (EIS_plotType == NYQUIST_PLOT)) {
			for (int a = 0; a < factor; a++) {
				if ((EIS_plotType == LOG_PLOT) && (Method == ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY)) {
					logVertical_Scale--;	// for log plots, just adjust magnitudes one log unit at a time...
					Vertical_Scale = (long) java.lang.Math.pow(10, logVertical_Scale);
					ChartFloor = -Vertical_Scale / 2;
					Major_Divisions = Vertical_Scale / 5;	// adjust magnitudes of linear scales, so data is framed as desired in linear axes too
				}
				else if ((EIS_plotType == LINEAR_PLOT) && (Method == STATIC_LOGGING)) { // "LINEAR_PLOT" button used to plot voltage / potential in STATIC_LOGGING
					if (potentialScale > 0.1) {
						potentialScale -= 0.1;    // only let user go up to 200 C...
						potentialDivisions = potentialScale / 4.0;
					}
				}
				else if ((EIS_plotType == NYQUIST_PLOT) && (Method == STATIC_LOGGING)) {	// "NYQUIST_PLOT" Button used to plot temperature
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
		if (Method == ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) {	// EIS impedance always positive (start vertical scale at 0)
			switch(EIS_plotType) {
				case LOG_PLOT:
					mChArea.getDefaultYAxis().getScale().setRange(0, logVertical_Scale);
					break;
				case LINEAR_PLOT:
					mChArea.getDefaultYAxis().getScale().setRange(0, Vertical_Scale);
					mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
					break;
				case NYQUIST_PLOT:
					mChArea.getDefaultYAxis().getScale().setRange(0, Vertical_Scale);
					mChArea.getDefaultXAxis().getScale().setRange(0, Vertical_Scale);	// make nyquist plot square- only first quadrant, same axes scales
					mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
					mChArea.getDefaultXAxis().getScale().setInterval(Major_Divisions);
					break;
				default:
					break;
			}
		}
		else if (Method == STATIC_LOGGING) {
			switch(EIS_plotType) {
				case LOG_PLOT:	// "LOG_PLOT" for STATIC_LOGGING is just linear plot of current...
					mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);
					mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
					break;
				case LINEAR_PLOT:	// "LINEAR_PLOT" for STATIC_LOGGING is potential plot vs time
					mChArea.getDefaultYAxis().getScale().setRange(potentialFloor, potentialScale);
					mChArea.getDefaultYAxis().getScale().setInterval(potentialDivisions);
					break;
				case NYQUIST_PLOT: // "NYQUIST_PLOT" for STATIC_LOGGING is temperature plot vs. time
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

	private String padCalendarItems(int number) {
    	String value = number + "";
    	if (number < 10) value = "0" + number;
    	return value;
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
			case(R.id.z_magnitude):// this is the impedance button for EIS
				if (phaseDisplay) {
					update = true;	// if not already on impedance setting, need to update chart
					phaseDisplay = false;
				}
				break;
			case(R.id.phi):	// phase select button for EIS
				if (!phaseDisplay) {
					update = true;
					phaseDisplay = true;
				}
				break;
			case(R.id.logplot):
				if (EIS_plotType != LOG_PLOT) {
					update = true;
					EIS_plotType = LOG_PLOT;
				}
				break;
			case(R.id.linearplot):
				if (EIS_plotType != LINEAR_PLOT) {
					update = true;
					EIS_plotType = LINEAR_PLOT;
				}
				break;
			case(R.id.nyquist):
				if (EIS_plotType != NYQUIST_PLOT) {
					update = true;
					EIS_plotType = NYQUIST_PLOT;
				}
				break;
			default:
				break;
		}
		if (update) {
			if ((Method == ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) || (Method == STATIC_LOGGING)) UpdateChart();
		}
	}

	/*
	Label Axes of EIS analysis appropriately (based on selection of data being plotted, logarithmic transform, and/or "nyquist" plot
	 */
	private void EIS_axisLabels() {
		switch (EIS_plotType) {
			case LOG_PLOT:
				if (Method == STATIC_LOGGING) {	// for static logging method, "LOG_PLOT" button used to plot current vs. time
					mChArea.getDefaultYAxis().getScale().setRange(ChartFloor, ChartFloor + Vertical_Scale);	// keep phase scale fixed...
					mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
					mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.current));
				}
				else if (Method == ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) {
					double startLogf = -1.5;//java.lang.Math.round(java.lang.Math.log10(StartFrequency) - 0.5);	// low range of x scale truncates to nearest integer value of log
					double endLogf = java.lang.Math.round(java.lang.Math.log10(EndFrequency) + 0.5); // high end of range rounds up to nearest integer to log value
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
					findViewById(R.id.chart_data_select).setVisibility(View.VISIBLE);
					findViewById(R.id.data_select_group).setVisibility(View.VISIBLE);
				}
				break;
			case LINEAR_PLOT:
				if (Method == STATIC_LOGGING) {	// for static logging method, "LINEAR_PLOT" button used to plot potential vs. time
					mChArea.getDefaultYAxis().getScale().setRange(potentialFloor, potentialScale);	// keep phase scale fixed...
					mChArea.getDefaultYAxis().getScale().setInterval(potentialDivisions);
					mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.potential));
				}
				else if (Method == ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) {
					double EISXInterval = 1000;
					mChArea.getDefaultXAxis().getScale().setRange(0, EndFrequency);
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
					findViewById(R.id.chart_data_select).setVisibility(View.VISIBLE);
					findViewById(R.id.data_select_group).setVisibility(View.VISIBLE);
				}
				break;
			case NYQUIST_PLOT:
				if (Method == STATIC_LOGGING) {	// for static logging method, "LINEAR_PLOT" button used to plot potential vs. time
					mChArea.getDefaultYAxis().getScale().setRange(temperatureFloor, temperatureScale);	// keep phase scale fixed...
					mChArea.getDefaultYAxis().getScale().setInterval(temperatureDivisions);
					mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.temperature));
				}
				else if (Method == ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY) {
					mChArea.getDefaultXAxis().setTitle(getResources().getString(R.string.zreal));
					mChArea.getDefaultYAxis().setTitle(getResources().getString(R.string.zimag));
					mChArea.getDefaultYAxis().getScale().setRange(0, Vertical_Scale);	// link horizontal and vertical scales...
					mChArea.getDefaultYAxis().getScale().setInterval(Major_Divisions);
					mChArea.getDefaultXAxis().getScale().setRange(0, Vertical_Scale);
					mChArea.getDefaultXAxis().getScale().setInterval(Major_Divisions);

					// also if nyquist plot is chosen, need to remove radio buttons for Bode plot options / impedance and phase vs. f
					findViewById(R.id.chart_data_select).setVisibility(View.GONE);
					findViewById(R.id.data_select_group).setVisibility(View.GONE);
				}
				break;
		}
	}

	/*
	EIS_chartDataSort- called when new EIS data arrives- decides which series to update with which data (so entire chart
	doesn't need to be rebuilt every time EIS data arrives
	 */
	private void EIS_chartDataSort(double f, double impedanceMagnitude, double phase) {
		switch (EIS_plotType) {
			case LOG_PLOT:
				if (phaseDisplay)
					series[1].getPoints().addXY(java.lang.Math.log10(f), phase);
				else
					series[0].getPoints().addXY(java.lang.Math.log10(f), java.lang.Math.log10(impedanceMagnitude));
				break;
			case LINEAR_PLOT:
				if (phaseDisplay)
					series[1].getPoints().addXY(f, phase);
				else
					series[0].getPoints().addXY(f, impedanceMagnitude);
				break;
			case NYQUIST_PLOT:
				double realZ = impedanceMagnitude * java.lang.Math.cos(phase * java.lang.Math.PI / 180.0);
				double imagZ = -impedanceMagnitude * java.lang.Math.sin(phase * java.lang.Math.PI / 180.0);
				series[0].getPoints().addXY(realZ, imagZ);
				break;
		}
	}

	/*
	EIS_chartDataSort- called when new EIS data arrives- decides which series to update with which data (so entire chart
	doesn't need to be rebuilt every time EIS data arrives
	 */
	private void staticLoggingDataSort() {
		switch (EIS_plotType) {
			case LOG_PLOT:	// for STATIC_LOGGING, LOG_PLOT is setting to plot current vs. time
				for (int j = 0; j < Time.size(); j++) {
					double time = safeParseDouble(Time.get(j)) / 60.0;	// time in minutes
					double current = safeParseDouble(Current.get(j));
					series[0].getPoints().addXY(time, current);
				}
				break;
			case LINEAR_PLOT: // for STATIC_LOGGING, LINEAR_PLOT is setting to plot potential vs. time
				for (int j = 0; j < Time.size(); j++) {
					double time = safeParseDouble(Time.get(j)) / 60.0;
					double eVolts = safeParseDouble(Potential.get(j));
					series[0].getPoints().addXY(time, eVolts);
				}
				break;
			case NYQUIST_PLOT: // for STATIC_LOGGING, NYQUIST_PLOT is setting to plot temperatures vs. time
				for (int j = 0; j < Time.size(); j++) {
					double time = Integer.parseInt(Time.get(j)) / 60.0;
					if (BoardT.get(j) != null) {
						double boardTemp = safeParseDouble(BoardT.get(j));
						series[0].getPoints().addXY(time, boardTemp);
						if (!TCTemp.get(j).equals(ABEStatActivity.NO_THERMOCOUPLE_STRING)) {
							double tcTemperature = safeParseDouble(TCTemp.get(j));
							series[1].getPoints().addXY(time, tcTemperature);
						}
					}
				}
				break;
			default:
				break;
		}
	}


	/*
	Try to parse a "double" string; return 1.0 if not a number
	 */
	public static double safeParseDouble(String numberString) {
		try {
			return Double.parseDouble(numberString);
		}
		catch (NumberFormatException WTH) {
			return 1.0;
		}
	}

	/*
	 *
	 */
	private void UpdateChart() {
		series[0].getPoints().clear();
		series[1].getPoints().clear();
		switch(Method) {
			case CYCLIC_VOLTAMMETRY:
				for (int j = 0; j < Potential.size(); j++) {
					double volt = safeParseDouble(Potential.get(j));    // convert to minutes before charting
					double currier = safeParseDouble(Current.get(j));
					series[0].getPoints().addXY(volt, currier);
				}
				break;
			case DIFFERENTIAL_PULSE_VOLTAMMETRY:
				for (int j = 0; j < Potential.size(); j++) {
					double volt = safeParseDouble(Potential.get(j));    // convert to minutes before charting
					double currier = safeParseDouble(Current.get(j));
					series[0].getPoints().addXY(volt, currier);
				}
				break;
			case ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY:
				EIS_axisLabels();
				for (int j = 0; j < Frequency.size(); j++) {
					double freq = safeParseDouble(Frequency.get(j));
					double impMagnitude = safeParseDouble(Z.get(j));
					double phaseAngle = safeParseDouble(Phi.get(j));
					EIS_chartDataSort(freq, impMagnitude, phaseAngle);
				}
				break;
			case STATIC_LOGGING:
				EIS_axisLabels();	// function also handles changes in axis labels for STATIC_LOGGING method...
				staticLoggingDataSort();
				break;
			default:
				break;
		}
	}

    private void appendChartData() {
		int index = Time.size() - 1;	// find index of most recent data in ArrayLists...
		if (ScanNumber.equals(SettingsDbAdapter.INFINITE) && (staticLoggingIndex > (60 * scansRemaining))) {	// for infinite scans, update x range if overrun default value...
			scansRemaining *= 2;
			double xTicks = scansRemaining / 4.0;
			mChArea.getDefaultXAxis().getScale().setRange(0, scansRemaining);
			mChArea.getDefaultXAxis().getScale().setInterval(xTicks);
		}
		if (index >= 0) {
			double time = safeParseDouble(Time.get(index)) / 60.0;
			switch (EIS_plotType) {
				case LOG_PLOT:	// for STATIC_LOGGING, LOG_PLOT is setting to plot current vs. time
					if (Current.size() == Time.size()) {
						double current = safeParseDouble(Current.get(index));
						series[0].getPoints().addXY(time, current);
					}
					break;
				case LINEAR_PLOT: // for STATIC_LOGGING, LINEAR_PLOT is setting to plot potential vs. time
					if (Potential.size() == Time.size()) {
						double eVolts = safeParseDouble(Potential.get(index));
						series[0].getPoints().addXY(time, eVolts);
					}
					break;
				case NYQUIST_PLOT: // for STATIC_LOGGING, NYQUIST_PLOT is setting to plot temperatures vs. time
					if (BoardT.size() >= Time.size()) {
						double tcTemp = safeParseDouble(TCTemp.get(index));
						double boardTemp = safeParseDouble(BoardT.get(index));
						series[0].getPoints().addXY(time, boardTemp);
						series[1].getPoints().addXY(time, tcTemp);
					}
					break;
				default:
					break;
			}
		}
	}

	/**
     * Sends a message.
     * @param message  A string of text to send.
     */
    public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (ABEStatActivity.mBTService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            ABEStatActivity.mBTService.write(send);
        }
    }

    private void newCycle() {
		sendMessage("c");
	}

	private void startLogging() {
    	dataRequest = new solicitStaticLoggingData();
    	loggingTimer.schedule(dataRequest, 0, 1000);
	}

	class solicitStaticLoggingData extends TimerTask {
		public void run() {
			if (ScanNumber.equals(SettingsDbAdapter.INFINITE)) sendMessage("v");
			else if (staticLoggingIndex <= scanDuration) {
				sendMessage("v");	// request new data for logging...
			}
			else {
				loggingTimer.cancel();	// once time elapsed on clock stop asking for more data
				scansRemaining = 0;	// this is flag that handler can show analysis is complete on the interface (can't change interface on different thread)
				rtHandler.obtainMessage(CYCLE_COMPLETE).sendToTarget();	// ask handler to show completed analysis on UI
			}
		}
	}

	private void analysisCompleteMessage() {
		String statusString = getResources().getString(R.string.analysis_complete);
		String statusStringMsg = String.format(statusString, Device);
		mTitle.setText(statusStringMsg);
	}

    // The Handler that updates Chart at the conclusion of the getData function in thread "BusyGettingData"
    private final Handler rtHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	int cindex;
        	int tindex;
        	//if(D) Log.e(TAG, "Enterred real time Handler");
			NumberFormat localeSpecificNF = NumberFormat.getInstance(Locale.getDefault());
			switch (msg.what) {
				case ABEStatActivity.MESSAGE_STATE_CHANGE:
					if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
					ABEStatActivity.BT_State = msg.arg1;
					switch (ABEStatActivity.BT_State) {
						case BluetoothService.STATE_CONNECTED:
							//Request_Firmware_Name();
							//mTitle.setText(R.string.title_connected_to);
							//mTitle.append(mConnectedDeviceName);
							//enableCommButtons();	// make sure communication buttons are enabled (in case buttons were disabled while disconnecting bluetooth)
							break;
						case BluetoothService.STATE_CONNECTING:
							//mTitle.setText(R.string.title_connecting);
							break;
						case BluetoothService.STATE_NONE:
							mTitle.setText(R.string.title_not_connected);
							ABEStatActivity.Firmware_Version = ABEStatActivity.NO_DEVICE;
							break;
					}
					break;
				case ABEStatActivity.BATT_READ:
					Battery = msg.arg1;
					String BattReportFormat = getResources().getString(R.string.battery_report);
					String BattReportMsg = String.format(BattReportFormat, Battery);
					//mBattery.setText("Battery: " + Battery + "%");
					mBattery.setText(BattReportMsg + "%");
					break;
				case ABEStatActivity.TEMPERATURE_READ:
					double Temps[] = (double[]) msg.obj;
					boardTemperature = Temps[0];
					double tcE = Temps[1];
					tcTemperature = ABEStatActivity.tcValue(boardTemperature, tcE);
					if (Method == STATIC_LOGGING) {
						BoardT.add("" + boardTemperature);
						double tcT = 0.0;
						try {
							tcT = localeSpecificNF.parse(tcTemperature).doubleValue();
						}
						catch(java.text.ParseException p) {

						}
						TCTemp.add("" + tcT);
						appendChartData();
					}
					Post_Temperature();
					break;
				case ABEStatActivity.SUBROUTINE_EXIT:
					if(D) Log.e(TAG, "ABE-Stat has indicated exit of latest sub-routine...");
					break;
				case CV_DATA:
					Bundle dBundle = (Bundle) msg.obj;
					String pot = dBundle.getString(POTENTIAL_KEY);
					String cur = dBundle.getString(CURRENT_KEY);
					Potential.add(pot);
					Current.add(cur);
					String CVReportFormat = getResources().getString(R.string.cv_report);
					double volt = safeParseDouble(pot);
					double current = safeParseDouble(cur);
					localeSpecificNF.setMaximumFractionDigits(7);
					String localV = localeSpecificNF.format(volt);
					localeSpecificNF.setMaximumFractionDigits(3);
					String localI = localeSpecificNF.format(current);
					String CVReportMsg = String.format(CVReportFormat, localV, localI);
					mReturnedData.setText(CVReportMsg);
					if (Method == STATIC_LOGGING) {
						Time.add("" + staticLoggingIndex);
						staticLoggingIndex++;
						appendChartData();
					}
					else series[0].getPoints().addXY(volt, current);
					break;
				case EIS_DATA:
					Bundle eisBundle = (Bundle) msg.obj;
					String freq = eisBundle.getString(FREQUENCY_KEY);
					String zmag = eisBundle.getString(IMPEDANCE_MAGNITUDE_KEY);
					String zphase = eisBundle.getString(IMPEDANCE_PHASE_KEY);
					Double fr = safeParseDouble(freq);
					Double im = safeParseDouble(zmag); // first just assign data into ArrayLists, and update textviews with data...
					Double ph = safeParseDouble(zphase);
					if (fr < 1.0) localeSpecificNF.setMaximumFractionDigits(2);
					else if (fr < 100.0) localeSpecificNF.setMaximumFractionDigits(1);
					else localeSpecificNF.setMaximumFractionDigits(0);
					String localF = localeSpecificNF.format(fr);
					localeSpecificNF.setMaximumFractionDigits(1);
					String localZ = localeSpecificNF.format(im);
					localeSpecificNF.setMaximumFractionDigits(2);
					String localPH = localeSpecificNF.format(ph);
					String EISReportFormat = getResources().getString(R.string.eis_report);
					String EISReportMsg = String.format(EISReportFormat, localF, localZ, localPH);
					mReturnedData.setText(EISReportMsg);
					Frequency.add(freq);
					Z.add(zmag);
					Phi.add(zphase);
					EIS_chartDataSort(fr, im, ph);
					break;
				case CYCLE_COMPLETE:
					if (Method == CYCLIC_VOLTAMMETRY || Method == DIFFERENTIAL_PULSE_VOLTAMMETRY){
						if (ScanNumber.equals(SettingsDbAdapter.INFINITE)) {
							newCycle();//sendMessage("c");	// send instruction to perform another cycle
						}
						else if (scansRemaining > 0) {
							scansRemaining--;
							newCycle();//sendMessage("c"); // send command for next scan, and decrement number of scans remaining
						}
						//scansRemaining--;
					}
					if ((scansRemaining == 0) || (Method == ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY)){
						//sendMessage(EXIT_STRING);// exit analysis routine- now we're finished...
						analysisCompleteMessage();
					}
					break;
				case EQ_TIME_LEFT:
					int timeLeft = msg.arg1;
					if (D) Log.e(TAG, "equilibration time remaining..." + timeLeft);
					String statusString = getResources().getString(R.string.equilibration_time_remaining);
					String statusStringMsg = String.format(statusString, Device, timeLeft);
					mTitle.setText(statusStringMsg);
					if (timeLeft == 0) {
						String analyzingFormat = getResources().getString(R.string.analyzing);
						String analyzingString = String.format(analyzingFormat, Device);
						mTitle.setText(analyzingString);
						if (Method == STATIC_LOGGING) {
							if (D) Log.e(TAG, "about to start static logging");
							staticLoggingIndex = 0;
							startLogging();
						}
					}
					break;
				default:
					break;
			}
        }
    };

    @Override
    public void onBackPressed()
    {
    	ExitDialog();//showDialog(QUIT_DIALOG); // show the quit dialog
    	return; // if user quits, dialog call will handle closing threads; otherwise return
    }

	@Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth services, and waypoint timer, notifications, etc...
		release_wakelock();
    }

	private static void start_wakelock() {
		if (wl == null) {
	        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "Real-Time Activity");
		}
		wl.acquire();
	}

	public static void release_wakelock() {
		if (wl != null) {
        	while (wl.isHeld()) wl.release();	// make sure to release any wakelocks on power
        	wl = null;
        }
	}

	/*
     * New version of data saving activity (starting v. 3.0.10)
     * */
    private void SaveData() {
    	if(D) Log.e(TAG, "Opening SaveData() function...");
    	File dir = AttachActivity.dir;//new File(Environment.getExternalStorageDirectory().getPath()+"/Smart-DART/");
    	dir.mkdirs();
//    	File file = new File(Environment.getExternalStorageDirectory(), FileName);

    	String DataText = "";

    	byte[] ByteArray;

    	// (try) to open file with FileName and append data to it
    	try {
    		File file = new File(dir, FileName);
    		if(D) Log.e(TAG, "Creating file: " + FileName);
    		FileOutputStream fos = new FileOutputStream(file);

//    		FileWriter writer = new FileWriter(FileName);

    		// in first lines of file, make sure commas in users method summary are changed to semicolon, since file uses comma delimitated data (otherwise information will not parse correctly)
    		switch (Method) {
				case CYCLIC_VOLTAMMETRY:
					DataText = SettingsDbAdapter.CYCLIC_VOLTAMMETRY + "\n";
					break;
				case ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY :
					DataText = SettingsDbAdapter.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY + "\n";
					break;
				case ANODIC_STRIPPING_VOLTAMMETRY:
					DataText = SettingsDbAdapter.ANODIC_STRIPPING_VOLTAMMETRY + "\n";
					break;
				case DIFFERENTIAL_PULSE_VOLTAMMETRY:
					DataText = SettingsDbAdapter.DIFFERENTIAL_PULSE_VOLTAMMETRY + "\n";
					break;
				case STATIC_LOGGING:
					DataText = SettingsDbAdapter.STATIC_LOGGING + "\n";
				default:
					break;
			}
			DataText += ((String) "Device: " + Device + "\nMethod: " + Title.replace(',', ';') + "\nAnalysis started: " + DateString + "\nFirmware Version: " + Firmware + "\n\n");

			DataText += mProcessParameters.getText().toString().replace(',', ';') + "\n\n";
			DataText += mTemperature.getText().toString().replace(',', '.') + "\n\n";
    		ByteArray = DataText.getBytes();
    		fos.write(ByteArray);

			switch (Method) {
				case CYCLIC_VOLTAMMETRY:
					DataText = "E (V),I (nA)\n";
					ByteArray = DataText.getBytes();
					fos.write(ByteArray);
					for (int j = 0; j < Potential.size(); j++) {
						DataText = Potential.get(j) + ',' + Current.get(j) + '\n';
						ByteArray = DataText.getBytes();
						fos.write(ByteArray);
					}
					DataText = "\n\n";
					ByteArray = DataText.getBytes();
					fos.write(ByteArray);
					break;
				case ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY :
					DataText = "f (Hz),Z (\u03A9),\u03C6 (\u00B0)\n";
					ByteArray = DataText.getBytes();
					fos.write(ByteArray);
					for (int j = 0; j < Frequency.size(); j++) {
						DataText = Frequency.get(j) + ',' + Z.get(j) + ',' + Phi.get(j) + '\n';
						ByteArray = DataText.getBytes();
						fos.write(ByteArray);
					}
					DataText = "\n\n";
					ByteArray = DataText.getBytes();
					fos.write(ByteArray);
					break;
				case ANODIC_STRIPPING_VOLTAMMETRY:
					break;
				case DIFFERENTIAL_PULSE_VOLTAMMETRY:
					DataText = "E (V),I (nA)\n";
					ByteArray = DataText.getBytes();
					fos.write(ByteArray);
					for (int j = 0; j < Potential.size(); j++) {
						DataText = Potential.get(j) + ',' + Current.get(j) + '\n';
						ByteArray = DataText.getBytes();
						fos.write(ByteArray);
					}
					DataText = "\n\n";
					ByteArray = DataText.getBytes();
					fos.write(ByteArray);
					break;
				case STATIC_LOGGING:
					DataText = "Time (s),E (V),I (nA),TBoard (\\u00B0C),TtC (\\u00B0C)\n";
					ByteArray = DataText.getBytes();
					fos.write(ByteArray);
					for (int j = 0; j < Time.size(); j++) {
						DataText = Time.get(j) + ',' + Potential.get(j) + ',' + Current.get(j);
						if (BoardT.get(j) != null) DataText += ',' + BoardT.get(j) + ',' + TCTemp.get(j);
						DataText += "\n";
						ByteArray = DataText.getBytes();
						fos.write(ByteArray);
					}
					DataText = "\n\n";
					ByteArray = DataText.getBytes();
					fos.write(ByteArray);
					break;
				default:
					break;
			}

    		fos.flush();
    		fos.close();
    		if(D) Log.e(TAG, "Closing file: " + FileName);
    	}
    	catch (FileNotFoundException e) {
    		File_Saved = false;
    		    // handle exception
    	}
    	catch (IOException e) {
    		File_Saved = false;
    		    // handle exception
    	}
    }

    private void Check_Saving_Options() {
    	String Storage_State = Environment.getExternalStorageState();
		if ((Save_Data_Check.isChecked()) && (!File_Saved)) {	// if requested (and file not already saved), try to save data...
			if (Environment.MEDIA_MOUNTED.equals(Storage_State)) {
				File_Saved = true;
				SaveData();
			}
		}
    }

    private void ExitRealTime() {
    	sendMessage(EXIT_STRING);	// send character instruction to exit real time routine on BioRanger (go to stand-by mode)- send it at least 16 times in case device is hung up in data display routine
    	Check_Saving_Options();
		// make sure this message is not displayed if user has not checked "Save Data" box
		Log.e(TAG, "removed callbacks to RFU Sentinel");
		release_wakelock();
		Log.e(TAG, "Cancel countdown timer");
		loggingTimer.cancel();
		Intent quitIntent = new Intent();
		quitIntent.putExtra(FILE_SAVED, File_Saved);
		quitIntent.putExtra(SAVE_FILE_CHECK, Save_Data_Check.isChecked());	// if "Save File" is not checked, don't toast external storage not available
		//quitIntent.putExtra(THREAD_INTERRUPTED, Thread_Interrupted);
		Log.e(TAG, "initiated quit intent, and finish loading intent data");
		setResult(Activity.RESULT_OK, quitIntent);
		finish();
    }
    
    private void ExitDialog() {
    	new AlertDialog.Builder(this)
		.setTitle(R.string.quit_query)
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				ExitRealTime();
			}
		})
		.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		})
		.create()
		.show();
    }
    
}
