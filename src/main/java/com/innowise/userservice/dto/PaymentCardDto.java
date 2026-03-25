package com.innowise.userservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentCardDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

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
}