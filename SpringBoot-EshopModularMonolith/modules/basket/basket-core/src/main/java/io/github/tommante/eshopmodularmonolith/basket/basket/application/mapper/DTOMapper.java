package io.github.tommante.eshopmodularmonolith.basket.basket.application.mapper;

import io.github.tommante.eshopmodularmonolith.basket.basket.contract.AddressDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.PaymentDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Payment;

public class DTOMapper {


    private DTOMapper() {

    }

    public static AddressDTO toDto(Address address){
        return new AddressDTO(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    public static PaymentDTO toDto(Payment payment) {
        return new PaymentDTO(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv());
    }
}
