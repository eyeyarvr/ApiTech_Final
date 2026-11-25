package com.apitech.ani;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

/**
 * One place for every kind of user feedback in the app:
 *   1. Toast messages      - toast() / toastLong()
 *   2. Snackbars           - snack()
 *   3. Dialog boxes        - info() / confirm()
 *   4. System notifications - notifyOrder()
 */
public final class Notify {
    private Notify() {}

    public static final String CHANNEL_ORDERS = "ani_orders";
    /** Intent extra: when true, MainActivity opens the Delivery tab. */
    public static final String EXTRA_OPEN_DELIVERY = "open_delivery";

    // ------------------------------------------------------------ 1. toast
    /** Short message that disappears on its own. Use for quick validation hints. */
    public static void toast(Context c, String msg) {
        Toast.makeText(c.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public static void toastLong(Context c, String msg) {
        Toast.makeText(c.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    // ------------------------------------------------------------ 2. snackbar
    /** Message at the bottom of the screen with no action button. */
    public static Snackbar snack(View anchor, String msg) {
        return snack(anchor, msg, null, null);
    }

    /**
     * Message at the bottom of the screen with an optional action (for example UNDO).
     * @param anchor any view on the current screen; the snackbar attaches to its window
     */
    public static Snackbar snack(View anchor, String msg, @Nullable String actionLabel, @Nullable Runnable action) {
        Context c = anchor.getContext();
        Snackbar s = Snackbar.make(anchor, msg, action == null ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG);
        s.setBackgroundTint(ContextCompat.getColor(c, R.color.evergreen));
        s.setTextColor(ContextCompat.getColor(c, R.color.white));
        s.setActionTextColor(ContextCompat.getColor(c, R.color.peony));
        if (action != null && actionLabel != null) s.setAction(actionLabel, v -> action.run());
        s.show();
        return s;
    }

    // ------------------------------------------------------------ 3. dialog box
    /** Simple information dialog with a single OK button. */
    public static void info(Context c, String title, String message) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /** Asks the user to confirm before doing something important. */
    public static void confirm(Context c, String title, String message, String yesLabel, Runnable onYes) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(yesLabel, (d, w) -> onYes.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------ 4. notification
    /** Registers the "Orders" channel. Required on Android 8+; safe to call every launch. */
    public static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ORDERS, "Orders", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Order confirmations and delivery updates");
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    /** True when the app is allowed to post notifications (Android 13+ needs a runtime permission). */
    public static boolean canNotify(Context c) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return false;
        return NotificationManagerCompat.from(c).areNotificationsEnabled();
    }

    /**
     * Posts a notification in the status bar. Tapping it opens the Delivery tab.
     * Does nothing if the user has not allowed notifications.
     * @param id notifications with the same id replace each other
     */
    public static void notifyOrder(Context c, int id, String title, String text) {
        if (!canNotify(c)) return;
        Intent open = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_OPEN_DELIVERY, true);
        PendingIntent pi = PendingIntent.getActivity(c, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL_ORDERS)
                .setSmallIcon(R.drawable.ic_leaf)
                .setColor(ContextCompat.getColor(c, R.color.evergreen))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);
        try {
            NotificationManagerCompat.from(c).notify(id, b.build());
        } catch (SecurityException ignored) {
            // permission was revoked between the check and the call
        }
    }
}
