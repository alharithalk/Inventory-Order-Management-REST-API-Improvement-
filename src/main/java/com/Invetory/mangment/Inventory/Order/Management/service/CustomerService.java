package com.Invetory.mangment.Inventory.Order.Management.service;

import com.Invetory.mangment.Inventory.Order.Management.entity.Customer;
import com.Invetory.mangment.Inventory.Order.Management.entity.CustomerPhone;
import com.Invetory.mangment.Inventory.Order.Management.repository.CustomerPhoneRepository;
import com.Invetory.mangment.Inventory.Order.Management.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerPhoneRepository customerPhoneRepository;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerPhoneRepository customerPhoneRepository) {
        this.customerRepository = customerRepository;
        this.customerPhoneRepository = customerPhoneRepository;
    }

    public Customer createCustomer(String firstName, String lastName, String email) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        return customerRepository.save(customer);
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public CustomerPhone addPhone(Long customerId, String phoneNumber) {
        Customer customer = getCustomerById(customerId);
        CustomerPhone phone = new CustomerPhone();
        phone.setCustomer(customer);
        phone.setPhoneNumber(phoneNumber);
        return customerPhoneRepository.save(phone);
    }
}
