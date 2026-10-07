package net.joseplay.api.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pagination<T> {
    private List<T> items;
    private int itemsPerPage;
    private int currentPage = 1;

    public Pagination(int itemsPerPage) {
        this(new ArrayList<>(), itemsPerPage);
    }

    public Pagination(List<T> items, int itemsPerPage) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        setItemsPerPage(itemsPerPage);
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {
        if (itemsPerPage <= 0) {
            throw new IllegalArgumentException("itemsPerPage must be greater than 0");
        }
        this.itemsPerPage = itemsPerPage;
        ensurePageBounds();
    }

    public List<T> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<T> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        ensurePageBounds();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        int total = getTotalPages();
        if (page < 1) {
            this.currentPage = 1;
        } else if (page > total) {
            this.currentPage = Math.max(1, total);
        } else {
            this.currentPage = page;
        }
    }

    public int getTotalPages() {
        if (items.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) items.size() / itemsPerPage);
    }

    public int getTotalItems() {
        return items.size();
    }

    public List<T> getItemsForCurrentPage() {
        return getItemsForPage(currentPage);
    }

    public List<T> getItemsForPage(int page) {
        if (items.isEmpty() || page < 1 || page > getTotalPages()) {
            return Collections.emptyList();
        }

        int fromIndex = (page - 1) * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, items.size());

        if (fromIndex >= items.size()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(items.subList(fromIndex, toIndex));
    }

    public boolean hasNextPage() {
        return currentPage < getTotalPages();
    }

    public boolean hasPreviousPage() {
        return currentPage > 1;
    }

    public boolean nextPage() {
        if (hasNextPage()) {
            currentPage++;
            return true;
        }
        return false;
    }

    public boolean previousPage() {
        if (hasPreviousPage()) {
            currentPage--;
            return true;
        }
        return false;
    }

    public boolean firstPage() {
        if (currentPage != 1) {
            currentPage = 1;
            return true;
        }
        return false;
    }

    public boolean lastPage() {
        int total = getTotalPages();
        if (currentPage != total) {
            currentPage = total;
            return true;
        }
        return false;
    }

    private void ensurePageBounds() {
        int total = getTotalPages();
        if (currentPage > total) {
            currentPage = Math.max(1, total);
        } else if (currentPage < 1) {
            currentPage = 1;
        }
    }
}
