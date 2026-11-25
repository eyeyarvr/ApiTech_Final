package com.apitech.ani;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/** Entry point: restores a saved login, then shows Welcome or the main app. */
public class MainActivity extends AppCompatActivity implements SessionHost {
    /** Set when the app was opened by tapping an order notification. */
    private boolean openDelivery;

    /** Android 13+ asks the user before the app may post notifications. */
    private final ActivityResultLauncher<String> notifPermission = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(), granted -> {
            if (!granted) Notify.toastLong(this, "Notifications are off. You can turn them on in Settings.");
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // restore the login first so fragments recreated by the system can read it
        if (Session.current == null) {
            long saved = Session.savedId(this);
            if (saved > 0) Session.current = Db.get(this).getUser(saved);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Notify.createChannel(this);
        if (savedInstanceState == null) {
            openDelivery = getIntent().getBooleanExtra(Notify.EXTRA_OPEN_DELIVERY, false);
            render();
            if (Session.current != null) askNotificationPermission();
        }
    }

    /** Called when a notification is tapped while the app is already open. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!intent.getBooleanExtra(Notify.EXTRA_OPEN_DELIVERY, false) || Session.current == null) return;
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        if (f instanceof ShellFragment && f.getView() != null) ((ShellFragment) f).showDelivery();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void render() {
        Fragment f = Session.current == null ? new WelcomeFragment() : ShellFragment.newInstance(openDelivery);
        openDelivery = false;
        getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
    }

    @Override public void onEnter(Profile profile) {
        Session.current = profile;
        Session.save(this, profile.id);
        render();
        Notify.toast(this, "Welcome, " + profile.name.split(" ")[0] + "!");
        askNotificationPermission();
    }

    @Override public void onLogout() {
        Session.clear(this);
        render();
        Notify.toast(this, "You’ve been logged out.");
    }

    /** Switches between buyer and seller mode and saves the choice on the account. */
    @Override public void onSwitchRole() {
        Profile p = Session.current;
        p.role = p.isFarmer() ? "customer" : "farmer";
        Db.get(this).setRole(p.id, p.role);
        render();
        Notify.toast(this, p.isFarmer() ? "Switched to farmer view." : "Switched to customer view.");
    }
}
