package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.model.Contact;
import com.ecommerce.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse> submitContact(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String email = request.get("email");
            String message = request.get("message");

            ApiResponse response = contactService.submitContact(name, email, message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to submit contact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to submit message: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllContacts() {
        try {
            List<Contact> contacts = contactService.getAllContacts();
            return ResponseEntity.ok(new ApiResponse(true, "Contacts fetched successfully", contacts));
        } catch (Exception e) {
            log.error("Failed to fetch contacts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to fetch contacts"));
        }
    }

    @GetMapping("/{contactId}")
    public ResponseEntity<?> getContactById(@PathVariable Long contactId) {
        try {
            Contact contact = contactService.getContactById(contactId);
            return ResponseEntity.ok(new ApiResponse(true, "Contact fetched successfully", contact));
        } catch (Exception e) {
            log.error("Failed to fetch contact", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Contact not found"));
        }
    }

    @DeleteMapping("/delete/{contactId}")
    public ResponseEntity<ApiResponse> deleteContact(@PathVariable Long contactId) {
        try {
            ApiResponse response = contactService.deleteContact(contactId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete contact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to delete contact"));
        }
    }
}