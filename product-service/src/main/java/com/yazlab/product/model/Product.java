package com.yazlab.product.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String name;
    private String description;
    private Double price;
    private Integer stock;

    public Product() {}

    public Product(String name, String description, Double price, Integer stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    public String getId()           { return id; }
    public String getName()         { return name; }
    public String getDescription()  { return description; }
    public Double getPrice()        { return price; }
    public Integer getStock()       { return stock; }

    public void setId(String id)                   { this.id = id; }
    public void setName(String name)               { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(Double price)             { this.price = price; }
    public void setStock(Integer stock)            { this.stock = stock; }
}
