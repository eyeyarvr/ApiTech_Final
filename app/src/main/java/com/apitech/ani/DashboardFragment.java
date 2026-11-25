package com.apitech.ani;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Port of the Dashboard component (farmer only). */
public class DashboardFragment extends Fragment {
    private static class Forecast {
        final String name, price, change;
        Forecast(String n, String p, String c) { name = n; price = p; change = c; }
        boolean falling() { return change.startsWith("\u2212"); }
    }

    private final List<Forecast> forecasts = Arrays.asList(
        new Forecast("Carrot", "\u20b187.50", "+\u20b14.20"),
        new Forecast("Cabbage", "\u20b154.25", "+\u20b12.10"),
        new Forecast("Lettuce", "\u20b1112.00", "+\u20b16.40"),
        new Forecast("Tomato", "\u20b196.75", "\u2212\u20b11.80"),
        new Forecast("Potato", "\u20b178.50", "+\u20b13.25"),
        new Forecast("Sayote", "\u20b142.00", "\u2212\u20b10.75"));
    private static final String NO_CROPS = "No crops found";

    private String selectedCrop = "Carrot";
    private ArrayAdapter<String> cropAdapter;
    private Spinner spCrop;
    private TextView tvTitle, tvPrice, tvChange, tvInsight;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        tvTitle = v.findViewById(R.id.tvForecastTitle);
        tvPrice = v.findViewById(R.id.tvPrice);
        tvChange = v.findViewById(R.id.tvChange);
        tvInsight = v.findViewById(R.id.tvInsight);
        spCrop = v.findViewById(R.id.spCrop);

        cropAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spCrop.setAdapter(cropAdapter);
        fillOptions("");
        spCrop.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View view, int pos, long id) {
                String name = cropAdapter.getItem(pos);
                if (name != null && !NO_CROPS.equals(name)) { selectedCrop = name; update(); }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        ((EditText) v.findViewById(R.id.etCropSearch)).addTextChangedListener(new Ui.SimpleWatcher() {
            @Override public void changed(String text) {
                for (Forecast f : forecasts) {
                    if (!text.isEmpty() && f.name.toLowerCase().contains(text.toLowerCase())) { selectedCrop = f.name; break; }
                }
                fillOptions(text);
                update();
            }
        });
        update();

        Profile currentMe = Session.current;
        if (currentMe == null && getContext() != null) {
            long saved = Session.savedId(requireContext());
            if (saved > 0) currentMe = Session.current = Db.get(requireContext()).getUser(saved);
        }
        if (currentMe == null) return v;
        final Profile me = currentMe;
        Db db = Db.get(requireContext());
        long dayMs = 86400000L;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis(), end = start + dayMs;
        int today = db.ordersBetween(me.id, start, end), yesterday = db.ordersBetween(me.id, start - dayMs, start);
        ((TextView) v.findViewById(R.id.tvOrdersToday)).setText(String.valueOf(today));
        ((TextView) v.findViewById(R.id.tvOrdersDelta)).setText((today >= yesterday ? "+" : "") + (today - yesterday) + " since yesterday");
        ((TextView) v.findViewById(R.id.tvStockTotal)).setText(String.valueOf(db.totalStock(me.id)));
        ((TextView) v.findViewById(R.id.tvListings)).setText("Across " + db.activeListings(me.id) + " active listings");
        ((TextView) v.findViewById(R.id.tvSalesToday)).setText("\u20b1" + String.format(java.util.Locale.US, "%,d", db.salesBetween(me.id, start, end)));
        ((TextView) v.findViewById(R.id.tvSalesWeek)).setText("\u20b1" + String.format(java.util.Locale.US, "%,d", db.salesBetween(me.id, end - 7 * dayMs, end)) + " this week");

        // inventory rows from the seller's own listings
        LinearLayout stock = v.findViewById(R.id.stockList);
        List<Product> mine = db.sellerProducts(me.id);
        int maxStock = 1;
        for (Product p : mine) maxStock = Math.max(maxStock, p.stock);
        StringBuilder low = new StringBuilder();
        for (Product p : mine) {
            View row = inflater.inflate(R.layout.item_stock, stock, false);
            Ui.image(row.findViewById(R.id.ivStock), p.image);
            ((TextView) row.findViewById(R.id.tvStockName)).setText(p.name);
            ((TextView) row.findViewById(R.id.tvStockAmount)).setText(p.stock + " " + p.unit + " available \u00b7 " + p.status);
            ((ProgressBar) row.findViewById(R.id.pbStock)).setProgress(p.stock * 100 / maxStock);
            stock.addView(row);
            if ("Active".equals(p.status) && p.stock <= 7) low.append(low.length() > 0 ? ", " : "").append(p.name);
        }
        if (mine.isEmpty()) {
            TextView t = new TextView(requireContext());
            t.setText("You have no listings yet. Add one under Account \u2192 Manage my business.");
            t.setPadding(0, Ui.dp(requireContext(), 12), 0, 0);
            stock.addView(t);
        }
        if (low.length() > 0) {
            Ui.show(v.findViewById(R.id.lowStockBox), true);
            ((TextView) v.findViewById(R.id.tvLowStock)).setText(low + " running low. Update stock after today\u2019s harvest.");
        }

        // supply sources from real listings
        LinearLayout bars = v.findViewById(R.id.barList);
        java.util.Map<String, Integer> supply = db.supplyByTown();
        int maxCount = 1;
        for (int n : supply.values()) maxCount = Math.max(maxCount, n);
        for (java.util.Map.Entry<String, Integer> e : supply.entrySet()) {
            View row = inflater.inflate(R.layout.item_bar, bars, false);
            int pct = e.getValue() * 100 / maxCount;
            ((TextView) row.findViewById(R.id.tvBarName)).setText(e.getKey());
            ((ProgressBar) row.findViewById(R.id.pbBar)).setProgress(pct);
            ((TextView) row.findViewById(R.id.tvBarPct)).setText(e.getValue() + " crops");
            bars.addView(row);
        }

        // seed marketplace
        LinearLayout seedList = v.findViewById(R.id.seedList);
        Runnable loadSeeds = () -> {
            seedList.removeAllViews();
            List<Db.Seed> seeds = db.listSeeds();
            ((TextView) v.findViewById(R.id.tvSeedCount)).setText("Seeds listed by farmers \u00b7 " + seeds.size() + " available");
            for (Db.Seed s : seeds) {
                View row = inflater.inflate(R.layout.item_seed, seedList, false);
                ((TextView) row.findViewById(R.id.tvSeedName)).setText(s.name);
                ((TextView) row.findViewById(R.id.tvSeedMeta)).setText(s.pack + " pack \u00b7 " + s.farm + " \u00b7 " + s.packs + " packs \u00b7 " + s.location);
                ((TextView) row.findViewById(R.id.tvSeedPrice)).setText("\u20b1" + s.price);
                seedList.addView(row);
            }
        };
        loadSeeds.run();

        View seedForm = v.findViewById(R.id.seedForm);
        v.findViewById(R.id.btnSellSeeds).setOnClickListener(b -> Ui.show(seedForm, seedForm.getVisibility() != View.VISIBLE));
        Spinner spSeed = v.findViewById(R.id.spSeedLocation);
        List<String> locs = new ArrayList<>();
        locs.add("Select location");
        locs.addAll(Arrays.asList(Data.CITIES));
        spSeed.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, locs));
        EditText etName = v.findViewById(R.id.etSeedName), etPack = v.findViewById(R.id.etSeedPack),
                 etPrice = v.findViewById(R.id.etSeedPrice), etPacks = v.findViewById(R.id.etSeedPacks);
        v.findViewById(R.id.btnPostSeeds).setOnClickListener(b -> {
            String name = etName.getText().toString().trim(), pack = etPack.getText().toString().trim();
            String price = etPrice.getText().toString().trim(), packs = etPacks.getText().toString().trim();
            if (name.isEmpty() || pack.isEmpty() || price.isEmpty() || packs.isEmpty() || spSeed.getSelectedItemPosition() == 0) {
                Notify.toast(requireContext(), "Please fill in every field.");
                return;
            }
            db.addSeed(me.id, name, pack, Integer.parseInt(price), Integer.parseInt(packs), (String) spSeed.getSelectedItem());
            etName.setText(""); etPack.setText(""); etPrice.setText(""); etPacks.setText("");
            Ui.show(seedForm, false);
            loadSeeds.run();
            if (getParentFragment() instanceof ShellFragment) ((ShellFragment) getParentFragment()).snack("Seeds posted.", null, null);
        });
        return v;
    }

    private void fillOptions(String query) {
        cropAdapter.clear();
        for (Forecast f : forecasts) {
            if (f.name.toLowerCase().contains(query.toLowerCase())) cropAdapter.add(f.name);
        }
        if (cropAdapter.isEmpty()) { cropAdapter.add(selectedCrop); cropAdapter.add(NO_CROPS); }
        int idx = cropAdapter.getPosition(selectedCrop);
        spCrop.setSelection(Math.max(idx, 0));
    }

    private void update() {
        Forecast cur = forecasts.get(0);
        for (Forecast f : forecasts) if (f.name.equals(selectedCrop)) cur = f;
        tvTitle.setText(cur.name + " market price");
        tvPrice.setText(cur.price);
        tvChange.setText((cur.falling() ? "\u2193 " : "\u2191 ") + cur.change + " this week");
        tvChange.setTextColor(ContextCompat.getColor(requireContext(), cur.falling() ? R.color.wine : R.color.evergreen));
        tvInsight.setText((cur.falling() ? "Prices are easing slightly. " : "Prices are gently rising. ")
                + "Review the latest trend before listing your " + cur.name.toLowerCase() + " harvest.");
    }
}
