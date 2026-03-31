package com.yazlab.user;

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
 * FAZ 4 - A: User Service CRUD endpoint testleri (TDD).
 *
 * RMM Seviye 2:
 *   POST   /users        → 201 Created  + Location header
 *   GET    /users/{id}   → 200 OK       (mevcut) | 404 (yok)
 *   PUT    /users/{id}   → 200 OK
 *   DELETE /users/{id}   → 204 No Content
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserCrudTest {

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
    void createUser_shouldReturn201AndLocationHeader() throws Exception {
        String body = """
                {"username": "alice", "email": "alice@test.com", "fullName": "Alice Smith"}
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void createUser_withMissingUsername_shouldReturn400() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "noname@test.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    @Test
    void getUser_withExistingId_shouldReturn200AndUser() throws Exception {
        // Önce kullanıcı oluştur
        MvcResult created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "bob", "email": "bob@test.com", "fullName": "Bob Jones"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    void getUser_withNonExistingId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/users/{id}", "000000000000000000000000"))
                .andExpect(status().isNotFound());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    void updateUser_withExistingId_shouldReturn200AndUpdatedUser() throws Exception {
        MvcResult created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "carol", "email": "carol@test.com", "fullName": "Carol White"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(put("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "carol", "email": "carol@test.com", "fullName": "Carol Black"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Carol Black"));
    }

    @Test
    void updateUser_withNonExistingId_shouldReturn404() throws Exception {
        mockMvc.perform(put("/users/{id}", "000000000000000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "ghost", "email": "ghost@test.com", "fullName": "Ghost"}
                                """))
                .andExpect(status().isNotFound());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void deleteUser_withExistingId_shouldReturn204() throws Exception {
        MvcResult created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "dave", "email": "dave@test.com", "fullName": "Dave Brown"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent());

        // Silindiğini doğrula
        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_withNonExistingId_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/users/{id}", "000000000000000000000000"))
                .andExpect(status().isNotFound());
    }
}
