package com.hitanshu.calculator.service;

import java.math.BigDecimal;
import java.math.MathContext;

import org.springframework.stereotype.Service;

/**
 * Core arithmetic. BigDecimal avoids floating-point surprises such as 0.1 + 0.2 = 0.30000000000000004.
 */
@Service
public class CalculatorService {

    // 16 significant digits, same precision as a double, so 1/3 terminates
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    public BigDecimal add(BigDecimal a, BigDecimal b) {
        return normalize(a.add(b));
    }

    public BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return normalize(a.subtract(b));
    }

    public BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return normalize(a.multiply(b));
    }

    public BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (b.signum() == 0) {
            throw new ArithmeticException("Division by zero is not allowed");
        }
        return normalize(a.divide(b, PRECISION));
    }

    // 6.00 -> 6, 2.50 -> 2.5
    private static BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
