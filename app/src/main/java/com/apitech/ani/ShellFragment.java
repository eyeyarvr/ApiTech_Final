package com.apitech.ani;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Top bar, page switcher and bottom navigation for a logged-in user. */
public class ShellFragment extends Fragment {
    private static final int HOME = 1, DASHBOARD = 2, DELIVERY = 3, ACCOUNT = 4;
    private Profile profile;
    private BottomNavigationView nav;
    private TextView badge;

    public static ShellFragment newInstance() { return newInstance(false); }

    /** @param openDelivery start on the Delivery tab (used when a notification is tapped) */
    public static ShellFragment newInstance(boolean openDelivery) {
        ShellFragment f = new ShellFragment();
        Bundle b = new Bundle();
        b.putBoolean("delivery", openDelivery);
        f.setArguments(b);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        profile = Session.current;
        if (profile == null && getContext() != null) {
            long saved = Session.savedId(requireContext());
            if (saved > 0) profile = Session.current = Db.get(requireContext()).getUser(saved);
        }
        View v = inflater.inflate(R.layout.fragment_shell, container, false);

        TextView avatar = v.findViewById(R.id.avatar);
        avatar.setText(profile != null ? Data.initials(profile.name) : "");
        badge = v.findViewById(R.id.tvCartBadge);

        nav = v.findViewById(R.id.bottomNav);
        nav.getMenu().add(0, HOME, 0, "Home").setIcon(R.drawable.ic_home);
        if (profile != null && profile.isFarmer()) nav.getMenu().add(0, DASHBOARD, 1, "Dashboard").setIcon(R.drawable.ic_chart);
        nav.getMenu().add(0, DELIVERY, 2, "Delivery").setIcon(R.drawable.ic_truck);
        nav.getMenu().add(0, ACCOUNT, 3, "Account").setIcon(R.drawable.ic_user);
        nav.setOnItemSelectedListener(item -> { showPage(item.getItemId()); return true; });

        avatar.setOnClickListener(b -> nav.setSelectedItemId(ACCOUNT));
        v.findViewById(R.id.btnSearchTop).setOnClickListener(b -> nav.setSelectedItemId(HOME));
        v.findViewById(R.id.btnBell).setOnClickListener(b -> showNotifications());
        v.findViewById(R.id.btnCart).setOnClickListener(b -> new CartDialog().show(getChildFragmentManager(), "cart"));

        if (state == null) {
            boolean delivery = getArguments() != null && getArguments().getBoolean("delivery");
            nav.setSelectedItemId(delivery ? DELIVERY : HOME);
        }
        refreshCart();
        return v;
    }

    private void showPage(int id) {
        Fragment f;
        switch (id) {
            case DASHBOARD: f = new DashboardFragment(); break;
            case DELIVERY: f = new DeliveryFragment(); break;
            case ACCOUNT: f = new AccountFragment(); break;
            default: f = new HomeFragment();
        }
        getChildFragmentManager().beginTransaction().replace(R.id.shellContent, f).commit();
    }

    /** Jumps to the Delivery tab (snackbar "TRACK" action and notification taps). */
    public void showDelivery() {
        if (nav != null) nav.setSelectedItemId(DELIVERY);
    }

    public void openCart() {
        new CartDialog().show(getChildFragmentManager(), "cart");
    }

    /** Snackbar that sits just above the bottom navigation bar. */
    public void snack(String msg, @Nullable String actionLabel, @Nullable Runnable action) {
        if (getView() == null) return;
        Notify.snack(getView(), msg, actionLabel, action).setAnchorView(nav);
    }

    /** Re-reads the basket count for the badge on the bag icon. */
    public void refreshCart() {
        if (profile == null || getContext() == null) return;
        int n = Db.get(requireContext()).cartCount(profile.id);
        badge.setText(String.valueOf(n));
        Ui.show(badge, n > 0);
    }

    public void openProduct(Product p) {
        ProductDetailDialog.newInstance(p.id).show(getChildFragmentManager(), "product");
    }

    /** Dialog box listing the orders that are waiting on the user (to pack, or on the way). */
    private void showNotifications() {
        if (profile == null || getContext() == null) return;
        Db db = Db.get(requireContext());
        List<String> lines = new ArrayList<>();
        for (Db.Order o : db.orders(profile.id, false))
            if (o.status < 3) lines.add("Your order: " + o.name + " × " + o.qty + "\n" + Data.STEPS[o.status].toUpperCase(Locale.ROOT));
        if (profile.isFarmer())
            for (Db.Order o : db.orders(profile.id, true))
                if (o.status < 3) lines.add("From " + o.otherName + ": " + o.name + " × " + o.qty + "\n" + Data.STEPS[o.status].toUpperCase(Locale.ROOT));

        if (lines.isEmpty()) {
            Notify.info(requireContext(), "Notifications", "No new notifications.");
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Notifications (" + lines.size() + ")")
                .setItems(lines.toArray(new String[0]), (d, i) -> showDelivery())
                .setPositiveButton("View deliveries", (d, w) -> showDelivery())
                .setNegativeButton("Close", null)
                .show();
    }
}
