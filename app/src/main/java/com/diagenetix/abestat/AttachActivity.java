package com.diagenetix.abestat;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import 	android.support.v4.content.FileProvider;

import com.diagenetix.widget.InteractiveArrayAdapter;
import com.diagenetix.widget.ModelCheckItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class AttachActivity extends ListActivity {
	
	private final int VIEWDATA = 0;
	
	private final boolean D = true;
	private final String TAG = "AttachActivity";
	
	private List<ModelCheckItem> Files_Available;
	private ListView LView;
	private ListAdapter adapter;
	
	private TextView mTitle;
	private Button shareOptionsButton;
	
	private boolean GrayScale = false;
	
	public static final File dir = new File(Environment.getExternalStorageDirectory().getPath()+"/ABE-Stat/");
	private static final String path = (Environment.getExternalStorageDirectory().getPath() + "/ABE-Stat/");

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    if(D) Log.e(TAG, "+++ ON CREATE +++");
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.attach_list);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        mTitle.setText(R.string.file_sharing);
        if(D) Log.e(TAG, "Finished with custom title...");
        
        UI_Setup();
	}
	
	/*
	 * setup user interface/ check boxes...
	 */
	private void UI_Setup()
	{
		//ArrayAdapter<ModelCheckItem> adapter = new InteractiveArrayAdapter(this, getModel());
		Files_Available = new ArrayList<ModelCheckItem>();
		for (String filename : dir.list()) {
			Files_Available.add(new ModelCheckItem(filename));
		}
		ArrayAdapter<ModelCheckItem> adapter = new InteractiveArrayAdapter(this, Files_Available);
		setListAdapter(adapter);

		
		// Get some views for later use
				// --
		LView = getListView();
		LView.setItemsCanFocus(false);
		
		shareOptionsButton = (Button) findViewById(R.id.share_options_button);
		shareOptionsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View x) {
				showPopup(x);
			}
		});
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
					case R.id.email_files:
						SendEmail();
						return true;
					case R.id.open_file:
						Data_View();
						return true;
					case R.id.delete:
						deleteFiles();
						return true;
					case R.id.return_to_main:
						finish();
						return true;
				}
				return false;
			}
		});
		popup.inflate(R.menu.share_options_menu);
		Menu pmenu = popup.getMenu();
		popup.show();
	}
	
	/* 
	 * Function returns an integer array, with first element as number of checked files, 
	 * and second element as index of last checked file
	 */
	private int[] CountCheckedFiles ()
	{
		int checkedFilesCount[] = {0, 0};
		
		for (int i = 0; i < Files_Available.size(); i++) {
			if (Files_Available.get(i).isSelected())
			{
				checkedFilesCount[0]++;
				checkedFilesCount[1] = i;
			}
		}
		return checkedFilesCount;
	}
	
	/* 
	 * View Button and Data_View_Activity added as part of new software version
	 * allow user option to view data stored in files.
	 */
	private void Data_View()
	{
		int checkedFilesCount[] = CountCheckedFiles();
		switch (checkedFilesCount[0])
		{
		case 0:{
			Toast.makeText(this, getString(R.string.no_data_to_view).toString(),
					Toast.LENGTH_SHORT).show();
			break;
		}
		case 1:{
			String filePathString = path + Files_Available.get(checkedFilesCount[1]).getName();
			Intent viewIntent = new Intent(this, Data_View_Activity.class);
			viewIntent.putExtra(Data_View_Activity.FILEPATHSTRING, filePathString);
			startActivityForResult(viewIntent, VIEWDATA);
			break;
		}
		default:
			Toast.makeText(this, getString(R.string.too_much_data_to_view).toString(),
					Toast.LENGTH_SHORT).show();
		}
	}
	
	private void SendEmail() {
		int checkedFilesCount[] = CountCheckedFiles();
		switch (checkedFilesCount[0])
		{
		case 0:{
			Toast.makeText(this, getString(R.string.no_data_to_share).toString(),
					Toast.LENGTH_SHORT).show();
			break;
		}
		default:{
			Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
	        ArrayList<Uri> uris = new ArrayList<Uri>();
	        //convert from paths to Android friendly Parcelable Uri's

	        for (int i = 0; i < Files_Available.size(); i++) {
				if (Files_Available.get(i).isSelected())
				{
					File fileIn = new File(path + Files_Available.get(i).getName());
					//Uri u = Uri.fromFile(fileIn);
					Uri u = FileProvider.getUriForFile(this,"com.diagenetix.abestat.provider", fileIn);
					uris.add(u);
				}
			}
	        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
	        emailIntent.setType("text/plain");
	        String mailId="";
	        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{mailId});
	        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "ABE-Stat Data");
	        this.startActivity(Intent.createChooser(emailIntent, "Send Email.."));
			break;
		}
		}
	}

	private void deleteFiles() {
		int checkedFilesCount[] = CountCheckedFiles();
		switch (checkedFilesCount[0])
		{
			case 0:{
				Toast.makeText(this, getString(R.string.no_files_to_delete).toString(),
						Toast.LENGTH_SHORT).show();
				break;
			}
			default:{
				new AlertDialog.Builder(this)
						.setTitle(getString(R.string.delete_dialog).toString())
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								for (int i = 0; i < Files_Available.size(); i++) {
									if (Files_Available.get(i).isSelected())
									{
										File fileIn = new File(path + Files_Available.get(i).getName());
										fileIn.delete();
									}
								}
								UI_Setup();	// need to reconfigure setup if we've deleted files...
							}
						})
						.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								dialog.dismiss();
							}
						})
						.create()
						.show();
				break;
			}
		}
	}
}
