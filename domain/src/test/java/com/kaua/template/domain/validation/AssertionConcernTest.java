package com.kaua.template.domain.validation;

import com.kaua.template.domain.UnitTest;
import com.kaua.template.domain.exceptions.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class AssertionConcernTest extends UnitTest {

    @Test
    void givenANullName_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person(null, "email", 20, 170, true, "password", "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenAnEmptyEmail_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", "", 20, 170, true, "password", "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenANullEmail_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", null, 20, 170, true, "password", "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenANameWithLessThan3Characters_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("na", "email", 20, 170, true, "password", "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenANameWithMoreThan50Characters_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("namessssssssssssssssssssssssssssssssssssssssssssssss", "email", 20, 170, true, "password", "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenAnInvalidNegativeHeight_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", "email", 20, -1, true, "password", "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenAnInvalidAgeLessThan18_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", "email", 17, 170, true, "password", "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenAnInvalidAgeEqualsTo18_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", "email", 18, 170, true, "password", "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenAnInvalidHeightLessThan150_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", "email", 20, 149, true, "password", "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenAFalseActive_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", "email", 20, 170, false, "password", "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenAnNullPassword_whenCreatePerson_shouldDoesNotThrowException() {
        Assertions.assertDoesNotThrow(() -> new Person("name", "email", 20, 170, true, null, "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenAnInvalidPasswordMoreThan20Characters_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", "email", 20, 170, true, "passwordpasswordpassword", "phonenumber", "12345678901", "address"));
    }

    @Test
    void givenAnInvalidPhonenumberNull_whenCreatePerson_shouldDoesNotThrowException() {
        Assertions.assertDoesNotThrow(() -> new Person("name", "email", 20, 170, true, "password", null, "12345678901", "address"));
    }

    @Test
    void givenAnInvalidPhonenumberLessThan10Characters_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", "email", 20, 170, true, "password", "123456789", "12345678901", "address"));
    }

    @Test
    void givenAValidValues_whenCreatePerson_shouldNotThrowException() {
        Assertions.assertDoesNotThrow(() -> new Person("name", "email", 20, 170, true, "password", "1234567890", "12345678901", "address"));
    }

    @Test
    void givenAnInvalidCpfNull_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", "email", 20, 170, true, "password", "1234567890", null, "address"));
    }

    @Test
    void givenAnInvalidCpfLessThan11Characters_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", "email", 20, 170, true, "password", "1234567890", "1234567890", "address"));
    }

    @Test
    void givenAnInvalidAddressNull_whenCreatePerson_shouldDoesNotThrowException() {
        Assertions.assertDoesNotThrow(
                () -> new Person("name", "email", 20, 170, true, "password", "1234567890", "12345678901", null));
    }

    @Test
    void givenAnInvalidAddressWithSpecialCharacters_whenCreatePerson_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class,
                () -> new Person("name", "email", 20, 170, true, "password", "1234567890", "12345678901", "address!@#$"));
    }

    @Test
    void givenAnEmptyItems_whenCreateItems_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class, () -> new Items(new HashSet<>(), List.of("sku1", "sku2")));
    }

    @Test
    void givenANullItems_whenCreateItems_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class, () -> new Items(null, List.of("sku1", "sku2")));
    }

    @Test
    void givenAValidItems_whenCreateItems_shouldNotThrowException() {
        Assertions.assertDoesNotThrow(() -> new Items(Set.of("item1", "item2"), List.of("sku1", "sku2")));
    }

    @Test
    void givenAnEmptySkus_whenCreateItems_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class, () -> new Items(Set.of("item1", "item2"), List.of()));
    }

    @Test
    void givenANullSkus_whenCreateItems_shouldThrowException() {
        Assertions.assertThrows(ValidationException.class, () -> new Items(Set.of("item1", "item2"), null));
    }

    private record Person(
            String name,
            String email,
            int age,
            int height,
            boolean active,
            String password,
            String phonenumber,
            String cpf,
            String address
    ) implements AssertionConcern {

        public Person {
            this.assertArgumentNotNull(name, "name", "should not be null");
            this.assertArgumentNotEmpty(email, "email", "should not be empty");
            this.assertArgumentMaxLength(name, 50, "name", "should not exceed 50 characters");
            this.assertArgumentMinLength(name, 3, "name", "should not be less than 3 characters");
            this.assertArgumentTrue(active, "active", "should be true");
            this.assertArgumentGreaterThan(age, 18, "age", "should be greater than 18");
            this.assertArgumentGreaterOrEquals(height, 150, "height", "should be greater or equals to 150");
            this.assertArgumentMaxLength(password, 20, "password", "should not exceed 20 characters");
            this.assertArgumentMinLength(phonenumber, 10, "phonenumber", "should not be less than 10 characters");
            this.assertArgumentFixedLength(cpf, 11, "cpf", "should have 11 characters");
            this.assertArgumentPattern(address, "^[a-zA-Z0-9 ]*$", "address", "should have only letters, numbers and spaces");
        }
    }

    private record Items(Set<String> items, List<String> skus) implements AssertionConcern {
        public Items {
            this.assertArgumentNotEmpty(items, "items", "should not be empty");
            this.assertArgumentNotEmpty(skus, "skus", "should not be empty");
        }
    }
}
