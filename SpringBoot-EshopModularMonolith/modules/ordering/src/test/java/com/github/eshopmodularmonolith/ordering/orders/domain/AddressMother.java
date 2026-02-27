package com.github.eshopmodularmonolith.ordering.orders.domain;

import com.github.eshopmodularmonolith.ordering.orders.domain.valueobjects.Address;

public class AddressMother {

    public static Address address() {
        return new Builder().build();
    }

    public static class Builder {
        private String firstName = "John";
        private String lastName = "Doe";
        private String emailAddress = "john.doe@mail.com";
        private String addressLine = "Groove street";
        private String country = "Middle Earth";
        private String state = "Mordor";
        private String zipCode = "2321023";

        public Address build(){
            return new Address(firstName, lastName, emailAddress, addressLine, country, state, zipCode);
        }
    }


}
