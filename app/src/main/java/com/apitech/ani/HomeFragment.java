package com.apitech.ani;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.List;

/** Browse and search listings from the database; filter by town; save favorites. */
public class HomeFragment extends Fragment {
    private Profile me;
    private Db db;
    private String query = "", town = null;
    private LayoutInflater inflater;
    private LinearLayout list;
    private GridLayout grid;
    private TextView empty;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        this.inflater = inflater;
        me = Session.current;
        if (me == null && getContext() != null) {
            long saved = Session.savedId(requireContext());
            if (saved > 0) me = Session.current = Db.get(requireContext()).getUser(saved);
        }
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        if (me == null) return v;
        db = Db.get(requireContext());

        ((TextView) v.findViewById(R.id.tvGreeting)).setText("Good morning, " + me.name.split(" ")[0] + ".");
        ((TextView) v.findViewById(R.id.tvSub)).setText(me.isFarmer()
                ? "See what\u2019s fresh nearby and keep an eye on today\u2019s harvest."
                : "What would you like to bring home today?");

        list = v.findViewById(R.id.productList);
        grid = v.findViewById(R.id.places);
        empty = v.findViewById(R.id.tvEmpty);

        EditText et = v.findViewById(R.id.etSearch);
        et.addTextChangedListener(new Ui.SimpleWatcher() {
            @Override public void changed(String text) { query = text; reloadProducts(); }
        });
        et.setOnEditorActionListener((tv, a, e) -> { hideKeyboard(tv); return true; });
        v.findViewById(R.id.btnSearch).setOnClickListener(b -> { hideKeyboard(et); reloadProducts(); });

        reloadProducts();
        buildPlaces();
        return v;
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void reloadProducts() {
        list.removeAllViews();
        List<Product> items = db.listProducts(me.id, query, town);
        for (Product p : items) list.addView(productCard(p));
        Ui.show(empty, items.isEmpty());
    }

    private void buildPlaces() {
        grid.removeAllViews();
        for (String city : Data.CITIES) {
            boolean active = city.equals(town);
            TextView chip = new TextView(requireContext());
            chip.setText(city + "  \u00b7  " + db.townCount(city) + " crops");
            chip.setTextSize(12);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            int pad = Ui.dp(requireContext(), 13);
            chip.setPadding(Ui.dp(requireContext(), 8), pad, Ui.dp(requireContext(), 8), pad);
            chip.setBackgroundResource(active ? R.drawable.bg_pill_sage : R.drawable.bg_pill_white);
            chip.setTextColor(ContextCompat.getColor(requireContext(), active ? R.color.evergreen : R.color.ink));
            chip.setOnClickListener(x -> { town = active ? null : city; reloadProducts(); buildPlaces(); });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            int m = Ui.dp(requireContext(), 4);
            lp.setMargins(m, m, m, m);
            grid.addView(chip, lp);
        }
    }

    private View productCard(Product p) {
        View c = inflater.inflate(R.layout.item_product, list, false);
        Ui.image(c.findViewById(R.id.ivImage), p.image);
        ((TextView) c.findViewById(R.id.tvTag)).setText(p.tag());
        ((TextView) c.findViewById(R.id.tvName)).setText(p.name);
        ((TextView) c.findViewById(R.id.tvPrice)).setText("\u20b1" + p.price + "/" + p.unit);
        ((TextView) c.findViewById(R.id.tvFarm)).setText(p.farm);
        ((TextView) c.findViewById(R.id.tvTown)).setText(p.town);
        TextView stock = c.findViewById(R.id.tvStock);
        stock.setText(p.stock > 0 ? p.stock + " available" : "Same-day sold out");
        stock.setTextColor(ContextCompat.getColor(requireContext(), p.stock > 0 ? R.color.evergreen : R.color.wine));
        c.setOnClickListener(x -> ((ShellFragment) getParentFragment()).openProduct(p));

        ImageView heart = c.findViewById(R.id.ivHeart);
        heart.setImageResource(db.isFav(me.id, p.id) ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        heart.setOnClickListener(x -> {
            boolean saved = db.toggleFav(me.id, p.id);
            heart.setImageResource(saved ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
            // snackbar with UNDO flips the favorite back
            ((ShellFragment) getParentFragment()).snack(p.name + (saved ? " saved to favorites." : " removed from favorites."), "UNDO", () ->
                    heart.setImageResource(db.toggleFav(me.id, p.id) ? R.drawable.ic_heart_filled : R.drawable.ic_heart));
        });
        return c;
    }
}
