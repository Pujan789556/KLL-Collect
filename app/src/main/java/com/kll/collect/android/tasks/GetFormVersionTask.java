package com.kll.collect.android.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.kll.collect.android.logic.FormDetailListAdapter;
import com.kll.collect.android.listeners.GetFormVersionListener;
import com.kll.collect.android.logic.FormDetails;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by Narotam on 6/17/2015.
 */
public class GetFormVersionTask extends AsyncTask<String,String,ArrayList<FormDetailListAdapter> > {

    private GetFormVersionListener getFormVersionListener;
    private Context context;
    private Activity activity;

    private ArrayList<FormDetailListAdapter> formDetailListAdapters;
    private FormDetailListAdapter formDetailListAdapter;

    private static final String FORMDETAIL_KEY = "formdetailkey";
    private HashMap<String, FormDetails> mFormNamesAndURLs;
    private ArrayList<HashMap<String, String>> mFormList;

    private ProgressDialog mProgressDialog;


    private String newFormVersion;
    private String oldFormVersion;

    public GetFormVersionTask(Context c,ArrayList<HashMap<String, String>> formList,HashMap<String, FormDetails> formNameAndUrls) {
        this.context = c;
        Activity a = (Activity) c;
        this.activity = a;
        this.getFormVersionListener = (GetFormVersionListener) a;
        this.mFormNamesAndURLs = formNameAndUrls;
        this.mFormList = formList;


    }


    @Override
    protected void onPreExecute(){
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMessage("Checking Forms");
        mProgressDialog.setTitle("Please Wait");
        mProgressDialog.show();
    }
    @Override
    protected ArrayList<FormDetailListAdapter> doInBackground(String... params) {

        formDetailListAdapters = new ArrayList<FormDetailListAdapter>();
        for (int i = 0; i < mFormList.size(); i++) {
            String url = mFormNamesAndURLs.get(mFormList.get(i).get(FORMDETAIL_KEY)).downloadUrl;
            String fileName = mFormNamesAndURLs.get(mFormList.get(i).get(FORMDETAIL_KEY)).formName;
            String name = (String) generateFileName(fileName);
            File file = new File("mnt/sdcard/kllcollect/forms/" + name + ".xml");
            newFormVersion = getNewFormVersion(url, fileName);
            oldFormVersion = getOldFormVersion(fileName, file);
            Log.i("New Form version",newFormVersion);
            Log.i("Old form version",oldFormVersion);
            formDetailListAdapter = new FormDetailListAdapter(newFormVersion,oldFormVersion);
            formDetailListAdapters.add(formDetailListAdapter);
        }

        return formDetailListAdapters;
    }

    @Override
    protected void onPostExecute(ArrayList<FormDetailListAdapter> version) {

        mProgressDialog.dismiss();
        getFormVersionListener.onGetFormVersionComplete(formDetailListAdapters);
    }


    private String getOldFormVersion(String fileName,File file) {
        String version = new String();
        if (file.exists()) {

            try {
                InputStream is = new FileInputStream(file.getPath());
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(new InputSource(is));
                doc.getDocumentElement().normalize();
                NodeList nodeList1 = doc.getElementsByTagName("h:html");
                for (int i = 0; i < nodeList1.getLength(); i++) {
                    Node node1 = nodeList1.item(i);
                    Element element1 = (Element) node1;
                    NodeList nodeList2 = element1.getElementsByTagName("h:head");
                    for (int j = 0; j < nodeList2.getLength(); j++) {
                        Node node2 = nodeList2.item(j);
                        Element element2 = (Element) node2;
                        NodeList nodeList3 = element2.getElementsByTagName("model");
                        for (int k = 0; k < nodeList3.getLength(); k++) {
                            Node node3 = nodeList3.item(k);
                            Element element3 = (Element) node3;
                            NodeList nodeList4 = element3.getElementsByTagName("instance");
                            for (int l = 0; l < nodeList4.getLength(); l++) {
                                Node node4 = nodeList4.item(l);
                                Element element4 = (Element) node4;
                                NodeList nodeList5 = element4.getElementsByTagName(fileName);
                                for (int m = 0; m < nodeList5.getLength(); m++) {
                                    Node node5 = nodeList5.item(m);
                                    Element element5 = (Element) node5;
                                    version = element5.getAttribute("version");
                                }
                            }
                        }
                    }

                }


        }catch(Exception e){
            Log.e("XML Pasing Excpetion = ", e.toString());
        }
    }

    return version;

}
    private String getNewFormVersion(String url,String fileName) {
        String version = new String();
        try {
            URL form_url = new URL(url);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(form_url.openStream()));
            doc.getDocumentElement().normalize();
            NodeList nodeList1 = doc.getElementsByTagName("h:html");
            for(int i =  0;i<nodeList1.getLength();i++){
                Node node1 = nodeList1.item(i);
                Element element1 = (Element) node1;
                NodeList nodeList2 = element1.getElementsByTagName("h:head");
                for(int j = 0;j<nodeList2.getLength();j++){
                    Node node2 = nodeList2.item(j);
                    Element element2 = (Element) node2;
                    NodeList nodeList3 = element2.getElementsByTagName("model");
                    for (int k = 0;k<nodeList3.getLength();k++){
                        Node node3 = nodeList3.item(k);
                        Element element3 = (Element) node3;
                        NodeList nodeList4 = element3.getElementsByTagName("instance");
                        for(int l = 0;l<nodeList4.getLength();l++){
                            Node node4 = nodeList4.item(l);
                            Element element4 = (Element) node4;
                            NodeList nodeList5 = element4.getElementsByTagName(fileName);
                            for(int m = 0;m<nodeList5.getLength();m++){
                                Node node5 = nodeList5.item(m);
                                Element element5 = (Element) node5;
                                version = element5.getAttribute("version");
                            }
                        }
                    }
                }

            }


        } catch (Exception e) {
            Log.e("XML Pasing Excpetion = ", e.toString());
        }
        // Log.i("Form Version",version);

        return version;

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

}
