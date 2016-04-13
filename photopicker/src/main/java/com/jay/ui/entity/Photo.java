package com.jay.ui.entity;

import android.text.TextUtils;

/**
 * Created on 2016/4/12
 *
 * @author Q.Jay
 * @version 1.0.0
 */
public class Photo {
    public String thumbnailsUri;
    public String uri;
    public boolean isChecked;

    public String getShowUri() {
        return TextUtils.isEmpty(thumbnailsUri)?uri:thumbnailsUri;
    }
}
