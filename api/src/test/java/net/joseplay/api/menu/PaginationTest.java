package net.joseplay.api.menu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginationTest {

    @Test
    void testEmptyList() {
        Pagination<String> pagination = new Pagination<>(10);
        assertEquals(1, pagination.getTotalPages());
        assertEquals(0, pagination.getTotalItems());
        assertTrue(pagination.getItemsForCurrentPage().isEmpty());
        assertFalse(pagination.hasNextPage());
        assertFalse(pagination.hasPreviousPage());
    }

    @Test
    void testPaginationMath() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        Pagination<Integer> pagination = new Pagination<>(numbers, 5);

        assertEquals(3, pagination.getTotalPages());
        assertEquals(12, pagination.getTotalItems());
        assertEquals(1, pagination.getCurrentPage());

        List<Integer> page1 = pagination.getItemsForCurrentPage();
        assertEquals(List.of(1, 2, 3, 4, 5), page1);
        assertTrue(pagination.hasNextPage());
        assertFalse(pagination.hasPreviousPage());

        assertTrue(pagination.nextPage());
        assertEquals(2, pagination.getCurrentPage());
        List<Integer> page2 = pagination.getItemsForCurrentPage();
        assertEquals(List.of(6, 7, 8, 9, 10), page2);
        assertTrue(pagination.hasNextPage());
        assertTrue(pagination.hasPreviousPage());

        assertTrue(pagination.nextPage());
        assertEquals(3, pagination.getCurrentPage());
        List<Integer> page3 = pagination.getItemsForCurrentPage();
        assertEquals(List.of(11, 12), page3);
        assertFalse(pagination.hasNextPage());
        assertTrue(pagination.hasPreviousPage());

        // Cannot go past last page
        assertFalse(pagination.nextPage());
        assertEquals(3, pagination.getCurrentPage());

        // Previous page
        assertTrue(pagination.previousPage());
        assertEquals(2, pagination.getCurrentPage());

        // First and last
        assertTrue(pagination.firstPage());
        assertEquals(1, pagination.getCurrentPage());
        assertFalse(pagination.firstPage());

        assertTrue(pagination.lastPage());
        assertEquals(3, pagination.getCurrentPage());
        assertFalse(pagination.lastPage());
    }

    @Test
    void testSetPageBounds() {
        List<String> items = List.of("A", "B", "C");
        Pagination<String> pagination = new Pagination<>(items, 2);
        assertEquals(2, pagination.getTotalPages());

        pagination.setCurrentPage(999);
        assertEquals(2, pagination.getCurrentPage());

        pagination.setCurrentPage(-5);
        assertEquals(1, pagination.getCurrentPage());
    }
}
