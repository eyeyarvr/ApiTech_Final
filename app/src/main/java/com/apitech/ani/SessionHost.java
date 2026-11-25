package com.apitech.ani;

/** Replaces the useState<Profile | null> session logic in App(). */
public interface SessionHost {
    void onEnter(Profile profile);
    void onLogout();
    void onSwitchRole();
}
