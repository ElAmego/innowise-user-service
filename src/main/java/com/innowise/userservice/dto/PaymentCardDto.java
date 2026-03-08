package com.innowise.userservice.dto;

import com.innowise.userservice.model.entity.User;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class PaymentCardDto {
    private Long id;

    @NotNull(message = "User is required")
    private User user;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "Card number must be 16 digits")
    private String number;

    @NotBlank(message = "Card holder is required")
    @Size(min = 1, max = 255, message = "Holder name must be between 2 and 100 characters")
    private String holder;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Card must not be expired")
    private LocalDate expirationDate;

    private Boolean active;

    public PaymentCardDto() { }

    public PaymentCardDto(Long id, User user, String number, String holder, LocalDate expirationDate,
                          Boolean active) {
        this.id = id;
        this.user = user;
        this.number = number;
        this.holder = holder;
        this.expirationDate = expirationDate;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}