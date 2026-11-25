package com.apitech.ani;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.DialogFragment;

/** Port of the ProductDetail modal. */
public class ProductDetailDialog extends DialogFragment {
    private Product product;
    private boolean preorderMode;
    private int quantity = 1;
    private boolean added = false;

    private View optToday, optPreorder, schedule, noticeOverlay;
    private TextView tvQty;
    private AppCompatButton btnAdd;

    private Profile me;
    private Spinner spDate, spTime;
    private boolean sameDayOk;

    public static ProductDetailDialog newInstance(long productId) {
        ProductDetailDialog d = new ProductDetailDialog();
        Bundle b = new Bundle();
        b.putLong("productId", productId);
        d.setArguments(b);
        return d;
    }

    @Override public int getTheme() { return R.style.Theme_Ani_Dialog; }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        me = Session.current;
        if (me == null && getContext() != null) {
            long saved = Session.savedId(requireContext());
            if (saved > 0) me = Session.current = Db.get(requireContext()).getUser(saved);
        }
        product = Db.get(requireContext()).getProduct(requireArguments().getLong("productId"));
        View v = inflater.inflate(R.layout.dialog_product, container, false);
        if (product == null || me == null) { dismissAllowingStateLoss(); return v; }
        sameDayOk = product.sameDayBlock(me.city) == null;
        preorderMode = !sameDayOk;

        Ui.image(v.findViewById(R.id.ivImage), product.image);
        ((TextView) v.findViewById(R.id.tvTag)).setText(product.tag());
        ((TextView) v.findViewById(R.id.tvName)).setText(product.name);
        ((TextView) v.findViewById(R.id.tvPrice)).setText("\u20b1" + product.price + " / " + product.unit);
        ((TextView) v.findViewById(R.id.tvFarm)).setText("Grown by " + product.farm);
        ((TextView) v.findViewById(R.id.tvLocation)).setText(product.location);
        v.findViewById(R.id.btnClose).setOnClickListener(b -> dismiss());

        String note = product.note(me.city);
        if (note != null) {
            Ui.show(v.findViewById(R.id.noteBox), true);
            ((TextView) v.findViewById(R.id.tvNote)).setText(note);
        }
        if (!product.description.isEmpty()) ((TextView) v.findViewById(R.id.tvDescription)).setText(product.description);

        optToday = v.findViewById(R.id.optToday);
        optPreorder = v.findViewById(R.id.optPreorder);
        schedule = v.findViewById(R.id.schedule);
        noticeOverlay = v.findViewById(R.id.noticeOverlay);
        tvQty = v.findViewById(R.id.tvQty);
        btnAdd = v.findViewById(R.id.btnAdd);

        Ui.show(optToday, sameDayOk);
        ((TextView) v.findViewById(R.id.tvTodayStock)).setText(product.stock + " in stock");
        Ui.show(optPreorder, product.preorder > 0);
        ((TextView) v.findViewById(R.id.tvPreSlots)).setText(product.preorder + " slots left");
        Ui.show(v.findViewById(R.id.optFull), product.preorder == 0);
        optToday.setOnClickListener(b -> { preorderMode = false; refresh(); });
        optPreorder.setOnClickListener(b -> { preorderMode = true; refresh(); });

        // delivery date options: the next 7 days; the third spinner is the time slot
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.getDefault());
        String[] days = new String[7];
        for (int d = 0; d < 7; d++) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_YEAR, d + 1);
            days[d] = fmt.format(cal.getTime());
        }
        spDate = v.findViewById(R.id.spMonth);
        spTime = v.findViewById(R.id.spTime);
        v.findViewById(R.id.spDate).setVisibility(View.GONE);
        setupSpinner(spDate, days, 0);
        setupSpinner(spTime, new String[]{"9\u201310 AM", "10\u201311 AM", "2\u20133 PM"}, 1);

        v.findViewById(R.id.btnMinus).setOnClickListener(b -> { quantity = Math.max(1, quantity - 1); added = false; refresh(); });
        v.findViewById(R.id.btnPlus).setOnClickListener(b -> { quantity++; added = false; refresh(); });
        btnAdd.setEnabled(sameDayOk || product.preorder > 0);
        btnAdd.setOnClickListener(b -> addToBasket());

        // notice-pop: sold out today but pre-order slots remain
        Ui.show(noticeOverlay, !sameDayOk && product.stock == 0 && product.preorder > 0);
        ((TextView) v.findViewById(R.id.tvNoticeBody)).setText(
                "Good news\u2014the next harvest has " + product.preorder + " pre-order slots available. Would you like to reserve one?");
        v.findViewById(R.id.btnNoticeCancel).setOnClickListener(b -> dismiss());
        v.findViewById(R.id.btnNoticePre).setOnClickListener(b -> {
            preorderMode = true; Ui.show(noticeOverlay, false); refresh();
        });
        refresh();
        return v;
    }

    private void setupSpinner(Spinner s, String[] items, int selected) {
        s.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, items));
        s.setSelection(selected);
    }

    private void addToBasket() {
        boolean pre = preorderMode && product.preorder > 0;
        int limit = pre ? product.preorder : product.stock;
        if (limit <= 0 || (!pre && !sameDayOk)) {
            Notify.toast(requireContext(), "This crop isn\u2019t available for that order type.");
            return;
        }
        if (quantity > limit) {
            Notify.toast(requireContext(), "Only " + limit + (pre ? " pre-order slots" : " " + product.unit) + " available.");
            return;
        }
        String schedule = pre ? spDate.getSelectedItem() + ", " + spTime.getSelectedItem() : null;
        Db.get(requireContext()).addToCart(me.id, product.id, quantity, pre ? "preorder" : "today", schedule);
        added = true;
        refresh();
        Fragment parent = getParentFragment();
        if (parent instanceof ShellFragment) {
            ShellFragment shell = (ShellFragment) parent;
            shell.refreshCart();
            if (getView() != null) Notify.snack(getView(), quantity + " × " + product.name + " added to basket.", "VIEW BASKET", () -> {
                dismiss();
                shell.openCart();
            });
        } else if (getView() != null) {
            Notify.snack(getView(), quantity + " × " + product.name + " added to basket.");
        }
    }

    private void refresh() {
        boolean showPre = preorderMode && product.preorder > 0;
        optToday.setBackgroundResource(!preorderMode ? R.drawable.bg_option_selected : R.drawable.bg_option);
        optPreorder.setBackgroundResource(preorderMode ? R.drawable.bg_option_selected : R.drawable.bg_option);
        Ui.show(schedule, showPre);
        tvQty.setText(String.valueOf(quantity));
        btnAdd.setText(added ? "\u2713 Added to basket" : "Add to basket \u00b7 \u20b1" + (product.price * quantity));
    }
}
