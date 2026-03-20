package com.innowise.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class UserWithCardsDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    private String name;

    @NotBlank(message = "Surname is required")
    @Size(min = 1, max = 255, message = "Surname must be between 1 and 255 characters")
    private String surname;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private Boolean active;

    private List<PaymentCardDto> cards;

    public UserWithCardsDto(UserDto userDto, List<PaymentCardDto> cards) {
        this.id = userDto.getId();
        this.email = userDto.getEmail();
        this.name = userDto.getName();
        this.surname = userDto.getSurname();
        this.birthDate = userDto.getBirthDate();
        this.active = userDto.getActive();
        this.cards = cards;
    }
}