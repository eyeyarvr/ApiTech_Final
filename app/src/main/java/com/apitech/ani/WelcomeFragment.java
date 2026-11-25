package com.apitech.ani;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Port of the Welcome component (intro / sign up in 2 steps / login). */
public class WelcomeFragment extends Fragment {
    private int step = 1;
    private String role = "customer";

    private View introCard, signupCard, loginCard, step1, step2, farmerQuestion, roleCustomer, roleFarmer;
    private TextView tvStep, tvTitle, tvLead, btnContinue;
    private EditText etName, etUsername, etEmail, etPhone, etAddress, etPassword, etLoginEmail, etLoginPassword;
    private Spinner spCity;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_welcome, container, false);
        Ui.image(v.findViewById(R.id.heroImage), Data.HERO);

        introCard = v.findViewById(R.id.introCard);
        signupCard = v.findViewById(R.id.signupCard);
        loginCard = v.findViewById(R.id.loginCard);
        step1 = v.findViewById(R.id.step1);
        step2 = v.findViewById(R.id.step2);
        farmerQuestion = v.findViewById(R.id.farmerQuestion);
        roleCustomer = v.findViewById(R.id.roleCustomer);
        roleFarmer = v.findViewById(R.id.roleFarmer);
        tvStep = v.findViewById(R.id.tvStep);
        tvTitle = v.findViewById(R.id.tvSignupTitle);
        tvLead = v.findViewById(R.id.tvSignupLead);
        btnContinue = v.findViewById(R.id.btnContinue);
        etName = v.findViewById(R.id.etName);
        etUsername = v.findViewById(R.id.etUsername);
        etEmail = v.findViewById(R.id.etEmail);
        etPhone = v.findViewById(R.id.etPhone);
        etAddress = v.findViewById(R.id.etAddress);
        etPassword = v.findViewById(R.id.etPassword);
        etLoginEmail = v.findViewById(R.id.etLoginEmail);
        etLoginPassword = v.findViewById(R.id.etLoginPassword);
        spCity = v.findViewById(R.id.spCity);

        List<String> cities = new ArrayList<>();
        cities.add("Select location");
        cities.addAll(Arrays.asList(Data.CITIES));
        spCity.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, cities));

        SessionHost host = (SessionHost) requireActivity();
        v.findViewById(R.id.btnCreate).setOnClickListener(b -> showMode("form"));
        v.findViewById(R.id.btnGoLogin).setOnClickListener(b -> showMode("login"));
        v.findViewById(R.id.btnGoSignup).setOnClickListener(b -> showMode("form"));
        v.findViewById(R.id.btnBackLogin).setOnClickListener(b -> showMode("intro"));
        v.findViewById(R.id.btnDemoCustomer).setOnClickListener(b -> demoLogin(host, "maria@example.com"));
        v.findViewById(R.id.btnDemoFarmer).setOnClickListener(b -> demoLogin(host, "ramon@delacruzfarm.ph"));
        v.findViewById(R.id.btnBackSignup).setOnClickListener(b -> {
            if (step == 1) showMode("intro"); else setStep(1);
        });
        roleCustomer.setOnClickListener(b -> setRole("customer"));
        roleFarmer.setOnClickListener(b -> setRole("farmer"));
        btnContinue.setOnClickListener(b -> submit(host));

        v.findViewById(R.id.btnLogin).setOnClickListener(b -> {
            String id = etLoginEmail.getText().toString().trim();
            String pw = etLoginPassword.getText().toString();
            if (id.isEmpty()) { etLoginEmail.setError("Required"); return; }
            if (pw.isEmpty()) { etLoginPassword.setError("Required"); return; }
            Profile p = Db.get(requireContext()).login(id, pw);
            if (p == null) { Notify.snack(v, "Wrong email or password."); return; }
            host.onEnter(p);
        });
        return v;
    }

    private void demoLogin(SessionHost host, String email) {
        Profile p = Db.get(requireContext()).login(email, "demo1234");
        if (p != null) host.onEnter(p);
    }

    private void showMode(String mode) {
        Ui.show(introCard, mode.equals("intro"));
        Ui.show(signupCard, mode.equals("form"));
        Ui.show(loginCard, mode.equals("login"));
    }

    private void setStep(int s) {
        step = s;
        Ui.show(step1, s == 1);
        Ui.show(step2, s == 2);
        tvStep.setText("STEP " + s + " OF 2");
        tvTitle.setText(s == 1 ? "Tell us about you" : "How will you use ApiTech?");
        tvLead.setText(s == 1 ? "A few details will help us make your local market feel personal."
                              : "Choose what feels right. You can always update this later.");
        btnContinue.setText(s == 1 ? "Continue  →" : "Finish registration  →");
    }

    private void setRole(String r) {
        role = r;
        roleCustomer.setBackgroundResource(r.equals("customer") ? R.drawable.bg_option_selected : R.drawable.bg_option);
        roleFarmer.setBackgroundResource(r.equals("farmer") ? R.drawable.bg_option_selected : R.drawable.bg_option);
        Ui.show(farmerQuestion, r.equals("farmer"));
    }

    private boolean required(EditText e) {
        if (e.getText().toString().trim().isEmpty()) { e.setError("Required"); return false; }
        return true;
    }

    private void submit(SessionHost host) {
        if (step == 1) {
            boolean ok = required(etName) & required(etUsername) & required(etEmail)
                    & required(etPhone) & required(etAddress) & required(etPassword);
            if (spCity.getSelectedItemPosition() == 0) { ok = false; Notify.toast(requireContext(), "Please select your location."); }
            if (ok) setStep(2);
            return;
        }
        try {
            Profile p = Db.get(requireContext()).register(role,
                    etName.getText().toString().trim(), etUsername.getText().toString().trim(),
                    etEmail.getText().toString().trim(), etPhone.getText().toString().trim(),
                    (String) spCity.getSelectedItem(), etAddress.getText().toString().trim(),
                    etPassword.getText().toString());
            host.onEnter(p);
        } catch (IllegalArgumentException e) {
            Notify.toastLong(requireContext(), e.getMessage());
            setStep(1);
        }
    }
}
