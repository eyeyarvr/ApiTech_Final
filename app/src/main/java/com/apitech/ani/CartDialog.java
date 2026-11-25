package com.apitech.ani;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.List;

/** The basket: change quantities, remove items and place the order. */
public class CartDialog extends DialogFragment {
    private Db db;
    private Profile me;
    private LinearLayout list;
    private TextView tvTotal;
    private LayoutInflater inflater;

    @Override public int getTheme() { return R.style.Theme_Ani_Dialog; }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        this.inflater = inflater;
        me = Session.current;
        if (me == null && getContext() != null) {
            long saved = Session.savedId(requireContext());
            if (saved > 0) me = Session.current = Db.get(requireContext()).getUser(saved);
        }
        View v = inflater.inflate(R.layout.dialog_cart, container, false);
        if (me == null) { dismissAllowingStateLoss(); return v; }
        db = Db.get(requireContext());
        list = v.findViewById(R.id.cartList);
        tvTotal = v.findViewById(R.id.tvTotal);
        ((TextView) v.findViewById(R.id.tvAddress)).setText(me.address + ", " + me.city);
        v.findViewById(R.id.btnClose).setOnClickListener(b -> dismiss());
        v.findViewById(R.id.btnCheckout).setOnClickListener(b -> confirmCheckout());
        reload();
        return v;
    }

    @Override public void onDismiss(@NonNull android.content.DialogInterface d) {
        super.onDismiss(d);
        if (getParentFragment() instanceof ShellFragment) ((ShellFragment) getParentFragment()).refreshCart();
    }

    /** Dialog box: asks before the order is placed. */
    private void confirmCheckout() {
        List<Db.CartItem> items = db.cart(me.id);
        if (items.isEmpty()) { Notify.toast(requireContext(), "Your basket is empty."); return; }
        int total = 0, count = 0;
        for (Db.CartItem it : items) { total += it.product.price * it.qty; count += it.qty; }
        final int fTotal = total, fCount = count;
        Notify.confirm(requireContext(), "Place this order?",
                count + (count == 1 ? " item" : " items") + " · ₱" + total
                        + "\nDeliver to: " + me.address + ", " + me.city,
                "Place order", () -> checkout(fCount, fTotal));
    }

    private void checkout(int count, int total) {
        String error = db.checkout(me.id, me.address + ", " + me.city);
        if (error != null) { Notify.toastLong(requireContext(), error); reload(); return; }
        // notification in the status bar + snackbar on the main screen
        Notify.notifyOrder(requireContext(), (int) (System.currentTimeMillis() % 100000), "Order placed",
                count + (count == 1 ? " item" : " items") + " · ₱" + total + ". Tap to track your delivery.");
        if (getParentFragment() instanceof ShellFragment) {
            ShellFragment shell = (ShellFragment) getParentFragment();
            shell.snack("Order placed!", "TRACK", shell::showDelivery);
        }
        dismiss();
    }

    private void reload() {
        list.removeAllViews();
        List<Db.CartItem> items = db.cart(me.id);
        int total = 0;
        if (items.isEmpty()) {
            TextView t = new TextView(requireContext());
            t.setText("Your basket is empty.");
            t.setPadding(0, Ui.dp(requireContext(), 40), 0, 0);
            t.setGravity(android.view.Gravity.CENTER);
            list.addView(t);
        }
        for (Db.CartItem it : items) {
            Product p = it.product;
            total += p.price * it.qty;
            View row = inflater.inflate(R.layout.item_cart, list, false);
            Ui.image(row.findViewById(R.id.ivCart), p.image);
            ((TextView) row.findViewById(R.id.tvCartName)).setText(p.name);
            ((TextView) row.findViewById(R.id.tvCartMeta)).setText(p.farm + " \u00b7 "
                    + ("today".equals(it.kind) ? "Same-day" : "Pre-order " + it.schedule));
            ((TextView) row.findViewById(R.id.tvCartLine)).setText("\u20b1" + (p.price * it.qty));
            ((TextView) row.findViewById(R.id.tvQty)).setText(String.valueOf(it.qty));
            row.findViewById(R.id.btnMinus).setOnClickListener(x -> { db.setCartQty(it.id, it.qty - 1); reload(); });
            row.findViewById(R.id.btnPlus).setOnClickListener(x -> { db.setCartQty(it.id, it.qty + 1); reload(); });
            row.findViewById(R.id.btnRemove).setOnClickListener(x -> {
                db.removeCart(it.id);
                reload();
                // snackbar with UNDO puts the item back
                Notify.snack(list, p.name + " removed from basket.", "UNDO", () -> {
                    db.addToCart(me.id, p.id, it.qty, it.kind, it.schedule);
                    reload();
                });
            });
            list.addView(row);
        }
        tvTotal.setText("\u20b1" + total);
    }
}
