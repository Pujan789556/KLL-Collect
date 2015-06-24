/*
 * Copyright (C) 2014 GeoODK
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Responsible for displaying buttons to launch the major activities. Launches
 * some activities based on returns of others.
 *
 * @author Jon Nordling (jonnordling@gmail.com)
 */

package com.kll.collect.android.activities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.prefs.Preferences;

import com.kll.collect.android.R;
import com.kll.collect.android.application.Collect;
import com.kll.collect.android.preferences.AdminPreferencesActivity;
import com.kll.collect.android.preferences.PreferencesActivity;
import com.kll.collect.android.preferences.ServerPreferenceActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;

import android.text.LoginFilter;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class GeoODK extends Activity  {
	private static final String t = "GeoODK";
	private static boolean EXIT = true;
	private AlertDialog mAlertDialog;
	private String[] assestFormList;
	private EditTextPreference mServerUrlPreference;
	private String serverURL;
	
    public static final String FORMS_PATH = Collect.ODK_ROOT + File.separator + "forms";

	public static final String KEY_SERVER_URL = "server_url";

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.geoodk_layout);



		Log.i(t, "Starting up, creating directories");
		try {
			Collect.createODKDirs();
		} catch (RuntimeException e) {
			createErrorDialog(e.getMessage(), EXIT);
			return;
		}
 		assestFormList = getAssetFormList();
		copyForms(assestFormList);



		ImageButton geoodk_collect_button = (ImageButton) findViewById(R.id.geoodk_collect_butt);
        geoodk_collect_button.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		        // Do something in response to button click
				Collect.getInstance().getActivityLogger().logAction(this, "fillBlankForm", "click");
				Intent i = new Intent(getApplicationContext(),FormChooserList.class);
				startActivity(i);
		    }
		});
       
        ImageButton geoodk_manage_but = (ImageButton) findViewById(R.id.geoodk_edit_butt);
		geoodk_manage_but.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logAction(this, "editSavedForm", "click");
				Intent i = new Intent(getApplicationContext(),
						InstanceChooserList.class);
				startActivity(i);
			}
		});
		ImageButton geoodk_get_survey = (ImageButton) findViewById(R.id.geoodk_get_survey_butt);
		geoodk_get_survey.setOnClickListener(new View.OnClickListener() {

		@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logAction(this, "get_survey", "click");
				AlertDialog.Builder builder = new AlertDialog.Builder(GeoODK.this);
				builder.setTitle("Confirm Update Form");
				builder.setMessage("Forms will be replaced");
				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences settings =
								PreferenceManager.getDefaultSharedPreferences(getBaseContext());
						String server =
								settings.getString(PreferencesActivity.KEY_SERVER_URL,
										getString(R.string.default_server_url));
						Log.i("URL", server);
						if (server.equals("")) {
							Intent i = new Intent(getApplicationContext(), ServerPreferenceActivity.class);
							startActivity(i);
						} else {
							Intent i = new Intent(getApplicationContext(), FormUpdateList.class);
							startActivity(i);
						}
					}
				});
			builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			}
		});
		geoodk_get_survey.setOnLongClickListener(new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logAction(this, "get_survey", "click");
				SharedPreferences settings =
						PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				String server =
						settings.getString(PreferencesActivity.KEY_SERVER_URL,
								getString(R.string.default_server_url));
				Log.i("URL", server);
				if(server.equals("")) {
					Intent i = new Intent(getApplicationContext(),ServerPreferenceActivity.class);
					startActivity(i);
				}else{
					Intent i = new Intent(getApplicationContext(),FormDownloadList.class);
					startActivity(i);

				}
				return true;
			}
		});
		
		ImageButton geoodk_settings_but = (ImageButton) findViewById(R.id.geoodk_settings_butt);
		geoodk_settings_but.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				Collect.getInstance()
				.getActivityLogger()
				.logAction(this,"Main_Settings","click");



				Intent ig = new Intent( getApplicationContext(), MainSettingsActivity.class);
						startActivity(ig);
			}
		});
		
		ImageButton geoodk_send_but = (ImageButton) findViewById(R.id.geoodk_send_data_butt);
		geoodk_send_but.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Collect.getInstance().getActivityLogger()
				.logAction(this, "uploadForms", "click");
					Intent i = new Intent(getApplicationContext(),
							InstanceUploaderList.class);
					startActivity(i);
			}
		});
		ImageButton geoodk_delete_but = (ImageButton) findViewById(R.id.geoodk_delete_data_butt);
		geoodk_delete_but.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logAction(this, "deleteSavedForms", "click");
				Intent i = new Intent(getApplicationContext(),
						FileManagerTabs.class);
				startActivity(i);
			}
		});
		//End of Main activity
    }
	


	private String[] getAssetFormList() {
		AssetManager assetManager = getAssets();
		String[] formList = null;
		try {
			formList = assetManager.list("forms");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//assetManager.list(path);
		// TODO Auto-generated method stub
		return formList;
	}



	private void copyForms(String[] forms){
		AssetManager assetManager = getAssets();
		InputStream in = null;
		OutputStream out = null;
		for (int i=0; forms.length>i; i++) {
			String filename = forms[i];
			File form_file = new File(FORMS_PATH,filename);
			if (!form_file.exists()){
				try {
					in = assetManager.open("forms/"+filename);
					out = new FileOutputStream(FORMS_PATH+File.separator+filename);
					copyFile(in, out);
					in.close();
		            out.flush();
		            out.close();
		            in = null;
		            out = null;
					
				} catch (IOException e) {
					Log.e("tag", "Failed to copy asset file: " + FORMS_PATH+File.separator+forms[i], e);
			}
				
			}
			 System.out.println(forms[i]);
		}
		
	}
	
	private void copyFile(InputStream in, OutputStream out) throws IOException
	{
	      byte[] buffer = new byte[1024];
	      int read;
	      while((read = in.read(buffer)) != -1)
	      {
	            out.write(buffer, 0, read);
	      }
	}
	
	private void createErrorDialog(String errorMsg, final boolean shouldExit) {
		Collect.getInstance().getActivityLogger()
				.logAction(this, "createErrorDialog", "show");
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertDialog.setMessage(errorMsg);
		DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				switch (i) {
				case DialogInterface.BUTTON_POSITIVE:
					Collect.getInstance()
							.getActivityLogger()
							.logAction(this, "createErrorDialog",
									shouldExit ? "exitApplication" : "OK");
					if (shouldExit) {
						finish();
					}
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.ok), errorListener);
		mAlertDialog.show();
	}
	


	
	
}
