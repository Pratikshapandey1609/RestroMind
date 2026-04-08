package com.restromind.app.menu.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "categories", schema = "restromind_menu",
    uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "name"}))
public class Category {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long restaurantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int sortIndex = 0;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private final List<Dish> dishes = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    public Category() {}

    public Long getId() { return id; }
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSortIndex() { return sortIndex; }
    public void setSortIndex(int sortIndex) { this.sortIndex = sortIndex; }
    public List<Dish> getDishes() { return dishes; }
    public Instant getCreatedAt() { return createdAt; }
}
