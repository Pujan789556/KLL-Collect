package com.kll.collect.android.preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.kll.collect.android.R;

import com.kll.collect.android.activities.FormUpdateList;
import com.kll.collect.android.activities.GeoODK;

import com.kll.collect.android.utilities.UrlUtils;



public class ServerPreferenceActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener{
    public static final String KEY_SERVER_URL = "server_url";
    public static final String KEY_PROTOCOL = "protocol";
    public static final String PROTOCOL_GOOGLE = "google";


    private String server_url;

    private EditTextPreference mServerUrlPreference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        AlertDialog.Builder server_alert = new AlertDialog.Builder(ServerPreferenceActivity.this);
        final EditText editText = new EditText(ServerPreferenceActivity.this);
        server_alert.setMessage("URL");
        server_alert.setTitle("Server URL not available");
        server_alert.setView(editText);

        server_alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences preferences = getSharedPreferences(
                        KEY_SERVER_URL, 0);


               server_url = editText.getText().toString();

                Log.i("URL", server_url);
                              String url = server_url;

                                // remove all trailing "/"s
                                while (url.endsWith("/")) {
                                    url = url.substring(0, url.length() - 1);
                                }

                                if (UrlUtils.isValidUrl(url) || url.equals("")) {
                                    mServerUrlPreference = (EditTextPreference) findPreference(KEY_SERVER_URL);
                                    mServerUrlPreference.setText(url);
                                    Intent i = new Intent(getApplicationContext(),
                                           FormUpdateList.class);
                                    startActivity(i);

                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            R.string.url_error, Toast.LENGTH_SHORT)
                                            .show();

                                }



            }

        });
        server_alert.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent(getApplicationContext(),
                        GeoODK.class);
                startActivity(i);
            }
        });


    server_alert.show();

    }



    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        preference.setSummary((CharSequence) newValue);
        return true;
    }
}
