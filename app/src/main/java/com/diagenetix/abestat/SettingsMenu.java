package com.diagenetix.abestat;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/*
 * Methods management menu (Activity), enables user to create or edit new methods through SettingsEdit Activity.
 */
public class SettingsMenu extends ListActivity {
	// for Debugging...
	private static final String TAG = "SettingsMenu";
	private static final boolean D = true;
	
    private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;

    // Note, FIRST is defined in menu class as the first integer value for group and item identifier integers
    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int CANCEL_ID = Menu.FIRST + 2;

    private SettingsDbAdapter mDbHelper;
    private Button addButton;
    private Button exitButton;
    private TextView mmTitle;

    private String Title;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	if(D) Log.e(TAG, "+++ ON CREATE +++");

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.methods_list);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
//        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the custom title
        mmTitle = (TextView) findViewById(R.id.title_left_text);
        mmTitle.setText(R.string.app_name);
        mmTitle = (TextView) findViewById(R.id.title_right_text);
        mmTitle.setText(R.string.analysis);

        if(D) Log.e(TAG, " about to declare SettingsDbAdapter...");
        mDbHelper = new SettingsDbAdapter(this);
        if(D) Log.e(TAG, "about to open SettingsDbAdapter");
        mDbHelper.open();
        if(D) Log.e(TAG, "about to set up list of methods...");

        Title = getIntent().getStringExtra(SettingsDbAdapter.KEY_TITLE);

        fillData();

        Button addButton = (Button) findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                createMethod();
            }
        });

        // Set up exit button to close database, leave activity and return to main screen
        //  No intent needed since no data needs to be passed
        //  (all manipulated data is available in accessible database mDbSettings)
        Button exitButton = (Button) findViewById(R.id.exit_button);
        exitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	Intent quitIntent = new Intent();
                quitIntent.putExtra(SettingsDbAdapter.KEY_TITLE, Title);
                //Log.e(TAG, "exiting with method " + Title);
            	setResult(RESULT_OK, quitIntent);
                finish();
            }
        });

        if(D) Log.e(TAG, "registering for context menu");
        registerForContextMenu(getListView());
        if(D) Log.e(TAG, "finished on create");
    }

    private void fillData() {
        // Get all of the rows from the database and create the item list
    	if(D) Log.e(TAG, "starting fillData()");
        Cursor methodsCursor = mDbHelper.fetchAllMethods();
        if(D) Log.e(TAG, "all methods fetched and assigned to cursor");
        //startManagingCursor(methodsCursor);

        if(D) Log.e(TAG, "now managing cursor");

        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{SettingsDbAdapter.KEY_TITLE};

        if(D) Log.e(TAG, "about to set up list of methods...");

        // and an array of the fields we want to bind those fields to (in this case just text1
        //  or the row formatting from methods_row.xml)
        int[] to = new int[]{R.id.text1};

        // Now create a simple cursor adapter and set it to display in UI
        int flags = 0;
        SimpleCursorAdapter methods =
            new SimpleCursorAdapter(this, R.layout.methods_row, methodsCursor, from, to, flags);
        setListAdapter(methods);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 1, R.string.menu_insert);
        menu.add(0, CANCEL_ID, 2, R.string.cancel);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case INSERT_ID:
                createMethod();
                return true;
            case CANCEL_ID:
            	return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 1, R.string.menu_delete);
        menu.add(0, CANCEL_ID, 2, R.string.cancel);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case DELETE_ID:
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                mDbHelper.deleteMethod(info.id);
                fillData();
                return true;
            case CANCEL_ID:
            	return true;
        }
        return super.onContextItemSelected(item);
    }

    private void createMethod() {
        Intent i = new Intent(this, SettingsEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, SettingsEdit.class);
        i.putExtra(SettingsDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK) {
        	Title = intent.getStringExtra(SettingsDbAdapter.KEY_TITLE);
        	//Log.e(TAG, "returned with method " + Title);
        	fillData();
        }
    }
}
