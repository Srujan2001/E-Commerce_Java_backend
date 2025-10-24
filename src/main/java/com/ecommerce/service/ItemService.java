package com.ecommerce.service;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.model.Item;
import com.ecommerce.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ApiResponse addItem(String itemName, String description, BigDecimal itemCost,
                               Integer itemQuantity, String itemCategory, String addedBy,
                               MultipartFile file) {
        try {
            String filename = null;
            if (file != null && !file.isEmpty()) {
                filename = fileStorageService.storeFile(file);
            }

            Item item = new Item();
            item.setItemName(itemName);
            item.setDescription(description);
            item.setItemCost(itemCost);
            item.setItemQuantity(itemQuantity);
            item.setItemCategory(itemCategory);
            item.setAddedBy(addedBy);
            item.setImgname(filename);

            itemRepository.save(item);

            return new ApiResponse(true, itemName + " added successfully", item);
        } catch (Exception e) {
            log.error("Failed to add item", e);
            return new ApiResponse(false, "Failed to add item: " + e.getMessage());
        }
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public List<Item> getItemsByAdmin(String adminEmail) {
        return itemRepository.findByAddedBy(adminEmail);
    }

    public List<Item> getItemsByCategory(String category) {
        return itemRepository.findByItemCategory(category);
    }

    public Item getItemById(UUID itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found with id: " + itemId));
    }

    public List<Item> searchItems(String keyword) {
        return itemRepository.searchItems(keyword);
    }

    @Transactional
    public ApiResponse updateItem(UUID itemId, String itemName, String description,
                                  BigDecimal itemCost, Integer itemQuantity,
                                  String itemCategory, String addedBy, MultipartFile file) {
        try {
            Item item = getItemById(itemId);

            // Check if the admin owns this item
            if (!item.getAddedBy().equals(addedBy)) {
                return new ApiResponse(false, "Unauthorized: You can only update your own items");
            }

            // Update fields
            item.setItemName(itemName);
            item.setDescription(description);
            item.setItemCost(itemCost);
            item.setItemQuantity(itemQuantity);
            item.setItemCategory(itemCategory);

            // Update image if provided
            if (file != null && !file.isEmpty()) {
                // Delete old image
                if (item.getImgname() != null) {
                    fileStorageService.deleteFile(item.getImgname());
                }
                // Store new image
                String filename = fileStorageService.storeFile(file);
                item.setImgname(filename);
            }

            itemRepository.save(item);

            return new ApiResponse(true, "Item updated successfully", item);
        } catch (Exception e) {
            log.error("Failed to update item", e);
            return new ApiResponse(false, "Failed to update item: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse deleteItem(UUID itemId, String addedBy) {
        try {
            Item item = getItemById(itemId);

            // Check if the admin owns this item
            if (!item.getAddedBy().equals(addedBy)) {
                return new ApiResponse(false, "Unauthorized: You can only delete your own items");
            }

            // Delete image file
            if (item.getImgname() != null) {
                fileStorageService.deleteFile(item.getImgname());
            }

            itemRepository.delete(item);

            return new ApiResponse(true, "Item deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete item", e);
            return new ApiResponse(false, "Failed to delete item: " + e.getMessage());
        }
    }
}