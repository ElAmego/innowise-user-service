package com.innowise.userservice.controller;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.service.PaymentCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paymentcards")
public class PaymentCardController {
    private static final int PAGINATION_SIZE = 25;
    private static final String PAGINATION_SORTED_BY = "id";
    private final PaymentCardService paymentCardService;

    @Autowired
    public PaymentCardController(PaymentCardService paymentCardService) {
        this.paymentCardService = paymentCardService;
    }

    @PostMapping
    public ResponseEntity<PaymentCardDto> createPaymentCard(@RequestBody PaymentCardDto paymentCardDto) {
        final PaymentCardDto createdCard = paymentCardService.createPaymentCard(paymentCardDto);
        return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardDto> getPaymentCardById(@PathVariable("id") Long id) {
        final PaymentCardDto paymentCard = paymentCardService.getPaymentCardById(id);
        return ResponseEntity.ok(paymentCard);
    }

    @GetMapping
    public ResponseEntity<Page<PaymentCardDto>> getAllPaymentCards(
            @PageableDefault(size = PAGINATION_SIZE, sort = PAGINATION_SORTED_BY, direction = Sort.Direction.ASC)
            Pageable pageable) {

        final Page<PaymentCardDto> cards = paymentCardService.getAllPaymentCards(pageable);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentCardDto>> getAllPaymentCardsByUserId(@PathVariable("userId") Long userId) {
        final List<PaymentCardDto> cards = paymentCardService.getAllPaymentCardsByUserId(userId);
        return ResponseEntity.ok(cards);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardDto> updatePaymentCard(@PathVariable("id") Long id,
                                                            @RequestBody PaymentCardDto paymentCardDto) {
        paymentCardDto.setId(id);
        final PaymentCardDto updatedCard = paymentCardService.updatePaymentCard(paymentCardDto);
        return ResponseEntity.ok(updatedCard);
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activatePaymentCard(@PathVariable("id") Long id) {
        boolean activated = paymentCardService.activatePaymentCard(id);

        if (!activated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePaymentCard(@PathVariable("id") Long id) {
        boolean deactivated = paymentCardService.deactivatePaymentCard(id);

        if (!deactivated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}