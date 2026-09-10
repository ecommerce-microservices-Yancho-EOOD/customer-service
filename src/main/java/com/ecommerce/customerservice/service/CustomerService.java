package com.ecommerce.customerservice.service;

import com.ecommerce.customerservice.dto.CustomerRequest;
import com.ecommerce.customerservice.dto.CustomerResponse;
import com.ecommerce.customerservice.entity.Customer;
import com.ecommerce.customerservice.exceptions.BusinessException;
import com.ecommerce.customerservice.exceptions.ResourceNotFoundException;
import com.ecommerce.customerservice.reposittory.CustomerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    // ============ CREATE ============
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("Creating customer: {}", request.getEmail());

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists: " + request.getEmail());
        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .isActive(true)
                .totalSpent(0.0)
                .loyaltyPoints(0)
                .registeredDate(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Customer created with ID: {}", saved.getId());

        return toResponse(saved);
    }

    public CustomerResponse getCustomer(UUID id) {
        log.info("Fetching customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        return toResponse(customer);
    }
    public List<CustomerResponse> getAllCustomers() {
        log.info("Fetching all customers");
        return customerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CustomerResponse> seachCustomer(String keyword)
    {
        log.info("Searching customers with keyword: {}", keyword);
        return customerRepository.searchCustomers(keyword).stream()
                .map(this::toResponse)
                .toList();
    }

    public CustomerResponse getCustomerByEmail(String email) {
        log.info("Fetching customer by email: {}", email);
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", email));

        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, CustomerRequest customerRequest)
    {
        log.info("Update customer with ID : {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        if(customerRequest.getFirstName() != null) customer.setFirstName(customerRequest.getFirstName());
        if (customerRequest.getLastName() != null) customer.setLastName(customerRequest.getLastName());
        if (customerRequest.getPhone() != null) customer.setPhone(customerRequest.getPhone());
        if (customerRequest.getAddress() != null) customer.setAddress(customerRequest.getAddress());
        if (customerRequest.getCity() != null) customer.setCity(customerRequest.getCity());
        if (customerRequest.getState() != null) customer.setState(customerRequest.getState());
        if (customerRequest.getZipCode() != null) customer.setZipCode(customerRequest.getZipCode());
        customer.setUpdatedAt(LocalDateTime.now());
        Customer updated = customerRepository.save(customer);
        log.info("Customer updated: {}", updated.getId());

        return toResponse(updated);
    }

    @Transactional
    public CustomerResponse addBalance(UUID id, Double amount)
    {
        log.info("Adding ${} to customer {}", amount, id);
        if (amount <= 0) {
            throw new BusinessException("Amount must be positive", "INVALID_AMOUNT");
        }
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setTotalSpent(customer.getTotalSpent() + amount);

        int pointsEarned = (int)(amount/10);
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + pointsEarned);
        customer.setUpdatedAt(LocalDateTime.now());
        Customer updated =customerRepository.save(customer);
        return toResponse(updated);
    }
    @Transactional
    public void deleteCustomer(UUID id) {
        log.info("Deleting customer with ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setIsActive(false);
        customer.setUpdatedAt(LocalDateTime.now());

        customerRepository.save(customer);
        log.info("Customer deleted: {}", id);
    }

    public String getCustomerSegment(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        double spent = customer.getTotalSpent();

        if (spent > 500) return "VIP";
        if (spent > 200) return "Premium";
        if (spent > 100) return "Regular";
        return "Basic";
    }


    private CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .city(customer.getCity())
                .state(customer.getState())
                .zipCode(customer.getZipCode())
                .totalSpent(customer.getTotalSpent())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .isActive(customer.getIsActive())
                .registeredDate(customer.getRegisteredDate())
                .build();
    }
}

