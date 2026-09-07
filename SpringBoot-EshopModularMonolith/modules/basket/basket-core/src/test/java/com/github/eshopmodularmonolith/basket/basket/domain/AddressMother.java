package com.github.eshopmodularmonolith.basket.basket.domain;


import com.github.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;

public class AddressMother {

    public static Address address() {
        return new Builder().build();
    }

    public static Address address(String state) {
        return new Builder().withState(state).build();
    }

    public static Address billing(){
        return address("Billing State");
    }

    public static Address shipping(){
        return address("Shipping State");
    }

    public static class Builder {
        private String firstName = "John";
        private String lastName = "Doe";
        private String emailAddress = "john.doe@mail.com";
        private String addressLine = "Groove street";
        private String country = "Middle Earth";
        private String state = "Mordor";
        private String zipCode = "2321023";

        public Builder withState(String state) {
            this.state = state;
            return this;
        }

        public Address build(){
            return new Address(firstName, lastName, emailAddress, addressLine, country, state, zipCode);
        }
    }


}
