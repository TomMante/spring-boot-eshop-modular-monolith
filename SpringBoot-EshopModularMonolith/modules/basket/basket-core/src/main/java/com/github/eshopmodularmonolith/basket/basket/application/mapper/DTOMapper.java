package com.github.eshopmodularmonolith.basket.basket.application.mapper;

import com.github.eshopmodularmonolith.basket.basket.contract.AddressDTO;
import com.github.eshopmodularmonolith.basket.basket.contract.PaymentDTO;
import com.github.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import com.github.eshopmodularmonolith.basket.basket.domain.valueobjects.Payment;

public class DTOMapper {


    private DTOMapper() {

    }

    public static AddressDTO toDto(Address address){
        return new AddressDTO(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    public static Address toDomain(AddressDTO address){
        return new Address(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    public static PaymentDTO toDto(Payment payment) {
        return new PaymentDTO(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv(), payment.paymentMethod());
    }

    public static Payment toDomain(PaymentDTO payment) {
        return new Payment(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv(), payment.paymentMethod());
    }
}
