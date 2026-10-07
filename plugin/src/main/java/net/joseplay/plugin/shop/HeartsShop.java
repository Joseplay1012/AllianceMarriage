package net.joseplay.plugin.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HeartsShop {
    private final Map<String, HeartsShopItem> items = new LinkedHashMap<>();
    private final ShopLayout layout;
    private final HeartsShopLoader loader;

    public HeartsShop(HeartsShopLoader loader, ShopLayout layout) {
        this.loader = loader;
        this.layout = layout != null ? layout : new HeartShopLayout();
        load();
    }

    public synchronized void load() {
        items.clear();
        items.putAll(loader.load());
    }

    public synchronized Map<String, HeartsShopItem> getItems() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(items));
    }

    public synchronized List<HeartsShopItem> getItemList() {
        return Collections.unmodifiableList(new ArrayList<>(items.values()));
    }

    public synchronized HeartsShopItem getItem(String id) {
        return items.get(id);
    }

    public ShopLayout getLayout() {
        return layout;
    }
}
