package com.restromind.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.oneOf;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restromind.app.auth.dto.AuthResponse;
import com.restromind.app.auth.dto.LoginRequest;
import com.restromind.app.auth.dto.RegisterRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class RestroMindIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    // Shared state across ordered tests
    static String adminToken;
    static String userToken;
    static Long restaurantId;
    static Long categoryId;
    static Long dishId;

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Test @Order(1)
    void register_admin_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("admin@test.com");
        req.setPassword("password123");
        req.setFullName("Test Admin");
        req.setRole("ADMIN");

        mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test @Order(2)
    void register_user_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@test.com");
        req.setPassword("password123");
        req.setFullName("Test User");
        req.setRole("USER");

        mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test @Order(3)
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("admin@test.com");
        req.setPassword("password123");
        req.setFullName("Dup");
        req.setRole("ADMIN");

        mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    @Test @Order(4)
    void login_admin_savesToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("password123");

        MvcResult result = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        AuthResponse resp = mapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        adminToken = resp.getAccessToken();
        assertThat(adminToken).isNotBlank();
    }

    @Test @Order(5)
    void login_user_savesToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("password123");

        MvcResult result = mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andReturn();

        AuthResponse resp = mapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        userToken = resp.getAccessToken();
    }

    @Test @Order(6)
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("wrong");

        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    // ── Restaurant Onboarding ─────────────────────────────────────────────────

    @Test @Order(10)
    void onboarding_step1_returns201() throws Exception {
        String body = """
            {"name":"Pizza Palace","logoUrl":"https://logo.png"}
            """;

        MvcResult result = mvc.perform(post("/restaurants/onboarding/step1")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.onboardingStep").value(1))
            .andReturn();

        var node = mapper.readTree(result.getResponse().getContentAsString());
        restaurantId = node.get("id").asLong();
    }

    @Test @Order(11)
    void onboarding_step1_missingName_returns400() throws Exception {
        mvc.perform(post("/restaurants/onboarding/step1")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test @Order(12)
    void onboarding_step1_noToken_returns403() throws Exception {
        mvc.perform(post("/restaurants/onboarding/step1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\"}"))
            .andExpect(status().isForbidden());
    }

    @Test @Order(13)
    void onboarding_step1_userRole_returns403() throws Exception {
        mvc.perform(post("/restaurants/onboarding/step1")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\"}"))
            .andExpect(status().isForbidden());
    }

    @Test @Order(14)
    void onboarding_step2_returns200() throws Exception {
        String body = """
            {"cuisineType":"ITALIAN","description":"Best pizza in town"}
            """;

        mvc.perform(post("/restaurants/onboarding/step2")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onboardingStep").value(2));
    }

    @Test @Order(15)
    void onboarding_step3_activatesRestaurant() throws Exception {
        String body = """
            {
              "addressLine1":"123 Main St","city":"Mumbai","state":"MH",
              "postalCode":"400001","country":"India","phone":"9876543210",
              "latitude":19.07,"longitude":72.87,"estimatedDeliveryTime":30,
              "operatingHours":[
                {"dayOfWeek":"MONDAY","openTime":"09:00","closeTime":"22:00"},
                {"dayOfWeek":"FRIDAY","openTime":"09:00","closeTime":"23:00"}
              ]
            }
            """;

        mvc.perform(post("/restaurants/onboarding/step3")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onboardingStep").value(3));
    }

    @Test @Order(16)
    void onboarding_step3_invalidHours_returns400() throws Exception {
        String body = """
            {
              "addressLine1":"123 Main St","city":"Mumbai","state":"MH",
              "postalCode":"400001","country":"India",
              "operatingHours":[
                {"dayOfWeek":"MONDAY","openTime":"22:00","closeTime":"09:00"}
              ]
            }
            """;

        mvc.perform(post("/restaurants/onboarding/step3")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test @Order(17)
    void getProfile_returns200() throws Exception {
        mvc.perform(get("/restaurants/me")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Pizza Palace"))
            .andExpect(jsonPath("$.status").value(oneOf("ACTIVE", "OPEN")));
    }

    @Test @Order(18)
    void setStatus_open_returns200() throws Exception {
        mvc.perform(patch("/restaurants/me/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"OPEN\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test @Order(19)
    void setStatus_invalid_returns400() throws Exception {
        mvc.perform(patch("/restaurants/me/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DRAFT\"}"))
            .andExpect(status().isBadRequest());
    }

    // ── Restaurant Discovery ──────────────────────────────────────────────────

    @Test @Order(20)
    void listRestaurants_public_returns200() throws Exception {
        mvc.perform(get("/restaurants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test @Order(21)
    void getRestaurantById_returns200() throws Exception {
        mvc.perform(get("/restaurants/" + restaurantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Pizza Palace"));
    }

    @Test @Order(22)
    void getRestaurantById_notFound_returns404() throws Exception {
        mvc.perform(get("/restaurants/99999"))
            .andExpect(status().isNotFound());
    }

    @Test @Order(23)
    void search_validQuery_returns200() throws Exception {
        mvc.perform(get("/restaurants/search?query=pizza"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test @Order(24)
    void search_shortQuery_returns400() throws Exception {
        mvc.perform(get("/restaurants/search?query=p"))
            .andExpect(status().isBadRequest());
    }

    // ── Menu ──────────────────────────────────────────────────────────────────

    @Test @Order(30)
    void createCategory_returns201() throws Exception {
        String body = "{\"name\":\"Starters\",\"sortIndex\":0}";

        MvcResult result = mvc.perform(post("/menu/restaurants/" + restaurantId + "/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Starters"))
            .andReturn();

        var node = mapper.readTree(result.getResponse().getContentAsString());
        categoryId = node.get("id").asLong();
    }

    @Test @Order(31)
    void createCategory_duplicate_returns409() throws Exception {
        mvc.perform(post("/menu/restaurants/" + restaurantId + "/categories")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Starters\",\"sortIndex\":0}"))
            .andExpect(status().isConflict());
    }

    @Test @Order(32)
    void addDish_returns201() throws Exception {
        String body = """
            {"name":"Garlic Bread","description":"Crispy","price":99.00}
            """;

        MvcResult result = mvc.perform(post("/menu/restaurants/" + restaurantId
                    + "/categories/" + categoryId + "/dishes")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Garlic Bread"))
            .andReturn();

        var node = mapper.readTree(result.getResponse().getContentAsString());
        dishId = node.get("id").asLong();
    }

    @Test @Order(33)
    void getMenu_public_showsAvailableDish() throws Exception {
        mvc.perform(get("/menu/restaurants/" + restaurantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories[0].dishes[0].name").value("Garlic Bread"));
    }

    @Test @Order(34)
    void toggleAvailability_false_hidesFromPublic() throws Exception {
        mvc.perform(patch("/menu/restaurants/" + restaurantId + "/dishes/" + dishId + "/availability")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"available\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isAvailable").value(false));

        // Public menu should now show 0 dishes
        mvc.perform(get("/menu/restaurants/" + restaurantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories[0].dishes").isEmpty());
    }

    @Test @Order(35)
    void updateDish_returns200() throws Exception {
        String body = """
            {"name":"Garlic Bread XL","description":"Extra crispy","price":149.00}
            """;

        mvc.perform(put("/menu/restaurants/" + restaurantId + "/dishes/" + dishId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Garlic Bread XL"))
            .andExpect(jsonPath("$.price").value(149.0));
    }

    @Test @Order(36)
    void deleteDish_returns204() throws Exception {
        mvc.perform(delete("/menu/restaurants/" + restaurantId + "/dishes/" + dishId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        // Dish should no longer be accessible
        mvc.perform(get("/menu/dishes/" + dishId))
            .andExpect(status().isNotFound());
    }

    @Test @Order(37)
    void deleteCategory_returns204() throws Exception {
        mvc.perform(delete("/menu/restaurants/" + restaurantId + "/categories/" + categoryId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());
    }

    // ── Auth Logout ───────────────────────────────────────────────────────────

    @Test @Order(90)
    void logout_returns200() throws Exception {
        mvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }
}
