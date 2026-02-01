package com.example.OrderService.service.order;

import com.example.OrderService.dto.item.ItemResponse;
import com.example.OrderService.dto.order.OrderResponse;
import com.example.OrderService.enums.Status;
import com.example.OrderService.security.model.AuthUser;
import com.example.OrderService.service.utils.JwtServiceTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.wiremock.integrations.testcontainers.WireMockContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrderServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtServiceTest jwtServiceTest;

    @Autowired
    private EntityManager entityManager;

    @Container
    private static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("foo")
            .withUsername("foo")
            .withPassword("secret");

    @Container
    static WireMockContainer wiremock =
            new WireMockContainer("wiremock/wiremock:3.13.1")
                    .withMapping("user-1", """
                            {
                              "request": {
                                "method": "GET",
                                "url": "/users/1"
                              },
                              "response": {
                                "status": 200,
                                "body": "{\\"id\\":1,\\"name\\":\\"Nastya\\",\\"surname\\":\\"Ivanova\\",\\"birthDay\\":\\"2006-04-03\\",\\"email\\":\\"nastya@gmail.com\\",\\"active\\":true,\\"createdAt\\":\\"2026-02-01T00:00:00\\",\\"updatedAt\\":\\"2026-02-01T00:00:00\\",\\"cards\\":[]}",
                                "headers": {
                                  "Content-Type": "application/json"
                                }
                              }
                            }
                            """)
                    .withMapping("user-2", """
                            {
                              "request": {
                                "method": "GET",
                                "url": "/users/2"
                              },
                              "response": {
                                "status": 200,
                                "body": "{\\"id\\":2,\\"name\\":null}",
                                "headers": {
                                  "Content-Type": "application/json"
                                }
                              }
                            }
                            """);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("external.user-service.url", () -> wiremock.getBaseUrl() + "/users");
    }

    protected void authenticate() {
        AuthUser authUser = new AuthUser(1L, "ADMIN");

        var authorities = Set.of(new SimpleGrantedAuthority(authUser.getRole()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        authUser,
                        null,
                        authorities
                )
        );
        SecurityContextHolder.setContext(context);
    }

    protected OrderResponse createOrder() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                        post("/items")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {"name": "Apple", "price": 10.5}
                                    """)
                )
                .andReturn();

        ItemResponse itemResponse = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                ItemResponse.class
        );

        String body = String.format("""
            {
              "items": [
                {
                  "quantity": 2,
                  "itemId": %d
                }
              ]
            }
            """, itemResponse.getId());

        MvcResult resultOrder = mockMvc.perform(
                        post("/orders")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andReturn();

        return objectMapper.readValue(
                resultOrder.getResponse().getContentAsString(),
                OrderResponse.class
        );
    }

    @Test
    void create_shouldCreateOrder() throws Exception {
        authenticate();

        MvcResult mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.post("/items")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name": "Apple", "price": 10.5}
                                        """)
                )
                .andReturn();

        ItemResponse itemResponse = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                ItemResponse.class
        );

        String body = String.format("""
                {
                  "items": [
                    {
                      "quantity": 2,
                      "itemId": %d
                    }
                  ]
                }
                """, itemResponse.getId());


        mockMvc.perform(
                        MockMvcRequestBuilders.post("/orders")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.name").value("Nastya"))
                .andExpect(jsonPath("$.items.size()").value(1));
    }

    @Test
    void getOrderById_shouldReturnForbidden_whenUserNotOwner() throws Exception {
        OrderResponse orderResponse = createOrder();

        mockMvc.perform(
                        get("/orders/" + orderResponse.getId())
                                .header("Authorization",
                                        "Bearer " + jwtServiceTest.generateToken("User", 2L, "USER"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldThrowUserNotFound() throws Exception {
        authenticate();

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/orders")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 2L, "USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "items": [
                                            {
                                              "quantity": 2,
                                              "itemId": 1
                                            }
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldThrowItemNotFound() throws Exception {
        authenticate();

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/orders")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "USER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "items": [
                                            {
                                              "quantity": 2,
                                              "itemId": 999
                                            }
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderById_shouldReturnOrder() throws Exception {
        authenticate();

        OrderResponse orderResponse = createOrder();

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/orders/" + orderResponse.getId())
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))

                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Nastya"))
                .andExpect(jsonPath("$.items.size()").value(1));
    }

    @Test
    void getOrderById_shouldReturnOrderNotFound_whenOrderDoesNotExist() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/orders/" + 1L)
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))

                )
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrderById_shouldReturnOrderNotFound_whenOrderDeleted() throws Exception {
        authenticate();

        OrderResponse orderResponse = createOrder();

        mockMvc.perform(
                MockMvcRequestBuilders.delete("/orders/" + orderResponse.getId())
                        .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
        );

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/orders/" + orderResponse.getId())
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))

                )
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_shouldReturnPagesWithOrders() throws Exception {
        authenticate();

        OrderResponse orderResponse = createOrder();

        OrderResponse orderResponse2 = createOrder();

        MvcResult result = mockMvc.perform(
                        get("/orders")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .param("page", "0")
                                .param("size", "10")
                                .param("start", orderResponse.getCreatedAt().minusMinutes(1).toString())
                                .param("end", orderResponse2.getCreatedAt().plusMinutes(1).toString())
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");

        Set<Long> ids = new HashSet<>();
        content.forEach(node -> ids.add(node.get("id").asLong()));

        assertTrue(ids.contains(orderResponse.getId()));
        assertTrue(ids.contains(orderResponse2.getId()));

        MvcResult result2 = mockMvc.perform(
                        get("/orders")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .param("page", "0")
                                .param("size", "10")
                                .param("end", orderResponse.getCreatedAt().minusMinutes(1).toString())
                )
                .andExpect(status().isOk())
                .andReturn();

        content = objectMapper.readTree(result2.getResponse().getContentAsString()).get("content");

        Set<Long> ids2 = new HashSet<>();
        content.forEach(node -> ids2.add(node.get("id").asLong()));

        assertFalse(ids2.contains(orderResponse.getId()));
        assertFalse(ids2.contains(orderResponse2.getId()));

        mockMvc.perform(
                        patch("/orders/" + orderResponse.getId())
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status": "SHIPPED"}
                                        """)
                )
                .andExpect(status().isOk());

        MvcResult result3 = mockMvc.perform(
                        get("/orders")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .param("page", "0")
                                .param("size", "10")
                                .param("statuses", Status.SHIPPED.toString())
                )
                .andExpect(status().isOk())
                .andReturn();

        content = objectMapper.readTree(result3.getResponse().getContentAsString()).get("content");

        Set<Long> ids3 = new HashSet<>();
        content.forEach(node -> ids3.add(node.get("id").asLong()));

        assertFalse(ids2.contains(orderResponse.getId()));
    }

    @Test
    void getOrdersByUserId_shouldReturnListOfOrders() throws Exception {
        authenticate();

        OrderResponse orderResponse = createOrder();
        OrderResponse orderResponse2 = createOrder();

        MvcResult result = mockMvc.perform(
                        get("/orders/" + orderResponse.getUser().getId() + "/all")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

        assertTrue(root.isArray());

        Set<Long> ids = new HashSet<>();
        root.forEach(node -> ids.add(node.get("id").asLong()));

        assertTrue(ids.contains(orderResponse.getId()));
        assertTrue(ids.contains(orderResponse2.getId()));
    }

    @Test
    void getOrdersByUserId_shouldReturnUserNotFound() throws Exception {
        mockMvc.perform(
                        get("/orders/" + 999L + "/all")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldUpdateOrder() throws Exception {
        OrderResponse orderResponse = createOrder();

        assertEquals(orderResponse.getStatus(), Status.CREATED.toString());

        MvcResult result = mockMvc.perform(
                        patch("/orders/" + orderResponse.getId())
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "SHIPPED"
                                        }
                                        """)
                )
                .andReturn();

        OrderResponse updated = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                OrderResponse.class
        );

        assertEquals(orderResponse.getId(), updated.getId());
        assertEquals(Status.SHIPPED.toString(), updated.getStatus());
    }

    @Test
    void getOrdersByUserId_shouldReturnForbidden_whenUserNotOwner() throws Exception {
        OrderResponse orderResponse = createOrder();

        mockMvc.perform(
                        get("/orders/1/all")
                                .header("Authorization",
                                        "Bearer " + jwtServiceTest.generateToken("User", 2L, "USER"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void update_shouldThrowOrderNotFoundException() throws Exception {
        mockMvc.perform(
                        patch("/orders/" + 999L)
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "status": "SHIPPED"
                                        }
                                        """)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldSoftDeleteOrder() throws Exception {
        OrderResponse orderResponse = createOrder();

        assertFalse(orderResponse.getDeleted());

        mockMvc.perform(
                        delete("/orders/" + orderResponse.getId())
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturnForbidden_whenUserNotOwner() throws Exception {
        OrderResponse orderResponse = createOrder();

        mockMvc.perform(
                        delete("/orders/" + orderResponse.getId())
                                .header("Authorization",
                                        "Bearer " + jwtServiceTest.generateToken("User", 2L, "USER"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_shouldReturnForbidden_whenUserIsOwnerButStatusNotCreated() throws Exception {
        OrderResponse orderResponse = createOrder();

        mockMvc.perform(
                        patch("/orders/" + orderResponse.getId())
                                .header("Authorization",
                                        "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {"status": "SHIPPED"}
                                    """)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        delete("/orders/" + orderResponse.getId())
                                .header("Authorization",
                                        "Bearer " + jwtServiceTest.generateToken("User", 1L, "USER"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_shouldThrowOrderNotFound_whenOrderDoesNotExist() throws Exception {
        mockMvc.perform(
                        delete("/orders/" + 999L)
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldThrowOrderNotFound_whenOrderAlreadyDeleted() throws Exception{
        OrderResponse orderResponse = createOrder();

        assertFalse(orderResponse.getDeleted());

        mockMvc.perform(
                        delete("/orders/" + orderResponse.getId())
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/orders/" + orderResponse.getId())
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                )
                .andExpect(status().isNotFound());
    }
}