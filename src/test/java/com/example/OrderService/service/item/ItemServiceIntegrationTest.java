package com.example.OrderService.service.item;

import com.example.OrderService.dto.item.ItemResponse;
import com.example.OrderService.service.utils.JwtServiceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ItemServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtServiceTest jwtServiceTest;

    @Container
    private static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16")
            .withDatabaseName("foo")
            .withUsername("foo")
            .withPassword("secret");

    @Container
    static WireMockContainer wiremock =
            new WireMockContainer("wiremock/wiremock:3.13.1")
                    .withMapping("user", """
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
                            """);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("external.user-service.url", wiremock::getBaseUrl);
    }

    @Test
    void create_shouldCreateItem() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/items")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                            {"name": "Apple", "price": 10.5}
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Apple"))
                .andExpect(jsonPath("$.price").value(10.5));
    }

    @Test
    void getItemById_shouldReturnItem() throws Exception {
        MvcResult resultItem = mockMvc.perform(
                        MockMvcRequestBuilders.post("/items")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                            {"name": "Apple", "price": 10.5}
                                        """)
                )
                .andReturn();

        ItemResponse itemResponse = objectMapper.readValue(
                resultItem.getResponse().getContentAsString(),
                ItemResponse.class
        );

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/items/" + itemResponse.getId())
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(itemResponse.getName()))
                .andExpect(jsonPath("$.price").value(itemResponse.getPrice()));
    }

    @Test
    void getItemById_shouldThrowItemNotFound() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/items/" + 999L)
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldDeleteItem() throws Exception {
        MvcResult resultItem = mockMvc.perform(
                        MockMvcRequestBuilders.post("/items")
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                            {"name": "Apple", "price": 10.5}
                                        """)
                )
                .andReturn();

        ItemResponse itemResponse = objectMapper.readValue(
                resultItem.getResponse().getContentAsString(),
                ItemResponse.class
        );

        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/items/" + itemResponse.getId())
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldThrowItemNotFound() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.delete("/items/" + 1L)
                                .header("Authorization", "Bearer " + jwtServiceTest.generateToken("User", 1L, "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
    }
}
