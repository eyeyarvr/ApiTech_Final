package com.apitech.ani;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Local SQLite database: users, listings, favorites, basket, orders, reviews and seeds. */
public class Db extends SQLiteOpenHelper {
    private static Db inst;
    public static synchronized Db get(Context c) {
        if (inst == null) inst = new Db(c.getApplicationContext());
        return inst;
    }
    private Db(Context c) { super(c, "ani.db", null, 1); }

    public static class CartItem { public long id; public Product product; public int qty; public String kind, schedule; }
    public static class Order {
        public long id, buyerId, sellerId, productId, createdAt;
        public int qty, total, status;
        public String name, image, kind, schedule, address, otherName, otherPhone;
        public boolean reviewed;
    }
    public static class Seed { public long id; public String name, pack, location, farm; public int price, packs; }

    // ---------------------------------------------------------------- schema
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT, name TEXT, username TEXT UNIQUE, email TEXT UNIQUE, phone TEXT, city TEXT, address TEXT, salt TEXT, hash TEXT, farm_name TEXT, farm_about TEXT)");
        db.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT, seller_id INTEGER, name TEXT, farm TEXT, location TEXT, town TEXT, price INTEGER, unit TEXT, stock INTEGER, preorder INTEGER, image TEXT, same_day INTEGER, description TEXT, status TEXT, updated_at INTEGER)");
        db.execSQL("CREATE TABLE favorites(user_id INTEGER, product_id INTEGER, PRIMARY KEY(user_id, product_id))");
        db.execSQL("CREATE TABLE cart(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, product_id INTEGER, qty INTEGER, kind TEXT, schedule TEXT)");
        db.execSQL("CREATE TABLE orders(id INTEGER PRIMARY KEY AUTOINCREMENT, buyer_id INTEGER, seller_id INTEGER, product_id INTEGER, product_name TEXT, image TEXT, qty INTEGER, total INTEGER, kind TEXT, schedule TEXT, status INTEGER, address TEXT, created_at INTEGER, reviewed INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE reviews(id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER, product_id INTEGER, user_id INTEGER, rating INTEGER, comment TEXT, created_at INTEGER)");
        db.execSQL("CREATE TABLE seeds(id INTEGER PRIMARY KEY AUTOINCREMENT, seller_id INTEGER, name TEXT, pack TEXT, price INTEGER, packs INTEGER, location TEXT, created_at INTEGER)");
        seed(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int o, int n) { }

    /** Demo marketplace so the app is usable on first launch. All demo passwords are demo1234. */
    private void seed(SQLiteDatabase db) {
        long day = 86400000L, now = System.currentTimeMillis();
        long nena = insertUser(db, "farmer", "Nena Pascual", "nena.garden", "nena@ani.demo", "+63 917 555 0101", "La Trinidad", "Purok 3, Strawberry Farm Road", "demo1234", "Lola Nena\u2019s Garden");
        long annie = insertUser(db, "farmer", "Annie Cruz", "annie.place", "annie@ani.demo", "+63 917 555 0102", "Baguio", "5 New Lucban Private Road", "demo1234", "Annie\u2019s Place");
        long sablan = insertUser(db, "farmer", "Joel Bayas", "sablan.sunrise", "joel@ani.demo", "+63 917 555 0103", "Sablan", "House 12, Purok 2, Bayabas", "demo1234", "Sablan Sunrise Farm");
        long tuba = insertUser(db, "farmer", "Tuba Hills", "tuba.hills", "tuba@ani.demo", "+63 917 555 0104", "Tuba", "Green Gate, Purok 6, Poblacion", "demo1234", "Tuba Hills Growers");
        long ramon = insertUser(db, "farmer", "Ramon Dela Cruz", "ramon.grows", "ramon@delacruzfarm.ph", "+63 917 123 4567", "La Trinidad", "Farm House 14, Purok 3, Wangal", "demo1234", "Dela Cruz Highland Farm");
        long maria = insertUser(db, "customer", "Maria Santos", "maria.s", "maria@example.com", "+63 917 123 4567", "Baguio", "Annie\u2019s Place, 5 New Lucban Private Road", "demo1234", null);

        long carrots = insertProduct(db, nena, "Highland Carrots", "Lola Nena\u2019s Garden", "Purok 3, Strawberry Farm Road", "La Trinidad", 85, "kg", 18, 30, Data.CARROTS, 1, now);
        insertProduct(db, annie, "Salad Greens Basket", "Annie\u2019s Place", "5 New Lucban Private Road", "Baguio", 140, "basket", 0, 12, Data.BASKET, 1, now - day);
        long tomatoes = insertProduct(db, sablan, "Vine-Ripe Tomatoes", "Sablan Sunrise Farm", "House 12, Purok 2, Bayabas", "Sablan", 95, "kg", 7, 16, Data.TOMATOES, 1, now - 2 * day);
        insertProduct(db, tuba, "Harvest Vegetable Box", "Tuba Hills Growers", "Green Gate, Purok 6, Poblacion", "Tuba", 380, "box", 4, 0, Data.VEGETABLES, 0, now - 3 * day);
        long rCarrots = insertProduct(db, ramon, "Highland Carrots", "Dela Cruz Highland Farm", "Farm House 14, Purok 3, Wangal", "La Trinidad", 85, "kg", 34, 20, Data.CARROTS, 1, now - 120 * day);
        insertProduct(db, ramon, "Green Lettuce", "Dela Cruz Highland Farm", "Farm House 14, Purok 3, Wangal", "La Trinidad", 55, "head", 18, 10, Data.BASKET, 1, now - 60 * day);
        insertProduct(db, ramon, "Red Tomatoes", "Dela Cruz Highland Farm", "Farm House 14, Purok 3, Wangal", "La Trinidad", 95, "kg", 7, 10, Data.TOMATOES, 1, now - 5 * day);

        insertOrder(db, maria, nena, carrots, "Highland Carrots", Data.CARROTS, 3, 255, "today", null, 2, "Annie\u2019s Place, 5 New Lucban Private Road, Baguio", now - 3 * 3600000L);
        insertOrder(db, maria, sablan, tomatoes, "Vine-Ripe Tomatoes", Data.TOMATOES, 2, 190, "preorder", null, 3, "House 8, Purok 4, Gibraltar Road, Baguio", now - 20 * day);
        insertOrder(db, maria, ramon, rCarrots, "Highland Carrots", Data.CARROTS, 2, 170, "today", null, 0, "Annie\u2019s Place, 5 New Lucban Private Road, Baguio", now - 3600000L);

        ContentValues s = new ContentValues();
        s.put("seller_id", tuba); s.put("name", "Baguio Wonder Bean Seeds"); s.put("pack", "500g"); s.put("price", 620); s.put("packs", 8); s.put("location", "Tublay"); s.put("created_at", now);
        db.insert("seeds", null, s);
        s.put("seller_id", nena); s.put("name", "Highland Carrot Seeds"); s.put("pack", "250g"); s.put("price", 450); s.put("packs", 14); s.put("location", "La Trinidad");
        db.insert("seeds", null, s);
    }

    private long insertUser(SQLiteDatabase db, String role, String name, String username, String email, String phone, String city, String address, String pw, String farm) {
        ContentValues v = new ContentValues();
        String salt = newSalt();
        v.put("role", role); v.put("name", name); v.put("username", username.toLowerCase()); v.put("email", email.toLowerCase());
        v.put("phone", phone); v.put("city", city); v.put("address", address);
        v.put("salt", salt); v.put("hash", hash(pw, salt)); v.put("farm_name", farm);
        return db.insertOrThrow("users", null, v);
    }

    private long insertProduct(SQLiteDatabase db, long seller, String name, String farm, String loc, String town, int price, String unit, int stock, int pre, String image, int sameDay, long updated) {
        ContentValues v = new ContentValues();
        v.put("seller_id", seller); v.put("name", name); v.put("farm", farm); v.put("location", loc); v.put("town", town);
        v.put("price", price); v.put("unit", unit); v.put("stock", stock); v.put("preorder", pre); v.put("image", image);
        v.put("same_day", sameDay); v.put("description", ""); v.put("status", "Active"); v.put("updated_at", updated);
        return db.insertOrThrow("products", null, v);
    }

    private void insertOrder(SQLiteDatabase db, long buyer, long seller, long product, String name, String image, int qty, int total, String kind, String schedule, int status, String address, long created) {
        ContentValues v = new ContentValues();
        v.put("buyer_id", buyer); v.put("seller_id", seller); v.put("product_id", product); v.put("product_name", name);
        v.put("image", image); v.put("qty", qty); v.put("total", total); v.put("kind", kind); v.put("schedule", schedule);
        v.put("status", status); v.put("address", address); v.put("created_at", created);
        db.insert("orders", null, v);
    }

    // ---------------------------------------------------------------- helpers
    private static String s(Cursor c, String col) { return c.getString(c.getColumnIndexOrThrow(col)); }
    private static int i(Cursor c, String col) { return c.getInt(c.getColumnIndexOrThrow(col)); }
    private static long l(Cursor c, String col) { return c.getLong(c.getColumnIndexOrThrow(col)); }

    private static String newSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return Base64.encodeToString(b, Base64.NO_WRAP);
    }
    private static String hash(String pw, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pw.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), 10000, 256);
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
            return Base64.encodeToString(h, Base64.NO_WRAP);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private int scalar(String sql, String... args) {
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }
    private static Profile profile(Cursor c) {
        return new Profile(l(c, "id"), s(c, "role"), s(c, "name"), s(c, "username"), s(c, "email"), s(c, "phone"), s(c, "city"), s(c, "address"));
    }
    private static Product product(Cursor c) {
        return new Product(l(c, "id"), l(c, "seller_id"), s(c, "name"), s(c, "farm"), s(c, "location"), s(c, "town"),
            i(c, "price"), s(c, "unit"), i(c, "stock"), i(c, "preorder"), s(c, "image"), i(c, "same_day") == 1,
            s(c, "description"), s(c, "status"), l(c, "updated_at"));
    }

    // ---------------------------------------------------------------- accounts
    /** Throws IllegalArgumentException with a user-readable message if the details are not valid. */
    public Profile register(String role, String name, String username, String email, String phone, String city, String address, String password) {
        if (password.length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (scalar("SELECT COUNT(*) FROM users WHERE email=?", email.toLowerCase()) > 0)
            throw new IllegalArgumentException("That email is already registered. Try logging in.");
        if (scalar("SELECT COUNT(*) FROM users WHERE username=?", username.toLowerCase()) > 0)
            throw new IllegalArgumentException("That username is taken.");
        long id = insertUser(getWritableDatabase(), role, name, username, email, phone, city, address, password,
                "farmer".equals(role) ? name + "\u2019s Farm" : null);
        return getUser(id);
    }

    /** Accepts an email or a username. Returns null when the credentials are wrong. */
    public Profile login(String idOrEmail, String password) {
        String key = idOrEmail.trim().toLowerCase();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM users WHERE email=? OR username=?", new String[]{key, key});
        try {
            if (!c.moveToFirst()) return null;
            if (!hash(password, s(c, "salt")).equals(s(c, "hash"))) return null;
            return profile(c);
        } finally { c.close(); }
    }

    public Profile getUser(long id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM users WHERE id=?", new String[]{"" + id});
        try { return c.moveToFirst() ? profile(c) : null; } finally { c.close(); }
    }

    public void updateProfile(Profile p) {
        ContentValues v = new ContentValues();
        v.put("name", p.name); v.put("phone", p.phone); v.put("city", p.city); v.put("address", p.address);
        getWritableDatabase().update("users", v, "id=?", new String[]{"" + p.id});
    }

    public void setRole(long id, String role) {
        ContentValues v = new ContentValues();
        v.put("role", role);
        getWritableDatabase().update("users", v, "id=?", new String[]{"" + id});
        if ("farmer".equals(role)) {
            Cursor c = getReadableDatabase().rawQuery("SELECT farm_name, name FROM users WHERE id=?", new String[]{"" + id});
            try {
                if (c.moveToFirst() && (c.getString(0) == null || c.getString(0).isEmpty())) setFarmName(id, c.getString(1) + "\u2019s Farm");
            } finally { c.close(); }
        }
    }

    public String farmName(long id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT farm_name FROM users WHERE id=?", new String[]{"" + id});
        try { return c.moveToFirst() && c.getString(0) != null ? c.getString(0) : ""; } finally { c.close(); }
    }
    public void setFarmName(long id, String name) {
        ContentValues v = new ContentValues();
        v.put("farm_name", name);
        getWritableDatabase().update("users", v, "id=?", new String[]{"" + id});
        ContentValues p = new ContentValues();
        p.put("farm", name);
        getWritableDatabase().update("products", p, "seller_id=?", new String[]{"" + id});
    }

    // ---------------------------------------------------------------- products
    public List<Product> listProducts(long excludeSeller, String query, String town) {
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE status='Active' AND seller_id<>?");
        List<String> args = new ArrayList<>();
        args.add("" + excludeSeller);
        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (name LIKE ? OR farm LIKE ? OR description LIKE ?)");
            String q = "%" + query.trim() + "%";
            args.add(q); args.add(q); args.add(q);
        }
        if (town != null) { sql.append(" AND town=?"); args.add(town); }
        sql.append(" ORDER BY updated_at DESC");
        return queryProducts(sql.toString(), args.toArray(new String[0]));
    }
    private List<Product> queryProducts(String sql, String... args) {
        List<Product> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try { while (c.moveToNext()) out.add(product(c)); } finally { c.close(); }
        return out;
    }
    public Product getProduct(long id) {
        List<Product> l = queryProducts("SELECT * FROM products WHERE id=?", "" + id);
        return l.isEmpty() ? null : l.get(0);
    }
    public List<Product> sellerProducts(long sellerId) {
        return queryProducts("SELECT * FROM products WHERE seller_id=? ORDER BY updated_at DESC", "" + sellerId);
    }
    public int townCount(String town) {
        return scalar("SELECT COUNT(*) FROM products WHERE status='Active' AND town=?", town);
    }
    public long addProduct(long seller, String name, String farm, String location, String town, int price, String unit, int stock, int preorder, String image, boolean sameDay, String desc) {
        ContentValues v = new ContentValues();
        v.put("seller_id", seller); v.put("name", name); v.put("farm", farm); v.put("location", location); v.put("town", town);
        v.put("price", price); v.put("unit", unit); v.put("stock", stock); v.put("preorder", preorder); v.put("image", image);
        v.put("same_day", sameDay ? 1 : 0); v.put("description", desc); v.put("status", "Active"); v.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow("products", null, v);
    }
    public void updateListing(long id, int price, int stock, String status) {
        ContentValues v = new ContentValues();
        v.put("price", price); v.put("stock", stock); v.put("status", status); v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("products", v, "id=?", new String[]{"" + id});
    }
    public void deleteProduct(long id) {
        getWritableDatabase().delete("products", "id=?", new String[]{"" + id});
        getWritableDatabase().delete("favorites", "product_id=?", new String[]{"" + id});
        getWritableDatabase().delete("cart", "product_id=?", new String[]{"" + id});
    }

    // ---------------------------------------------------------------- favorites
    public boolean isFav(long uid, long pid) {
        return scalar("SELECT COUNT(*) FROM favorites WHERE user_id=? AND product_id=?", "" + uid, "" + pid) > 0;
    }
    /** Returns the new state: true when the crop is now saved. */
    public boolean toggleFav(long uid, long pid) {
        if (isFav(uid, pid)) {
            getWritableDatabase().delete("favorites", "user_id=? AND product_id=?", new String[]{"" + uid, "" + pid});
            return false;
        }
        ContentValues v = new ContentValues();
        v.put("user_id", uid); v.put("product_id", pid);
        getWritableDatabase().insert("favorites", null, v);
        return true;
    }
    public List<Product> favorites(long uid) {
        return queryProducts("SELECT p.* FROM products p JOIN favorites f ON f.product_id=p.id WHERE f.user_id=?", "" + uid);
    }

    // ---------------------------------------------------------------- basket
    public void addToCart(long uid, long pid, int qty, String kind, String schedule) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT id, qty FROM cart WHERE user_id=? AND product_id=? AND kind=?", new String[]{"" + uid, "" + pid, kind});
        try {
            if (c.moveToFirst()) {
                db.execSQL("UPDATE cart SET qty=?, schedule=? WHERE id=?", new Object[]{c.getInt(1) + qty, schedule, c.getLong(0)});
                return;
            }
        } finally { c.close(); }
        ContentValues v = new ContentValues();
        v.put("user_id", uid); v.put("product_id", pid); v.put("qty", qty); v.put("kind", kind); v.put("schedule", schedule);
        db.insert("cart", null, v);
    }
    public List<CartItem> cart(long uid) {
        List<CartItem> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT c.id AS cid, c.qty AS cqty, c.kind AS ckind, c.schedule AS csched, p.* FROM cart c JOIN products p ON p.id=c.product_id WHERE c.user_id=?", new String[]{"" + uid});
        try {
            while (c.moveToNext()) {
                CartItem it = new CartItem();
                it.id = l(c, "cid"); it.qty = i(c, "cqty"); it.kind = s(c, "ckind"); it.schedule = s(c, "csched");
                it.product = product(c);
                out.add(it);
            }
        } finally { c.close(); }
        return out;
    }
    public int cartCount(long uid) { return scalar("SELECT COALESCE(SUM(qty),0) FROM cart WHERE user_id=?", "" + uid); }
    public void setCartQty(long id, int qty) {
        if (qty <= 0) removeCart(id);
        else getWritableDatabase().execSQL("UPDATE cart SET qty=? WHERE id=?", new Object[]{qty, id});
    }
    public void removeCart(long id) { getWritableDatabase().delete("cart", "id=?", new String[]{"" + id}); }

    /** Turns the basket into orders and reduces stock. Returns null on success or an error message. */
    public String checkout(long uid, String address) {
        SQLiteDatabase db = getWritableDatabase();
        List<CartItem> items = cart(uid);
        if (items.isEmpty()) return "Your basket is empty.";
        db.beginTransaction();
        try {
            for (CartItem it : items) {
                Product p = getProduct(it.product.id);
                if (p == null || !"Active".equals(p.status)) return it.product.name + " is no longer available.";
                if ("today".equals(it.kind)) {
                    if (p.stock < it.qty) return "Only " + p.stock + " " + p.unit + " of " + p.name + " left for same-day.";
                    db.execSQL("UPDATE products SET stock=stock-? WHERE id=?", new Object[]{it.qty, p.id});
                } else {
                    if (p.preorder < it.qty) return "Only " + p.preorder + " pre-order slots left for " + p.name + ".";
                    db.execSQL("UPDATE products SET preorder=preorder-? WHERE id=?", new Object[]{it.qty, p.id});
                }
                insertOrder(db, uid, p.sellerId, p.id, p.name, p.image, it.qty, p.price * it.qty, it.kind, it.schedule, 0, address, System.currentTimeMillis());
            }
            db.delete("cart", "user_id=?", new String[]{"" + uid});
            db.setTransactionSuccessful();
            return null;
        } finally { db.endTransaction(); }
    }

    // ---------------------------------------------------------------- orders
    public List<Order> orders(long uid, boolean asSeller) {
        String mine = asSeller ? "seller_id" : "buyer_id", other = asSeller ? "buyer_id" : "seller_id";
        List<Order> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT o.*, u.name AS oname, u.phone AS ophone FROM orders o JOIN users u ON u.id=o." + other + " WHERE o." + mine + "=? ORDER BY o.created_at DESC", new String[]{"" + uid});
        try {
            while (c.moveToNext()) {
                Order o = new Order();
                o.id = l(c, "id"); o.buyerId = l(c, "buyer_id"); o.sellerId = l(c, "seller_id"); o.productId = l(c, "product_id");
                o.name = s(c, "product_name"); o.image = s(c, "image"); o.qty = i(c, "qty"); o.total = i(c, "total");
                o.kind = s(c, "kind"); o.schedule = s(c, "schedule"); o.status = i(c, "status"); o.address = s(c, "address");
                o.createdAt = l(c, "created_at"); o.reviewed = i(c, "reviewed") == 1;
                o.otherName = s(c, "oname"); o.otherPhone = s(c, "ophone");
                out.add(o);
            }
        } finally { c.close(); }
        return out;
    }
    public void advance(long orderId) { getWritableDatabase().execSQL("UPDATE orders SET status=status+1 WHERE id=? AND status<3", new Object[]{orderId}); }

    public void addReview(Order o, long userId, int rating, String comment) {
        ContentValues v = new ContentValues();
        v.put("order_id", o.id); v.put("product_id", o.productId); v.put("user_id", userId);
        v.put("rating", rating); v.put("comment", comment); v.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insert("reviews", null, v);
        getWritableDatabase().execSQL("UPDATE orders SET reviewed=1 WHERE id=?", new Object[]{o.id});
    }

    // ---------------------------------------------------------------- seller stats
    public int ordersBetween(long seller, long from, long to) {
        return scalar("SELECT COUNT(*) FROM orders WHERE seller_id=? AND created_at>=? AND created_at<?", "" + seller, "" + from, "" + to);
    }
    public int salesBetween(long seller, long from, long to) {
        return scalar("SELECT COALESCE(SUM(total),0) FROM orders WHERE seller_id=? AND created_at>=? AND created_at<?", "" + seller, "" + from, "" + to);
    }
    public int totalStock(long seller) { return scalar("SELECT COALESCE(SUM(stock),0) FROM products WHERE seller_id=? AND status='Active'", "" + seller); }
    public int activeListings(long seller) { return scalar("SELECT COUNT(*) FROM products WHERE seller_id=? AND status='Active'", "" + seller); }
    public int orderCount(long seller) { return scalar("SELECT COUNT(*) FROM orders WHERE seller_id=?", "" + seller); }
    public double avgRating(long seller) {
        Cursor c = getReadableDatabase().rawQuery("SELECT AVG(r.rating) FROM reviews r JOIN products p ON p.id=r.product_id WHERE p.seller_id=?", new String[]{"" + seller});
        try { return c.moveToFirst() && !c.isNull(0) ? c.getDouble(0) : 0; } finally { c.close(); }
    }
    public Map<String, Integer> supplyByTown() {
        Map<String, Integer> out = new LinkedHashMap<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT town, COUNT(*) AS n FROM products WHERE status='Active' GROUP BY town ORDER BY n DESC", null);
        try { while (c.moveToNext()) out.put(c.getString(0), c.getInt(1)); } finally { c.close(); }
        return out;
    }

    // ---------------------------------------------------------------- seeds
    public List<Seed> listSeeds() {
        List<Seed> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT s.*, u.farm_name AS fname, u.name AS uname FROM seeds s JOIN users u ON u.id=s.seller_id ORDER BY s.created_at DESC", null);
        try {
            while (c.moveToNext()) {
                Seed d = new Seed();
                d.id = l(c, "id"); d.name = s(c, "name"); d.pack = s(c, "pack"); d.price = i(c, "price"); d.packs = i(c, "packs");
                d.location = s(c, "location");
                String f = s(c, "fname");
                d.farm = f == null || f.isEmpty() ? s(c, "uname") : f;
                out.add(d);
            }
        } finally { c.close(); }
        return out;
    }
    public void addSeed(long seller, String name, String pack, int price, int packs, String location) {
        ContentValues v = new ContentValues();
        v.put("seller_id", seller); v.put("name", name); v.put("pack", pack); v.put("price", price); v.put("packs", packs);
        v.put("location", location); v.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insert("seeds", null, v);
    }
}
