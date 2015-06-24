package com.kll.collect.android.listeners;

import com.kll.collect.android.logic.FormDetailListAdapter;

import java.util.ArrayList;

/**
 * Created by Narotam on 6/17/2015.
 */
public interface GetFormVersionListener {
    void onGetFormVersionComplete(ArrayList<FormDetailListAdapter> version);
}
