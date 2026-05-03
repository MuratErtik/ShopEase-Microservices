package org.n11bootcamp.inventoryservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.inventoryservice.dtos.reponses.InventoryResponse;
import org.n11bootcamp.inventoryservice.dtos.requests.ReleaseStockRequest;
import org.n11bootcamp.inventoryservice.dtos.requests.ReserveStockRequest;
import org.n11bootcamp.inventoryservice.dtos.requests.UpdateStockRequest;
import org.n11bootcamp.inventoryservice.entities.Inventory;
import org.n11bootcamp.inventoryservice.entities.OutboxEvent;
import org.n11bootcamp.inventoryservice.enums.EventType;
import org.n11bootcamp.inventoryservice.exceptions.ExceededReservedAmountException;
import org.n11bootcamp.inventoryservice.exceptions.InventoryAlreadyExistsException;
import org.n11bootcamp.inventoryservice.exceptions.InventoryNotFoundException;
import org.n11bootcamp.inventoryservice.repositories.InventoryRepository;
import org.n11bootcamp.inventoryservice.repositories.OutboxEventRepository;
import org.n11bootcamp.inventoryservice.services.impl.InventoryServiceImpl;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private OutboxEventRepository outboxRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private InventoryServiceImpl inventoryService;

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    // --- Create Inventory Tests ---

    @Test
    @DisplayName("createInventory should save and return response when inventory does not exist")
    void createInventory_success() {
        when(inventoryRepository.existsByProductId(PRODUCT_ID)).thenReturn(false);
        Inventory inventory = Inventory.builder()
                .id(new UUID(0,0))
                .productId(PRODUCT_ID)
                .sellerId(SELLER_ID)
                .availableQuantity(100)
                .reservedQuantity(0)
                .build();
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        InventoryResponse response = inventoryService.createInventory(PRODUCT_ID, SELLER_ID, "test@test.com", 100);

        assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(response.getAvailableQuantity()).isEqualTo(100);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("createInventory should throw exception when inventory already exists")
    void createInventory_alreadyExists() {
        when(inventoryRepository.existsByProductId(PRODUCT_ID)).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.createInventory(PRODUCT_ID, SELLER_ID, "test@test.com", 100))
                .isInstanceOf(InventoryAlreadyExistsException.class);
    }

    // --- Reserve Stock Tests ---

    @Test
    @DisplayName("reserveStock should decrease available and increase reserved when stock is sufficient")
    void reserveStock_success() throws JsonProcessingException {
        // given
        Inventory inventory = Inventory.builder()
                .id(new UUID(0,0)).productId(PRODUCT_ID).availableQuantity(10).reservedQuantity(0).build();

        when(inventoryRepository.findByProductIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(inventory));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        ReserveStockRequest request = new ReserveStockRequest(ORDER_ID, PRODUCT_ID, 4);

        // when
        inventoryService.reserveStock(request);

        // then
        assertThat(inventory.getAvailableQuantity()).isEqualTo(6);
        assertThat(inventory.getReservedQuantity()).isEqualTo(4);
        verify(inventoryRepository).save(inventory);
        verify(outboxRepository).save(any(OutboxEvent.class)); // STOCK_RESERVED event
    }

    @Test
    @DisplayName("reserveStock should save failed outbox event when stock is insufficient")
    void reserveStock_insufficientStock() throws JsonProcessingException {
        // given
        Inventory inventory = Inventory.builder()
                .id(new UUID(0,0)).productId(PRODUCT_ID).availableQuantity(2).reservedQuantity(0)
                .sellerEmail("test@gmail.com")
                .build();

        when(inventoryRepository.findByProductIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(inventory));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        ReserveStockRequest request = new ReserveStockRequest(ORDER_ID, PRODUCT_ID, 5);

        // when
        inventoryService.reserveStock(request);

        // then
        verify(inventoryRepository, never()).save(any());

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.STOCK_RESERVATION_FAILED);
    }

    // --- Release Stock Tests ---

    @Test
    @DisplayName("releaseStock should increase available and decrease reserved")
    void releaseStock_success() throws JsonProcessingException {
        Inventory inventory = Inventory.builder()
                .id(new UUID(0,0)).productId(PRODUCT_ID).availableQuantity(5).reservedQuantity(5).build();

        when(inventoryRepository.findByProductIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);

        ReleaseStockRequest request = new ReleaseStockRequest(ORDER_ID, PRODUCT_ID, 3);

        inventoryService.releaseStock(request);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(8);
        assertThat(inventory.getReservedQuantity()).isEqualTo(2);
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("releaseStock should throw exception when release amount exceeds reserved")
    void releaseStock_exceedsReserved() {
        Inventory inventory = Inventory.builder()
                .productId(PRODUCT_ID).reservedQuantity(2).build();

        when(inventoryRepository.findByProductIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(inventory));

        ReleaseStockRequest request = new ReleaseStockRequest(ORDER_ID, PRODUCT_ID, 5);

        assertThatThrownBy(() -> inventoryService.releaseStock(request))
                .isInstanceOf(ExceededReservedAmountException.class);
    }

    // --- Confirm Stock Tests ---

    @Test
    @DisplayName("confirmStock should finalize sale and clear reserved quantity")
    void confirmStock_success() {
        Inventory inventory = Inventory.builder()
                .productId(PRODUCT_ID).reservedQuantity(10).build();

        when(inventoryRepository.findByProductIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(inventory));

        inventoryService.confirmStock(PRODUCT_ID, ORDER_ID, 10);

        assertThat(inventory.getReservedQuantity()).isZero();
        assertThat(inventory.getLastConfirmedOrderId()).isEqualTo(ORDER_ID);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("confirmStock should ignore duplicate requests (Idempotency)")
    void confirmStock_idempotent() {
        Inventory inventory = Inventory.builder()
                .productId(PRODUCT_ID).lastConfirmedOrderId(ORDER_ID).build();

        when(inventoryRepository.findByProductIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(inventory));

        inventoryService.confirmStock(PRODUCT_ID, ORDER_ID, 10);

        verify(inventoryRepository, never()).save(any());
        verify(inventoryRepository).findByProductIdWithLock(PRODUCT_ID);
    }

    // --- Helper Tests ---

    @Test
    @DisplayName("getByProductId should throw exception when not found")
    void getByProductId_notFound() {
        when(inventoryRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getByProductId(PRODUCT_ID))
                .isInstanceOf(InventoryNotFoundException.class);
    }
}