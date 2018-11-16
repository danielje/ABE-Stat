package com.diagenetix.abestat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * This is the basic starting Activity for operating ABEStat,
 * from which activities, methods, and classes are called to manage
 * device connections (DeviceListActivity/ BluetoothService), 
 * settings for analysis (SettingsEdit/ SettingsDbAdapter), communicate
 * (BluetoothService) and give instructions to device (e.g., preheat,
 * take single reading, or implement real-time ABEStat analysis).
 */
public class ABEStatActivity extends Activity {
	// for Debugging...
	private static final String TAG = "ABEStat";
	private static final boolean D = true;
	
    // Message types sent from the BluetoothService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_DEVICE_NAME = 2;
	public static final int MESSAGE_TOAST = 3;
	public static final int NO_STORAGE_READ = 4;
	public static final int SAT_CODE_READ = 5;
	public static final int TEMPERATURE_READ = 7;
	public static final int SUBROUTINE_EXIT = 11;
	public static final int NO_BLUETOOTH = 12;
	public static final int BLUETOOTH_NOT_ON = 13;
	public static final int BATT_READ = 16;
	public static final int FIRMWARE_VERSION_READ = 17;
	public static final int BOARD_TEMPERATURE_READ = 18;
	public static final int ENGAGED = 19;

	public static final int CELL_CURRENT_READ = 20;
	public static final int OPEN_CIRCUIT_VOLTAGE_READ = 21;
	public static final int RESISTANCE_READ = 22;
	public static final int CAPACITANCE_READ = 23;

	// Configuration and Measurement Settings
	public static final int RESISTANCE = 1;
	public static final int CAPACITANCE = 2;
	public static final int OPEN_CIRCUIT_VOLTAGE = 3;
	public static final int BIASED_CURRENT = 4;
	public static final int THREE_ELECTRODE = 5;
	public static final int TWO_ELECTRODE = 6;

	/*
	polynomial conversion coefficients for converting between temperature (in C) and thermoelectric potential (in uV)
	Coefficients taken from http://www.omega.com/techref/pdf/z198-201.pdf
	 */
	public static final double[] typeK_TtoECoefficients = {-1.7600413686e1, 3.8921204975e1,
		1.8558770032e-2, -9.9457592874e-5, 3.1840945719e-7, -5.6072844889e-10, 5.6075059059e-13,
		-3.2020720003e-16, 9.7151147152e-20, -1.2104721275e-23};
	public static final double[] typeK_alphaCoefficients = {1.185976e2, -1.183432e-4, -1.269686e2};
	public static final double[] typeK_EtoTCoefficients = {0, 2.508355e-2, 7.860106e-8, -2.503131e-10,
		8.315270e-14, -1.228034e-17, 9.804036e-22, -4.413030e-26, 1.057734e-30, -1.052755e-35};

	// Key names received from the BluetoothService Handler, or used by activity intents
	public static final String DEVICE_NAME = "device_name";
	public static final String DEVICE_ADDRESS = "device_addres";
	public static final String FIRMWARE_VERSION_NAME = "firmware_version";
	public static final String TOAST = "toast";
	public static final String NO_DEVICE = "no_device_connected";
	public static final String BATTERY = "batt";

	public static final String NO_THERMOCOUPLE_STRING = "n/c";
	
	// Intent request codes
	private static final int CONNECT_MENU = 1;
	private static final int ANALYSIS_SETTINGS = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	private static final int SHARE_DATA = 4;		// intent request code to preheat device
	private static final int RTLAMP = 6;	// real time LAMP intent request code

	private static final int PERMISSIONS_REQUEST_CODE = 10;
	
	public static String Firmware_Version = NO_DEVICE;

	// Database values / settings for analysis
	private static String analysisTitle = "Default CV";
	private static String analysisMethod = SettingsEdit.defMethod;
	private static String analysisStartFrequency = SettingsEdit.defStartFrequency;
	private static String analysisEndFrequency = SettingsEdit.defEndFrequency;
	private static String analysisIntervalFrequency = SettingsEdit.defIntervalFrequency;
	private static String analysisSignalAmplitude = SettingsEdit.defSignalAmplitude;
	private static String analysisBiasVoltage = SettingsEdit.defBiasVoltage;
	private static String analysisStartVoltage = SettingsEdit.defStartVoltage;
	private static String analysisEndVoltage = SettingsEdit.defEndVoltage;
	private static String analysisScanRate = SettingsEdit.defScanRate;
	private static String analysisScanNumber = SettingsEdit.defScanNumber;
	private static String analysisStepFrequency = SettingsEdit.defStepFrequency;
	private static String analysisStepE = SettingsEdit.defStepE;
	private static String analysisPulseAmplitude = SettingsEdit.defPulseAmplitude;
	private static String analysisElectrodeConfiguration = SettingsEdit.defElectrodeConfiguration;
	private static String analysisEquilibriumTime = SettingsEdit.defEquilibriumTime;

	// Layout Views
	private static TextView mProcessParameters;
	private static TextView mDataView;
	private static TextView mTitle;
	private static Button mConnectButton;
	private static Button mAnalysisButton;
	private static Button mOptionsButton;
	private static Spinner mMethodList;

	private static SeekBar mBiasVoltageSlider;
	private static EditText mBiasVoltageValue;
	
	// Process parameters for Real-Time LAMP

	private static int electrodeConfiguration;
	private static int measurement;
	private static int biasVoltage;

	private static double boardTemperature = 20.0;
	private static String tcTemperature = NO_THERMOCOUPLE_STRING;

	private static int Battery;
	public static int BT_State;
	private static double Temp;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	public static String mConnectedDeviceAddress = "";

	public static BluetoothAdapter mBluetoothAdapter = null;
	// Member object for SettingsDbAdapter (SQLite database for methods settings)
	private SettingsDbAdapter mDbHelper;
	// Member object for the Bluetooth services
	public static BluetoothService mBTService = null;
	// Flag indicating intention to connect in DeviceListActivity (false when already connected to bluetooth device)

	private static Handler TimeoutHandler = new Handler(); // Handler to periodically request device temperature (when BT connected)

	public static int BTCommLoss = 0;
	public static boolean Kinect = true;

	public static boolean ABEbusy = false;

	public static boolean writingMessage = false;	// don't let multiple methods try to write messages at the same time
		// could lead to data parsing errors
		// set true at start of sendMessage, then set back to false at end of BluetoothService.write

	//private static boolean biasInFocus = false;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);

		int noPermissionCount = 0;	// count up the number app permissions not granted (used to request all remaining permissions at once)

		ArrayList<String> PermissionsList = new ArrayList<String>();
		if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getApplicationContext(), this)) {
			noPermissionCount++;	// increment the count of no permissions
			PermissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}

		String[] PermissionsString = new String[noPermissionCount];
		PermissionsList.toArray(PermissionsString);

		if (noPermissionCount > 0) {
			ActivityCompat.requestPermissions(this, PermissionsString, PERMISSIONS_REQUEST_CODE);
		}

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(D) Log.e(TAG, "+++ BT Adapter result obtained... +++");

        Battery = 100;	// initialize battery to zero (avoid null pointer exception if need to know battery charge)

        // Set up SQLite database managing analysis methods
        mDbHelper = new SettingsDbAdapter(this);
        mDbHelper.open();
        Cursor c = mDbHelper.fetchAllMethods();
        if (c.getCount()==0) {
        	mDbHelper.createMethod(getResources().getString(R.string.default_cv), SettingsDbAdapter.CYCLIC_VOLTAMMETRY, SettingsEdit.defStartFrequency, SettingsEdit.defEndFrequency,
                    SettingsEdit.defIntervalFrequency, SettingsEdit.defSignalAmplitude, SettingsEdit.defBiasVoltage, SettingsEdit.defStartVoltage, SettingsEdit.defEndVoltage,
					SettingsEdit.defScanRate, SettingsEdit.defScanNumber, SettingsEdit.defStepFrequency, SettingsEdit.defStepE, SettingsEdit.defPulseAmplitude,
                    SettingsEdit.defElectrodeConfiguration, SettingsEdit.defEquilibriumTime, "extra", "extra", "extra", "extra", "extra", "extra");
			mDbHelper.createMethod(getResources().getString(R.string.default_dpv), SettingsDbAdapter.DIFFERENTIAL_PULSE_VOLTAMMETRY, SettingsEdit.defStartFrequency, SettingsEdit.defEndFrequency,
					SettingsEdit.defIntervalFrequency, SettingsEdit.defSignalAmplitude, SettingsEdit.defBiasVoltage, SettingsEdit.defStartVoltage, SettingsEdit.defEndVoltage,
					SettingsEdit.defScanRate, SettingsEdit.defScanNumber, SettingsEdit.defStepFrequency, SettingsEdit.defStepE, SettingsEdit.defPulseAmplitude,
					SettingsEdit.defElectrodeConfiguration, SettingsEdit.defEquilibriumTime, "extra", "extra", "extra", "extra", "extra", "extra");
        	mDbHelper.createMethod(getResources().getString(R.string.default_eis), SettingsDbAdapter.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY, SettingsEdit.defStartFrequency, SettingsEdit.defEndFrequency,
                    SettingsEdit.defIntervalFrequency, SettingsEdit.defSignalAmplitude, SettingsEdit.defBiasVoltage, SettingsEdit.defStartVoltage, SettingsEdit.defEndVoltage,
                    SettingsEdit.defScanRate, SettingsEdit.defScanNumber, SettingsEdit.defStepFrequency, SettingsEdit.defStepE, SettingsEdit.defPulseAmplitude,
                    SettingsEdit.defElectrodeConfiguration, SettingsEdit.defEquilibriumTime, "extra", "extra", "extra", "extra", "extra", "extra");
        }

        biasVoltage = 0;
		measurement = OPEN_CIRCUIT_VOLTAGE;
		electrodeConfiguration = THREE_ELECTRODE;

		setupUserInterface();

		Kinect = true;
		hideDeviceControls();
    }

	@Override
	public void onStart() {
		super.onStart();
		if(D) Log.e(TAG, "++ ON START ++");

		//setupUserInterface();
		BT_State = mBTService.getState();
		if(D) Log.e(TAG, "bluetooth state = " + BT_State);
		Real_Time_Activity.release_wakelock();
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if(D) Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
		if(Kinect) {
			hideDeviceControls();
			if (mBTService == null) {
				if(D) Log.e(TAG, "Set Up New Bluetooth Service");
				mBTService = new BluetoothService(this, mHandler);
				if(D) Log.e(TAG, "Bluetooth Service Set up...");
				}
			else {
				// Only if the state is STATE_NONE do we know that we haven't started already...
				if(D) Log.e(TAG, "BluetoothService Running...");
				if (mBTService.getState() == BluetoothService.STATE_NONE) {
					// Start the bluetooth service
					if(D) Log.e(TAG, "Start bluetooth service...");
					//mBTService.start();
					}
			}
		}
		else showDeviceControls();
		//setupUserInterface();
	}

	public static boolean checkPermission(String strPermission,Context _c,Activity _a){
		int result = ContextCompat.checkSelfPermission(_c, strPermission);
		if (result == PackageManager.PERMISSION_GRANTED){
			return true;
		} else {
			return false;
		}
	}

	/*public static void requestPermission(String strPermission,int perCode,Context _c,Activity _a){
		if (ActivityCompat.shouldShowRequestPermissionRationale(_a,strPermission)){
			Toast.makeText(this,"GPS permission allows us to improve sample location accuracy- please set permission.",Toast.LENGTH_LONG).show();
		} else {
			ActivityCompat.requestPermissions(_a,new String[]{strPermission},perCode);
		}
	}*/


	// this function sets up the user interface for managing connections, editing and initiating analytical methods
	private void setupUserInterface() {
		//Log.d(TAG, "setupUserInterface()");

        mProcessParameters = (TextView) findViewById(R.id.process_parameters);
        mDataView = (TextView) findViewById(R.id.return_data);
        //Log.d(TAG, "going to populate parameters textview");
        PopulateProcessParameters();
        //Log.d(TAG, "parameters textview populated...");

		// Customize active buttons depending on connection status (disable I/O buttons until connection complete)
        if (Kinect) {
//        	findViewById(R.id.standardization_button).setVisibility(View.INVISIBLE);
//        	findViewById(R.id.singledata_button).setVisibility(View.INVISIBLE);
        	hideDeviceControls();
        }

        // initialize the connect button to go to the bluetooth connection menu (DeviceListActivity)
        mConnectButton = (Button) findViewById(R.id.connection_manager);
        mConnectButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View w) {
        		// Go to device connection menu when click on connection_manager button
                //Log.e(TAG, "++ clicked connection manager... ++");
                if (mBluetoothAdapter == null) { // if no bluetooth device on android...
                	Message msg = mHandler.obtainMessage(NO_BLUETOOTH);	// send message to handler to send a toast
                	mHandler.sendMessage(msg); // alerting user that external data storage can't be read...
                }
                else if (!mBluetoothAdapter.isEnabled()) { // if bluetooth is not on...
                	Message msg = mHandler.obtainMessage(BLUETOOTH_NOT_ON);
                	mHandler.sendMessage(msg);
                	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else Manage_Connections();
        	}
        });

        // initialize the real_time_analysis button to go to prompt one temperature/ fluorescence reading set from device
        mAnalysisButton = (Button) findViewById(R.id.start_analysis_button);
        mAnalysisButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View z) {
        		// Go to method to initiate real time lamp activity when real time lamp button clicked
                Real_Time_Analysis();
        	}
        });

        mOptionsButton = (Button) findViewById(R.id.options_button);
        mOptionsButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View x) {
                showPopup(x);
        	}
        });

		mBiasVoltageSlider = (SeekBar) findViewById(R.id.bias_slider);
		mBiasVoltageSlider.setMax(3000);	// -1500 mV to 1500 mV in 1 mV increments
		mBiasVoltageSlider.setProgress(1500);	// start at 0V
		mBiasVoltageSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
				int value = progress - 1500;
				mBiasVoltageValue.setText(Integer.toString(value));
			}
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}
			public void onStopTrackingTouch(SeekBar seekBar) {
				int value = mBiasVoltageSlider.getProgress() - 1500;
				checkBias(value);
			}
		});

		mBiasVoltageValue = (EditText) findViewById(R.id.bias_value);
		mBiasVoltageValue.setText(Integer.toString(biasVoltage));
		//mBiasVoltageValue.setFocusableInTouchMode(false);

		mBiasVoltageValue.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				mBiasVoltageValue.requestFocus();
				if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key
					int value = SettingsEdit.localeSpecificParsedIntString(mBiasVoltageValue.getText().toString());
					checkBias(value);
					mBiasVoltageValue.clearFocus();
					//biasInFocus = false;
					return true; // indicate that we handled event, won't propagate it
				}
				return false; // when we don't handle other keys, propagate event further
			}
		});

		mBiasVoltageValue.clearFocus();

        // setup spinner to allow selection of analysis method
        setupMethodsSpinner();

		// Initialize the BluetoothService to perform bluetooth connections
		mBTService = new BluetoothService(this, mHandler);
	}


    /*
     * Code for accessing controls from the options menu (save space on user interface
     * and don't let user accidentally stop/ start logging data for example)
     */
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
		        case R.id.data_sharing:
		        	String Storage_State = Environment.getExternalStorageState();
		        	File dir = AttachActivity.dir;
		        	//if(dir.exists() && dir.isDirectory()) {
	                if ((Environment.MEDIA_MOUNTED.equals(Storage_State) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(Storage_State)) && dir.exists() && dir.isDirectory()) // make sure directory is available
	                	Share_Data();
	                else {
	                	Message msg = mHandler.obtainMessage(NO_STORAGE_READ);	// send message to handler to send a toast
	            		mHandler.sendMessage(msg); // alerting user that external data storage can't be read...
	                }
		            return true;
		        case R.id.methods_edit:
		        	Methods_Edit();
		        	return true;
		        case R.id.quit_button3:
		        	EndActivity();
		        	return true;
		        }
		        return false;
			}
		});
        popup.inflate(R.menu.option_menu);
        Menu pmenu = popup.getMenu();
        popup.show();
    }


	private void checkBias(int bias) {
		if (bias > 1500) bias = 1500;
		else if (bias < -1500) bias = -1500;
		mBiasVoltageSlider.setProgress(bias + 1500);
		NumberFormat nFormat = NumberFormat.getInstance(Locale.getDefault());
		mBiasVoltageValue.setText(nFormat.format(bias));
		biasVoltage = bias;
		//sendMessage("v" + mVtoVString(biasVoltage) + '\t');	only send this as a preface to biased current measurement
	}

	private String mVtoVString (int mVValue) {	// return a formatted string, converting integer mV value to V
		NumberFormat nf = NumberFormat.getInstance(); // get instance
		nf.setMaximumFractionDigits(3); // set decimal places
		return nf.format(mVValue / 1000.0);
	}

    public static void hideSoftKeyboard (Activity activity, View view)
	{
		InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
	}

	private void Share_Data() {
		TimeoutHandler.removeCallbacks(Timeout);	// stop asking for temperature
		File App_Directory = AttachActivity.dir;
		File[] Available_Files = App_Directory.listFiles();
		if (Available_Files != null) {	// check to make sure that there are files available before launching file share activity
			Intent sharedataIntent = new Intent(this, AttachActivity.class);
			startActivityForResult(sharedataIntent, SHARE_DATA);
		}
	}

	/*
	 * send simple code when any radio button is clicked
	 */
	public void onRadioButtonClicked(View v) {
		if(D) Log.e(TAG, "Radio Button Selected");
		showDeviceControls();	// this function already handles what to do with any combination of button presses
		/*RadioButton rb = (RadioButton) v;
		switch(rb.getId()) {
			case(R.id.resistance):
				measurement = RESISTANCE;
				hideBiasVoltage();
				showElectrodeConfiguration();
				sendMessage("r");
				break;
			case(R.id.capacitance):
				measurement = CAPACITANCE;
				hideBiasVoltage();
				showElectrodeConfiguration();
				sendMessage("z");
				break;
			case(R.id.biased_current):
				measurement = BIASED_CURRENT;
				showBiasVoltage();
				showElectrodeConfiguration();
				sendMessage("v" + mVtoVString(biasVoltage) + "\ti");	// set bias voltage, and instruction to read cell current
				break;
			case(R.id.open_circuit):
				sendMessage("f0");	// make sure switches are disconnected to get open circuit reading...
				measurement = OPEN_CIRCUIT_VOLTAGE;
				hideBiasVoltage();
				hideElectrodeConfiguration();
				sendMessage("o");
				break;
			case(R.id.two_electrode):
				electrodeConfiguration = TWO_ELECTRODE;
				sendMessage("f2");
				break;
			case(R.id.three_electrode):
				electrodeConfiguration = THREE_ELECTRODE;
				sendMessage("f3");
				break;
			default:
				break;
		}*/
	}

	private void hideBiasVoltage() {
		findViewById(R.id.bias_slider).setVisibility(View.INVISIBLE);
		findViewById(R.id.bias_voltage).setVisibility(View.INVISIBLE);
		findViewById(R.id.bias_value).setVisibility(View.INVISIBLE);
	}

	private void showBiasVoltage() {
		findViewById(R.id.bias_slider).setVisibility(View.VISIBLE);
		findViewById(R.id.bias_voltage).setVisibility(View.VISIBLE);
		findViewById(R.id.bias_value).setVisibility(View.VISIBLE);
		mBiasVoltageSlider.clearFocus();
	}

	private void hideElectrodeConfiguration() {
		findViewById(R.id.e_config).setVisibility(View.INVISIBLE);
		findViewById(R.id.e_config_buttons).setVisibility(View.INVISIBLE);
	}

	private void showElectrodeConfiguration() {
		findViewById(R.id.e_config).setVisibility(View.VISIBLE);
		findViewById(R.id.e_config_buttons).setVisibility(View.VISIBLE);
	}

	private void hideDeviceControls() {
		findViewById(R.id.start_analysis_button).setVisibility(View.INVISIBLE);
		findViewById(R.id.measurement_settings).setVisibility(View.INVISIBLE);
	}

	private void showDeviceControls() {
		findViewById(R.id.start_analysis_button).setVisibility(View.VISIBLE);
		findViewById(R.id.measurement_settings).setVisibility(View.VISIBLE);

		// First check the electrode configuration and make sure controller sets correctly...
		RadioButton radB;
		if (measurement != OPEN_CIRCUIT_VOLTAGE) {
			radB = (RadioButton) findViewById(R.id.three_electrode);
			if (radB.isChecked()) {	// if not set to open circuit voltage check electrode config
				electrodeConfiguration = THREE_ELECTRODE;
				sendMessage("f3");
				Log.e(TAG, "requesting 3 electrode configuration");
			}
			radB = (RadioButton) findViewById(R.id.two_electrode);
			if (radB.isChecked()) {	// // if not set to open circuit voltage check electrode config
				electrodeConfiguration = TWO_ELECTRODE;
				sendMessage("f2");
				Log.e(TAG, "requesting 2 electrode configuration");
			}
		}
		else {
			sendMessage("f0");	// otherwise we're in open circuit voltage mode- make sure we keep open circuit configuration
			Log.e(TAG, "requesting open circuit configuration");
		}

		// then check which measurement- set it for recurring requests and make first data request...
		radB = (RadioButton) findViewById(R.id.resistance);
		if (radB.isChecked()) {
			measurement = RESISTANCE;
			hideBiasVoltage();
			showElectrodeConfiguration();
			if (!ABEbusy) sendMessage("r");
		}
		radB = (RadioButton) findViewById(R.id.capacitance);
		if (radB.isChecked()) {
			measurement = CAPACITANCE;
			hideBiasVoltage();
			showElectrodeConfiguration();
			if (!ABEbusy) sendMessage("z");
		}
		radB = (RadioButton) findViewById(R.id.biased_current);
		if (radB.isChecked()) {
			measurement = BIASED_CURRENT;
			if (!ABEbusy) sendMessage("v" + mVtoVString(biasVoltage) + '\t');
			showBiasVoltage();
			showElectrodeConfiguration();
			if (!ABEbusy) sendMessage("i");
		}
		radB = (RadioButton) findViewById(R.id.open_circuit);
		if (radB.isChecked()) {
			measurement = OPEN_CIRCUIT_VOLTAGE;
			hideBiasVoltage();
			hideElectrodeConfiguration();
			if (!ABEbusy) sendMessage("o");	// make sure data request is preceded by command to float the reference contact...
		}
	}

	/*
	 * Function to set up the spinner to allow user to select from among defined methods in database
	 */
	private void setupMethodsSpinner() {
        mMethodList = (Spinner) findViewById(R.id.method_spinner);

		Cursor c = mDbHelper.fetchAllMethods();
		//startManagingCursor(c);

        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{SettingsDbAdapter.KEY_TITLE};

        if(D) Log.e(TAG, "about to set up list of methods...");

        // and an array of the fields we want to bind those fields to (in this case, make *GODDAMNED*
        //   sure that you use android.R.id.text1 for the text view when using android.R.layout.simple_spinner_dropdown_item
        //   in the setDropDownViewResource
        int[] to = new int[]{android.R.id.text1};

        int position = 0;

        c.moveToFirst();
        while (c.isAfterLast() == false) {
            if (analysisTitle.equals(c.getString(c.getColumnIndexOrThrow(SettingsDbAdapter.KEY_TITLE))))
            	position = c.getPosition();
            c.moveToNext();
        }

        Log.e(TAG, "Position: " + position);

        //c.moveToPosition(position);

        // Set up cursor adapter to populate member fields from database (methods title) as a spinner/ drop down menu
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item, c, from, to, 0);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Assign cursor adapter to spinner selections
        mMethodList.setAdapter(adapter);
        mMethodList.setSelection(position);

        mMethodList.setOnItemSelectedListener(new OnItemSelectedListener() {

        	// When new method is selected, update the method parameters (process temperatures and times for real time LAMP)
        	//    and update the parameters displayed on the user interface
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				Log.e(TAG, "new method selected");
				Cursor curse = mDbHelper.fetchMethod(id);
				//startManagingCursor(curse);
				analysisMethod = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_METHOD));

				analysisTitle = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_TITLE));
				analysisStartFrequency = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_START_FREQUENCY));
				analysisEndFrequency = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_END_FREQUENCY));
				analysisIntervalFrequency = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_INTERVAL_FREQUENCY));
				analysisSignalAmplitude = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_SIGNAL_AMPLITUDE));
				analysisBiasVoltage = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_BIAS_VOLTAGE));
				analysisStartVoltage = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_START_VOLTAGE));
				analysisEndVoltage = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_END_VOLTAGE));
				analysisScanRate = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_SCAN_RATE));
				analysisScanNumber = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_SCAN_NUMBER));
				analysisStepFrequency = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_STEP_FREQUENCY));
				analysisStepE = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_STEP_E));
				analysisPulseAmplitude = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_PULSE_AMPLITUDE));
				analysisElectrodeConfiguration = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_ELECTRODE_CONFIGURATION));
				analysisEquilibriumTime = curse.getString(curse.getColumnIndexOrThrow(SettingsDbAdapter.KEY_EQUILIBRIUM_TIME));


				PopulateProcessParameters();
			}


			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
        });
		mMethodList.setSelection(position);
	}

	// Write out the process parameters to the process parameters TextView (when Activity is created, and anytime a new method
	//  is selected by method_spinner
	public void PopulateProcessParameters() {
		String MethodSummary = "";
		String ParameterStringFormat = "";
		//CharSequence csParameters;

		String electrodeConfigString = "";
		if (analysisElectrodeConfiguration.equals(SettingsDbAdapter.OPEN_CIRCUIT_CONFIGURATION))
			electrodeConfigString = getResources().getString(R.string.open_circuit_config).toString();
		else if (analysisElectrodeConfiguration.equals(SettingsDbAdapter.TWO_ELECTRODE_CONFIGURATION))
			electrodeConfigString = getResources().getString(R.string.two_electrode_configuration).toString();
		else if (analysisElectrodeConfiguration.equals(SettingsDbAdapter.THREE_ELECTRODE_CONFIGURATION))
			electrodeConfigString = getResources().getString(R.string.three_electrode_configuration).toString();

		if (analysisScanNumber.equals(SettingsDbAdapter.INFINITE)) analysisScanNumber = getResources().getString(R.string.infinite).toString();

		if (analysisMethod.equals(SettingsDbAdapter.CYCLIC_VOLTAMMETRY)) {
			ParameterStringFormat = getResources().getString(R.string.cv_method_string);
			MethodSummary = String.format(ParameterStringFormat, analysisTitle,
					analysisStartVoltage, analysisEndVoltage, analysisScanRate,
					analysisEquilibriumTime, analysisScanNumber, electrodeConfigString);
		}
		else if (analysisMethod.equals(SettingsDbAdapter.DIFFERENTIAL_PULSE_VOLTAMMETRY)) {
			ParameterStringFormat = getResources().getString(R.string.dpv_method_string);
			MethodSummary = String.format(ParameterStringFormat, analysisTitle,
					analysisStartVoltage, analysisEndVoltage, analysisStepFrequency, analysisStepE, analysisPulseAmplitude,
					analysisEquilibriumTime, analysisScanNumber, electrodeConfigString);
		}
		else if (analysisMethod.equals(SettingsDbAdapter.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY)) {
			ParameterStringFormat = getResources().getString(R.string.eis_method_string);
			String amplitude = "";
			switch (Integer.parseInt(analysisSignalAmplitude)) {
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
			MethodSummary = String.format(ParameterStringFormat, analysisTitle,
					analysisStartFrequency, analysisEndFrequency, analysisIntervalFrequency, amplitude,
					analysisEquilibriumTime, analysisBiasVoltage, electrodeConfigString);
		}
		else if (analysisMethod.equals(SettingsDbAdapter.STATIC_LOGGING)) {
			String potentialString = "";
			String durationString = "";
			ParameterStringFormat = getResources().getString(R.string.applied_potential_string);
			if (!analysisElectrodeConfiguration.equals(SettingsDbAdapter.OPEN_CIRCUIT_CONFIGURATION))
				potentialString = String.format(ParameterStringFormat, analysisStartVoltage); // don't report applied potential unless not open circuit potential
			ParameterStringFormat = getResources().getString(R.string.record_duration_string);
			if (analysisScanNumber.equals(SettingsDbAdapter.INFINITE))
				durationString = getResources().getString(R.string.open_ended_duration).toString();
			else durationString = String.format(ParameterStringFormat, analysisScanNumber);
			ParameterStringFormat = getResources().getString(R.string.static_record_string);
			MethodSummary = String.format(ParameterStringFormat, analysisTitle,
					analysisEquilibriumTime, durationString, electrodeConfigString, potentialString);
		}
		//CharSequence csParameters = MethodSummary;

		mProcessParameters.setText(MethodSummary);//(Html.fromHtml(MethodSummary, Html.FROM_HTML_MODE_LEGACY));
	}

	public void Manage_Connections() {
		TimeoutHandler.removeCallbacks(Timeout); // stop asking for temperature data while managing connection
		Intent connectIntent = null;
		Log.e(TAG, "starting deviceListActivity intent");
		connectIntent = new Intent(this, DeviceListActivity.class);
		Log.e(TAG, "initialized intent for device list activity");
		startActivityForResult(connectIntent, CONNECT_MENU);
	}

	public void Real_Time_Analysis() {
		TimeoutHandler.removeCallbacks(Timeout);	// stop asking for temperature (new activity will take over)
		//if (Battery < (5 + (5 * RcTemp / 65))) Toast.makeText(this, R.string.insufficient_battery, Toast.LENGTH_SHORT).show();
		//else {
		Intent realtimeIntent = null;
		realtimeIntent = new Intent(this, Real_Time_Activity.class);
		//realtimeIntent.putExtra(FIRMWARE_VERSION_NAME, Firmware_Version);

		realtimeIntent.putExtra(SettingsDbAdapter.KEY_TITLE, analysisTitle);
		realtimeIntent.putExtra(DEVICE_NAME, mConnectedDeviceName);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_METHOD, analysisMethod);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_START_FREQUENCY, analysisStartFrequency);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_END_FREQUENCY, analysisEndFrequency);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_INTERVAL_FREQUENCY, analysisIntervalFrequency);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_SIGNAL_AMPLITUDE, analysisSignalAmplitude);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_BIAS_VOLTAGE, analysisBiasVoltage);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_START_VOLTAGE, analysisStartVoltage);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_END_VOLTAGE, analysisEndVoltage);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_SCAN_RATE, analysisScanRate);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_SCAN_NUMBER, analysisScanNumber);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_STEP_FREQUENCY, analysisStepFrequency);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_STEP_E, analysisStepE);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_PULSE_AMPLITUDE, analysisPulseAmplitude);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_ELECTRODE_CONFIGURATION, analysisElectrodeConfiguration);
		realtimeIntent.putExtra(SettingsDbAdapter.KEY_EQUILIBRIUM_TIME, analysisEquilibriumTime);
		realtimeIntent.putExtra(FIRMWARE_VERSION_NAME, Firmware_Version);

		startActivityForResult(realtimeIntent, RTLAMP);
		//}
	}

	private void EndActivity() {
		Log.e(TAG, "Callbacks to display temperature removed...");
		TimeoutHandler.removeCallbacks(Timeout);
    	Log.e(TAG, "BT data collection thread is closed...");
        //if (mBTService != null) mBTService.disconnectDevice();
    	if (mBTService.getState() == BluetoothService.STATE_CONNECTED) mBTService.disconnectDevice();
    	// if (mBTService != null) mBTService.stop();
    	Log.e(TAG, "mBTService is disconnected...");
        //mBTService = null;
        Log.e(TAG, "BT service ended...");
        Kinect = true;
        Log.e(TAG, "Kinect flag raised for next invocation of Activity");
        Real_Time_Activity.release_wakelock();
        finish();
	}

	@Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
        if(D) Log.e(TAG, "bluetooth state = " + mBTService.getState());
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
        if(D) Log.e(TAG, "bluetooth state = " + mBTService.getState());
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.e(TAG, "enterred onDestroy...");
        // Stop the Bluetooth services
        EndActivity();
    }

    @Override
    public void onBackPressed()
    {
    	super.onBackPressed();
    	Log.e(TAG, "Back Button Pressed...");
    	EndActivity();
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
        	Log.e(TAG, "not connected- can't sendMessage");
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
			while (writingMessage);	// wait until any messages from other threads are finished...
            // Get the message bytes and tell the BluetoothService to write
			writingMessage = true;
            byte[] send = message.getBytes();
            mBTService.write(send);
        }
    }

    //send instruction to Smart-DART to request firmware version...
    private void Request_Firmware_Name ()
    {
    	sendMessage("n");	// 'n' is code for device to return its firmware version
    }

    public String thermocoupleString() {
		String tcString = tcTemperature;
		if (!tcString.equals(NO_THERMOCOUPLE_STRING)) tcString += getResources().getString(R.string.degrees_c).toString();
		return tcString;
	}

	private void reportResistance(String resistor) {

		mDataView.setText(String.format(getResources().getString(R.string.resistor_report), resistor, boardTemperature, thermocoupleString()));
	}

	private void reportCapacitance(String capacitor, String phase) {
		mDataView.setText(String.format(getResources().getString(R.string.capacitor_report), capacitor, phase, boardTemperature, thermocoupleString()));
	}

	private void reportOpenCircuitVoltage(String volts) {
		mDataView.setText(String.format(getResources().getString(R.string.vopen_report), volts, boardTemperature, thermocoupleString()));
	}

	private void reportBiasedCurrent(String current, String potential) {
		mDataView.setText(String.format(getResources().getString(R.string.current_report), current, potential, boardTemperature, thermocoupleString()));
	}

	/*
	Estimate thermocouple temperature from the given reference temperature and observed thermoelectric voltage
	if thermoelectric voltage is too high we know there is not a thermocouple plugged in so we report not connected
	 */
	public static String tcValue(double refTemp, double E) {
		String ThermocoupleValueString = NO_THERMOCOUPLE_STRING;
		if (E < 50000) {    // only estimate thermocouple temperature if E < 50 mV, or about 1300C; above that there is definitely no thermocouple plugged in
			double refE = typeK_alphaCoefficients[0] * java.lang.Math.exp(typeK_alphaCoefficients[1] *
					java.lang.Math.pow((refTemp + typeK_alphaCoefficients[2]), 2));
			refE += typeK_TtoECoefficients[0];
			for (int c = 1; c < 10; c++) {
				refE += typeK_TtoECoefficients[c] * java.lang.Math.pow(refTemp, c);
			}
			refE += E;    // now add the observed thermoelectric potential to estimated "virtual" reference potential
			double tcT = typeK_EtoTCoefficients[0];
			for (int c = 1; c < 10; c++) {
				tcT += typeK_EtoTCoefficients[c] * java.lang.Math.pow(refE, c);
			}
			NumberFormat nf = NumberFormat.getInstance(Locale.getDefault()); // get instance
			nf.setMaximumFractionDigits(2); // set decimal places
			ThermocoupleValueString = nf.format(tcT);// + "\u00B0C";
		}
		return ThermocoupleValueString;
	}

	/*
	Arguments reported here are board refernce temperature from ADS1220 (in C), and thermocouple E (in uV)
	 */
	private void parseTemperatures(double refT, double Potential) {
		boardTemperature = refT;
		tcTemperature = tcValue(refT, Potential);
	}

	private void requestFirmware() {
		sendMessage("n");	// request firmware name...
	}

    // The Handler that gets information back from the BluetoothService
    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	NumberFormat localeSpecificNF = NumberFormat.getInstance(Locale.getDefault());
            switch (msg.what) {
				case MESSAGE_STATE_CHANGE:
					if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
					BT_State = msg.arg1;
					switch (BT_State) {
						case BluetoothService.STATE_CONNECTED:
							Request_Firmware_Name(); // request firmware name...
							mTitle.setText(R.string.title_connected_to);
							mTitle.append(mConnectedDeviceName);
							BTCommLoss = 0;
							requestFirmware();
							TimeoutHandler.postDelayed(Timeout, 1000); // periodically keep requesting device temperature and checking comm
							showDeviceControls();	// make sure communication buttons are enabled (in case buttons were disabled while disconnecting bluetooth)
							break;
						case BluetoothService.STATE_CONNECTING:
							mTitle.setText(R.string.title_connecting);
							hideDeviceControls();
							TimeoutHandler.removeCallbacks(Timeout);
							break;
						case BluetoothService.STATE_NONE:
							mTitle.setText(R.string.title_not_connected);
							hideDeviceControls();
							Firmware_Version = NO_DEVICE;
							TimeoutHandler.removeCallbacks(Timeout);
							break;
					}
					break;
				case MESSAGE_DEVICE_NAME:
					// save the connected device's name
					mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
					mConnectedDeviceAddress = msg.getData().getString(DEVICE_ADDRESS);
					Toast.makeText(getApplicationContext(), "Connected to "
							+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
					Toast.makeText(getApplicationContext(), "with BT device address "
							+ mConnectedDeviceAddress, Toast.LENGTH_SHORT).show();
					break;
				case MESSAGE_TOAST:
					Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
							Toast.LENGTH_SHORT).show();
					break;
				case NO_STORAGE_READ:
					Toast.makeText(getApplicationContext(), R.string.files_not_read, Toast.LENGTH_LONG).show();
					break;
				case NO_BLUETOOTH:
					Toast.makeText(getApplicationContext(), R.string.no_bluetooth_device, Toast.LENGTH_LONG).show();
					break;
				case BLUETOOTH_NOT_ON:
					Toast.makeText(getApplicationContext(), R.string.bluetooth_not_on, Toast.LENGTH_LONG).show();
					break;
				case TEMPERATURE_READ:	//
					double Temps[] = (double[]) msg.obj;
					double refTemp = Temps[0];
					double tcE = Temps[1];
					//if (D) Log.d(TAG, "Temperature value extracted: " + Temp);
					parseTemperatures(refTemp, tcE);
					break;
				case SUBROUTINE_EXIT:
					if(D) Log.e(TAG, "Smart-DART has indicated exit of latest sub-routine...");
					break;
				case BATT_READ:
					if (D) Log.d(TAG, "Battery charge status " + msg.arg1 + "%");
					mTitle.setText(mConnectedDeviceName);
					Battery = msg.arg1;
					mTitle.append(": " + Battery + "%");
					break;
				case FIRMWARE_VERSION_READ:
					Firmware_Version = msg.obj.toString();
					Toast.makeText(getApplicationContext(), "Firmware Version: " + Firmware_Version, Toast.LENGTH_LONG).show();
					break;
				case RESISTANCE_READ:
					Bundle rBundle = (Bundle) msg.obj;
					String r = rBundle.getString(BluetoothService.RESISTANCE_KEY);
					double resistance = Double.parseDouble(r);
					localeSpecificNF.setMaximumFractionDigits(1);
					String localR = localeSpecificNF.format(resistance);
					reportResistance(localR);
					break;
				case CAPACITANCE_READ:
					Bundle cBundle = (Bundle) msg.obj;
					String c = cBundle.getString(BluetoothService.CAPACITANCE_KEY);
					String f = cBundle.getString(BluetoothService.PHASE_KEY);
					double fase = Double.parseDouble(f);
					localeSpecificNF.setMaximumFractionDigits(2);
					String localFase = localeSpecificNF.format(fase);
					reportCapacitance(c, localFase);
					break;
				case CELL_CURRENT_READ:
					Bundle iBundle = (Bundle) msg.obj;
					String i = iBundle.getString(BluetoothService.CURRENT_KEY);
					String e = iBundle.getString(BluetoothService.VOLTAGE_KEY);
					double cur = Double.parseDouble(i);
					double vol = Double.parseDouble(e);
					localeSpecificNF.setMaximumFractionDigits(3);
					String localCur = localeSpecificNF.format(cur);
					localeSpecificNF.setMaximumFractionDigits(2);
					String localVol = localeSpecificNF.format(vol);
					reportBiasedCurrent(localCur, localVol);
					break;
				case ENGAGED:
					ABEbusy = true;
					break;
				case OPEN_CIRCUIT_VOLTAGE_READ:
					Bundle vBundle = (Bundle) msg.obj;
					String v = vBundle.getString(BluetoothService.VOLTAGE_KEY);
					double volta = Double.parseDouble(v);
					localeSpecificNF.setMaximumFractionDigits(4);
					String localVolta = localeSpecificNF.format(volta);
					reportOpenCircuitVoltage(localVolta);
					break;
				default:
            	break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode + " from request code/ activity " + requestCode);
        if (mBTService.getState() == BluetoothService.STATE_CONNECTED) // if bluetooth connected, start asking for device temperature again
        	TimeoutHandler.postDelayed(Timeout, 1000); // when returning back to this activity
        switch (requestCode) {
        case CONNECT_MENU:
            // When DeviceListActivity returns...
        	Log.e(TAG, "returned from bluetooth deviceListActivity");
        	mDataView.setText("");	// blank out fluorescence data readings
            if (resultCode == Activity.RESULT_OK) {
                if(Kinect) {
                	connectDevice(data, true); // if connect flag was set when calling DeviceListActivity, connect device
                }
                else {	// disconnect device...
                	Log.e(TAG, "instruction received to disconnect bluetooth");
                	if (mBTService.getState() == BluetoothService.STATE_CONNECTED)
						mBTService.disconnectDevice();
                }
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
        	Log.e(TAG, "returned from intent to enable bluetooth");
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up user interface
                setupUserInterface();
            } else {
                // User did not enable Bluetooth or an error occured, so exit application
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
//                finish();
            }
            break;
        // After returning from the methods edit menu, repopulate method parameters (they may have changed)
        case ANALYSIS_SETTINGS:
        	Log.e(TAG, "returned from Methods Editor...");
        	if (resultCode == Activity.RESULT_OK) {
        		analysisTitle = data.getStringExtra(SettingsDbAdapter.KEY_TITLE);
        		setupMethodsSpinner();	// make sure to update available methods when returning from methods editor
        		mDataView.setText("");	// blank out fluorescence data readings
        	}
        	break;
        case RTLAMP:
        	Real_Time_Activity.release_wakelock();
        	if (resultCode == Activity.RESULT_OK) {
        		boolean File_Saved = data.getBooleanExtra(Real_Time_Activity.FILE_SAVED, true);
        		boolean File_Save_Checked = data.getBooleanExtra(Real_Time_Activity.SAVE_FILE_CHECK, true);

        		if ((!File_Saved) && File_Save_Checked) Toast.makeText(this, R.string.file_not_written, Toast.LENGTH_LONG).show();
        		if (mBTService.getState() == BluetoothService.STATE_CONNECTED) {
        			mBTService.newHandler(mHandler); // reassign the bluetooth service back to the local handler
					RadioButton radB = (RadioButton) findViewById(R.id.open_circuit);
					radB.setChecked(true);	// by default after any analysis go to open circuit configuration...
					measurement = OPEN_CIRCUIT_VOLTAGE;
        			showDeviceControls();	// (show device controls parse given new interface settings and sends appropriate instruction for analysis
        			Kinect = false;
        		}
        		else {
        			Kinect = true;
        			/*mBTService.stop();*/
        			mBTService = null;
        			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        			mBTService = new BluetoothService(this, mHandler);
        			hideDeviceControls();	// if device was disconnected (idle after reaction complete) disable interface buttons
        			mTitle.setText(R.string.title_not_connected); // and show not connected in title bar
        		}
        		mDataView.setText("");	// blank out fluorescence data readings
        	}
        	break;
        case SHARE_DATA:
        	Log.e(TAG, "Returned from Share/ attach activity");
        	// Code when returning from e-mailing data files
        	mDataView.setText("");	// blank out fluorescence data readings
        	break;
        }
    }
    
    private Runnable Timeout = new Runnable() {
    	public void run() {
			int timeDelay = 1000;
			//if (measurement == CAPACITANCE) timeDelay = 2000;	// capacitance measurement can be slow, so only request every 2 seconds; with use of "ABEbusy", don't need to worry about sending a backlog of instructions...
    		TimeoutHandler.postDelayed(this, timeDelay);
			if (!ABEbusy) showDeviceControls(); // as long as most recent message from ABE-Stat hasn't indicated it's still busy analyzing, send new instruction requesting analysis
    		if ((mBTService.getState() == BluetoothService.STATE_CONNECTED) && (BTCommLoss > 3)) {	// looks like connection was lost- reset microcontroller & disconnect
    			TimeoutHandler.removeCallbacks(Timeout);	// and stop asking for temperature
    			mBTService.disconnectDevice();
    		}
    		if (!ABEbusy) BTCommLoss++;	// increment count of missed BT transmissions (should get at least one transmission each time this function is called
			ABEbusy = false;	// reset ABEbusy flag so that a device can't be fooled by a single "busy" transmission by ABE-Stat (i.e. if lose communication subsequent to this flag)
    	}
    };

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mBTService.connect(device, secure);
    }
    
    private void Methods_Edit() {
    	TimeoutHandler.removeCallbacks(Timeout);	// stop asking for temperature
    	Intent serverIntent = null;
    	serverIntent = new Intent(this, SettingsMenu.class);
    	serverIntent.putExtra(SettingsDbAdapter.KEY_TITLE, analysisTitle);
        if(D) Log.e(TAG, "about to start SettingsMenu activity");
        startActivityForResult(serverIntent, ANALYSIS_SETTINGS);
    }
};