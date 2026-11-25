package com.apitech.ani;

import java.io.Serializable;
import java.util.Calendar;

/** A crop listing stored in the products table. */
public class Product implements Serializable {
    public final long id, sellerId, updatedAt;
    public final int price, stock, preorder;
    public final String name, farm, location, town, unit, image, description, status;
    public final boolean sameDay;

    public Product(long id, long sellerId, String name, String farm, String location, String town,
                   int price, String unit, int stock, int preorder, String image, boolean sameDay,
                   String description, String status, long updatedAt) {
        this.id = id; this.sellerId = sellerId; this.name = name; this.farm = farm;
        this.location = location; this.town = town; this.price = price; this.unit = unit;
        this.stock = stock; this.preorder = preorder; this.image = image; this.sameDay = sameDay;
        this.description = description; this.status = status; this.updatedAt = updatedAt;
    }

    public String tag() {
        if (stock == 0 && preorder > 0) return "Pre-order";
        if (stock == 0) return "Sold out";
        if (stock <= 5) return "Limited";
        return "Fresh today";
    }

    /** Returns why same-day delivery is not possible for this buyer, or null if it is. */
    public String sameDayBlock(String buyerCity) {
        if (!sameDay) return "This seller isn\u2019t accepting same-day orders.";
        if (stock <= 0) return "Out of stock for same-day orders. The next harvest is available for pre-order.";
        if (!Data.isNear(buyerCity, town))
            return "Same-day delivery isn\u2019t available because this farm is too far from your address.";
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 17)
            return "Same-day orders closed at 5 PM. You can pre-order for another day.";
        return null;
    }

    public String note(String buyerCity) {
        String b = sameDayBlock(buyerCity);
        if (b == null) return null;
        return preorder <= 0 ? b + " Pre-order slots are also full." : b;
    }
}
