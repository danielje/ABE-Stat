package com.diagenetix.abestat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple database access helper class to select, modify, or create a setting
 * for real-time analysis with assorted electrochemical / electrical measurements.
 */
public class SettingsDbAdapter {
	// for Debugging...
	private static final boolean D = true;
	private static final String TAG = "SettingsDbAdapter";
	
	// Keys for data fields in each method
    public static final String KEY_TITLE = "title";
    public static final String KEY_METHOD = "analytical_method";
    public static final String KEY_START_FREQUENCY = "start_f";
    public static final String KEY_END_FREQUENCY = "end_f";
    public static final String KEY_INTERVAL_FREQUENCY = "delta_f";
    public static final String KEY_SIGNAL_AMPLITUDE = "ac_amplitude";
    public static final String KEY_BIAS_VOLTAGE = "bias_v";
    public static final String KEY_START_VOLTAGE = "start_v";
    public static final String KEY_END_VOLTAGE = "end_v";
    public static final String KEY_SCAN_RATE = "scan_rate";
    public static final String KEY_SCAN_NUMBER = "scan_number";
    public static final String KEY_STEP_FREQUENCY = "step_freq";
    public static final String KEY_STEP_E = "step_e";
    public static final String KEY_PULSE_AMPLITUDE = "pulse_amplitude";
    public static final String KEY_ELECTRODE_CONFIGURATION = "e_config";
    public static final String KEY_EQUILIBRIUM_TIME = "equil_time";
    
    public static final String KEY_EXTRA01 = "extra01";	// extra database entries reserved for future use...
    public static final String KEY_EXTRA02 = "extra02";
    public static final String KEY_EXTRA03 = "extra03";
    public static final String KEY_EXTRA04 = "extra04";
    public static final String KEY_EXTRA05 = "extra05";
    public static final String KEY_EXTRA06 = "extra06";
    
    public static final String KEY_ROWID = "_id";

    public static final String TRUE = "true";
    public static final String FALSE = "false";
    public static final String CYCLIC_VOLTAMMETRY = "cv";
    public static final String ELECTROCHEMICAL_IMPEDANCE_SPECTROSCOPY = "eis";
    public static final String DIFFERENTIAL_PULSE_VOLTAMMETRY = "dpv";
    public static final String ANODIC_STRIPPING_VOLTAMMETRY = "asv";
    public static final String STATIC_LOGGING = "slo";
    public static final String TWO_ELECTRODE_CONFIGURATION = "2ec";
    public static final String THREE_ELECTRODE_CONFIGURATION = "3ec";
    public static final String OPEN_CIRCUIT_CONFIGURATION = "0ec";

    public static final String INFINITE = "infinite";
    
    // ORDER BY clause for sqlite database (sort database query alphabetically by "title" column)
//    private static final String Order_By_Clause = KEY_TITLE;

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final String DATABASE_NAME = "abestat_notes";
    private static final String DATABASE_TABLE = "abestat_methods";
    private static final int DATABASE_VERSION = 2;
    
    /**
     * Database creation sql statement- create database with given non-null column fields...
     */
//    private static final String DATABASE_CREATE =
//        "create table " + DATABASE_TABLE + " (" + KEY_ROWID + " integer primary key autoincrement, " + KEY_TITLE + " text not null, " + KEY_RXTEMP + " text not null, " + KEY_RXTIME + " text not null, " + KEY_DNTEMP + " text not null, " + KEY_DNTIME + " text not null, " + KEY_DATAINTERVAL + " text not null);";
    private static final String DATABASE_CREATE =
        "create table abestat_methods (_id integer primary key autoincrement, title text not null, analytical_method text not null, start_f text not null, end_f text not null,"
        + "delta_f text not null, ac_amplitude text not null, bias_v text not null, start_v text not null, end_v text not null, scan_rate text not null,"
        + "scan_number text not null, step_freq text not null, step_e text not null, pulse_amplitude text not null, e_config text not null, equil_time text not null,"
        + "extra01 text not null, extra02 text not null, extra03 text not null, extra04 text not null, extra05 text not null, extra06 text not null);";
    
    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public SettingsDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the methods database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public SettingsDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new method setting with title and settings provided. If the method is
     * successfully created return the new rowId for that method, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the method
     * @return rowId or -1 if failed
     */
    public long createMethod(String title, String method, String start_f, String end_f,
    		String delta_f, String ac_amp, String bias_v, String start_v, String end_v,
    		String scan_rate, String scan_number, String step_freq, String step_e,
    		String pulse_amplitude, String e_config, String equil_time, String extra01, String extra02,
    		String extra03, String extra04, String extra05, String extra06) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_METHOD, method);
        initialValues.put(KEY_START_FREQUENCY, start_f);
        initialValues.put(KEY_END_FREQUENCY, end_f);
        initialValues.put(KEY_INTERVAL_FREQUENCY, delta_f);
        initialValues.put(KEY_SIGNAL_AMPLITUDE, ac_amp);
        initialValues.put(KEY_BIAS_VOLTAGE, bias_v);
        initialValues.put(KEY_START_VOLTAGE, start_v);
        initialValues.put(KEY_END_VOLTAGE, end_v);
        initialValues.put(KEY_SCAN_RATE, scan_rate);
        initialValues.put(KEY_SCAN_NUMBER, scan_number);
        initialValues.put(KEY_STEP_FREQUENCY, step_freq);
        initialValues.put(KEY_STEP_E, step_e);
        initialValues.put(KEY_PULSE_AMPLITUDE, pulse_amplitude);
        initialValues.put(KEY_ELECTRODE_CONFIGURATION, e_config);
        initialValues.put(KEY_EQUILIBRIUM_TIME, equil_time);
        initialValues.put(KEY_EXTRA01, extra01);
        initialValues.put(KEY_EXTRA02, extra02);
        initialValues.put(KEY_EXTRA03, extra03);
        initialValues.put(KEY_EXTRA04, extra04);
        initialValues.put(KEY_EXTRA05, extra05);
        initialValues.put(KEY_EXTRA06, extra06);

        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }

    /**
     * Delete the method with the given rowId
     * 
     * @param rowId id of method to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteMethod(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all methods in the database
     * 
     * @return Cursor over all methods
     */
    public Cursor fetchAllMethods() {
        if(D) Log.e(TAG, "about to return cursor with data");

        String[] Data_Cols = new String[] {KEY_ROWID, KEY_TITLE, KEY_METHOD, KEY_START_FREQUENCY, KEY_END_FREQUENCY,
        		KEY_INTERVAL_FREQUENCY, KEY_SIGNAL_AMPLITUDE, KEY_BIAS_VOLTAGE, KEY_START_VOLTAGE, KEY_END_VOLTAGE, KEY_SCAN_RATE,
        		KEY_SCAN_NUMBER, KEY_STEP_FREQUENCY, KEY_STEP_E, KEY_PULSE_AMPLITUDE, KEY_ELECTRODE_CONFIGURATION,
                KEY_EQUILIBRIUM_TIME, KEY_EXTRA01, KEY_EXTRA02, KEY_EXTRA03, KEY_EXTRA04, KEY_EXTRA05, KEY_EXTRA06};
        Cursor answer = mDb.query(DATABASE_TABLE, Data_Cols, null, null, null, null, KEY_TITLE);
        if(D) Log.e(TAG, "returned from database with cursor");
        return answer;
    }

    /**
     * Return a Cursor positioned at the method that matches the given rowId
     * 
     * @param rowId id of method to retrieve
     * @return Cursor positioned to matching method, if found
     * @throws SQLException if method could not be found/retrieved
     */
    public Cursor fetchMethod(long rowId) throws SQLException {

        if(D) Log.e(TAG, "about to get cursor in fetchmethod");
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE, KEY_METHOD, KEY_START_FREQUENCY, KEY_END_FREQUENCY,
                            KEY_INTERVAL_FREQUENCY, KEY_SIGNAL_AMPLITUDE, KEY_BIAS_VOLTAGE, KEY_START_VOLTAGE, KEY_END_VOLTAGE, KEY_SCAN_RATE,
                            KEY_SCAN_NUMBER, KEY_STEP_FREQUENCY, KEY_STEP_E, KEY_PULSE_AMPLITUDE, KEY_ELECTRODE_CONFIGURATION,
                            KEY_EQUILIBRIUM_TIME, KEY_EXTRA01, KEY_EXTRA02, KEY_EXTRA03, KEY_EXTRA04, KEY_EXTRA05, KEY_EXTRA06},
                    KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the method using the details provided. The method to be updated is
     * specified using the rowId, and it is altered to use the data
     * values passed in
     * 
     * @param rowId id of method to update
     * @return true if the method was successfully updated, false otherwise
     */
    public boolean updateMethod(long rowId, String title, String method, String start_f, String end_f,
                                String delta_f, String ac_amp, String bias_v, String start_v, String end_v,
                                String scan_rate, String scan_number, String step_freq, String step_e,
                                String pulse_amplitude, String e_config, String equil_time, String extra1, String extra2, String extra3,
                                String extra4, String extra5, String extra6) {
        ContentValues args = new ContentValues();
        args.put(KEY_TITLE, title);
        args.put(KEY_METHOD, method);
        args.put(KEY_START_FREQUENCY, start_f);
        args.put(KEY_END_FREQUENCY, end_f);
        args.put(KEY_INTERVAL_FREQUENCY, delta_f);
        args.put(KEY_SIGNAL_AMPLITUDE, ac_amp);
        args.put(KEY_BIAS_VOLTAGE, bias_v);
        args.put(KEY_START_VOLTAGE, start_v);
        args.put(KEY_END_VOLTAGE, end_v);
        args.put(KEY_SCAN_RATE, scan_rate);
        args.put(KEY_SCAN_NUMBER, scan_number);
        args.put(KEY_STEP_FREQUENCY, step_freq);
        args.put(KEY_STEP_E, step_e);
        args.put(KEY_PULSE_AMPLITUDE, pulse_amplitude);
        args.put(KEY_ELECTRODE_CONFIGURATION, e_config);
        args.put(KEY_EQUILIBRIUM_TIME, equil_time);
        args.put(KEY_EXTRA01, extra1);
        args.put(KEY_EXTRA02, extra2);
        args.put(KEY_EXTRA03, extra3);
        args.put(KEY_EXTRA04, extra4);
        args.put(KEY_EXTRA05, extra5);
        args.put(KEY_EXTRA06, extra6);
        //args.put(KEY_GREENGAIN, green_gain);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
