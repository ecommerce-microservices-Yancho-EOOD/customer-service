package com.ecommerce.customerservice.controller;

import com.ecommerce.customerservice.dto.ApiResponse;
import com.ecommerce.customerservice.dto.CustomerRequest;
import com.ecommerce.customerservice.dto.CustomerResponse;
import com.ecommerce.customerservice.reposittory.CustomerRepository;
import com.ecommerce.customerservice.service.CustomerService;
import io.micrometer.core.ipc.http.HttpSender;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor

public class CustomerController {
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.createCustomer(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }
    @GetMapping("/email")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(@RequestParam String email) {
        return ResponseEntity.ok(customerService.getCustomerByEmail(email));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers()
    {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }
    @GetMapping("/{id}/segment")
    public ResponseEntity<ApiResponse> getCustomerSegment(@PathVariable UUID id) {
        String segment = customerService.getCustomerSegment(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Customer segment: " + segment)
                .data(segment)
                .build());
    }
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @PostMapping("/{id}/balance")
    public ResponseEntity<CustomerResponse> addBalance(
            @PathVariable UUID id,
            @RequestParam Double amount) {
        return ResponseEntity.ok(customerService.addBalance(id, amount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Customer deleted successfully")
                .build());
    }

}
