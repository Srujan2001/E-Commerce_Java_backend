package com.ecommerce.service;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.model.Contact;
import com.ecommerce.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactRepository contactRepository;

    @Transactional
    public ApiResponse submitContact(String name, String email, String message) {
        try {
            Contact contact = new Contact();
            contact.setName(name);
            contact.setEmail(email);
            contact.setMessage(message);

            contactRepository.save(contact);

            return new ApiResponse(true, "Your message has been submitted successfully!", contact);
        } catch (Exception e) {
            log.error("Failed to submit contact", e);
            return new ApiResponse(false, "Failed to submit message: " + e.getMessage());
        }
    }

    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }

    public Contact getContactById(Long contactId) {
        return contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found with id: " + contactId));
    }

    @Transactional
    public ApiResponse deleteContact(Long contactId) {
        try {
            contactRepository.deleteById(contactId);
            return new ApiResponse(true, "Contact deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete contact", e);
            return new ApiResponse(false, "Failed to delete contact: " + e.getMessage());
        }
    }
}