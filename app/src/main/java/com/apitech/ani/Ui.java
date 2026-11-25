package com.apitech.ani;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.io.File;

/** Small helpers that replace the React <Icon/> component and <img src> tags. */
public final class Ui {
    private Ui() {}

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Equivalent of <Icon name="..."/>: loads res/drawable/ic_<name>.xml. */
    public static Drawable icon(Context c, String name) {
        int id = c.getResources().getIdentifier("ic_" + name, "drawable", c.getPackageName());
        return ContextCompat.getDrawable(c, id);
    }

    public static void setIcon(ImageView iv, String name) {
        iv.setImageDrawable(icon(iv.getContext(), name));
    }

    public static void image(ImageView iv, String url) {
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Object model = url != null && url.startsWith("/") ? new File(url) : url;
        Glide.with(iv).load(model).centerCrop().into(iv);
    }

    public static String ago(long ts) {
        long days = (System.currentTimeMillis() - ts) / 86400000L;
        if (days < 1) return "today";
        if (days < 30) return days + (days == 1 ? " day ago" : " days ago");
        long months = days / 30;
        return months + (months == 1 ? " month ago" : " months ago");
    }

    public static void show(View v, boolean visible) {
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public static abstract class SimpleWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
        @Override public void afterTextChanged(Editable s) { changed(s.toString()); }
        public abstract void changed(String text);
    }
}
