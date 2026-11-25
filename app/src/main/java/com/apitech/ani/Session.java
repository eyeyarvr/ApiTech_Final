package com.apitech.ani;

import android.content.Context;
import android.content.SharedPreferences;

/** Keeps the logged-in user in memory and remembers their id between app launches. */
public final class Session {
    private Session() {}
    public static Profile current;

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("ani", Context.MODE_PRIVATE);
    }
    public static void save(Context c, long userId) { prefs(c).edit().putLong("uid", userId).apply(); }
    public static void clear(Context c) { current = null; prefs(c).edit().remove("uid").apply(); }
    public static long savedId(Context c) { return prefs(c).getLong("uid", 0); }
}
