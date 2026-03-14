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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(AppConfiguration.class)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowIntegrationTest {

    static {
        System.setProperty("docker.host", "tcp://localhost:2375");
        System.out.println("Docker host configured: " + System.getProperty("docker.host"));
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PaymentCardDao paymentCardDao;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Long testUserId;
    private Long testCardId;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", postgres::getJdbcUrl);
        registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
        registry.add("DB_DRIVER", () -> "org.postgresql.Driver");

        registry.add("redis.host", redis::getHost);
        registry.add("redis.port", () -> String.valueOf(redis.getMappedPort(6379)));

        System.out.println("✅ Test Database URL: " + postgres.getJdbcUrl());
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        paymentCardDao.deleteAll();
        userDao.deleteAll();
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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJson))
                    .andExpect(status().isCreated());

            final List<User> users = userDao.findAll();
            assertEquals(1, users.size());
            assertEquals("john.doe@test.com", users.get(0).getEmail());

            testUserId = users.get(0).getId();
        }

        @Test
        @Order(2)
        void createUser_DuplicateEmail_ShouldReturn409() throws Exception {
            final User user = new User();
            user.setName("Jane");
            user.setSurname("Smith");
            user.setEmail("jane.smith@test.com");
            user.setBirthDate(LocalDate.of(1992, 5, 10));
            user.setActive(true);
            userDao.save(user);

            final UserDto userDto = new UserDto();
            userDto.setName("Jane");
            userDto.setSurname("Johnson");
            userDto.setEmail("jane.smith@test.com");
            userDto.setBirthDate(LocalDate.of(1995, 3, 15));
            userDto.setActive(true);

            final String userJson = objectMapper.writeValueAsString(userDto);

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJson))
                    .andExpect(status().isConflict());

            assertEquals(1, userDao.count());
        }

        @Test
        @Order(3)
        void getUserById_Success_ShouldReturn200() throws Exception {
            final User user = new User();
            user.setName("Alice");
            user.setSurname("Wonder");
            user.setEmail("alice@test.com");
            user.setBirthDate(LocalDate.of(1988, 7, 20));
            user.setActive(true);
            User savedUser = userDao.save(user);

            mockMvc.perform(get("/users/{id}", savedUser.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedUser.getId()))
                    .andExpect(jsonPath("$.name").value("Alice"))
                    .andExpect(jsonPath("$.email").value("alice@test.com"));
        }

        @Test
        @Order(4)
        void getUserById_NotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/users/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(5)
        void getAllUsers_WithPagination_ShouldReturnPage() throws Exception {
            for (int i = 0; i < 5; i++) {
                final User user = new User();
                user.setName("User" + i);
                user.setSurname("Last" + i);
                user.setEmail("user" + i + "@test.com");
                user.setBirthDate(LocalDate.of(1990, 1, 1));
                user.setActive(true);
                userDao.save(user);
            }

            mockMvc.perform(get("/users")
                            .param("page", "0")
                            .param("size", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(3))
                    .andExpect(jsonPath("$.totalElements").value(5))
                    .andExpect(jsonPath("$.totalPages").value(2));

            mockMvc.perform(get("/users")
                            .param("page", "1")
                            .param("size", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        @Order(6)
        void updateUser_Success_ShouldReturn200() throws Exception {
            final User user = new User();
            user.setName("Bob");
            user.setSurname("Brown");
            user.setEmail("bob@test.com");
            user.setBirthDate(LocalDate.of(1985, 3, 25));
            user.setActive(true);
            User savedUser = userDao.save(user);

            final UserDto updateDto = new UserDto();
            updateDto.setName("Robert");
            updateDto.setSurname("Brown Jr");
            updateDto.setEmail("robert.brown@test.com");
            updateDto.setBirthDate(LocalDate.of(1985, 3, 25));
            updateDto.setActive(true);

            String updateJson = objectMapper.writeValueAsString(updateDto);

            mockMvc.perform(put("/users/{id}", savedUser.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Robert"))
                    .andExpect(jsonPath("$.email").value("robert.brown@test.com"));

            final User updatedUser = userDao.findById(savedUser.getId()).orElseThrow();
            assertEquals("Robert", updatedUser.getName());
            assertEquals("robert.brown@test.com", updatedUser.getEmail());
        }

        @Test
        @Order(7)
        void updateUser_NotFound_ShouldReturn404() throws Exception {
            final UserDto updateDto = new UserDto();
            updateDto.setId(99999L);
            updateDto.setName("Test");

            final String updateJson = objectMapper.writeValueAsString(updateDto);

            mockMvc.perform(put("/users/99999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(8)
        void activateUser_Success_ShouldReturn204() throws Exception {
            final User user = new User();
            user.setName("Charlie");
            user.setSurname("Activate");
            user.setEmail("charlie@test.com");
            user.setBirthDate(LocalDate.of(1995, 8, 12));
            user.setActive(false);
            final User savedUser = userDao.save(user);

            mockMvc.perform(patch("/users/{id}/activate", savedUser.getId()))
                    .andExpect(status().isNoContent());

            final User activatedUser = userDao.findById(savedUser.getId()).orElseThrow();
            assertTrue(activatedUser.getActive());
        }

        @Test
        @Order(9)
        void activateUser_NotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(patch("/users/99999/activate"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(10)
        void deactivateUser_Success_ShouldReturn204() throws Exception {
            final User user = new User();
            user.setName("David");
            user.setSurname("Deactivate");
            user.setEmail("david@test.com");
            user.setBirthDate(LocalDate.of(1992, 11, 5));
            user.setActive(true);

            final User savedUser = userDao.save(user);

            mockMvc.perform(patch("/users/{id}/deactivate", savedUser.getId()))
                    .andExpect(status().isNoContent());

            final User deactivatedUser = userDao.findById(savedUser.getId()).orElseThrow();
            assertFalse(deactivatedUser.getActive());
        }

        @Test
        @Order(11)
        void deactivateUser_NotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(patch("/users/99999/deactivate"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PaymentCardControllerTests {

        private User createTestUser() {
            final User user = new User();
            user.setName("Card");
            user.setSurname("Owner");
            user.setEmail("card.owner@test.com");
            user.setBirthDate(LocalDate.of(1988, 4, 15));
            user.setActive(true);
            return userDao.save(user);
        }

        @Test
        @Order(12)
        void createPaymentCard_Success_ShouldReturn201() throws Exception {
            final User user = createTestUser();

            final UserDto userDto = new UserDto();
            userDto.setId(user.getId());

            final PaymentCardDto cardDto = new PaymentCardDto();
            cardDto.setUser(userDto);
            cardDto.setNumber("1234567890123456");
            cardDto.setHolder("Card Owner");
            cardDto.setExpirationDate(LocalDate.now().plusYears(3));
            cardDto.setActive(true);

            final String cardJson = objectMapper.writeValueAsString(cardDto);

            final MvcResult result = mockMvc.perform(post("/paymentcards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cardJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.number").value("1234567890123456"))
                    .andExpect(jsonPath("$.holder").value("Card Owner"))
                    .andReturn();

            final List<PaymentCard> cards = paymentCardDao.findAll();
            assertEquals(1, cards.size());
            assertEquals(user.getId(), cards.get(0).getUser().getId());

            testCardId = cards.get(0).getId();
        }

        @Test
        @Order(13)
        void createPaymentCard_DuplicateNumber_ShouldReturn409() throws Exception {
            final User user = createTestUser();

            final PaymentCard card = new PaymentCard();
            card.setNumber("9999888877776666");
            card.setHolder("Test Holder");
            card.setExpirationDate(LocalDate.now().plusYears(2));
            card.setActive(true);
            card.setUser(user);
            paymentCardDao.save(card);

            final UserDto userDto = new UserDto();
            userDto.setId(user.getId());

            final PaymentCardDto cardDto = new PaymentCardDto();
            cardDto.setUser(userDto);
            cardDto.setNumber("9999888877776666");
            cardDto.setHolder("Another Holder");
            cardDto.setExpirationDate(LocalDate.now().plusYears(1));
            cardDto.setActive(true);

            final String cardJson = objectMapper.writeValueAsString(cardDto);

            mockMvc.perform(post("/paymentcards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cardJson))
                    .andExpect(status().isConflict());

            assertEquals(1, paymentCardDao.count());
        }

        @Test
        @Order(14)
        void createPaymentCard_UserNotFound_ShouldReturn404() throws Exception {
            final UserDto userDto = new UserDto();
            userDto.setId(99999L);

            final PaymentCardDto cardDto = new PaymentCardDto();
            cardDto.setUser(userDto);
            cardDto.setNumber("1111222233334444");
            cardDto.setHolder("No User");
            cardDto.setExpirationDate(LocalDate.now().plusYears(2));
            cardDto.setActive(true);

            final String cardJson = objectMapper.writeValueAsString(cardDto);

            mockMvc.perform(post("/paymentcards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cardJson))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(15)
        void getPaymentCardById_Success_ShouldReturn200() throws Exception {
            final User user = createTestUser();

            final PaymentCard card = new PaymentCard();
            card.setNumber("5555444433332222");
            card.setHolder("Get Card Test");
            card.setExpirationDate(LocalDate.now().plusYears(3));
            card.setActive(true);
            card.setUser(user);

            final PaymentCard savedCard = paymentCardDao.save(card);

            mockMvc.perform(get("/paymentcards/{id}", savedCard.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedCard.getId()))
                    .andExpect(jsonPath("$.number").value("5555444433332222"))
                    .andExpect(jsonPath("$.holder").value("Get Card Test"));
        }

        @Test
        @Order(16)
        void getPaymentCardById_NotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/paymentcards/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(17)
        void getAllPaymentCards_WithPagination_ShouldReturnPage() throws Exception {
            final User user = createTestUser();

            for (int i = 0; i < 5; i++) {
                final PaymentCard card = new PaymentCard();

                card.setNumber(String.format("111122223333444%d", i));
                card.setHolder("Page Test");
                card.setExpirationDate(LocalDate.now().plusYears(2));
                card.setActive(true);
                card.setUser(user);
                paymentCardDao.save(card);
            }

            mockMvc.perform(get("/paymentcards")
                            .param("page", "0")
                            .param("size", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(3))
                    .andExpect(jsonPath("$.totalElements").value(5));
        }

        @Test
        @Order(18)
        void getAllPaymentCardsByUserId_Success_ShouldReturnList() throws Exception {
            final User user = createTestUser();

            for (int i = 0; i < 3; i++) {
                final PaymentCard card = new PaymentCard();
                card.setNumber(String.format("777788889999000%d", i));
                card.setHolder("By User Test");
                card.setExpirationDate(LocalDate.now().plusYears(2));
                card.setActive(true);
                card.setUser(user);
                paymentCardDao.save(card);
            }

            mockMvc.perform(get("/paymentcards/user/{userId}", user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].number").value("7777888899990000"))
                    .andExpect(jsonPath("$[1].number").value("7777888899990001"))
                    .andExpect(jsonPath("$[2].number").value("7777888899990002"));
        }

        @Test
        @Order(19)
        void getAllPaymentCardsByUserId_UserNotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/paymentcards/user/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(20)
        void updatePaymentCard_Success_ShouldReturn200() throws Exception {
            final User user = createTestUser();

            final PaymentCard card = new PaymentCard();
            card.setNumber("1234123412341234");
            card.setHolder("Original Holder");
            card.setExpirationDate(LocalDate.now().plusYears(2));
            card.setActive(true);
            card.setUser(user);

            final PaymentCard savedCard = paymentCardDao.save(card);

            final UserDto userDto = new UserDto();
            userDto.setId(user.getId());

            final PaymentCardDto updateDto = new PaymentCardDto();
            updateDto.setId(savedCard.getId());
            updateDto.setUser(userDto);
            updateDto.setNumber("9999999999999999");
            updateDto.setHolder("Updated Holder");
            updateDto.setExpirationDate(LocalDate.now().plusYears(4));
            updateDto.setActive(false);

            final String updateJson = objectMapper.writeValueAsString(updateDto);

            mockMvc.perform(put("/paymentcards/{id}", savedCard.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.number").value("9999999999999999"))
                    .andExpect(jsonPath("$.holder").value("Updated Holder"))
                    .andExpect(jsonPath("$.active").value(false));

            final PaymentCard updatedCard = paymentCardDao.findById(savedCard.getId()).orElseThrow();
            assertEquals("9999999999999999", updatedCard.getNumber());
            assertEquals("Updated Holder", updatedCard.getHolder());
            assertFalse(updatedCard.getActive());
        }

        @Test
        @Order(21)
        void updatePaymentCard_NotFound_ShouldReturn404() throws Exception {
            final UserDto userDto = new UserDto();
            userDto.setId(1L);

            final PaymentCardDto updateDto = new PaymentCardDto();
            updateDto.setId(99999L);
            updateDto.setUser(userDto);
            updateDto.setNumber("1111111111111111");

            final String updateJson = objectMapper.writeValueAsString(updateDto);

            mockMvc.perform(put("/paymentcards/99999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(22)
        void activatePaymentCard_Success_ShouldReturn204() throws Exception {
            final User user = createTestUser();

            final PaymentCard card = new PaymentCard();
            card.setNumber("8888777766665555");
            card.setHolder("Activate Test");
            card.setExpirationDate(LocalDate.now().plusYears(2));
            card.setActive(false);
            card.setUser(user);

            final PaymentCard savedCard = paymentCardDao.save(card);

            mockMvc.perform(patch("/paymentcards/{id}/activate", savedCard.getId()))
                    .andExpect(status().isNoContent());

            final PaymentCard activatedCard = paymentCardDao.findById(savedCard.getId()).orElseThrow();
            assertTrue(activatedCard.getActive());
        }

        @Test
        @Order(23)
        void activatePaymentCard_NotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(patch("/paymentcards/99999/activate"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @Order(24)
        void deactivatePaymentCard_Success_ShouldReturn204() throws Exception {
            final User user = createTestUser();

            final PaymentCard card = new PaymentCard();
            card.setNumber("4444333322221111");
            card.setHolder("Deactivate Test");
            card.setExpirationDate(LocalDate.now().plusYears(2));
            card.setActive(true);
            card.setUser(user);
            PaymentCard savedCard = paymentCardDao.save(card);

            mockMvc.perform(patch("/paymentcards/{id}/deactivate", savedCard.getId()))
                    .andExpect(status().isNoContent());

            final PaymentCard deactivatedCard = paymentCardDao.findById(savedCard.getId()).orElseThrow();
            assertFalse(deactivatedCard.getActive());
        }

        @Test
        @Order(25)
        void deactivatePaymentCard_NotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(patch("/paymentcards/99999/deactivate"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UserWithCardsTests {

        @Test
        @Order(26)
        void getUserWithCards_Success_ShouldReturn200() throws Exception {
            final User user = new User();
            user.setName("Complete");
            user.setSurname("User");
            user.setEmail("complete.user@test.com");
            user.setBirthDate(LocalDate.of(1987, 6, 10));
            user.setActive(true);

            final User savedUser = userDao.save(user);

            for (int i = 0; i < 2; i++) {
                final PaymentCard card = new PaymentCard();
                card.setNumber("555566667777888" + i);
                card.setHolder("Complete User");
                card.setExpirationDate(LocalDate.now().plusYears(3));
                card.setActive(true);
                card.setUser(savedUser);

                paymentCardDao.save(card);
            }

            mockMvc.perform(get("/users/{id}/with-cards", savedUser.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedUser.getId()))
                    .andExpect(jsonPath("$.name").value("Complete"))
                    .andExpect(jsonPath("$.cards.length()").value(2));
        }

        @Test
        @Order(27)
        void getUserWithCards_UserNotFound_ShouldReturn404() throws Exception {
            mockMvc.perform(get("/users/99999/with-cards"))
                    .andExpect(status().isNotFound());
        }
    }
}