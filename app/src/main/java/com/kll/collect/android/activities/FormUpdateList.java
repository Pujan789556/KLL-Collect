package com.kll.collect.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import android.widget.SimpleAdapter;

import android.widget.Toast;
import com.kll.collect.android.R;
import com.kll.collect.android.logic.FormDetailListAdapter;
import com.kll.collect.android.application.Collect;
import com.kll.collect.android.listeners.DeleteFormsListener;
import com.kll.collect.android.listeners.DiskSyncListener;
import com.kll.collect.android.listeners.FormDownloaderListener;
import com.kll.collect.android.listeners.FormListDownloaderListener;
import com.kll.collect.android.logic.FormDetails;
import com.kll.collect.android.listeners.GetFormVersionListener;

import com.kll.collect.android.preferences.PreferencesActivity;
import com.kll.collect.android.tasks.DiskSyncTask;
import com.kll.collect.android.tasks.DownloadFormListTask;
import com.kll.collect.android.tasks.DownloadFormsTask;

import com.kll.collect.android.tasks.GetFormVersionTask;
import com.kll.collect.android.utilities.FileUtils;
import com.kll.collect.android.utilities.WebUtils;



import java.io.File;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;



/**
 * Created by Narotam on 6/9/2015.
 */
public class FormUpdateList extends Activity implements FormListDownloaderListener,
        FormDownloaderListener,DiskSyncListener,GetFormVersionListener,
        DeleteFormsListener {
    private static final String t = "RemoveFileManageList";

    private String mAlertMsg;

    private static final int PROGRESS_DIALOG = 1;
    private static final int AUTH_DIALOG = 2;

    private boolean mAlertShowing = false;
    private boolean downloadComplete = false;
    private ProgressDialog mProgressDialog;
    private AlertDialog mAlertDialog;

    private ArrayList<File> formsToDelete = new ArrayList<File>();
    private ArrayList<String> fileNameToDelete = new ArrayList<>();


    private String mAlertTitle;

    private String formVersion = null;
    private String formVersionOld = null;
    private DownloadFormListTask mDownloadFormListTask;
    private DownloadFormsTask mDownloadFormsTask;


    private HashMap<String, FormDetails> mFormNamesAndURLs;
    private SimpleAdapter mFormListAdapter;
    private ArrayList<HashMap<String, String>> mFormList;

    private ArrayList<FormDetailListAdapter> formDetailListAdapters;
    private  FormDetailListAdapter formDetailListAdapter;

    private static final String FORMNAME = "formname";
    private static final String FORMDETAIL_KEY = "formdetailkey";
    private static final String FORMID_DISPLAY = "formiddisplay";
    private static final String FORMLIST = "formlist";

    private static final boolean EXIT = true;
    private static final boolean DO_NOT_EXIT = false;
    private boolean mShouldExit;
    private static final String SHOULD_EXIT = "shouldexit";

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.geoodk_layout);
        setTitle(getString(R.string.app_name) + " > " + getString(R.string.get_forms));
        mAlertMsg = getString(R.string.please_wait);
        mAlertMsg = "Please wait a moment";



        if (savedInstanceState != null && savedInstanceState.containsKey(FORMLIST)) {
            mFormList =
                    (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable(FORMLIST);
        } else {
            mFormList = new ArrayList<HashMap<String, String>>();
        }

        if (getLastNonConfigurationInstance() instanceof DownloadFormListTask) {
            mDownloadFormListTask = (DownloadFormListTask) getLastNonConfigurationInstance();
            if (mDownloadFormListTask.getStatus() == AsyncTask.Status.FINISHED) {
                try {
                    dismissDialog(PROGRESS_DIALOG);
                } catch (IllegalArgumentException e) {
                    Log.i(t, "Attempting to close a dialog that was not previously opened");
                }
                mDownloadFormsTask = null;
            }
        } else if (getLastNonConfigurationInstance() instanceof DownloadFormsTask) {
            mDownloadFormsTask = (DownloadFormsTask) getLastNonConfigurationInstance();
            if (mDownloadFormsTask.getStatus() == AsyncTask.Status.FINISHED) {
                try {
                    dismissDialog(PROGRESS_DIALOG);
                } catch (IllegalArgumentException e) {
                    Log.i(t, "Attempting to close a dialog that was not previously opened");
                }
                mDownloadFormsTask = null;
            }
        } else if (getLastNonConfigurationInstance() == null) {
            // first time, so get the formlist

            downloadFormList();


        }

        String[] data = new String[] {
                FORMNAME, FORMID_DISPLAY, FORMDETAIL_KEY
        };
        int[] view = new int[] {
                R.id.text1, R.id.text2
        };

        mFormListAdapter =
                new SimpleAdapter(this, mFormList, R.layout.two_item_multiple_choice, data, view);


    }

    private void downloadFormList() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Intent i = new Intent(getApplicationContext(),GeoODK.class);
            startActivity(i);
            Toast.makeText(this, R.string.no_connection, Toast.LENGTH_SHORT).show();

        }else{
            mFormNamesAndURLs = new HashMap<String,FormDetails>();
            if(mProgressDialog !=null){
                mProgressDialog.setMessage(getString(R.string.please_wait));

            }
            showDialog(PROGRESS_DIALOG);

            if (mDownloadFormListTask != null &&
                    mDownloadFormListTask.getStatus() != AsyncTask.Status.FINISHED) {
                return; // we are already doing the download!!!
            } else if (mDownloadFormListTask != null) {
                mDownloadFormListTask.setDownloaderListener(null);
                mDownloadFormListTask.cancel(true);
                mDownloadFormListTask = null;
            }

            mDownloadFormListTask = new DownloadFormListTask();
            mDownloadFormListTask.setDownloaderListener(this);
            mDownloadFormListTask.execute();

        }

    }
    private void downloadSelectedFiles() {

        int totalCount = 0;
        ArrayList<FormDetails> filesToDownload = new ArrayList<FormDetails>();

        Log.i("Size", Integer.toString(mFormNamesAndURLs.size()));
        for (int i = 0; i < mFormList.size(); i++)
            filesToDownload.add(mFormNamesAndURLs.get(mFormList.get(i).get(FORMDETAIL_KEY)));
        totalCount = filesToDownload.size();
        Log.i("Total count", Integer.toString(totalCount));
        Collect.getInstance().getActivityLogger().logAction(this, "downloadSelectedFiles", Integer.toString(totalCount));

        if (totalCount > 0) {
            // show dialog box
            showDialog(PROGRESS_DIALOG);

            mDownloadFormsTask = new DownloadFormsTask();
            mDownloadFormsTask.setDownloaderListener(this);
            mDownloadFormsTask.execute(filesToDownload);
        } else {
            Toast.makeText(getApplicationContext(), R.string.noselect_error, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void formListDownloadingComplete(HashMap<String, FormDetails> result) {
        // dismissDialog(PROGRESS_DIALOG);
        mDownloadFormListTask.setDownloaderListener(null);
        mDownloadFormListTask = null;

        if (result == null) {
            Log.e(t, "Formlist Downloading returned null.  That shouldn't happen");
            // Just displayes "error occured" to the user, but this should never happen.
            createAlertDialog(getString(R.string.load_remote_form_error),
                    getString(R.string.error_occured), EXIT);
            return;
        }

        if (result.containsKey(DownloadFormListTask.DL_AUTH_REQUIRED)) {
            // need authorization
            showDialog(AUTH_DIALOG);
        } else if (result.containsKey(DownloadFormListTask.DL_ERROR_MSG)) {
            // Download failed
            String dialogMessage =
                    getString(R.string.list_failed_with_error,
                            result.get(DownloadFormListTask.DL_ERROR_MSG).errorStr);
            String dialogTitle = getString(R.string.load_remote_form_error);
            createAlertDialog(dialogTitle, dialogMessage, DO_NOT_EXIT);

        } else {
            // Everything worked. Clear the list and add the results.
            Log.i("Everything","Ok");
            Log.i("Size",Integer.toString(result.size()));
            mFormNamesAndURLs = result;

            mFormList.clear();

            ArrayList<String> ids = new ArrayList<String>(mFormNamesAndURLs.keySet());
            for (int i = 0; i < result.size(); i++) {
                String formDetailsKey = ids.get(i);
                FormDetails details = mFormNamesAndURLs.get(formDetailsKey);
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(FORMNAME, details.formName);
                item.put(FORMID_DISPLAY,
                        ((details.formVersion == null) ? "" : (getString(R.string.version) + " " + details.formVersion + " ")) +
                                "ID: " + details.formID );
                item.put(FORMDETAIL_KEY, formDetailsKey);

                // Insert the new form in alphabetical order.
                if (mFormList.size() == 0) {
                    mFormList.add(item);
                } else {
                    int j;
                    for (j = 0; j < mFormList.size(); j++) {
                        HashMap<String, String> compareMe = mFormList.get(j);
                        String name = compareMe.get(FORMNAME);
                        if (name.compareTo(mFormNamesAndURLs.get(ids.get(i)).formName) > 0) {
                            break;
                        }
                    }
                    mFormList.add(j, item);
                }
            }
            mFormListAdapter.notifyDataSetChanged();
            Log.i("Before check", Integer.toString(mFormList.size()));
            mProgressDialog.dismiss();
            GetFormVersionTask getFormVersionTask = new GetFormVersionTask(this,mFormList,mFormNamesAndURLs);
            getFormVersionTask.execute();

        }

    }
    //Check if the form already exist or not
    private void checkForm() {

        int j = 0;
        for (int i = 0; i < mFormList.size(); i++) {
            String name = (String) generateFileName(mFormNamesAndURLs.get(mFormList.get(i).get(FORMDETAIL_KEY)).formName);
            File file = new File(Collect.FORMS_PATH + File.separator + name + ".xml");
            File mediaFile = new File(Collect.FORMS_PATH + File.separator + name + "-media");

            if (file.exists()) {
                formVersion = formDetailListAdapters.get(j).getVersionNew();
                formVersionOld = formDetailListAdapters.get(j).getVersionOld();

                Log.i("New form Version", formVersion);
                Log.i("Old form Version", formVersionOld);
                if (formVersion.equals(formVersionOld)) {
                    Log.i("No update","Already exist");
                    mFormList.remove(i);
                    i--;
                }else{
                    Log.i("Form to update", "");

                    Log.i(name, "will be deleted");

                        formsToDelete.add(file);
                        formsToDelete.add(mediaFile);


                    }

                }

            j++;

            }



    }



    private String generateFileName(String fileName) {
        String name = new String();
        int index = 0;
        for(int i = 0;i<fileName.length();i++) {
            if (fileName.charAt(i) == '_') {
                name = name.concat(fileName.substring(index, i));
                name = name.concat(" ");

                index = i+1;
            }else if(i==(fileName.length()-1)){
                name = name.concat(fileName.substring(index, i+1));

            }
        }
        return name;
    }


    private void createAlertDialog(String title, String message, final boolean shouldExit) {
        Collect.getInstance().getActivityLogger().logAction(this, "createAlertDialog", "show");
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setTitle(title);
        mAlertDialog.setMessage(message);
        DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_POSITIVE: // ok
                        Collect.getInstance().getActivityLogger().logAction(this, "createAlertDialog", "OK");
                        Intent intent = new Intent(getApplicationContext(),GeoODK.class);
                        startActivity(intent);
                        // just close the dialog
                        mAlertShowing = false;
                        // successful download, so quit
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), quitListener);
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertMsg = message;
        mAlertTitle = title;
        mAlertShowing = true;
        mShouldExit = shouldExit;
        mAlertDialog.show();
    }

    @Override
    public void progressUpdate(String currentFile, int progress, int total) {
        mAlertMsg = getString(R.string.fetching_file, currentFile, progress, total);
        mProgressDialog.setMessage(mAlertMsg);
    }

    @Override
    public void onGetFormVersionComplete(ArrayList<FormDetailListAdapter> formDetailList){
        formDetailListAdapters = formDetailList;

        checkForm();
        deleteForms(); //delete from sd card
        Log.i("After check", Integer.toString(mFormList.size()));
        if(mFormList.size() > 0) {
            downloadSelectedFiles();
        }else{
            Toast.makeText(this, R.string.form_upto_date, Toast.LENGTH_LONG).show();
            Intent i = new Intent(getApplicationContext(),GeoODK.class);
            startActivity(i);
        }

    }
    @Override
    public void deleteComplete(int deletedForms) {
        Log.i(t, "Delete forms complete");



    }
    @Override
    public void formsDownloadingComplete(HashMap<FormDetails, String> result) {
        if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(null);
        }

        if (mProgressDialog.isShowing()) {
            // should always be true here
            mProgressDialog.dismiss();
        }

        Set<FormDetails> keys = result.keySet();
        StringBuilder b = new StringBuilder();
        for (FormDetails k : keys) {
            b.append(k.formName +
                    " (" +
                    ((k.formVersion != null) ?
                            (this.getString(R.string.version) + ": " + k.formVersion + " ")
                            : "") +
                    "ID: " + k.formID + ") - " +
                    result.get(k));
            b.append("\n\n");
        }

        syncDisk(); //sync the database and sd card
        deleteTemp();
        createAlertDialog(getString(R.string.download_forms_result), b.toString().trim(), EXIT);
    }

    private void deleteTemp() {
    File tempFile = new File(Collect.TEMP_FORMS_PATH);
        File[] children = tempFile.listFiles();
     for(int i = 0;i<children.length;i++){
         boolean deleted = children[i].delete();
         Log.i("Form deleted from temp",children[i].getName());
     }
    }

    private void syncDisk() {
        DiskSyncTask mDiskSyncTask = (DiskSyncTask) getLastNonConfigurationInstance();
        if (mDiskSyncTask == null) {
            Log.i(t, "Starting new disk sync task");
            mDiskSyncTask = new DiskSyncTask();
            mDiskSyncTask.setDiskSyncListener(this);
            mDiskSyncTask.execute((Void[]) null);
        }
    }

    private void deleteForms() {
        Log.i("Total form to replaced", Integer.toString(formsToDelete.size() ));
        copyToTemp(formsToDelete);
        for(int i = 0;i<formsToDelete.size();i++){
            boolean deleted = formsToDelete.get(i).delete();
        }
    }

    private void copyToTemp(ArrayList<File> formsToDelete) {
        for(int i = 0;i<formsToDelete.size();i++) {
            File file = new File(Collect.TEMP_FORMS_PATH + File.separator + formsToDelete.get(i).getName());
                       try {
            InputStream in = new FileInputStream(formsToDelete.get(i));
            OutputStream out = new FileOutputStream(file);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                Log.e("IO Exception", e.toString());
            }
            Log.i("Form copied to temp",formsToDelete.get(i).getName());
        }

    }


    @Override
    public void SyncComplete(String result) {
        Log.i(t, "Disk scan complete");

    }
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.PROGRESS_DIALOG", "show");
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.PROGRESS_DIALOG", "OK");
                                dialog.dismiss();
                                // we use the same progress dialog for both
                                // so whatever isn't null is running
                                if (mDownloadFormListTask != null) {
                                    mDownloadFormListTask.setDownloaderListener(null);
                                    mDownloadFormListTask.cancel(true);
                                    mDownloadFormListTask = null;
                                }
                                if (mDownloadFormsTask != null) {
                                    mDownloadFormsTask.setDownloaderListener(null);
                                    mDownloadFormsTask.cancel(true);
                                    mDownloadFormsTask = null;
                                }
                            }
                        };
                mProgressDialog.setTitle(getString(R.string.downloading_data));
                mProgressDialog.setMessage(mAlertMsg);
                mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
                return mProgressDialog;
            case AUTH_DIALOG:
                Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.AUTH_DIALOG", "show");
                AlertDialog.Builder b = new AlertDialog.Builder(this);

                LayoutInflater factory = LayoutInflater.from(this);
                final View dialogView = factory.inflate(R.layout.server_auth_dialog, null);

                // Get the server, username, and password from the settings
                SharedPreferences settings =
                        PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                String server =
                        settings.getString(PreferencesActivity.KEY_SERVER_URL,
                                getString(R.string.default_server_url));

                String formListUrl = getString(R.string.default_odk_formlist);
                final String url =
                        server + settings.getString(PreferencesActivity.KEY_FORMLIST_URL, formListUrl);
                Log.i(t, "Trying to get formList from: " + url);

                EditText username = (EditText) dialogView.findViewById(R.id.username_edit);
                String storedUsername = settings.getString(PreferencesActivity.KEY_USERNAME, null);
                username.setText(storedUsername);

                EditText password = (EditText) dialogView.findViewById(R.id.password_edit);
                String storedPassword = settings.getString(PreferencesActivity.KEY_PASSWORD, null);
                password.setText(storedPassword);

                b.setTitle(getString(R.string.server_requires_auth));
                b.setMessage(getString(R.string.server_auth_credentials, url));
                b.setView(dialogView);
                b.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.AUTH_DIALOG", "OK");

                        EditText username = (EditText) dialogView.findViewById(R.id.username_edit);
                        EditText password = (EditText) dialogView.findViewById(R.id.password_edit);

                        Uri u = Uri.parse(url);

                        WebUtils.addCredentials(username.getText().toString(), password.getText()
                                .toString(), u.getHost());
                        downloadFormList();
                    }
                });
                b.setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Collect.getInstance().getActivityLogger().logAction(this, "onCreateDialog.AUTH_DIALOG", "Cancel");

                                finish();
                            }
                        });

                b.setCancelable(false);
                mAlertShowing = false;
                return b.create();
        }
        return null;
    }


}
