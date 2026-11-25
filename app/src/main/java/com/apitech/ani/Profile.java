package com.apitech.ani;

import java.io.Serializable;

/** A signed-up user. role is "customer" or "farmer". */
public class Profile implements Serializable {
    public long id;
    public String role, name, username, email, phone, city, address;

    public Profile(long id, String role, String name, String username, String email,
                   String phone, String city, String address) {
        this.id = id; this.role = role; this.name = name; this.username = username;
        this.email = email; this.phone = phone; this.city = city; this.address = address;
    }

    public boolean isFarmer() { return "farmer".equals(role); }
}
