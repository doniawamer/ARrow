package com.wikitude.samples;

import java.io.File;

import android.Manifest;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.wikitude.sdksamples.R;

public class MainSamplesListActivity extends ListActivity {

	public static final String EXTRAS_KEY_ACTIVITY_TITLE_STRING = "activityTitle";
	public static final String EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL = "activityArchitectWorldUrl";

	public static final String EXTRAS_KEY_ACTIVITY_IR = "activityIr";
	public static final String EXTRAS_KEY_ACTIVITY_GEO = "activityGeo";

	public static final String EXTRAS_KEY_ACTIVITIES_ARCHITECT_WORLD_URLS_ARRAY = "activitiesArchitectWorldUrls";
	public static final String EXTRAS_KEY_ACTIVITIES_TILES_ARRAY = "activitiesTitles";
	public static final String EXTRAS_KEY_ACTIVITIES_CLASSNAMES_ARRAY = "activitiesClassnames";

	public static final String EXTRAS_KEY_ACTIVITIES_IR_ARRAY = "activitiesIr";
	public static final String EXTRAS_KEY_ACTIVITIES_GEO_ARRAY = "activitiesGeo";

	private static final int WIKITUDE_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int WIKITUDE_PERMISSIONS_REQUEST_GPS = 2;
	private int _lastSelectedListItemPosition = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(this.getContentViewId());

		this.setTitle(this.getActivityTitle());

		/* extract names of samples from res/arrays */
		final String[] values = this.getListLabels();

		/* use default list-ArrayAdapter */
		this.setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, android.R.id.text1, values));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		/* get className of activity to call when clicking item at position x */
		_lastSelectedListItemPosition = position;

		if ( ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
			ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, WIKITUDE_PERMISSIONS_REQUEST_CAMERA);
		} else {
			if (this.getActivitiesGeo()[_lastSelectedListItemPosition]) {
				if ( ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
					ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, WIKITUDE_PERMISSIONS_REQUEST_GPS);
				} else {
					loadExample();
				}
			} else {
				loadExample();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case WIKITUDE_PERMISSIONS_REQUEST_CAMERA: {
				if ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    if ( this.getActivitiesGeo()[_lastSelectedListItemPosition] ) {
                        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, WIKITUDE_PERMISSIONS_REQUEST_GPS);
                        }
                    } else {
                        loadExample();
                    }
				} else {
					Toast.makeText(this, "Sorry, augmented reality doesn't work without reality.\n\nPlease grant camera permission.", Toast.LENGTH_LONG).show();
				}
				return;
			}
            case WIKITUDE_PERMISSIONS_REQUEST_GPS: {
                if ( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    loadExample();
                } else {
                    Toast.makeText(this, "Sorry, this example requires access to your location in order to work properly.\n\nPlease grant location permission.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
		}
	}

	protected final String[] getListLabels() {
		return getIntent().getExtras().getStringArray(
				EXTRAS_KEY_ACTIVITIES_TILES_ARRAY);
	}

	protected String getActivityTitle() {
		return getIntent().getExtras().getString(
				EXTRAS_KEY_ACTIVITY_TITLE_STRING);
	}

	protected String[] getListActivities() {
		return getIntent().getExtras().getStringArray(
				EXTRAS_KEY_ACTIVITIES_CLASSNAMES_ARRAY);
	}

	protected String[] getArchitectWorldUrls() {
		return getIntent().getExtras().getStringArray(
				EXTRAS_KEY_ACTIVITIES_ARCHITECT_WORLD_URLS_ARRAY);
	}

	protected boolean[] getActivitiesIr() {
		return getIntent().getExtras().getBooleanArray(
				EXTRAS_KEY_ACTIVITIES_IR_ARRAY);
	}
	
	protected boolean[] getActivitiesGeo() {
		return getIntent().getExtras().getBooleanArray(
				EXTRAS_KEY_ACTIVITIES_GEO_ARRAY);
	}
	
	protected int getContentViewId() {
		return R.layout.list_sample;
	}

	private void loadExample() {
		try {

			if ( _lastSelectedListItemPosition >= 0 ) {

				final String className = getListActivities()[_lastSelectedListItemPosition];
				final Intent intent = new Intent(this, Class.forName(className));
				intent.putExtra(EXTRAS_KEY_ACTIVITY_TITLE_STRING,
						this.getListLabels()[_lastSelectedListItemPosition]);
				intent.putExtra(EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL, "samples"
						+ File.separator + this.getArchitectWorldUrls()[_lastSelectedListItemPosition]
						+ File.separator + "index.html");
				intent.putExtra(EXTRAS_KEY_ACTIVITY_IR,
						this.getActivitiesIr()[_lastSelectedListItemPosition]);
				intent.putExtra(EXTRAS_KEY_ACTIVITY_GEO,
						this.getActivitiesGeo()[_lastSelectedListItemPosition]);

				/* launch activity */
				this.startActivity(intent);
			}

		} catch (Exception e) {
			/*
			 * may never occur, as long as all SampleActivities exist and are
			 * listed in manifest
			 */
			final String className = getListActivities()[_lastSelectedListItemPosition];
			Toast.makeText(this, className + "\nnot defined/accessible",
					Toast.LENGTH_SHORT).show();
		}
	}
}
