package com.diagenetix.abestat;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.ParseException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity called to enable selecting, creating, and modifying
 * settings for Smart-DART analysis. 
 */
public class SettingsEdit extends Activity {
	// for Debugging...
	private static final String TAG = "SettingsEdit";
	private static final boolean D = true;

	// default method settings
	public static final String defMethod = SettingsDbAdapter.CYCLIC_VOLTAMMETRY;
	public static final String defStartFrequency = "0.1";
	public static final String defEndFrequency = "100000";
	public static final String defIntervalFrequency = "10";
	public static final String defSignalAmplitude = "3";
	public static final String defBiasVoltage = "0.0";
	public static final String defStartVoltage = "-1.0";
	public static final String defEndVoltage = "1.0";
	public static final String defScanRate = "0.1";
	public static final String defScanNumber = SettingsDbAdapter.INFINITE;
	public static final String defStepFrequency = "25";
	public static final String defStepE = "2";
	public static final String defPulseAmplitude = "25";
	public static final String defElectrodeConfiguration = SettingsDbAdapter.THREE_ELECTRODE_CONFIGURATION;
	public static final String defEquilibriumTime = "10";

	private static final int frequencySpinnerSpan = 6;	// decades of frequency available for EIS
	
	// database variables
	private static String Title;
	private static String Method = defMethod;

	private static double startFrequency = Double.parseDouble(defStartFrequency);
	private static double endFrequency = Double.parseDouble(defEndFrequency);
	private static int intervalFrequency = Integer.parseInt(defIntervalFrequency);
	private static String signalAmplitude = defSignalAmplitude;
	private static double biasVoltage = Double.parseDouble(defBiasVoltage);
	private static double startVoltage = Double.parseDouble(defStartVoltage);
	private static double endVoltage = Double.parseDouble(defEndVoltage);
	private static double scanRate = Double.parseDouble(defScanRate);

	private static int scanNumber = 1;
	private static String scanNumberString = defScanNumber;	// separate dataTypes for scanNumber & scanNumberString, because can be "infinite"

	private static int stepFrequency = Integer.parseInt(defStepFrequency);
	private static int stepE = Integer.parseInt(defStepE);
	private static int pulseAmplitude = Integer.parseInt(defPulseAmplitude);

	private static String electrodeConfiguration = defElectrodeConfiguration;
	private static int equilibriumTime = Integer.parseInt(defEquilibriumTime);
	
	// class variables
    private static SettingsDbAdapter mDbHelper;
    private static EditText mTitleText;
	private static Spinner mMethodSpinner;

	// Interactive interface widgets...
    private static EditText mStartFrequencyText;
    private static SeekBar mStartFrequencySlider;
    private static EditText mEndFrequencyText;
    private static SeekBar mEndFrequencySlider;
    private static EditText mIntervalFrequencyText;
    private static SeekBar mIntervalFrequencySlider;
    private static EditText mBiasVoltageText;
    private static SeekBar mBiasVoltageSlider;
    private static EditText mStartVoltageText;
    private static SeekBar mStartVoltageSlider;
    private static EditText mEndVoltageText;
    private static SeekBar mEndVoltageSlider;
    private static EditText mScanRateText;
    private static SeekBar mScanRateSlider;
    private static EditText mScanNumberText;
    private static SeekBar mScanNumberSlider;
	private static CheckBox mInfiniteScans;
    private static EditText mStepFrequencyText;
    private static SeekBar mStepFrequencySlider;
    private static EditText mStepEText;
    private static SeekBar mStepESlider;
    private static EditText mPulseAmplitudeText;
	private static SeekBar mPulseAmplitudeSlider;
	private static EditText mEquilibriumTimeText;
	private static SeekBar mEquilibriumTimeSlider;

    private static Long mRowId;
    private static TextView mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        mDbHelper = new SettingsDbAdapter(this);
        mDbHelper.open();
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.method_edit);
        
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
//        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        mTitle.setText(R.string.analysis);
        
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        
        if(D) Log.e(TAG, "++ title set ++");

        mTitleText = (EditText) findViewById(R.id.title);
		mMethodSpinner = (Spinner) findViewById(R.id.available_methods);

        mStartFrequencyText = (EditText) findViewById(R.id.start_frequency_value);
        mStartFrequencySlider = (SeekBar) findViewById(R.id.start_frequency_slider);
        mEndFrequencyText = (EditText) findViewById(R.id.end_frequency_value);
        mEndFrequencySlider = (SeekBar) findViewById(R.id.end_frequency_slider);
        mIntervalFrequencyText = (EditText) findViewById(R.id.interval_frequency_value);
        mIntervalFrequencySlider = (SeekBar) findViewById(R.id.interval_frequency_slider);
        mBiasVoltageText = (EditText) findViewById(R.id.bias_voltage_value);
        mBiasVoltageSlider = (SeekBar) findViewById(R.id.bias_voltage_slider);

        mStartVoltageText = (EditText) findViewById(R.id.start_voltage_value);
        mStartVoltageSlider = (SeekBar) findViewById(R.id.start_voltage_slider);
        mEndVoltageText = (EditText) findViewById(R.id.end_voltage_value);
        mEndVoltageSlider = (SeekBar) findViewById(R.id.end_voltage_slider);
        mScanRateText = (EditText) findViewById(R.id.scan_rate_value);
        mScanRateSlider = (SeekBar) findViewById(R.id.scan_rate_slider);
        mScanNumberText = (EditText) findViewById(R.id.scan_number_value);
        mScanNumberSlider = (SeekBar) findViewById(R.id.scan_number_slider);

        mStepFrequencyText = (EditText) findViewById(R.id.step_frequency_value);
        mStepFrequencySlider = (SeekBar) findViewById(R.id.step_frequency_slider);
        mStepEText = (EditText) findViewById(R.id.step_e_value);
        mStepESlider = (SeekBar) findViewById(R.id.step_e_slider);
        mPulseAmplitudeText = (EditText) findViewById(R.id.pulse_amp_value);
        mPulseAmplitudeSlider = (SeekBar) findViewById(R.id.pulse_amp_slider);

		mInfiniteScans = (CheckBox) findViewById(R.id.infinite_scans);
		checkInfiniteScans();

        mEquilibriumTimeText = (EditText) findViewById(R.id.equilibrium_time_value);
        mEquilibriumTimeSlider = (SeekBar) findViewById(R.id.equilibrium_time_slider);

        mInfiniteScans.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		checkInfiniteScans();
        	}
        });
        
        Button confirmButton = (Button) findViewById(R.id.confirm);

        // Check saved instance state for RowID (if Activity was interrupted);
        //  if no RowID, then check data passed from intent (parent activity) for RowID
        //  (if mRowId == null, then we know that this is a new method being created from SettingsEdit
        //  and we have to pass info to the SettingsDbAdapter.createMethod; otherwise data including
        //  mRowId is passed to SettingsDbAdapter.updateMethod
        mRowId = (savedInstanceState == null) ? null :
        	(Long) savedInstanceState.getSerializable(SettingsDbAdapter.KEY_ROWID);
        if (mRowId == null) {
        	Bundle extras = getIntent().getExtras();
        	mRowId = extras != null ? extras.getLong(SettingsDbAdapter.KEY_ROWID):null;
        }
        
        confirmButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (mTitleText == null) mTitleText.setText(R.string.method_default);
                saveState();
                Intent quitIntent = new Intent();
                quitIntent.putExtra(SettingsDbAdapter.KEY_TITLE, Title);
            	setResult(RESULT_OK, quitIntent);
                finish();
            }
        });

		double lowFrequency = 0.1;
		int lowFreqCounter = 0;

		populateFields();	// populate method spinner, and parameter values- using the method passed from intent or saved instance, or default values if no intent data
    }

    /*
    Check if infinite scans checkbox is selected (if so, remove numerical interface elements to set value
     */
    private void checkInfiniteScans() {
		if (mInfiniteScans.isChecked()) {
			mScanNumberText.setVisibility(View.GONE);
			mScanNumberSlider.setVisibility(View.GONE);
			scanNumberString = defScanNumber;
		}
		else {
			mScanNumberText.setVisibility(View.VISIBLE);
			mScanNumberSlider.setVisibility(View.VISIBLE);
			scanNumberString = Integer.toString(scanNumber);
		}
	}
    
    /*
     * update the interface when checkboxes clicked.
     */
    private void setupInterface() {
    	TextView tv = (TextView) findViewById(R.id.start_voltage_heading);
		TextView dv = (TextView) findViewById(R.id.scan_number_heading);
    	if (!Method.equals(SettingsDbAdapter.STATIC_LOGGING)) {	// if changed from static logging, restore parts of GUI...
			removeOpenCircuitButton();
			tv.setText(getString(R.string.start_voltage));
    		dv.setText(getString(R.string.scan_number));
    		findViewById(R.id.end_voltage_heading).setVisibility(View.VISIBLE);
    		findViewById(R.id.end_voltage_value).setVisibility(View.VISIBLE);
			findViewById(R.id.end_voltage_slider).setVisibility(View.VISIBLE);
			findViewById(R.id.voltage_range_settings).setVisibility(View.VISIBLE);
		}
    	else {	// if we are just recording voltage and current, for static conditions, change interface accordingly
			tv.setText(getString(R.string.applied_potential));
			dv.setText(getString(R.string.log_duration));
			findViewById(R.id.open_circuit_button).setVisibility(View.VISIBLE);
			if (electrodeConfiguration.equals(SettingsDbAdapter.OPEN_CIRCUIT_CONFIGURATION))
				findViewById(R.id.voltage_range_settings).setVisibility(View.VISIBLE);
			findViewById(R.id.voltammetry_settings).setVisibility(View.VISIBLE);
			findViewById(R.id.cv_settings).setVisibility(View.GONE);
			findViewById(R.id.dpv_settings).setVisibility(View.GONE);
			findViewById(R.id.eis_settings).setVisibility(View.GONE);
			findViewById(R.id.end_voltage_heading).setVisibility(View.GONE);
			findViewById(R.id.end_voltage_value).setVisibility(View.GONE);
			findViewById(R.id.end_voltage_slider).setVisibility(View.GONE);
		}
    	if (Method.equals(SettingsDbAdapter.CYCLIC_VOLTAMMETRY)) {
            findViewById(R.id.voltammetry_settings).setVisibility(View.VISIBLE);
    		findViewById(R.id.cv_settings).setVisibility(View.VISIBLE);
            findViewById(R.id.dpv_settings).setVisibility(View.GONE);
			findViewById(R.id.eis_settings).setVisibility(View.GONE);
    	}
        else if (Method.equals(SettingsDbAdapter.DIFFERENTIAL_PULSE_VOLTAMMETRY)) {
            findViewById(R.id.voltammetry_settings).setVisibility(View.VISIBLE);
            findViewById(R.id.cv_settings).setVisibility(View.GONE);
            findViewById(R.id.dpv_settings).setVisibility(View.VISIBLE);
            findViewById(R.id.eis_settings).setVisibility(View.GONE);
        }
    	else if (Method.equals(SettingsDbAdapter.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY)) {
			findViewById(R.id.voltammetry_settings).setVisibility(View.GONE);
			findViewById(R.id.eis_settings).setVisibility(View.VISIBLE);
		}
    }
    
    /*
	 * Function for when radio buttons are toggled (either select optical ch1 
	 * (Fluor 1, first bank of 8 RFU values starting at index 0) or ch2 (second bank starting at index 8)
	 */
	public void onRadioButtonClicked(View v) {
		if(D) Log.e(TAG, "Radio Button Selected");
		RadioButton rb = (RadioButton) v;
		switch(rb.getId()) {
			case(R.id.open_circuit_button):
				electrodeConfiguration = SettingsDbAdapter.OPEN_CIRCUIT_CONFIGURATION;
				StartVoltage_Check();
				break;
			case(R.id.three_electrode_button):
				electrodeConfiguration = SettingsDbAdapter.THREE_ELECTRODE_CONFIGURATION;
				break;
			case(R.id.two_electrode_button):
				electrodeConfiguration = SettingsDbAdapter.TWO_ELECTRODE_CONFIGURATION;
				break;
			case(R.id.amp0):
				signalAmplitude = "0";
				break;
			case(R.id.amp1):
				signalAmplitude = "1";
				break;
			case(R.id.amp2):
				signalAmplitude = "2";
				break;
			case(R.id.amp3):
				signalAmplitude = "3";
				break;
			default:
				break;
		}
	}
	
	private void clearTextFocuses() {
		mTitle.clearFocus();
		mStartFrequencyText.clearFocus();
		mEndFrequencyText.clearFocus();
		mIntervalFrequencyText.clearFocus();
		mBiasVoltageText.clearFocus();
		mStartVoltageText.clearFocus();
		mEndVoltageText.clearFocus();
		mScanRateText.clearFocus();
		mScanNumberText.clearFocus();
		mEquilibriumTimeText.clearFocus();
        mBiasVoltageText.clearFocus();
        mStepEText.clearFocus();
        mStepFrequencyText.clearFocus();
	}
    
    /*
     * Populate EditText fields for editing methods. If mRowId and method title exist, populate fields from database; otherwise
     * use default values.
     */
    private void populateFields() {
		List<String> spinnerArray = new ArrayList<String>();
		spinnerArray.add(getString(R.string.cyclic_voltammetry));
        spinnerArray.add(getString(R.string.differential_pulse_voltammetry));
		spinnerArray.add(getString(R.string.electrochemical_impedance_spectroscopy));
		spinnerArray.add(getString(R.string.static_logging));
		//spinnerArray.add(getString(R.string.anodic_stripping_voltammetry));

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item, spinnerArray);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mMethodSpinner.setAdapter(adapter);
		mMethodSpinner.setSelection(adapter.getPosition(getString(R.string.cyclic_voltammetry)));    // by default set selection as cyclic voltammetry
		Method = SettingsDbAdapter.CYCLIC_VOLTAMMETRY;	// set to cyclic voltammetry by default (if will be overwritten if savedInstanceState exists, or mRowId passed in Bundle when launching activity

		if (mRowId != null) {
			if (D) Log.e(TAG, "About to fetch methods (query database)...");
			Cursor method = mDbHelper.fetchMethod(mRowId);
//    		startManagingCursor(method);
			Title = method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_TITLE));
			mTitleText.setText(Title);

			Method = method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_METHOD));
			startFrequency = localeSpecificParsedDoubleString(method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_START_FREQUENCY)));
			endFrequency = localeSpecificParsedDoubleString(method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_END_FREQUENCY)));
			intervalFrequency = localeSpecificParsedIntString(method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_INTERVAL_FREQUENCY)));
			signalAmplitude = method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_SIGNAL_AMPLITUDE));
			biasVoltage = localeSpecificParsedDoubleString(method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_BIAS_VOLTAGE)));
			startVoltage = localeSpecificParsedDoubleString(method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_START_VOLTAGE)));
			endVoltage = localeSpecificParsedDoubleString(method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_END_VOLTAGE)));
			scanRate = localeSpecificParsedDoubleString(method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_SCAN_RATE)));
			scanNumberString = method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_SCAN_NUMBER));
			if (!scanNumberString.equals(SettingsDbAdapter.INFINITE)) {
				scanNumber = localeSpecificParsedIntString(scanNumberString);
				mInfiniteScans.setChecked(false);
				checkInfiniteScans();
			}
			stepFrequency = localeSpecificParsedIntString(method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_STEP_FREQUENCY)));
			stepE = localeSpecificParsedIntString(method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_STEP_E)));
			pulseAmplitude = localeSpecificParsedIntString(method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_PULSE_AMPLITUDE)));
			electrodeConfiguration = method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_ELECTRODE_CONFIGURATION));
			equilibriumTime = localeSpecificParsedIntString(method.getString(method.getColumnIndexOrThrow(SettingsDbAdapter.KEY_EQUILIBRIUM_TIME)));
		}
		int frequencyExponent = (int) (java.lang.Math.log10(startFrequency));
		StartFrequency_Check(frequencyExponent);
		frequencyExponent = (int) (java.lang.Math.log10(endFrequency));
		EndFrequency_Check(frequencyExponent);
		IntervalFrequency_Check();
		BiasVoltage_Check();
		StartVoltage_Check();
		EndVoltage_Check();
		ScanRate_Check();
		StepE_Check();
		StepFrequency_Check();
		PulseAmplitude_Check();
		ScanNumber_Check();
		EquilibriumTime_Check();
		RadioButton radBo;
		if (Method.equals(SettingsDbAdapter.CYCLIC_VOLTAMMETRY)) {
			mMethodSpinner.setSelection(((ArrayAdapter) mMethodSpinner.getAdapter()).getPosition(getString(R.string.cyclic_voltammetry)));
			radBo = (RadioButton) findViewById(R.id.open_circuit_button);
			radBo.setVisibility(View.GONE);	// can't do CV in open circuit configuration, so don't even show this option
			if (electrodeConfiguration.equals(SettingsDbAdapter.OPEN_CIRCUIT_CONFIGURATION))
				electrodeConfiguration = SettingsDbAdapter.THREE_ELECTRODE_CONFIGURATION;	// if someone accidentally selected open circuit config for CV, change to 3 electrode config
		}
		else if (Method.equals(SettingsDbAdapter.DIFFERENTIAL_PULSE_VOLTAMMETRY)) {
            mMethodSpinner.setSelection(((ArrayAdapter) mMethodSpinner.getAdapter()).getPosition(getString(R.string.differential_pulse_voltammetry)));
			radBo = (RadioButton) findViewById(R.id.open_circuit_button);
			radBo.setVisibility(View.GONE);	// can't do DPV in open circuit configuration, so don't even show this option
			if (electrodeConfiguration.equals(SettingsDbAdapter.OPEN_CIRCUIT_CONFIGURATION))
				electrodeConfiguration = SettingsDbAdapter.THREE_ELECTRODE_CONFIGURATION;	// if someone accidentally selected open circuit config for CV, change to 3 electrode config
        }
		else if (Method.equals(SettingsDbAdapter.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY)) {
			mMethodSpinner.setSelection(((ArrayAdapter) mMethodSpinner.getAdapter()).getPosition(getString(R.string.electrochemical_impedance_spectroscopy)));
			radBo = (RadioButton) findViewById(R.id.open_circuit_button);
			radBo.setVisibility(View.GONE);	// can't do EIS in open circuit configuration, so don't even show this option
			if (electrodeConfiguration.equals(SettingsDbAdapter.OPEN_CIRCUIT_CONFIGURATION))
				electrodeConfiguration = SettingsDbAdapter.THREE_ELECTRODE_CONFIGURATION;	// if someone accidentally selected open circuit config for CV, change to 3 electrode config
		}
		else if (Method.equals(SettingsDbAdapter.STATIC_LOGGING)) {
			mMethodSpinner.setSelection(((ArrayAdapter) mMethodSpinner.getAdapter()).getPosition(getString(R.string.static_logging)));
			radBo = (RadioButton) findViewById(R.id.open_circuit_button);
			radBo.setVisibility(View.VISIBLE);	// might want to make a record of open circuit potentials- so for this method allow option...
		}
			/*else if (Method.equals(SettingsDbAdapter.DIFFERENTIAL_PULSE_VOLTAMMETRY)) {
				mMethodSpinner.setSelection(((ArrayAdapter)mMethodSpinner.getAdapter()).getPosition(getString(R.string.differential_pulse_voltammetry)));
			}
			else if (Method.equals(SettingsDbAdapter.ANODIC_STRIPPING_VOLTAMMETRY)) {
				mMethodSpinner.setSelection(((ArrayAdapter)mMethodSpinner.getAdapter()).getPosition(getString(R.string.anodic_stripping_voltammetry)));
			}*/

		// also check saved electrode configuration to make sure correct radio button is selected at start...
		if (electrodeConfiguration.equals(SettingsDbAdapter.THREE_ELECTRODE_CONFIGURATION)) {
			radBo = (RadioButton) findViewById(R.id.three_electrode_button);
			radBo.setChecked(true);
		}
		else if (electrodeConfiguration.equals(SettingsDbAdapter.TWO_ELECTRODE_CONFIGURATION)) {
			radBo = (RadioButton) findViewById(R.id.two_electrode_button);
			radBo.setChecked(true);
		}
		else if (electrodeConfiguration.equals(SettingsDbAdapter.OPEN_CIRCUIT_CONFIGURATION)) {
			radBo = (RadioButton) findViewById(R.id.open_circuit_button);
			radBo.setChecked(true);
		}
		if (signalAmplitude.equals("0")) {
			radBo = (RadioButton) findViewById(R.id.amp0);
			radBo.setChecked(true);
		}
		else if (signalAmplitude.equals("1")) {
			radBo = (RadioButton) findViewById(R.id.amp1);
			radBo.setChecked(true);
		}
		else if (signalAmplitude.equals("2")) {
			radBo = (RadioButton) findViewById(R.id.amp2);
			radBo.setChecked(true);
		}
		else  {
			signalAmplitude = "3";
			radBo = (RadioButton) findViewById(R.id.amp3);
			radBo.setChecked(true);
		}

		setupInterface();    // set up the interface based on the selected settings

		mMethodSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int position, long id) {
				TextView dv = (TextView) findViewById(R.id.scan_number_heading);
				dv.setText(getString(R.string.scan_number));

				if (position == ((ArrayAdapter) mMethodSpinner.getAdapter()).getPosition(getString(R.string.cyclic_voltammetry))) {
					Method = SettingsDbAdapter.CYCLIC_VOLTAMMETRY;
				} else if (position == ((ArrayAdapter) mMethodSpinner.getAdapter()).getPosition(getString(R.string.electrochemical_impedance_spectroscopy))) {
					Method = SettingsDbAdapter.ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY;
				} else if (position == ((ArrayAdapter) mMethodSpinner.getAdapter()).getPosition(getString(R.string.differential_pulse_voltammetry))) {
					Method = SettingsDbAdapter.DIFFERENTIAL_PULSE_VOLTAMMETRY;
				} else if (position == ((ArrayAdapter) mMethodSpinner.getAdapter()).getPosition(getString(R.string.static_logging))) {
					Method = SettingsDbAdapter.STATIC_LOGGING;
					dv.setText(getString(R.string.log_duration));
					RadioButton radBo = (RadioButton) findViewById(R.id.open_circuit_button);
					radBo.setVisibility(View.VISIBLE);	// might want to make a record of open circuit potentials- so for this method allow option...
				}
				setupInterface();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});
    	
    	mStartFrequencyText.setOnKeyListener(new View.OnKeyListener() {
		    @Override
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        // your custom implementation
		        if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
		            startFrequency = localeSpecificParsedDoubleString(mStartFrequencyText.getText().toString());
		            int frequencyExponent = (int) (java.lang.Math.log10(startFrequency));
		            StartFrequency_Check(frequencyExponent);	// -1 in argument means that function was called from editText
						// so evaluate startFrequency as the nearest value above the startFrequency value enterred in EditText
					clearTextFocuses();
		            return true; // indicate that we handled event, won't propagate it
		        }
		        return false; // when we don't handle other keys, propagate event further
		    }
		});
    	
    	mEndFrequencyText.setOnKeyListener(new View.OnKeyListener() {
		    @Override
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        // your custom implementation
		        if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
		            endFrequency = localeSpecificParsedDoubleString(mEndFrequencyText.getText().toString());
					int frequencyExponent = (int) (java.lang.Math.log10(endFrequency));
		            EndFrequency_Check(frequencyExponent);
					clearTextFocuses();
		            return true; // indicate that we handled event, won't propagate it
		        }
		        return false; // when we don't handle other keys, propagate event further
		    }
		});
    	
    	mIntervalFrequencyText.setOnKeyListener(new View.OnKeyListener() {
		    @Override
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        // your custom implementation
		        if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
		            intervalFrequency = localeSpecificParsedIntString(mIntervalFrequencyText.getText().toString());
		            IntervalFrequency_Check();
					clearTextFocuses();
		            return true; // indicate that we handled event, won't propagate it
		        }
		        return false; // when we don't handle other keys, propagate event further
		    }
		});
    	
    	mBiasVoltageText.setOnKeyListener(new View.OnKeyListener() {
		    @Override
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        // your custom implementation
		        if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
		            biasVoltage = localeSpecificParsedDoubleString(mBiasVoltageText.getText().toString());
		            BiasVoltage_Check();
					clearTextFocuses();
		            return true; // indicate that we handled event, won't propagate it
		        }
		        return false; // when we don't handle other keys, propagate event further
		    }
		});
    	
    	mStartVoltageText.setOnKeyListener(new View.OnKeyListener() {
		    @Override
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        // your custom implementation
		        if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
		            startVoltage = localeSpecificParsedDoubleString(mStartVoltageText.getText().toString());
		            StartVoltage_Check();
					clearTextFocuses();
		            return true; // indicate that we handled event, won't propagate it
		        }
		        return false; // when we don't handle other keys, propagate event further
		    }
		});
    	
    	mEndVoltageText.setOnKeyListener(new View.OnKeyListener() {
		    @Override
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        // your custom implementation
		        if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
		            endVoltage = localeSpecificParsedDoubleString(mEndVoltageText.getText().toString());
		            EndVoltage_Check();
					clearTextFocuses();
		            return true; // indicate that we handled event, won't propagate it
		        }
		        return false; // when we don't handle other keys, propagate event further
		    }
		});
    	
    	mScanRateText.setOnKeyListener(new View.OnKeyListener() {
		    @Override
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        // your custom implementation
		        if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
		            scanRate = localeSpecificParsedDoubleString(mScanRateText.getText().toString());
		            ScanRate_Check();
					clearTextFocuses();
		            return true; // indicate that we handled event, won't propagate it
		        }
		        return false; // when we don't handle other keys, propagate event further
		    }
		});

        mStepEText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // your custom implementation
                if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
                    stepE = localeSpecificParsedIntString(mStepEText.getText().toString());
                    StepE_Check();
                    clearTextFocuses();
                    return true; // indicate that we handled event, won't propagate it
                }
                return false; // when we don't handle other keys, propagate event further
            }
        });

        mStepFrequencyText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // your custom implementation
                if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
                    stepFrequency = localeSpecificParsedIntString(mStepFrequencyText.getText().toString());
                    StepFrequency_Check();
                    clearTextFocuses();
                    return true; // indicate that we handled event, won't propagate it
                }
                return false; // when we don't handle other keys, propagate event further
            }
        });

        mPulseAmplitudeText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // your custom implementation
                if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
                    pulseAmplitude = localeSpecificParsedIntString(mPulseAmplitudeText.getText().toString());
                    PulseAmplitude_Check();
                    clearTextFocuses();
                    return true; // indicate that we handled event, won't propagate it
                }
                return false; // when we don't handle other keys, propagate event further
            }
        });
    	
    	mScanNumberText.setOnKeyListener(new View.OnKeyListener() {
		    @Override
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        // your custom implementation
		        if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
		            scanNumber = localeSpecificParsedIntString(mScanNumberText.getText().toString());
		            ScanNumber_Check();
					clearTextFocuses();
		            return true; // indicate that we handled event, won't propagate it
		        }
		        return false; // when we don't handle other keys, propagate event further
		    }
		});
    	
    	mEquilibriumTimeText.setOnKeyListener(new View.OnKeyListener() {
		    @Override
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        // your custom implementation
		        if (KeyEvent.KEYCODE_ENTER == keyCode) { // match ENTER key            {
		            equilibriumTime = localeSpecificParsedIntString(mEquilibriumTimeText.getText().toString());
		            EquilibriumTime_Check();
					clearTextFocuses();
		            return true; // indicate that we handled event, won't propagate it
		        }
		        return false; // when we don't handle other keys, propagate event further
		    }
		});

		mStartFrequencySlider.setMax(frequencySpinnerSpan);
		mEndFrequencySlider.setMax(frequencySpinnerSpan);
    	
    	mStartFrequencySlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
    			//startFrequency = progress + 5000;
				int frequencyExponent = progress - 1;
    			//StartFrequency_Check(progress);
				StartFrequency_Check(frequencyExponent);
    			clearTextFocuses();
    		}
    		public void onStartTrackingTouch(SeekBar seekBar) {
    			// TODO Auto-generated method stub
    		}
    		public void onStopTrackingTouch(SeekBar seekBar) {
    			
    		}
    	});
    	
    	mEndFrequencySlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
    			//endFrequency = progress + 5000;
				int frequencyExponent = progress - 1;
    			EndFrequency_Check(frequencyExponent);
    			clearTextFocuses();
    		}
    		public void onStartTrackingTouch(SeekBar seekBar) {
    			// TODO Auto-generated method stub
    		}
    		public void onStopTrackingTouch(SeekBar seekBar) {
    			
    		}
    	});
    	
    	mIntervalFrequencySlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
    			intervalFrequency = progress + 1;
    			IntervalFrequency_Check();
    			clearTextFocuses();
    		}
    		public void onStartTrackingTouch(SeekBar seekBar) {
    			// TODO Auto-generated method stub
    		}
    		public void onStopTrackingTouch(SeekBar seekBar) {
    			
    		}
    	});
    	
    	mBiasVoltageSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
    			biasVoltage = (progress - 1500.0) / 1000.0;
    			BiasVoltage_Check();
    			clearTextFocuses();
    		}
    		public void onStartTrackingTouch(SeekBar seekBar) {
    			// TODO Auto-generated method stub
    		}
    		public void onStopTrackingTouch(SeekBar seekBar) {
    			
    		}
    	});
    	
    	mStartVoltageSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
    			startVoltage = (progress - 1500.0) / 1000.0;;
    			StartVoltage_Check();
    			clearTextFocuses();
    		}
    		public void onStartTrackingTouch(SeekBar seekBar) {
    			// TODO Auto-generated method stub
    		}
    		public void onStopTrackingTouch(SeekBar seekBar) {
    			
    		}
    	});
    	
    	mEndVoltageSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
    			endVoltage = (progress - 1500.0) / 1000.0;;
    			EndVoltage_Check();
    			clearTextFocuses();
    		}
    		public void onStartTrackingTouch(SeekBar seekBar) {
    			// TODO Auto-generated method stub
    		}
    		public void onStopTrackingTouch(SeekBar seekBar) {
    			
    		}
    	});
    	
    	mScanRateSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
    			scanRate = (progress + 1.0) / 100.0;
    			ScanRate_Check();
    			clearTextFocuses();
    		}
    		public void onStartTrackingTouch(SeekBar seekBar) {
    			// TODO Auto-generated method stub
    		}
    		public void onStopTrackingTouch(SeekBar seekBar) {
    			
    		}
    	});

        mStepESlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                stepE = (progress + 1);
                StepE_Check();
                clearTextFocuses();
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mStepFrequencySlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                stepFrequency = (progress + 1);
                StepFrequency_Check();
                clearTextFocuses();
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mPulseAmplitudeSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                pulseAmplitude = (progress + 1);
                PulseAmplitude_Check();
                clearTextFocuses();
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    	
    	mScanNumberSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
    			scanNumber = progress + 1;
    			ScanNumber_Check();
    			clearTextFocuses();
    		}
    		public void onStartTrackingTouch(SeekBar seekBar) {
    			// TODO Auto-generated method stub
    		}
    		public void onStopTrackingTouch(SeekBar seekBar) {
    			
    		}
    	});
    	
    	mEquilibriumTimeSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
    			equilibriumTime = progress;
    			EquilibriumTime_Check();
    			clearTextFocuses();
    		}
    		public void onStartTrackingTouch(SeekBar seekBar) {
    			// TODO Auto-generated method stub
    		}
    		public void onStopTrackingTouch(SeekBar seekBar) {
    			
    		}
    	});
    	
    	clearTextFocuses();
    }

    public static double localeSpecificParsedDoubleString(String s) {
		NumberFormat numberStringParser = NumberFormat.getInstance(Locale.getDefault());
		try {
			return numberStringParser.parse(s).doubleValue();
		} catch (ParseException p) {
			p.printStackTrace();
			return 0.10;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0.10;
		}
	}

	public static int localeSpecificParsedIntString(String s) {
		NumberFormat numberStringParser = NumberFormat.getInstance(Locale.getDefault());
		try {
			return numberStringParser.parse(s).intValue();
		} catch (ParseException p) {
			p.printStackTrace();
			return 1;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 1;
		}
	}
    
    private void StartFrequency_Check(int value) {
		int endFrequencyExponent = (int) java.lang.Math.log10(endFrequency);
		NumberFormat nFormat = NumberFormat.getInstance(Locale.getDefault());
		nFormat.setMaximumFractionDigits(0);
		if (value > endFrequencyExponent) value = endFrequencyExponent;
		if (value > 5) value = 5;
		if (value < -1) value = -1;
		if (value == -1) nFormat.setMaximumFractionDigits(1);

		startFrequency = java.lang.Math.pow(10, value);
		mStartFrequencyText.setText(nFormat.format(startFrequency));
		mStartFrequencySlider.setProgress(value + 1);
    }
    
    private void EndFrequency_Check(int value) {
    	int startFrequencyExponent = (int) java.lang.Math.log10(startFrequency);
		NumberFormat nFormat = NumberFormat.getInstance(Locale.getDefault());
		nFormat.setMaximumFractionDigits(0);	// default frequencies above 0.1 don't show last digit
    	if (value < startFrequencyExponent) value = startFrequencyExponent;
		if (value < -1) value = -1;
		if (value == -1) {
			nFormat.setMaximumFractionDigits(1);
		}
    	else if (value > 5) value = 5;
		endFrequency = java.lang.Math.pow(10, value);
		mEndFrequencyText.setText(nFormat.format(endFrequency));
		mEndFrequencySlider.setProgress(value + 1);
    }
    
    private void IntervalFrequency_Check() {
    	if (intervalFrequency > 20) intervalFrequency = 20;
        else if (intervalFrequency < 1) intervalFrequency = 1;
		NumberFormat nFormat = NumberFormat.getInstance(Locale.getDefault());
        mIntervalFrequencyText.setText(nFormat.format(intervalFrequency));
        mIntervalFrequencySlider.setProgress(intervalFrequency - 1);
    }
    
    private void BiasVoltage_Check() {
    	if (biasVoltage > 1.5) biasVoltage = 1.5;
        else if (biasVoltage < -1.5) biasVoltage = -1.5;
		NumberFormat nf = NumberFormat.getInstance(Locale.getDefault()); // get instance
		nf.setMaximumFractionDigits(3); // set decimal places
		String biasString = nf.format(biasVoltage);
		int biasprogress = (int) ((biasVoltage + 1.5) * 1000);
        mBiasVoltageText.setText(biasString);
        mBiasVoltageSlider.setProgress(biasprogress);
    }
    
    private void StartVoltage_Check() {
    	if (Method.equals(SettingsDbAdapter.STATIC_LOGGING) && startVoltage > 1.5)
    		startVoltage = 1.5;
    	else if (electrodeConfiguration.equals(SettingsDbAdapter.OPEN_CIRCUIT_CONFIGURATION))
    		startVoltage = 0.0;
        else if (startVoltage < -1.5) startVoltage = -1.5;
		NumberFormat nf = NumberFormat.getInstance(Locale.getDefault()); // get instance
		nf.setMaximumFractionDigits(3); // set decimal places
		String startVoltString = nf.format(startVoltage);
		int startVoltprogress = (int) ((startVoltage + 1.5) * 1000);
        mStartVoltageText.setText(startVoltString);
        mStartVoltageSlider.setProgress(startVoltprogress);
    }
    
    private void EndVoltage_Check() {
    	if (endVoltage > 1.5) endVoltage = 1.5;
        else if (endVoltage < startVoltage) endVoltage = startVoltage;
		NumberFormat nf = NumberFormat.getInstance(Locale.getDefault()); // get instance
		nf.setMaximumFractionDigits(3); // set decimal places
		String endVoltString = nf.format(endVoltage);
		int endVoltprogress = (int) ((endVoltage + 1.5) * 1000);
        mEndVoltageText.setText(endVoltString);
        mEndVoltageSlider.setProgress(endVoltprogress);
    }
    
    private void ScanRate_Check() {
    	if (scanRate > 0.2) scanRate = 0.2;
        else if (scanRate < 0.01) scanRate = 0.01;
		NumberFormat nf = NumberFormat.getInstance(Locale.getDefault()); // get instance
		nf.setMaximumFractionDigits(2); // set decimal places
		String scanRateString = nf.format(scanRate);
		int scanRateprogress = (int) ((scanRate - 0.01) * 100);
        mScanRateText.setText(scanRateString);
        mScanRateSlider.setProgress(scanRateprogress);
    }

    private void StepE_Check() {
        if (stepE > 20) stepE = 20;
        if (stepE >= pulseAmplitude) stepE = pulseAmplitude - 1;    // make sure step potential is smaller than pulse amplitude
        else if (stepE < 1) stepE = 1;
        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault()); // get instance
        nf.setMaximumFractionDigits(0); // set decimal places
        String stepEString = nf.format(stepE);
        mStepEText.setText(stepEString);
        mStepESlider.setProgress(stepE - 1);
    }

    private void PulseAmplitude_Check() {
        if (pulseAmplitude > 50) pulseAmplitude = 50;
        if (pulseAmplitude <= stepE) pulseAmplitude = stepE + 1;    // make sure step potential is smaller than pulse amplitude
        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault()); // get instance
        nf.setMaximumFractionDigits(0); // set decimal places
        String pulseAmpString = nf.format(pulseAmplitude);
        mPulseAmplitudeText.setText(pulseAmpString);
        mPulseAmplitudeSlider.setProgress(pulseAmplitude - 1);
    }

    private void StepFrequency_Check() {
        if (stepFrequency > 50) stepFrequency = 50;
        if (stepFrequency < 1) stepFrequency = 1;    // make sure step potential is smaller than pulse amplitude
        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault()); // get instance
        nf.setMaximumFractionDigits(0); // set decimal places
        String stepFrequencyString = nf.format(stepFrequency);
        mStepFrequencyText.setText(stepFrequencyString);
        mStepFrequencySlider.setProgress(stepFrequency - 1);
    }
    
    private void ScanNumber_Check() {
    	if (scanNumber > 30) scanNumber = 30;
        else if (scanNumber < 1) scanNumber = 1;
        mScanNumberText.setText(Integer.toString(scanNumber));
        mScanNumberSlider.setProgress(scanNumber - 1);
    }
    
    private void EquilibriumTime_Check() {
    	if (equilibriumTime > 60) equilibriumTime = 60;
        else if (equilibriumTime < 0) equilibriumTime = 0;
        mEquilibriumTimeText.setText(Integer.toString(equilibriumTime));
        mEquilibriumTimeSlider.setProgress(equilibriumTime);
    }

    /*
    remove open circuit radio button, and check three electrode configuration button if user selects any
    method for which open circuit configuration shouldn't be an option
     */
    private void removeOpenCircuitButton() {
		findViewById(R.id.open_circuit_button).setVisibility(View.GONE);
		if (electrodeConfiguration.equals(SettingsDbAdapter.OPEN_CIRCUIT_CONFIGURATION)) {
			electrodeConfiguration = SettingsDbAdapter.THREE_ELECTRODE_CONFIGURATION;
			RadioButton rb = (RadioButton) findViewById(R.id.three_electrode_button);
			rb.setChecked(true);
		}
	}
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	//saveState();
    	outState.putSerializable(SettingsDbAdapter.KEY_ROWID, mRowId);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	//saveState();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();

    	populateFields();
    }
    
    /*
     * If no RowId exists, create new method with new RowId in SettingsDbAdapter
     * and record the newly assigned RowId (to send back to this activity OnResume()
     * Otherwise update row in database with given RowId
     * EnforceLimits is called on each bit of retrieved data to make sure it remains within the prescribed limits
     */
    private void saveState() {
    	Title = mTitleText.getText().toString();
    	String startFrequencyString = mStartFrequencyText.getText().toString();
    	String endFrequencyString = mEndFrequencyText.getText().toString();
    	String intervalFrequencyString = mIntervalFrequencyText.getText().toString();
    	String biasVoltageString = mBiasVoltageText.getText().toString();
    	String startVoltageString = mStartVoltageText.getText().toString();
    	String endVoltageString = mEndVoltageText.getText().toString();
    	String scanRateString = mScanRateText.getText().toString();
        String stepEString = mStepEText.getText().toString();
        String pulseAmplitudeString = mPulseAmplitudeText.getText().toString();
        String stepFrequencyString = mStepFrequencyText.getText().toString();
		scanNumberString = mScanNumberText.getText().toString(); // populate scan number from corresponding EditText
		if (mInfiniteScans.isChecked())  scanNumberString = SettingsDbAdapter.INFINITE; // unless infinite scans is selected...
		String equilibriumTimeString = mEquilibriumTimeText.getText().toString();

		if (mRowId == null) mDbHelper.createMethod(Title, Method, startFrequencyString, endFrequencyString,
				intervalFrequencyString, signalAmplitude, biasVoltageString, startVoltageString, endVoltageString,
				scanRateString, scanNumberString, stepFrequencyString, stepEString, pulseAmplitudeString,
				electrodeConfiguration, equilibriumTimeString, "extra", "extra", "extra", "extra", "extra", "extra");
		else mDbHelper.updateMethod(mRowId, Title, Method, startFrequencyString, endFrequencyString,
				intervalFrequencyString, signalAmplitude, biasVoltageString, startVoltageString, endVoltageString,
				scanRateString, scanNumberString, stepFrequencyString, stepEString, pulseAmplitudeString,
				electrodeConfiguration, equilibriumTimeString, "extra", "extra", "extra", "extra", "extra", "extra");
    }
}
