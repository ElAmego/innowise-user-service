package com.innowise.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.innowise.userservice.configuration.AppConfiguration;
import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.dto.UserDto;
import com.innowise.userservice.model.dao.PaymentCardDao;
import com.innowise.userservice.model.dao.UserDao;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(AppConfiguration.class)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowIntegrationTest {

    static {
        System.setProperty("docker.host", "tcp://localhost:2375");
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PaymentCardDao paymentCardDao;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Long adminUserId;
    private Long regularUserId;

    private static final String ADMIN_TOKEN = "Bearer test-admin-token";
    private static final String USER_TOKEN = "Bearer test-user-token";

    @BeforeAll
    static void beforeAll() {
        System.setProperty("DB_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USERNAME", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());
        System.setProperty("DB_DRIVER", "org.postgresql.Driver");
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        paymentCardDao.deleteAll();
        userDao.deleteAll();

        adminUserId = createTestUser("Admin", "User", "admin@test.com", true);
        regularUserId = createTestUser("Regular", "User", "user@test.com", true);

        User user = userDao.findById(regularUserId).orElseThrow();
        for (int i = 0; i < 2; i++) {
            final PaymentCard card = new PaymentCard();
            card.setNumber("555566667777888" + i);
            card.setHolder("Regular User");
            card.setExpirationDate(LocalDate.now().plusYears(3));
            card.setActive(true);
            card.setUser(user);
            paymentCardDao.save(card);
        }
    }

    private Long createTestUser(String name, String surname, String email, boolean active) {
        User user = new User();
        user.setName(name);
        user.setSurname(surname);
        user.setEmail(email);
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setActive(active);
        return userDao.save(user).getId();
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UserControllerTests {

        @Test
        @Order(1)
        void createUser_Success_ShouldReturn201() throws Exception {
            final UserDto userDto = new UserDto();
            userDto.setName("John");
            userDto.setSurname("Doe");
            userDto.setEmail("john.doe@test.com");
            userDto.setBirthDate(LocalDate.of(1990, 1, 1));
            userDto.setActive(true);

            final String userJson = objectMapper.writeValueAsString(userDto);

            mockMvc.perform(post("/users")
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJson))
                    .andExpect(status().isCreated());

            final List<User> users = userDao.findAll();
            assertEquals(3, users.size());
        }

        @Test
        @Order(2)
        void createUser_DuplicateEmail_ShouldReturn409() throws Exception {
            final UserDto userDto = new UserDto();
            userDto.setName("Jane");
            userDto.setSurname("Johnson");
            userDto.setEmail("user@test.com");
            userDto.setBirthDate(LocalDate.of(1995, 3, 15));
            userDto.setActive(true);

            final String userJson = objectMapper.writeValueAsString(userDto);

            mockMvc.perform(post("/users")
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJson))
                    .andExpect(status().isConflict());

            assertEquals(2, userDao.count());
        }

        @Test
        @Order(3)
        void getUserById_Success_ShouldReturn200() throws Exception {
            mockMvc.perform(get("/users/{id}", regularUserId)
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(regularUserId))
                    .andExpect(jsonPath("$.name").value("Regular"))
                    .andExpect(jsonPath("$.email").value("user@test.com"));
        }

        @Test
        @Order(4)
        void getUserById_NotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/users/99999")
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(5)
        void getAllUsers_WithPagination_ShouldReturnPage() throws Exception {
            mockMvc.perform(get("/users")
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN")
                            .param("page", "0")
                            .param("size", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @Order(6)
        void updateUser_Success_ShouldReturn200() throws Exception {
            final UserDto updateDto = new UserDto();
            updateDto.setId(regularUserId);
            updateDto.setName("Robert");
            updateDto.setSurname("Brown Jr");
            updateDto.setEmail("robert.brown@test.com");
            updateDto.setBirthDate(LocalDate.of(1985, 3, 25));
            updateDto.setActive(true);

            String updateJson = objectMapper.writeValueAsString(updateDto);

            mockMvc.perform(put("/users/{id}", regularUserId)
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Robert"))
                    .andExpect(jsonPath("$.email").value("robert.brown@test.com"));
        }

        @Test
        @Order(7)
        void updateUser_NotFound_ShouldReturn400() throws Exception {
            final UserDto updateDto = new UserDto();
            updateDto.setId(99999L);
            updateDto.setName("Test");

            final String updateJson = objectMapper.writeValueAsString(updateDto);

            mockMvc.perform(put("/users/99999")
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Order(8)
        void activateUser_Success_ShouldReturn204() throws Exception {
            User inactiveUser = new User();
            inactiveUser.setName("Charlie");
            inactiveUser.setSurname("Activate");
            inactiveUser.setEmail("charlie@test.com");
            inactiveUser.setBirthDate(LocalDate.of(1995, 8, 12));
            inactiveUser.setActive(false);
            final User savedUser = userDao.save(inactiveUser);

            mockMvc.perform(patch("/users/{id}/activate", savedUser.getId())
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNoContent());

            final User activatedUser = userDao.findById(savedUser.getId()).orElseThrow();
            assertTrue(activatedUser.getActive());
        }

        @Test
        @Order(9)
        void activateUser_NotFound_ShouldReturn204() throws Exception {
            mockMvc.perform(patch("/users/99999/activate")
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @Order(10)
        void deactivateUser_Success_ShouldReturn204() throws Exception {
            mockMvc.perform(patch("/users/{id}/deactivate", regularUserId)
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNoContent());

            final User deactivatedUser = userDao.findById(regularUserId).orElseThrow();
            assertFalse(deactivatedUser.getActive());
        }

        @Test
        @Order(11)
        void deactivateUser_NotFound_ShouldReturn204() throws Exception {
            mockMvc.perform(patch("/users/99999/deactivate")
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PaymentCardControllerTests {

        private Long createTestUser() {
            User user = new User();
            user.setName("Card");
            user.setSurname("Owner");
            user.setEmail("card.owner@test.com");
            user.setBirthDate(LocalDate.of(1988, 4, 15));
            user.setActive(true);
            return userDao.save(user).getId();
        }

        @Test
        @Order(12)
        void createPaymentCard_Success_ShouldReturn201() throws Exception {
            Long userId = createTestUser();

            final PaymentCardDto cardDto = new PaymentCardDto();
            cardDto.setNumber("1234567890123456");
            cardDto.setHolder("Card Owner");
            cardDto.setExpirationDate(LocalDate.now().plusYears(3));
            cardDto.setActive(true);

            final String cardJson = objectMapper.writeValueAsString(cardDto);

            mockMvc.perform(post("/users/{userId}/payment-card", userId)
                            .header("Authorization", USER_TOKEN)
                            .requestAttr("userId", userId)
                            .requestAttr("userRole", "ROLE_USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cardJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.number").value("1234567890123456"))
                    .andExpect(jsonPath("$.holder").value("Card Owner"));

            final List<PaymentCard> cards = paymentCardDao.findAll();
            assertEquals(3, cards.size());
        }

        @Test
        @Order(13)
        void createPaymentCard_DuplicateNumber_ShouldReturn409() throws Exception {
            Long userId = createTestUser();
            User user = userDao.findById(userId).orElseThrow();

            final PaymentCard card = new PaymentCard();
            card.setNumber("9999888877776666");
            card.setHolder("Test Holder");
            card.setExpirationDate(LocalDate.now().plusYears(2));
            card.setActive(true);
            card.setUser(user);
            paymentCardDao.save(card);

            final PaymentCardDto cardDto = new PaymentCardDto();
            cardDto.setNumber("9999888877776666");
            cardDto.setHolder("Another Holder");
            cardDto.setExpirationDate(LocalDate.now().plusYears(1));
            cardDto.setActive(true);

            final String cardJson = objectMapper.writeValueAsString(cardDto);

            mockMvc.perform(post("/users/{userId}/payment-card", userId)
                            .header("Authorization", USER_TOKEN)
                            .requestAttr("userId", userId)
                            .requestAttr("userRole", "ROLE_USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cardJson))
                    .andExpect(status().isConflict());

            assertEquals(3, paymentCardDao.count());
        }

        @Test
        @Order(14)
        void createPaymentCard_UserNotFound_ShouldReturn404() throws Exception {
            final PaymentCardDto cardDto = new PaymentCardDto();
            cardDto.setNumber("1111222233334444");
            cardDto.setHolder("No User");
            cardDto.setExpirationDate(LocalDate.now().plusYears(2));
            cardDto.setActive(true);

            final String cardJson = objectMapper.writeValueAsString(cardDto);

            mockMvc.perform(post("/users/{userId}/payment-card", 99999L)
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cardJson))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(15)
        void getPaymentCardById_Success_ShouldReturn200() throws Exception {
            Long userId = createTestUser();
            User user = userDao.findById(userId).orElseThrow();

            final PaymentCard card = new PaymentCard();
            card.setNumber("5555444433332222");
            card.setHolder("Get Card Test");
            card.setExpirationDate(LocalDate.now().plusYears(3));
            card.setActive(true);
            card.setUser(user);

            final PaymentCard savedCard = paymentCardDao.save(card);

            mockMvc.perform(get("/users/{userId}/payment-card/{cardId}", userId, savedCard.getId())
                            .header("Authorization", USER_TOKEN)
                            .requestAttr("userId", userId)
                            .requestAttr("userRole", "ROLE_USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedCard.getId()))
                    .andExpect(jsonPath("$.number").value("5555444433332222"))
                    .andExpect(jsonPath("$.holder").value("Get Card Test"));
        }

        @Test
        @Order(16)
        void getPaymentCardById_NotFound_ShouldReturn404() throws Exception {
            Long userId = createTestUser();

            mockMvc.perform(get("/users/{userId}/payment-card/{cardId}", userId, 99999L)
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(17)
        void getAllPaymentCardsByUserId_Success_ShouldReturnList() throws Exception {
            Long userId = createTestUser();
            User user = userDao.findById(userId).orElseThrow();

            for (int i = 0; i < 3; i++) {
                final PaymentCard card = new PaymentCard();
                card.setNumber(String.format("777788889999000%d", i));
                card.setHolder("By User Test");
                card.setExpirationDate(LocalDate.now().plusYears(2));
                card.setActive(true);
                card.setUser(user);
                paymentCardDao.save(card);
            }

            mockMvc.perform(get("/users/{userId}/payment-card", userId)
                            .header("Authorization", USER_TOKEN)
                            .requestAttr("userId", userId)
                            .requestAttr("userRole", "ROLE_USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3));
        }

        @Test
        @Order(18)
        void getAllPaymentCardsByUserId_UserNotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/users/{userId}/payment-card", 99999L)
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(19)
        void updatePaymentCard_Success_ShouldReturn200() throws Exception {
            Long userId = createTestUser();
            User user = userDao.findById(userId).orElseThrow();

            final PaymentCard card = new PaymentCard();
            card.setNumber("1234123412341234");
            card.setHolder("Original Holder");
            card.setExpirationDate(LocalDate.now().plusYears(2));
            card.setActive(true);
            card.setUser(user);

            final PaymentCard savedCard = paymentCardDao.save(card);

            final PaymentCardDto updateDto = new PaymentCardDto();
            updateDto.setNumber("9999999999999999");
            updateDto.setHolder("Updated Holder");
            updateDto.setExpirationDate(LocalDate.now().plusYears(4));
            updateDto.setActive(false);

            final String updateJson = objectMapper.writeValueAsString(updateDto);

            mockMvc.perform(put("/users/{userId}/payment-card/{cardId}", userId, savedCard.getId())
                            .header("Authorization", USER_TOKEN)
                            .requestAttr("userId", userId)
                            .requestAttr("userRole", "ROLE_USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.number").value("9999999999999999"))
                    .andExpect(jsonPath("$.holder").value("Updated Holder"))
                    .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        @Order(20)
        void updatePaymentCard_NotFound_ShouldReturn400() throws Exception {
            Long userId = createTestUser();

            final PaymentCardDto updateDto = new PaymentCardDto();
            updateDto.setNumber("1111111111111111");
            updateDto.setHolder("Test");

            final String updateJson = objectMapper.writeValueAsString(updateDto);

            mockMvc.perform(put("/users/{userId}/payment-card/{cardId}", userId, 99999L)
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Order(21)
        void activatePaymentCard_Success_ShouldReturn204() throws Exception {
            Long userId = createTestUser();
            User user = userDao.findById(userId).orElseThrow();

            final PaymentCard card = new PaymentCard();
            card.setNumber("8888777766665555");
            card.setHolder("Activate Test");
            card.setExpirationDate(LocalDate.now().plusYears(2));
            card.setActive(false);
            card.setUser(user);

            final PaymentCard savedCard = paymentCardDao.save(card);

            mockMvc.perform(patch("/users/{userId}/payment-card/{cardId}/activate", userId, savedCard.getId())
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNoContent());

            final PaymentCard activatedCard = paymentCardDao.findById(savedCard.getId()).orElseThrow();
            assertTrue(activatedCard.getActive());
        }

        @Test
        @Order(22)
        void activatePaymentCard_NotFound_ShouldReturn404() throws Exception {
            Long userId = createTestUser();

            mockMvc.perform(patch("/users/{userId}/payment-card/{cardId}/activate", userId, 99999L)
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(23)
        void deactivatePaymentCard_Success_ShouldReturn204() throws Exception {
            Long userId = createTestUser();
            User user = userDao.findById(userId).orElseThrow();

            final PaymentCard card = new PaymentCard();
            card.setNumber("4444333322221111");
            card.setHolder("Deactivate Test");
            card.setExpirationDate(LocalDate.now().plusYears(2));
            card.setActive(true);
            card.setUser(user);
            PaymentCard savedCard = paymentCardDao.save(card);

            mockMvc.perform(patch("/users/{userId}/payment-card/{cardId}/deactivate", userId, savedCard.getId())
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNoContent());

            final PaymentCard deactivatedCard = paymentCardDao.findById(savedCard.getId()).orElseThrow();
            assertFalse(deactivatedCard.getActive());
        }

        @Test
        @Order(24)
        void deactivatePaymentCard_NotFound_ShouldReturn404() throws Exception {
            Long userId = createTestUser();

            mockMvc.perform(patch("/users/{userId}/payment-card/{cardId}/deactivate", userId, 99999L)
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UserWithCardsTests {

        @Test
        @Order(25)
        void getUserWithCards_Success_ShouldReturn200() throws Exception {
            Long userId = regularUserId;

            List<PaymentCard> cards = paymentCardDao.findAllByUserId(userId);
            assertEquals(2, cards.size());

            mockMvc.perform(get("/users/{userId}/payment-card-with-user", userId)
                            .header("Authorization", USER_TOKEN)
                            .requestAttr("userId", userId)
                            .requestAttr("userRole", "ROLE_USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.cards.length()").value(2));
        }

        @Test
        @Order(26)
        void getUserWithCards_UserNotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/users/{userId}/payment-card-with-user", 99999L)
                            .header("Authorization", ADMIN_TOKEN)
                            .requestAttr("userId", adminUserId)
                            .requestAttr("userRole", "ROLE_ADMIN"))
                    .andExpect(status().isNotFound());
        }
    }
}