package org.n11bootcamp.orderservice.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Shipping address must not be blank")
    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    private String shippingAddress;

    @NotBlank(message = "Card holder name must not be blank")
    private String cardHolderName;

    @NotBlank(message = "Card number must not be blank")
    @Pattern(regexp = "\\d{15,16}", message = "Card number must be 15 or 16 digits")
    private String cardNumber;

    @NotBlank(message = "Expire month must not be blank")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Expire month must be between 01-12")
    private String expireMonth;

    @NotBlank(message = "Expire year must not be blank")
    @Pattern(regexp = "^\\d{4}$", message = "Expire year must be 4 digits")
    private String expireYear;

    @NotBlank(message = "CVC must not be blank")
    @Pattern(regexp = "\\d{3,4}", message = "CVC must be 3 or 4 digits")
    private String cvc;

    private String buyerEmail;
}