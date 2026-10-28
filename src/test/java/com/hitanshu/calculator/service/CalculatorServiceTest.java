package com.hitanshu.calculator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalculatorServiceTest {

    private final CalculatorService calculator = new CalculatorService();

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({"2, 3, 5", "-4, 1.5, -2.5", "0.1, 0.2, 0.3", "0, 0, 0"})
    void add(String a, String b, String expected) {
        assertEquals(bd(expected), calculator.add(bd(a), bd(b)));
    }

    @ParameterizedTest(name = "{0} - {1} = {2}")
    @CsvSource({"10, 4, 6", "4, 10, -6", "1.5, 0.5, 1", "-3, -3, 0"})
    void subtract(String a, String b, String expected) {
        assertEquals(bd(expected), calculator.subtract(bd(a), bd(b)));
    }

    @ParameterizedTest(name = "{0} * {1} = {2}")
    @CsvSource({"6, 7, 42", "-2, 3.5, -7", "0, 99, 0", "1.1, 1.1, 1.21"})
    void multiply(String a, String b, String expected) {
        assertEquals(bd(expected), calculator.multiply(bd(a), bd(b)));
    }

    @ParameterizedTest(name = "{0} / {1} = {2}")
    @CsvSource({"10, 2, 5", "7, 2, 3.5", "-9, 3, -3", "1, 3, 0.3333333333333333", "100, 10, 10"})
    void divide(String a, String b, String expected) {
        assertEquals(bd(expected), calculator.divide(bd(a), bd(b)));
    }

    @Test
    @DisplayName("Dividing by zero throws ArithmeticException")
    void divideByZero() {
        ArithmeticException ex = assertThrows(ArithmeticException.class,
                () -> calculator.divide(bd("5"), bd("0")));
        assertEquals("Division by zero is not allowed", ex.getMessage());
    }

    @Test
    @DisplayName("Dividing by 0.00 is also treated as division by zero")
    void divideByZeroWithScale() {
        assertThrows(ArithmeticException.class, () -> calculator.divide(bd("5"), bd("0.00")));
    }
}
