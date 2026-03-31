package com.yazlab.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FAZ 4 - A: Product Service CRUD endpoint testleri (TDD).
 *
 * RMM Seviye 2:
 *   POST   /products        → 201 Created  + Location header
 *   GET    /products/{id}   → 200 OK       (mevcut) | 404 (yok)
 *   PUT    /products/{id}   → 200 OK
 *   DELETE /products/{id}   → 204 No Content
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCrudTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanDb() {
        mongoTemplate.getDb().drop();
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Test
    void createProduct_shouldReturn201AndLocationHeader() throws Exception {
        String body = """
                {"name": "Laptop", "description": "High-end laptop", "price": 1500.00, "stock": 10}
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void createProduct_withMissingName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price": 99.99}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    @Test
    void getProduct_withExistingId_shouldReturn200AndProduct() throws Exception {
        MvcResult created = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mouse", "description": "Wireless mouse", "price": 29.99, "stock": 50}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Mouse"));
    }

    @Test
    void getProduct_withNonExistingId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/products/{id}", "000000000000000000000000"))
                .andExpect(status().isNotFound());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    void updateProduct_withExistingId_shouldReturn200AndUpdatedProduct() throws Exception {
        MvcResult created = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Keyboard", "description": "Mechanical", "price": 79.99, "stock": 20}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Keyboard", "description": "Mechanical RGB", "price": 89.99, "stock": 15}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Mechanical RGB"))
                .andExpect(jsonPath("$.price").value(89.99));
    }

    @Test
    void updateProduct_withNonExistingId_shouldReturn404() throws Exception {
        mockMvc.perform(put("/products/{id}", "000000000000000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ghost", "description": "N/A", "price": 0.0, "stock": 0}
                                """))
                .andExpect(status().isNotFound());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void deleteProduct_withExistingId_shouldReturn204() throws Exception {
        MvcResult created = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Monitor", "description": "4K display", "price": 399.99, "stock": 5}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_withNonExistingId_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/products/{id}", "000000000000000000000000"))
                .andExpect(status().isNotFound());
    }
}
