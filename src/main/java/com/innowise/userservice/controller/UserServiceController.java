package com.innowise.userservice.controller;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.dto.UserDto;
import com.innowise.userservice.service.PaymentCardService;
import com.innowise.userservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserServiceController {
    private static final int PAGINATION_SIZE = 15;
    private static final String PAGINATION_SORTED_BY = "id";
    private final UserService userService;
    private final PaymentCardService paymentCardService;

    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @PageableDefault(size = PAGINATION_SIZE, sort = PAGINATION_SORTED_BY, direction = Sort.Direction.ASC)
            Pageable pageable,
            HttpServletRequest request
    ) {
        checkAdminAccess(request);
        final Page<UserDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto, HttpServletRequest request) {
        checkAdminAccess(request);
        userService.createUser(userDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/specification/{name}/{surname}")
    public ResponseEntity<Page<UserDto>> findAllSpecification(
            @PathVariable("name") String name,
            @PathVariable("surname") String surname,
            @PageableDefault(size = PAGINATION_SIZE, sort = PAGINATION_SORTED_BY, direction = Sort.Direction.ASC)
            Pageable pageable,
            HttpServletRequest request
    ) {
        checkAdminAccess(request);
        final Page<UserDto> userDtoPage = userService.findAll(name, surname, pageable);
        return ResponseEntity.ok(userDtoPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> findUserById(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        String role = getRoleFromRequest(request);

        if (!"ROLE_ADMIN".equals(role) && !userId.equals(id)) {
            throw new AccessDeniedException("Access denied. You can only view your own profile.");
        }

        final UserDto userDto = userService.getUserById(id);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody UserDto userDto,
            HttpServletRequest request
    ) {
        Long userId = getUserIdFromRequest(request);
        String role = getRoleFromRequest(request);

        if (!"ROLE_ADMIN".equals(role) && !userId.equals(id)) {
            throw new AccessDeniedException("Access denied. You can only update your own profile.");
        }

        userDto.setId(id);
        final UserDto updatedUser = userService.updateUser(userDto);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable("id") Long id, HttpServletRequest request) {
        checkAdminAccess(request);
        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable("id") Long id, HttpServletRequest request) {
        checkAdminAccess(request);
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/payment-card-with-user")
    public ResponseEntity<UserDto> getUserWithCards(@PathVariable("userId") Long userId, HttpServletRequest request) {
        Long currentUserId = getUserIdFromRequest(request);
        String role = getRoleFromRequest(request);

        if (!"ROLE_ADMIN".equals(role) && !currentUserId.equals(userId)) {
            throw new AccessDeniedException("Access denied. You can only view your own cards.");
        }

        final UserDto userWithCardsDto = userService.getUserWithCardsById(userId);
        return ResponseEntity.ok(userWithCardsDto);
    }

    @PostMapping("/{userId}/payment-card")
    public ResponseEntity<PaymentCardDto> createUserPaymentCard(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody PaymentCardDto paymentCardDto,
            HttpServletRequest request
    ) {
        Long currentUserId = getUserIdFromRequest(request);
        String role = getRoleFromRequest(request);

        if (!"ROLE_ADMIN".equals(role) && !currentUserId.equals(userId)) {
            throw new AccessDeniedException("Access denied. You can only add cards to your own profile.");
        }

        final PaymentCardDto createdCard = paymentCardService.createUserPaymentCard(userId, paymentCardDto);
        return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}/payment-card/{cardId}")
    public ResponseEntity<PaymentCardDto> getUserPaymentCardById(
            @PathVariable("userId") Long userId,
            @PathVariable("cardId") Long cardId,
            HttpServletRequest request
    ) {
        Long currentUserId = getUserIdFromRequest(request);
        String role = getRoleFromRequest(request);

        if (!"ROLE_ADMIN".equals(role) && !currentUserId.equals(userId)) {
            throw new AccessDeniedException("Access denied. You can only view your own cards.");
        }

        final PaymentCardDto paymentCard = paymentCardService.getUserPaymentCardById(userId, cardId);
        return ResponseEntity.ok(paymentCard);
    }

    @GetMapping("/payment-card/specification/{number}")
    public ResponseEntity<Page<PaymentCardDto>> findAllSpecification(
            @PathVariable("number") String number,
            @PageableDefault(size = PAGINATION_SIZE, sort = PAGINATION_SORTED_BY, direction = Sort.Direction.ASC)
            Pageable pageable,
            HttpServletRequest request
    ) {
        checkAdminAccess(request);
        final Page<PaymentCardDto> paymentCardDto = paymentCardService.getAllPaymentCardsSpecification(number, pageable);
        return ResponseEntity.ok(paymentCardDto);
    }

    @GetMapping("/{userId}/payment-card")
    public ResponseEntity<List<PaymentCardDto>> getAllPaymentCardsByUserId(@PathVariable("userId") Long userId, HttpServletRequest request) {
        Long currentUserId = getUserIdFromRequest(request);
        String role = getRoleFromRequest(request);

        if (!"ROLE_ADMIN".equals(role) && !currentUserId.equals(userId)) {
            throw new AccessDeniedException("Access denied. You can only view your own cards.");
        }

        final List<PaymentCardDto> cards = paymentCardService.getAllPaymentCardsByUserId(userId);
        return ResponseEntity.ok(cards);
    }

    @PutMapping("/{userId}/payment-card/{cardId}")
    public ResponseEntity<PaymentCardDto> updatePaymentCard(
            @PathVariable("userId") Long userId,
            @PathVariable("cardId") Long cardId,
            @Valid @RequestBody PaymentCardDto paymentCardDto,
            HttpServletRequest request
    ) {
        Long currentUserId = getUserIdFromRequest(request);
        String role = getRoleFromRequest(request);

        if (!"ROLE_ADMIN".equals(role) && !currentUserId.equals(userId)) {
            throw new AccessDeniedException("Access denied. You can only update your own cards.");
        }

        paymentCardDto.setId(cardId);
        final PaymentCardDto updatedCard = paymentCardService.updateUserPaymentCard(userId, paymentCardDto);
        return ResponseEntity.ok(updatedCard);
    }

    @PatchMapping("/{userId}/payment-card/{cardId}/activate")
    public ResponseEntity<Void> activatePaymentCard(
            @PathVariable("userId") Long userId,
            @PathVariable("cardId") Long cardId,
            HttpServletRequest request
    ) {
        String role = getRoleFromRequest(request);

        if (!"ROLE_ADMIN".equals(role)) {
            throw new AccessDeniedException("Access denied. Only ADMIN can activate cards.");
        }

        paymentCardService.activateUserPaymentCard(userId, cardId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/payment-card/{cardId}/deactivate")
    public ResponseEntity<Void> deactivateUserPaymentCard(
            @PathVariable("userId") Long userId,
            @PathVariable("cardId") Long cardId,
            HttpServletRequest request
    ) {
        String role = getRoleFromRequest(request);

        if (!"ROLE_ADMIN".equals(role)) {
            throw new AccessDeniedException("Access denied. Only ADMIN can activate cards.");
        }

        paymentCardService.deactivateUserPaymentCard(userId, cardId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new AccessDeniedException("User ID not found in request");
        }
        return (Long) userId;
    }

    private String getRoleFromRequest(HttpServletRequest request) {
        Object role = request.getAttribute("userRole");
        if (role == null) {
            throw new AccessDeniedException("Role not found in request");
        }
        return (String) role;
    }

    private void checkAdminAccess(HttpServletRequest request) {
        String role = getRoleFromRequest(request);
        if (!"ROLE_ADMIN".equals(role)) {
            throw new AccessDeniedException("Access denied. ADMIN role required.");
        }
    }
}