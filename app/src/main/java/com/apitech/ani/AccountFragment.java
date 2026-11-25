package com.apitech.ani;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Profile, favorites and (for farmers) the business manager: farm name, new crops, edit listings. */
public class AccountFragment extends Fragment {
    private Db db;
    private Profile me;
    private LayoutInflater inflater;
    private LinearLayout listingList;
    private Button btnPickPhoto;
    private String pickedImage;
    private View root;

    private final ActivityResultLauncher<String> picker = registerForActivityResult(
        new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            pickedImage = copyToAppStorage(uri);
            if (pickedImage != null && btnPickPhoto != null) btnPickPhoto.setText("\u2713 Photo selected");
        });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        this.inflater = inflater;
        me = Session.current;
        if (me == null && getContext() != null) {
            long saved = Session.savedId(requireContext());
            if (saved > 0) me = Session.current = Db.get(requireContext()).getUser(saved);
        }
        root = inflater.inflate(R.layout.fragment_account, container, false);
        if (me == null) return root;
        db = Db.get(requireContext());
        bindProfile();
        bindMenu();
        if (me.isFarmer()) bindBusiness();

        SessionHost host = (SessionHost) requireActivity();
        Button sw = root.findViewById(R.id.btnSwitch);
        sw.setText(me.isFarmer() ? "Switch to customer view" : "Start selling (switch to farmer view)");
        sw.setOnClickListener(b -> host.onSwitchRole());
        root.findViewById(R.id.btnLogout).setOnClickListener(b ->
            Notify.confirm(requireContext(), "Log out?", "You’ll need your email and password to come back in.",
                    "Log out", host::onLogout));
        root.findViewById(R.id.btnEditProfile).setOnClickListener(b -> editProfile());
        return root;
    }

    // ------------------------------------------------------------ profile
    private void bindProfile() {
        ((TextView) root.findViewById(R.id.tvAvatar)).setText(Data.initials(me.name));
        ((TextView) root.findViewById(R.id.tvRole)).setText(me.isFarmer() ? "FARMER & SELLER" : "CUSTOMER");
        ((TextView) root.findViewById(R.id.tvName)).setText(me.name);
        ((TextView) root.findViewById(R.id.tvLocation)).setText(me.city + ", Benguet");
        ((TextView) root.findViewById(R.id.tvFullName)).setText(me.name);
        ((TextView) root.findViewById(R.id.tvUsername)).setText("@" + me.username);
        ((TextView) root.findViewById(R.id.tvEmail)).setText(me.email);
        ((TextView) root.findViewById(R.id.tvPhone)).setText(me.phone);
        ((TextView) root.findViewById(R.id.tvAddress)).setText(me.address + ",\n" + me.city);
        ((TextView) root.findViewById(R.id.tvMenuHeading)).setText(me.isFarmer() ? "Farm & account" : "Shopping & account");
    }

    private void editProfile() {
        View d = inflater.inflate(R.layout.dialog_edit_profile, null);
        EditText name = d.findViewById(R.id.etEpName), phone = d.findViewById(R.id.etEpPhone), addr = d.findViewById(R.id.etEpAddress);
        Spinner city = d.findViewById(R.id.spEpCity);
        city.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, Data.CITIES));
        name.setText(me.name); phone.setText(me.phone); addr.setText(me.address);
        city.setSelection(Math.max(0, Arrays.asList(Data.CITIES).indexOf(me.city)));
        new AlertDialog.Builder(requireContext()).setTitle("Edit profile").setView(d)
            .setPositiveButton("Save", (x, w) -> {
                if (name.getText().toString().trim().isEmpty()) { toast("Name is required."); return; }
                me.name = name.getText().toString().trim();
                me.phone = phone.getText().toString().trim();
                me.address = addr.getText().toString().trim();
                me.city = (String) city.getSelectedItem();
                db.updateProfile(me);
                bindProfile();
                snack("Profile updated.");
            })
            .setNegativeButton("Cancel", null).show();
    }

    // ------------------------------------------------------------ menu
    private void bindMenu() {
        int favs = db.favorites(me.id).size();
        String[][] menu = me.isFarmer()
            ? new String[][]{{"store", "Manage my business", "Edit farm details and crop listings"},
                             {"bag", "My products", db.sellerProducts(me.id).size() + " listings"},
                             {"heart", "Saved crops", favs + " saved items"},
                             {"settings", "Settings", "Notifications, privacy, and payments"}}
            : new String[][]{{"heart", "Saved crops", favs + " saved items"},
                             {"settings", "Settings", "Notifications, privacy, and payments"}};
        LinearLayout list = root.findViewById(R.id.menuList);
        for (String[] m : menu) {
            View row = inflater.inflate(R.layout.item_menu, list, false);
            Ui.setIcon(row.findViewById(R.id.ivMenuIcon), m[0]);
            ((TextView) row.findViewById(R.id.tvMenuTitle)).setText(m[1]);
            ((TextView) row.findViewById(R.id.tvMenuSub)).setText(m[2]);
            row.setOnClickListener(x -> {
                if (m[0].equals("heart")) showFavorites();
                else if (m[0].equals("store") || m[0].equals("bag")) { View box = root.findViewById(R.id.managerBox); Ui.show(box, true); box.requestFocus(); }
                else Notify.info(requireContext(), "Settings", "Notifications, privacy and payment settings aren’t available yet.");
            });
            list.addView(row);
        }
    }

    private void showFavorites() {
        List<Product> favs = db.favorites(me.id);
        if (favs.isEmpty()) { toast("You haven\u2019t saved any crops yet. Tap the heart on a crop."); return; }
        String[] names = new String[favs.size()];
        for (int i = 0; i < names.length; i++) names[i] = favs.get(i).name + " \u00b7 " + favs.get(i).farm + " \u00b7 \u20b1" + favs.get(i).price + "/" + favs.get(i).unit;
        new AlertDialog.Builder(requireContext()).setTitle("Saved crops")
            .setItems(names, (d, i) -> ((ShellFragment) getParentFragment()).openProduct(favs.get(i)))
            .setNegativeButton("Close", null).show();
    }

    // ------------------------------------------------------------ farmer business
    private void bindBusiness() {
        Ui.show(root.findViewById(R.id.businessPanel), true);
        Ui.image(root.findViewById(R.id.ivCover), Data.HERO);
        ((TextView) root.findViewById(R.id.tvFarmName)).setText(db.farmName(me.id));
        bindStats();
        EditText farm = root.findViewById(R.id.etFarmName);
        farm.setText(db.farmName(me.id));
        root.findViewById(R.id.btnSaveFarm).setOnClickListener(b -> {
            String n = farm.getText().toString().trim();
            if (n.isEmpty()) { toast("Enter a farm name."); return; }
            db.setFarmName(me.id, n);
            ((TextView) root.findViewById(R.id.tvFarmName)).setText(n);
            snack("Farm name saved.");
        });

        View manager = root.findViewById(R.id.managerBox), form = root.findViewById(R.id.newListingForm);
        root.findViewById(R.id.btnManage).setOnClickListener(b -> Ui.show(manager, manager.getVisibility() != View.VISIBLE));
        root.findViewById(R.id.btnNewListing).setOnClickListener(b -> Ui.show(form, form.getVisibility() != View.VISIBLE));

        Spinner unit = root.findViewById(R.id.spCropUnit), town = root.findViewById(R.id.spCropTown);
        unit.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, Data.UNITS));
        List<String> towns = new ArrayList<>();
        towns.add("Select location");
        towns.addAll(Arrays.asList(Data.CITIES));
        town.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, towns));

        btnPickPhoto = root.findViewById(R.id.btnPickPhoto);
        btnPickPhoto.setOnClickListener(b -> picker.launch("image/*"));

        EditText name = root.findViewById(R.id.etCropName), price = root.findViewById(R.id.etCropPrice),
                 stock = root.findViewById(R.id.etCropStock), pre = root.findViewById(R.id.etCropPreorder),
                 desc = root.findViewById(R.id.etCropDesc);
        CheckBox sameDay = root.findViewById(R.id.cbSameDay);
        root.findViewById(R.id.btnPublish).setOnClickListener(b -> {
            if (name.getText().toString().trim().isEmpty() || price.getText().toString().isEmpty()
                    || stock.getText().toString().isEmpty() || town.getSelectedItemPosition() == 0) {
                toast("Please add a name, price, stock and location.");
                return;
            }
            int preSlots = pre.getText().toString().isEmpty() ? 0 : Integer.parseInt(pre.getText().toString());
            db.addProduct(me.id, name.getText().toString().trim(), db.farmName(me.id), me.address, (String) town.getSelectedItem(),
                Integer.parseInt(price.getText().toString()), (String) unit.getSelectedItem(),
                Integer.parseInt(stock.getText().toString()), preSlots,
                pickedImage != null ? pickedImage : Data.BASKET, sameDay.isChecked(), desc.getText().toString().trim());
            name.setText(""); price.setText(""); stock.setText(""); pre.setText("0"); desc.setText("");
            pickedImage = null;
            btnPickPhoto.setText("Choose a photo");
            Ui.show(form, false);
            loadListings();
            bindStats();
            snack("Crop listing published.");
        });

        listingList = root.findViewById(R.id.listingList);
        loadListings();
    }

    private void bindStats() {
        ((TextView) root.findViewById(R.id.tvStatProducts)).setText(db.activeListings(me.id) + "\nProducts");
        double r = db.avgRating(me.id);
        ((TextView) root.findViewById(R.id.tvStatRating)).setText((r == 0 ? "\u2014" : String.format(Locale.US, "%.1f", r)) + "\nRating");
        ((TextView) root.findViewById(R.id.tvStatOrders)).setText(db.orderCount(me.id) + "\nOrders");
    }

    private void loadListings() {
        listingList.removeAllViews();
        for (Product p : db.sellerProducts(me.id)) {
            View row = inflater.inflate(R.layout.item_listing, listingList, false);
            Ui.image(row.findViewById(R.id.ivListing), p.image);
            ((TextView) row.findViewById(R.id.tvListingName)).setText(p.name);
            ((TextView) row.findViewById(R.id.tvListingAgo)).setText("Last updated " + Ui.ago(p.updatedAt));
            boolean stale = (System.currentTimeMillis() - p.updatedAt) > 30L * 86400000L;
            TextView badge = row.findViewById(R.id.tvListingBadge);
            badge.setText(stale ? "Needs update" : p.status);
            badge.setBackgroundResource(stale ? R.drawable.bg_peony : R.drawable.bg_pill_sage);
            badge.setTextColor(ContextCompat.getColor(requireContext(), stale ? R.color.wine : R.color.evergreen));
            ((TextView) row.findViewById(R.id.tvPriceLabel)).setText("Price per " + p.unit);
            EditText price = row.findViewById(R.id.etListingPrice), stock = row.findViewById(R.id.etListingStock);
            price.setText(String.valueOf(p.price));
            stock.setText(String.valueOf(p.stock));
            Spinner status = row.findViewById(R.id.spListingStatus);
            status.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, Data.STATUSES));
            status.setSelection(Math.max(0, Arrays.asList(Data.STATUSES).indexOf(p.status)));
            row.findViewById(R.id.btnListingSave).setOnClickListener(b -> {
                if (price.getText().toString().isEmpty() || stock.getText().toString().isEmpty()) { toast("Enter a price and stock."); return; }
                db.updateListing(p.id, Integer.parseInt(price.getText().toString()), Integer.parseInt(stock.getText().toString()), (String) status.getSelectedItem());
                snack("Listing updated.");
                loadListings();
                bindStats();
            });
            row.findViewById(R.id.btnListingDelete).setOnClickListener(b ->
                new AlertDialog.Builder(requireContext()).setTitle("Delete " + p.name + "?")
                    .setMessage("This removes the listing. Past orders are kept.")
                    .setPositiveButton("Delete", (d, w) -> { db.deleteProduct(p.id); loadListings(); bindStats(); snack(p.name + " deleted."); })
                    .setNegativeButton("Cancel", null).show());
            listingList.addView(row);
        }
    }

    // ------------------------------------------------------------ helpers
    private String copyToAppStorage(Uri uri) {
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            File out = new File(requireContext().getFilesDir(), "crop_" + System.currentTimeMillis() + ".jpg");
            try (OutputStream os = new FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            }
            return out.getAbsolutePath();
        } catch (Exception e) {
            toast("Couldn\u2019t read that photo.");
            return null;
        }
    }

    private void toast(String s) { Notify.toast(requireContext(), s); }

    private void snack(String s) {
        if (getParentFragment() instanceof ShellFragment) ((ShellFragment) getParentFragment()).snack(s, null, null);
    }
}
