package org.n11bootcamp.cartservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.n11bootcamp.cartservice.clients.InventoryClient;
import org.n11bootcamp.cartservice.clients.ProductClient;
import org.n11bootcamp.cartservice.dtos.reponses.InventoryResponse;
import org.n11bootcamp.cartservice.dtos.reponses.ProductResponse;
import org.n11bootcamp.cartservice.dtos.requests.AddCartItemRequest;
import org.n11bootcamp.cartservice.dtos.requests.UpdateCartItemRequest;
import org.n11bootcamp.cartservice.exceptions.CartItemNotFoundException;
import org.n11bootcamp.cartservice.exceptions.InsufficientStockException;
import org.n11bootcamp.cartservice.models.Cart;
import org.n11bootcamp.cartservice.models.CartItem;
import org.n11bootcamp.cartservice.repositories.CartRepository;
import org.n11bootcamp.cartservice.services.impl.CartServiceImpl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartRepository    cartRepository;
    @Mock private ProductClient     productClient;
    @Mock private InventoryClient   inventoryClient;
    @InjectMocks private CartServiceImpl cartService;

    private static final UUID USER_ID    = UUID.fromString("aaaa0000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("bbbb0000-0000-0000-0000-000000000001");
    private static final UUID SELLER_ID  = UUID.fromString("cccc0000-0000-0000-0000-000000000001");



    private ProductResponse product() {
        ProductResponse p = new ProductResponse();
        p.setId(PRODUCT_ID);
        p.setName("Widget");
        p.setPrice(new BigDecimal("49.99"));
        p.setImageUrl("http://img.test/widget.jpg");
        p.setSellerId(SELLER_ID);
        return p;
    }

    private InventoryResponse inventory(int available) {
        InventoryResponse i = new InventoryResponse();
        i.setProductId(PRODUCT_ID);
        i.setAvailableQuantity(available);
        return i;
    }

    private CartItem cartItem(int quantity) {
        return CartItem.builder()
                .productId(PRODUCT_ID)
                .name("Widget")
                .price(new BigDecimal("49.99"))
                .quantity(quantity)
                .imageUrl("http://img.test/widget.jpg")
                .sellerId(SELLER_ID)
                .build();
    }



    @Test
    @DisplayName("getCart should return an empty cart with zero totals when no items are stored")
    void getCart_returnsEmptyCart_whenRedisHasNoItems() {
        when(cartRepository.findAllItems(USER_ID)).thenReturn(Collections.emptyList());

        Cart cart = cartService.getCart(USER_ID);

        assertThat(cart.getUserId()).isEqualTo(USER_ID);
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cart.getTotalItems()).isZero();
    }

    @Test
    @DisplayName("getCart should aggregate totals correctly over all stored items")
    void getCart_returnsCorrectTotals_whenItemsExist() {
        // 2 × 49.99 = 99.98
        when(cartRepository.findAllItems(USER_ID)).thenReturn(List.of(cartItem(2)));

        Cart cart = cartService.getCart(USER_ID);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getTotalItems()).isEqualTo(2);
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("99.98");
    }



    @Test
    @DisplayName("addItem should save the item with the requested quantity when the cart is empty")
    void addItem_savesNewItem_whenCartIsEmpty() {
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product());
        when(inventoryClient.getInventory(PRODUCT_ID)).thenReturn(inventory(10));
        when(cartRepository.findItem(USER_ID, PRODUCT_ID)).thenReturn(null);
        when(cartRepository.findAllItems(USER_ID)).thenReturn(List.of(cartItem(3)));

        cartService.addItem(USER_ID, new AddCartItemRequest(PRODUCT_ID, 3));

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartRepository).saveItem(eq(USER_ID), saved.capture());

        CartItem item = saved.getValue();
        assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getName()).isEqualTo("Widget");
        assertThat(item.getSellerId()).isEqualTo(SELLER_ID);
    }

    @Test
    @DisplayName("addItem should merge the requested quantity with the existing cart quantity")
    void addItem_mergesQuantity_whenItemAlreadyInCart() {
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product());
        when(inventoryClient.getInventory(PRODUCT_ID)).thenReturn(inventory(10));
        when(cartRepository.findItem(USER_ID, PRODUCT_ID)).thenReturn(cartItem(2)); // 2 already in cart
        when(cartRepository.findAllItems(USER_ID)).thenReturn(List.of(cartItem(5)));

        cartService.addItem(USER_ID, new AddCartItemRequest(PRODUCT_ID, 3)); // +3

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartRepository).saveItem(eq(USER_ID), saved.capture());
        assertThat(saved.getValue().getQuantity()).isEqualTo(5); // 2 + 3
    }



    @Test
    @DisplayName("addItem should throw InsufficientStockException when available stock is less than requested")
    void addItem_throwsInsufficientStock_whenStockBelowRequested() {
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product());
        when(inventoryClient.getInventory(PRODUCT_ID)).thenReturn(inventory(2));
        when(cartRepository.findItem(USER_ID, PRODUCT_ID)).thenReturn(null);

        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(PRODUCT_ID, 5)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Available: 2")
                .hasMessageContaining("Requested: 5");

        verify(cartRepository, never()).saveItem(any(), any());
    }

    @Test
    @DisplayName("addItem should throw InsufficientStockException when cart + new quantity exceeds available stock")
    void addItem_throwsInsufficientStock_whenMergedQuantityExceedsStock() {
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product());
        when(inventoryClient.getInventory(PRODUCT_ID)).thenReturn(inventory(4));
        when(cartRepository.findItem(USER_ID, PRODUCT_ID)).thenReturn(cartItem(3)); // 3 in cart already

        // 3 + 2 = 5 > 4 available
        assertThatThrownBy(() -> cartService.addItem(USER_ID, new AddCartItemRequest(PRODUCT_ID, 2)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Available: 4")
                .hasMessageContaining("Requested: 5");

        verify(cartRepository, never()).saveItem(any(), any());
    }



    @Test
    @DisplayName("updateItem should skip inventory check and save when quantity is decreased")
    void updateItem_skipsInventoryCheck_whenDecreasingQuantity() {
        when(cartRepository.findItem(USER_ID, PRODUCT_ID)).thenReturn(cartItem(5));
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product());
        when(cartRepository.findAllItems(USER_ID)).thenReturn(List.of(cartItem(3)));

        cartService.updateItem(USER_ID, PRODUCT_ID, new UpdateCartItemRequest(3));

        verifyNoInteractions(inventoryClient);
        verify(cartRepository).saveItem(eq(USER_ID), any());
    }

    @Test
    @DisplayName("updateItem should check inventory and save when quantity is increased")
    void updateItem_checksInventory_whenIncreasingQuantity() {
        when(cartRepository.findItem(USER_ID, PRODUCT_ID)).thenReturn(cartItem(2));
        when(inventoryClient.getInventory(PRODUCT_ID)).thenReturn(inventory(10));
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(product());
        when(cartRepository.findAllItems(USER_ID)).thenReturn(List.of(cartItem(5)));

        cartService.updateItem(USER_ID, PRODUCT_ID, new UpdateCartItemRequest(5));

        verify(inventoryClient).getInventory(PRODUCT_ID);
        verify(cartRepository).saveItem(eq(USER_ID), any());
    }

    @Test
    @DisplayName("updateItem should refresh product details (name, price, image) on every update")
    void updateItem_refreshesProductDetails_onSave() {
        ProductResponse freshProduct = product();
        freshProduct.setPrice(new BigDecimal("59.99")); // price changed in catalog

        when(cartRepository.findItem(USER_ID, PRODUCT_ID)).thenReturn(cartItem(3));
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(freshProduct);
        when(cartRepository.findAllItems(USER_ID)).thenReturn(List.of(cartItem(2)));

        cartService.updateItem(USER_ID, PRODUCT_ID, new UpdateCartItemRequest(2));

        ArgumentCaptor<CartItem> saved = ArgumentCaptor.forClass(CartItem.class);
        verify(cartRepository).saveItem(eq(USER_ID), saved.capture());
        assertThat(saved.getValue().getPrice()).isEqualByComparingTo("59.99");
        assertThat(saved.getValue().getQuantity()).isEqualTo(2);
    }



    @Test
    @DisplayName("updateItem should throw CartItemNotFoundException when the item is not in the cart")
    void updateItem_throwsCartItemNotFound_whenItemMissing() {
        when(cartRepository.findItem(USER_ID, PRODUCT_ID)).thenReturn(null);

        assertThatThrownBy(() ->
                cartService.updateItem(USER_ID, PRODUCT_ID, new UpdateCartItemRequest(2)))
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessageContaining(PRODUCT_ID.toString());

        verify(cartRepository, never()).saveItem(any(), any());
        verifyNoInteractions(inventoryClient, productClient);
    }

    @Test
    @DisplayName("updateItem should throw InsufficientStockException when increasing to more than available stock")
    void updateItem_throwsInsufficientStock_whenStockInsufficient() {
        when(cartRepository.findItem(USER_ID, PRODUCT_ID)).thenReturn(cartItem(2));
        when(inventoryClient.getInventory(PRODUCT_ID)).thenReturn(inventory(3)); // only 3 available

        assertThatThrownBy(() ->
                cartService.updateItem(USER_ID, PRODUCT_ID, new UpdateCartItemRequest(5)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Available: 3")
                .hasMessageContaining("Requested: 5");

        verify(cartRepository, never()).saveItem(any(), any());
        verifyNoInteractions(productClient);
    }



    @Test
    @DisplayName("removeItem should delegate to the repository when the item exists in the cart")
    void removeItem_removesItem_whenExists() {
        when(cartRepository.itemExists(USER_ID, PRODUCT_ID)).thenReturn(true);

        cartService.removeItem(USER_ID, PRODUCT_ID);

        verify(cartRepository).removeItem(USER_ID, PRODUCT_ID);
    }

    @Test
    @DisplayName("removeItem should throw CartItemNotFoundException when the item is not in the cart")
    void removeItem_throwsCartItemNotFound_whenNotExists() {
        when(cartRepository.itemExists(USER_ID, PRODUCT_ID)).thenReturn(false);

        assertThatThrownBy(() -> cartService.removeItem(USER_ID, PRODUCT_ID))
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessageContaining(PRODUCT_ID.toString());

        verify(cartRepository, never()).removeItem(any(), any());
    }



    @Test
    @DisplayName("clearCart should delegate to the repository")
    void clearCart_delegatesToRepository() {
        cartService.clearCart(USER_ID);
        verify(cartRepository).clearCart(USER_ID);
    }
}