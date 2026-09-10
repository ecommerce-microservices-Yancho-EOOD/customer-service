package com.ecommerce.customerservice.service;

import com.ecommerce.customerservice.dto.CustomerRequest;
import com.ecommerce.customerservice.dto.CustomerResponse;
import com.ecommerce.customerservice.entity.Customer;
import com.ecommerce.customerservice.exceptions.BusinessException;
import com.ecommerce.customerservice.exceptions.ResourceNotFoundException;
import com.ecommerce.customerservice.reposittory.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer sampleCustomer;
    private CustomerRequest sampleRequest;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();

        sampleCustomer = Customer.builder()
                .id(customerId)
                .firstName("Yancho")
                .lastName("Shterev")
                .email("yancho@example.com")
                .phone("0888829291")
                .address("Some street 1")
                .city("Nessebar")
                .state("Burgas")
                .zipCode("8230")
                .totalSpent(0.0)
                .loyaltyPoints(0)
                .isActive(true)
                .build();

        sampleRequest = CustomerRequest.builder()
                .firstName("Yancho")
                .lastName("Shterev")
                .email("yancho@example.com")
                .phone("0888829291")
                .address("Some street 1")
                .city("Nessebar")
                .state("Burgas")
                .zipCode("8230")
                .build();
    }

    // ============ CREATE ============

    @Test
    void createCustomer_shouldSaveAndReturnResponse_whenEmailIsNew() {
        when(customerRepository.existsByEmail(sampleRequest.getEmail())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerResponse response = customerService.createCustomer(sampleRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("yancho@example.com");
        assertThat(response.getId()).isEqualTo(customerId);

        // Verify the entity that was actually passed to save() was built correctly
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer captured = captor.getValue();
        assertThat(captured.getFirstName()).isEqualTo("Yancho");
        assertThat(captured.getIsActive()).isTrue();
        assertThat(captured.getTotalSpent()).isEqualTo(0.0);
    }

    @Test
    void createCustomer_shouldThrowBusinessException_whenEmailAlreadyExists() {
        when(customerRepository.existsByEmail(sampleRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(sampleRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already exists");

        // Make sure we never tried to save a duplicate
        verify(customerRepository, never()).save(any());
    }

    // ============ READ ============

    @Test
    void getCustomer_shouldReturnResponse_whenCustomerExists() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));

        CustomerResponse response = customerService.getCustomer(customerId);

        assertThat(response.getId()).isEqualTo(customerId);
        assertThat(response.getEmail()).isEqualTo("yancho@example.com");
    }

    @Test
    void getCustomer_shouldThrowResourceNotFoundException_whenCustomerDoesNotExist() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(customerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCustomerByEmail_shouldReturnResponse_whenFound() {
        when(customerRepository.findByEmail("yancho@example.com"))
                .thenReturn(Optional.of(sampleCustomer));

        CustomerResponse response = customerService.getCustomerByEmail("yancho@example.com");

        assertThat(response.getEmail()).isEqualTo("yancho@example.com");
    }

    @Test
    void getCustomerByEmail_shouldThrow_whenNotFound() {
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerByEmail("missing@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllCustomers_shouldReturnMappedList() {
        Customer second = Customer.builder()
                .id(UUID.randomUUID())
                .firstName("Second")
                .lastName("Customer")
                .email("second@example.com")
                .isActive(true)
                .totalSpent(0.0)
                .loyaltyPoints(0)
                .build();

        when(customerRepository.findAll()).thenReturn(List.of(sampleCustomer, second));

        List<CustomerResponse> responses = customerService.getAllCustomers();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(CustomerResponse::getEmail)
                .containsExactlyInAnyOrder("yancho@example.com", "second@example.com");
    }

    // ============ UPDATE ============

    @Test
    void updateCustomer_shouldUpdateOnlyProvidedFields() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerRequest partialUpdate = CustomerRequest.builder()
                .firstName("UpdatedName")
                // everything else left null on purpose
                .build();

        CustomerResponse response = customerService.updateCustomer(customerId, partialUpdate);

        assertThat(response.getFirstName()).isEqualTo("UpdatedName");
        // lastName wasn't in the request, so it should be untouched
        assertThat(response.getLastName()).isEqualTo("Shterev");
    }

    @Test
    void updateCustomer_shouldThrow_whenCustomerNotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateCustomer(customerId, sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============ BALANCE / LOYALTY ============

    @Test
    void addBalance_shouldIncreaseTotalSpentAndAwardLoyaltyPoints() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = customerService.addBalance(customerId, 100.0);

        assertThat(response.getTotalSpent()).isEqualTo(100.0);
        assertThat(response.getLoyaltyPoints()).isEqualTo(10); // 100 / 10
    }

    @Test
    void addBalance_shouldThrowBusinessException_whenAmountIsZeroOrNegative() {
        assertThatThrownBy(() -> customerService.addBalance(customerId, 0.0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be positive");

        assertThatThrownBy(() -> customerService.addBalance(customerId, -50.0))
                .isInstanceOf(BusinessException.class);

        // Should fail fast before ever touching the repository
        verifyNoInteractions(customerRepository);
    }

    @Test
    void addBalance_shouldThrow_whenCustomerNotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.addBalance(customerId, 50.0))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============ DELETE (soft delete) ============

    @Test
    void deleteCustomer_shouldSetIsActiveFalse_ratherThanRemovingRow() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        customerService.deleteCustomer(customerId);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getIsActive()).isFalse();

        // Confirm this is a soft delete — deleteById should never be called
        verify(customerRepository, never()).deleteById(any());
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void deleteCustomer_shouldThrow_whenCustomerNotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.deleteCustomer(customerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============ SEGMENTATION ============

    @Test
    void getCustomerSegment_shouldReturnVIP_whenSpentOver500() {
        sampleCustomer.setTotalSpent(600.0);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));

        assertThat(customerService.getCustomerSegment(customerId)).isEqualTo("VIP");
    }

    @Test
    void getCustomerSegment_shouldReturnPremium_whenSpentBetween200And500() {
        sampleCustomer.setTotalSpent(250.0);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));

        assertThat(customerService.getCustomerSegment(customerId)).isEqualTo("Premium");
    }

    @Test
    void getCustomerSegment_shouldReturnRegular_whenSpentBetween100And200() {
        sampleCustomer.setTotalSpent(150.0);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));

        assertThat(customerService.getCustomerSegment(customerId)).isEqualTo("Regular");
    }

    @Test
    void getCustomerSegment_shouldReturnBasic_whenSpentIsLowOrZero() {
        sampleCustomer.setTotalSpent(50.0);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));

        assertThat(customerService.getCustomerSegment(customerId)).isEqualTo("Basic");
    }

    // ============ SEARCH ============

    @Test
    void searchCustomer_shouldReturnMatchingCustomers() {
        when(customerRepository.searchCustomers("yancho")).thenReturn(List.of(sampleCustomer));

        List<CustomerResponse> results = customerService.seachCustomer("yancho");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("yancho@example.com");
    }
}