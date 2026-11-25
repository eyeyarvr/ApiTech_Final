package com.apitech.ani;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Shared constants: photos, towns and the delivery-distance rule. */
public final class Data {
    private Data() {}

    public static final String HERO = "https://images.unsplash.com/photo-1567306295427-94503f8300d7?auto=format&fit=crop&w=1600&q=85";
    public static final String CARROTS = "https://images.unsplash.com/photo-1474440692490-2e83ae13ba29?auto=format&fit=crop&w=900&q=85";
    public static final String BASKET = "https://images.unsplash.com/photo-1624668430039-0175a0fbf006?auto=format&fit=crop&w=900&q=85";
    public static final String TOMATOES = "https://images.unsplash.com/photo-1631021967261-c57ee4dfa9bb?auto=format&fit=crop&w=900&q=85";
    public static final String VEGETABLES = "https://images.unsplash.com/photo-1627989147125-a004d05946d3?auto=format&fit=crop&w=900&q=85";

    public static final String[] CITIES = {"Baguio", "La Trinidad", "Itogon", "Sablan", "Tuba", "Tublay"};
    public static final String[] UNITS = {"kg", "head", "box", "basket", "pack"};
    public static final String[] STATUSES = {"Active", "Paused", "Sold out"};
    public static final String[] STEPS = {"Confirmed", "Packed", "On the way", "Delivered"};

    /** Towns close enough for same-day delivery (pairs are symmetric). */
    private static final Set<String> NEAR = new HashSet<>(Arrays.asList(
        "Baguio|La Trinidad", "Baguio|Itogon", "Baguio|Tuba", "La Trinidad|Tublay",
        "La Trinidad|Itogon", "Itogon|Tuba", "Tuba|Sablan", "Tublay|Sablan"));

    public static boolean isNear(String a, String b) {
        if (a == null || b == null) return false;
        return a.equals(b) || NEAR.contains(a + "|" + b) || NEAR.contains(b + "|" + a);
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && sb.length() < 2) sb.append(part.charAt(0));
        }
        return sb.toString().toUpperCase();
    }
}
