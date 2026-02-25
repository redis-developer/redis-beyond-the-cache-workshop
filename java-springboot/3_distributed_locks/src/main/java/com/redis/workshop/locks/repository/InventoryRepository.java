package com.redis.workshop.locks.repository;

import com.redis.workshop.locks.model.Inventory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryRepository {
    private static final long DEFAULT_ID = 1L;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Inventory> inventoryMapper = (rs, rowNum) -> new Inventory(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getInt("quantity")
    );

    public InventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Inventory getDefaultInventory() {
        return jdbcTemplate.queryForObject(
            "SELECT id, name, quantity FROM inventory WHERE id = ?",
            inventoryMapper,
            DEFAULT_ID
        );
    }

    public int updateQuantity(long id, int newQuantity) {
        return jdbcTemplate.update(
            "UPDATE inventory SET quantity = ? WHERE id = ?",
            newQuantity,
            id
        );
    }

    public void resetInventory(int quantity) {
        jdbcTemplate.update(
            "UPDATE inventory SET quantity = ? WHERE id = ?",
            quantity,
            DEFAULT_ID
        );
    }
}
