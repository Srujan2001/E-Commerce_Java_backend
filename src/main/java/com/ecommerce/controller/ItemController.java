package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.model.Item;
import com.ecommerce.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addItem(
            @RequestParam("itemName") String itemName,
            @RequestParam("description") String description,
            @RequestParam("itemCost") BigDecimal itemCost,
            @RequestParam("itemQuantity") Integer itemQuantity,
            @RequestParam("itemCategory") String itemCategory,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            ApiResponse response = itemService.addItem(itemName, description, itemCost,
                    itemQuantity, itemCategory, adminEmail, file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to add item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to add item: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllItems() {
        try {
            List<Item> items = itemService.getAllItems();
            return ResponseEntity.ok(new ApiResponse(true, "Items fetched successfully", items));
        } catch (Exception e) {
            log.error("Failed to fetch items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to fetch items"));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<?> getItemsByAdmin(Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            List<Item> items = itemService.getItemsByAdmin(adminEmail);
            return ResponseEntity.ok(new ApiResponse(true, "Items fetched successfully", items));
        } catch (Exception e) {
            log.error("Failed to fetch items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to fetch items"));
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<?> getItemsByCategory(@PathVariable String category) {
        try {
            List<Item> items = itemService.getItemsByCategory(category);
            return ResponseEntity.ok(new ApiResponse(true, "Items fetched successfully", items));
        } catch (Exception e) {
            log.error("Failed to fetch items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to fetch items"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getItemById(@PathVariable UUID id) {
        try {
            Item item = itemService.getItemById(id);
            return ResponseEntity.ok(new ApiResponse(true, "Item fetched successfully", item));
        } catch (Exception e) {
            log.error("Failed to fetch item", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Item not found"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchItems(@RequestParam String keyword) {
        try {
            List<Item> items = itemService.searchItems(keyword);
            return ResponseEntity.ok(new ApiResponse(true, "Search completed", items));
        } catch (Exception e) {
            log.error("Search failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Search failed"));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse> updateItem(
            @PathVariable UUID id,
            @RequestParam("itemName") String itemName,
            @RequestParam("description") String description,
            @RequestParam("itemCost") BigDecimal itemCost,
            @RequestParam("itemQuantity") Integer itemQuantity,
            @RequestParam("itemCategory") String itemCategory,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            ApiResponse response = itemService.updateItem(id, itemName, description, itemCost,
                    itemQuantity, itemCategory, adminEmail, file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to update item: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deleteItem(@PathVariable UUID id,
                                                  Authentication authentication) {
        try {
            String adminEmail = authentication.getName();
            ApiResponse response = itemService.deleteItem(id, adminEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to delete item: " + e.getMessage()));
        }
    }
}