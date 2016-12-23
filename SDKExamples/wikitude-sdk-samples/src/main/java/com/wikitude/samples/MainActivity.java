package com.wikitude.samples;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.ListActivity;
import android.content.Intent;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.StartupConfiguration;
import com.wikitude.sdksamples.R;
import com.wikitude.tools.device.features.MissingDeviceFeatures;


/**
 * Activity launched when pressing app-icon.
 * It uses very basic ListAdapter for UI representation
 */
public class MainActivity extends ListActivity{

	private Map<Integer, List<SampleMeta>> samples;

	private Set<String> irSamples;
	private Set<String> geoSamples;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );

		irSamples = getListFrom("samples/samples_ir.lst");
		geoSamples = getListFrom("samples/samples_geo.lst");

		this.setContentView( this.getContentViewId() );

		// ensure to clean cache when it is no longer required
		MainActivity.deleteDirectoryContent ( ArchitectView.getCacheDirectoryAbsoluteFilePath(this) );

		// extract names of samples from res/arrays
		final String[] values = this.getListLabels();

		// use default list-ArrayAdapter */
		this.setListAdapter( new ArrayAdapter<String>( this, android.R.layout.simple_list_item_1, android.R.id.text1, values ) );
		TextView t = (TextView) findViewById(R.id.textView1);
		String versionInfo = "SDK build date: " + ArchitectView.getBuildProperty("build.date");
		t.setText(versionInfo);
	}

	private Set<String> getListFrom(String fname) {
		HashSet<String> data = new HashSet<String>();
		try {
			BufferedReader burr = new BufferedReader(new InputStreamReader(getAssets().open(fname)));
			String line;
			while ((line = burr.readLine()) != null) {
				data.add(line);
			}
			burr.close();
		} catch (FileNotFoundException e) {
			Log.w("Wikitude SDK Samples", "Can't read list from file " + fname);
		} catch (IOException e) {
			Log.w("Wikitude SDK Samples", "Can't read list from file " + fname);
		}
		return data;
	}

	@Override
	protected void onListItemClick( ListView l, View v, int position, long id ) {
		super.onListItemClick( l, v, position, id );

			final Intent intent = new Intent( this, MainSamplesListActivity.class );

			final List<SampleMeta> activitiesToLaunch = getActivitiesToLaunch(position);
			final String activityTitle = activitiesToLaunch.get(0).categoryId + ". " + activitiesToLaunch.get(0).categoryName.replace("$", " ");
			String[] activityTitles = new String[activitiesToLaunch.size()];
			String[] activityUrls = new String[activitiesToLaunch.size()];
			String[] activityClasses = new String[activitiesToLaunch.size()];

			boolean[] activitiesIr = new boolean[activitiesToLaunch.size()];
			boolean[] activitiesGeo = new boolean[activitiesToLaunch.size()];

			// check if AR.VideoDrawables are supported on the current device. if not -> show hint-Toast message
			if (activitiesToLaunch.get(0).categoryName.contains("Video") && ! MainActivity.isVideoDrawablesSupported()) {
				Toast.makeText(this, R.string.videosrawables_fallback, Toast.LENGTH_LONG).show();
			}

			// find out which Activity to launch when sample row was pressed, some handle document.location = architectsdk:// events, others inject poi data from native via javascript
			for (int i= 0; i< activitiesToLaunch.size(); i++) {
				final SampleMeta meta = activitiesToLaunch.get(i);

				activityTitles[i] = (meta.categoryId + "." + meta.sampleId + " " + meta.sampleName.replace("$", " "));
				activityUrls[i] = meta.path;
				activitiesIr[i] = meta.hasIr;
				activitiesGeo[i] = meta.hasGeo;

				if (meta.categoryId.equals("5") && meta.sampleId==1) {
					activityClasses[i] = ("com.wikitude.samples.SampleCamContentFromNativeActivity");
				} else if (meta.categoryId.equals("8") && meta.sampleId==1) {
					activityClasses[i] = ("com.wikitude.samples.SampleFrontCamActivity");
				} else if (meta.categoryId.equals("9") && meta.sampleId==1) {
					activityClasses[i] = ("com.wikitude.samples.SamplePluginActivity");
                } else if (meta.categoryId.equals("9") && meta.sampleId==2) {
                    activityClasses[i] = ("com.wikitude.samples.FaceDetectionPluginActivity");
				} else if (meta.categoryId.equals("9") && meta.sampleId==3) {
					activityClasses[i] = ("com.wikitude.samples.CustomCameraActivity");
				} else if (meta.categoryId.equals("9") && meta.sampleId==4) {
					activityClasses[i] = ("com.wikitude.samples.MarkerTrackingPluginActivity");
				} else {
					activityClasses[i] = ("com.wikitude.samples.SampleCamActivity");
				}
			}

			intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_ARCHITECT_WORLD_URLS_ARRAY, activityUrls);
			intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_CLASSNAMES_ARRAY, activityClasses);
			intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_TILES_ARRAY, activityTitles);
			intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_IR_ARRAY, activitiesIr);
			intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITIES_GEO_ARRAY, activitiesGeo);
			intent.putExtra(MainSamplesListActivity.EXTRAS_KEY_ACTIVITY_TITLE_STRING, activityTitle);

			/* launch activity */
			this.startActivity( intent );

	}

	protected final String[] getListLabels() {
		boolean includeIR = (ArchitectView.getSupportedFeaturesForDevice(getApplicationContext()) & StartupConfiguration.Features.Tracking2D) != 0;
		boolean includeGeo = (ArchitectView.getSupportedFeaturesForDevice(getApplicationContext()) & StartupConfiguration.Features.Geo) != 0;

		MissingDeviceFeatures missingDeviceFeatures = ArchitectView.isDeviceSupported(this, StartupConfiguration.Features.Tracking2D | StartupConfiguration.Features.Geo);

		if (missingDeviceFeatures.areFeaturesMissing()) {
			Toast toast =  Toast.makeText(this, missingDeviceFeatures.getMissingFeatureMessage() +
					"Because of this some samples may not be visible.", Toast.LENGTH_LONG);
			toast.show();
		}

		samples = getActivitiesToLaunch(includeIR, includeGeo);
		final String[] labels = new String[samples.keySet().size()];
		for (int i = 0; i<labels.length; i++) {
			labels[i] = samples.get(i).get(0).categoryId + ". " + samples.get(i).get(0).categoryName.replace("$", " ");
		}
		return labels;
	}

	protected int getContentViewId() {
		return R.layout.list_startscreen;
	}

	public void buttonClicked(final View view)
	 {
		try {
			this.startActivity( new Intent( this, Class.forName( "com.wikitude.samples.utils.urllauncher.ARchitectUrlLauncherActivity" ) ) );
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	 }

	/**
	 * deletes content of given directory
	 * @param path
	 */
	private static void deleteDirectoryContent(final String path) {
		try {
			final File dir = new File (path);
			if (dir.exists() && dir.isDirectory()) {
				final String[] children = dir.list();
		        for (int i = 0; i < children.length; i++) {
		            new File(dir, children[i]).delete();
		        }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<SampleMeta> getActivitiesToLaunch(final int position){
		return samples.get(position);
	}

	private Map<Integer, List<SampleMeta>> getActivitiesToLaunch(boolean includeIR, boolean includeGeo){
		final Map<Integer, List<SampleMeta>> pos2activites = new HashMap<Integer, List<SampleMeta>>();

		String[] assetsIWant;

		try {
			assetsIWant = getAssets().list("samples");
			int pos = -1;
			String lastCategoryId = "";
			for(final String asset: assetsIWant) {
				if (!asset.substring(asset.length() - 4).contains(".")) {
					try {
						// don't include sample if it requires IR functionality on
						// devices which don't support it.
						boolean needIr = irSamples.contains(asset);
						boolean needGeo = geoSamples.contains(asset);
						if ((includeIR || !needIr) && (includeGeo || !needGeo)) {
							SampleMeta sampleMeta = new SampleMeta(asset, needIr, needGeo);
							if (!sampleMeta.categoryId.equals(lastCategoryId)) {
								pos++;
								pos2activites.put(pos, new ArrayList<SampleMeta>());
							}
							pos2activites.get(pos).add(sampleMeta);
							lastCategoryId = sampleMeta.categoryId;
						}
					} catch (IllegalArgumentException e) {
						// Log.e("Ignored Asset to load", asset + " invalid: "+ e.getMessage());
					}
				}
			}

		return pos2activites;


		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static class SampleMeta {

		final String path, categoryName, sampleName, categoryId;
		final int sampleId;
		final boolean hasGeo, hasIr;

		public SampleMeta(String path, boolean hasIr, boolean hasGeo) {
			super();
			this.path = path;
			this.hasGeo = hasGeo;
			this.hasIr = hasIr;
			if (path.indexOf("_")<0) {
				throw new IllegalArgumentException("all files in asset folder must be folders and define category and subcategory as predefined (with underscore)");
			}
			this.categoryId = path.substring(0, path.indexOf("_"));
			path = path.substring(path.indexOf("_")+1);
			this.categoryName = path.substring(0, path.indexOf("_"));
			path = path.substring(path.indexOf("_")+1);
			this.sampleId = Integer.parseInt(path.substring(0, path.indexOf("_")));
			path = path.substring(path.indexOf("_")+1);
			this.sampleName = path;
		}

		@Override
		public String toString() {
			return "categoryId:" + this.categoryId + ", categoryName:" + this.categoryName + ", sampleId:" + this.sampleId +", sampleName: " + this.sampleName + ", path: " + this.path;
		}
	}

	/**
	 * helper to check if video-drawables are supported by this device. recommended to check before launching ARchitect Worlds with videodrawables
	 * @return true if AR.VideoDrawables are supported, false if fallback rendering would apply (= show video fullscreen)
	 */
	public static final boolean isVideoDrawablesSupported() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			// Lollipop: assume it's ok
			// because creating a new GL context only to check this extension is overkill
			return true;
		} else {
			String extensions = GLES20.glGetString( GLES20.GL_EXTENSIONS );
			return extensions != null && extensions.contains( "GL_OES_EGL_image_external" );
		}
	}


}
