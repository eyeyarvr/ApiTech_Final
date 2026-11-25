package com.apitech.ani;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Orders from the database. Customers track what they bought; farmers see what to prepare
 * and move each order along (Confirmed -> Packed -> On the way -> Delivered).
 */
public class DeliveryFragment extends Fragment {
    private Db db;
    private Profile me;
    private boolean sellerView, deliveredTab;
    private LayoutInflater inflater;
    private LinearLayout list;
    private TextView tabProgress, tabDelivered, tvNo;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        this.inflater = inflater;
        me = Session.current;
        if (me == null && getContext() != null) {
            long saved = Session.savedId(requireContext());
            if (saved > 0) me = Session.current = Db.get(requireContext()).getUser(saved);
        }
        View v = inflater.inflate(R.layout.fragment_delivery, container, false);
        if (me == null) return v;
        db = Db.get(requireContext());
        sellerView = me.isFarmer();
        list = v.findViewById(R.id.orderList);
        tabProgress = v.findViewById(R.id.tabProgress);
        tabDelivered = v.findViewById(R.id.tabDelivered);
        tvNo = v.findViewById(R.id.tvNoOrders);

        if (me.isFarmer()) {
            Ui.show(v.findViewById(R.id.sellerMode), true);
            Button toggle = v.findViewById(R.id.btnToggleView);
            TextView title = v.findViewById(R.id.tvModeTitle), body = v.findViewById(R.id.tvModeBody);
            Runnable apply = () -> {
                title.setText(sellerView ? "Seller delivery view" : "Buyer delivery view");
                body.setText(sellerView ? "You\u2019re viewing orders that you\u2019re preparing for customers."
                                        : "You\u2019re viewing orders you bought from other farms.");
                toggle.setText(sellerView ? "View buyer orders" : "View seller orders");
                reload();
            };
            toggle.setOnClickListener(b -> { sellerView = !sellerView; apply.run(); });
            tabProgress.setOnClickListener(b -> { deliveredTab = false; reload(); });
            tabDelivered.setOnClickListener(b -> { deliveredTab = true; reload(); });
            apply.run();
        } else {
            tabProgress.setOnClickListener(b -> { deliveredTab = false; reload(); });
            tabDelivered.setOnClickListener(b -> { deliveredTab = true; reload(); });
            reload();
        }
        return v;
    }

    private void reload() {
        list.removeAllViews();
        List<Db.Order> all = db.orders(me.id, sellerView);
        int inProgress = 0, delivered = 0;
        for (Db.Order o : all) if (o.status < 3) inProgress++; else delivered++;
        styleTab(tabProgress, "In progress  " + inProgress, !deliveredTab);
        styleTab(tabDelivered, "Delivered  " + delivered, deliveredTab);
        int shown = 0;
        for (Db.Order o : all) {
            if ((o.status == 3) != deliveredTab) continue;
            list.addView(card(o));
            shown++;
        }
        Ui.show(tvNo, shown == 0);
        tvNo.setText(deliveredTab ? "No delivered orders yet." : "No orders in progress.");
    }

    private void styleTab(TextView t, String text, boolean active) {
        t.setText(text);
        t.setBackgroundResource(active ? R.drawable.bg_pill_sage : R.drawable.bg_pill_white);
        t.setTextColor(ContextCompat.getColor(requireContext(), active ? R.color.evergreen : R.color.muted));
    }

    private View card(Db.Order o) {
        View c = inflater.inflate(R.layout.item_order, list, false);
        Ui.image(c.findViewById(R.id.ivOrder), o.image);
        ((TextView) c.findViewById(R.id.tvStatus)).setText(Data.STEPS[o.status].toUpperCase(Locale.ROOT));
        ((TextView) c.findViewById(R.id.tvName)).setText(o.name + " \u00d7 " + o.qty);
        ((TextView) c.findViewById(R.id.tvTotal)).setText("\u20b1" + o.total);
        String when = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(new Date(o.createdAt));
        ((TextView) c.findViewById(R.id.tvMeta)).setText("Order #ANI-" + o.id + " \u00b7 " + when + " \u00b7 "
                + ("today".equals(o.kind) ? "Same-day" : "Pre-order " + o.schedule));

        StringBuilder steps = new StringBuilder();
        for (int i = 0; i < Data.STEPS.length; i++) {
            steps.append(i <= o.status ? "\u2713 " : "\u25cb ").append(Data.STEPS[i]);
            if (i < Data.STEPS.length - 1) steps.append("   ");
        }
        ((TextView) c.findViewById(R.id.tvSteps)).setText(steps);
        ((TextView) c.findViewById(R.id.tvAddrLabel)).setText(sellerView ? "DELIVER TO " + o.otherName.toUpperCase(Locale.ROOT)
                : (o.status == 3 ? "DELIVERED TO" : "DELIVERING TO"));
        ((TextView) c.findViewById(R.id.tvAddress)).setText(o.address);

        Button a = c.findViewById(R.id.btnA), b = c.findViewById(R.id.btnB);
        a.setText("Contact " + (sellerView ? "customer" : "seller"));
        a.setOnClickListener(x -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + o.otherPhone.replace(" ", "")))));

        if (sellerView) {
            if (o.status < 3) {
                b.setText("Mark as " + Data.STEPS[o.status + 1].toLowerCase(Locale.ROOT));
                b.setOnClickListener(x -> {
                    String next = Data.STEPS[o.status + 1].toLowerCase(Locale.ROOT);
                    // dialog box: confirm before the order moves to the next step
                    Notify.confirm(requireContext(), "Mark as " + next + "?",
                            "Order #ANI-" + o.id + " · " + o.name + " × " + o.qty + " for " + o.otherName + ".",
                            "Yes, update", () -> {
                                db.advance(o.id);
                                reload();
                                shell().snack("Order #ANI-" + o.id + " marked as " + next + ".", null, null);
                                Notify.notifyOrder(requireContext(), (int) o.id, "Order #ANI-" + o.id + " update",
                                        o.name + " × " + o.qty + " for " + o.otherName + " is now " + next + ".");
                            });
                });
            } else b.setVisibility(View.GONE);
        } else if (o.status == 3) {
            b.setText(o.reviewed ? "\u2605 Review sent" : "\u2605 Write review");
            b.setEnabled(!o.reviewed);
            b.setOnClickListener(x -> reviewDialog(o));
            Button again = a;
            again.setText("Order again");
            again.setOnClickListener(x -> {
                Product p = db.getProduct(o.productId);
                if (p == null || !"Active".equals(p.status)) { toast("That crop isn\u2019t available anymore."); return; }
                boolean today = p.sameDayBlock(me.city) == null;
                if (!today && p.preorder <= 0) { toast("Sold out for now."); return; }
                db.addToCart(me.id, p.id, 1, today ? "today" : "preorder", today ? null : "Pick a date in your basket");
                shell().refreshCart();
                shell().snack(p.name + " added to your basket.", "VIEW BASKET", shell()::openCart);
            });
        } else b.setVisibility(View.GONE);
        return c;
    }

    private void toast(String s) { Notify.toast(requireContext(), s); }

    private ShellFragment shell() { return (ShellFragment) getParentFragment(); }

    private void reviewDialog(Db.Order o) {
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(requireContext(), 20);
        box.setPadding(pad, pad, pad, 0);
        RatingBar bar = new RatingBar(requireContext());
        bar.setNumStars(5); bar.setStepSize(1f); bar.setRating(5f);
        EditText comment = new EditText(requireContext());
        comment.setHint("Tell others about this harvest (optional)");
        box.addView(bar);
        box.addView(comment);
        new AlertDialog.Builder(requireContext())
                .setTitle("Review " + o.name)
                .setView(box)
                .setPositiveButton("Send", (d, w) -> {
                    db.addReview(o, me.id, Math.max(1, (int) bar.getRating()), comment.getText().toString().trim());
                    reload();
                    shell().snack("Thanks! Your review was sent.", null, null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
