package org.mylove.tprt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mylove.tprt.ModUsed.ModMaths.OiMaths;

import static org.junit.jupiter.api.Assertions.*;

public class OiMathsTest {

    @Test
    @DisplayName("Verify linear sieve prime detection and boundary handling")
    void testPrimeSieve() {
        assertFalse(OiMaths.PRIME.isPrime(0), "0 is not prime");
        assertFalse(OiMaths.PRIME.isPrime(1), "1 is not prime");
        assertFalse(OiMaths.PRIME.isPrime(-5), "Negative numbers are not prime");
        assertFalse(OiMaths.PRIME.isPrime(OiMaths.PRIME.MAXR + 10), "Numbers beyond MAXR return false");

        assertTrue(OiMaths.PRIME.isPrime(2), "2 is prime");
        assertTrue(OiMaths.PRIME.isPrime(3), "3 is prime");
        assertTrue(OiMaths.PRIME.isPrime(5), "5 is prime");
        assertTrue(OiMaths.PRIME.isPrime(7), "7 is prime");
        assertTrue(OiMaths.PRIME.isPrime(13), "13 is prime");
        assertTrue(OiMaths.PRIME.isPrime(97), "97 is prime");

        assertFalse(OiMaths.PRIME.isPrime(4), "4 is composite");
        assertFalse(OiMaths.PRIME.isPrime(9), "9 is composite");
        assertFalse(OiMaths.PRIME.isPrime(100), "100 is composite");

        assertEquals(2, OiMaths.PRIME.getPrime(1), "1st prime is 2");
        assertEquals(3, OiMaths.PRIME.getPrime(2), "2nd prime is 3");
        assertEquals(5, OiMaths.PRIME.getPrime(3), "3rd prime is 5");
        assertEquals(-1, OiMaths.PRIME.getPrime(0), "0th prime is invalid");
        assertEquals(-1, OiMaths.PRIME.getPrime(-1), "Negative index is invalid");

        assertEquals(2, OiMaths.PRIME.biggerPrime(0));
        assertEquals(2, OiMaths.PRIME.biggerPrime(1));
        assertEquals(3, OiMaths.PRIME.biggerPrime(2));
        assertEquals(5, OiMaths.PRIME.biggerPrime(3));

        assertEquals(-1, OiMaths.PRIME.smallerPrime(2));
        assertEquals(2, OiMaths.PRIME.smallerPrime(3));
        assertEquals(3, OiMaths.PRIME.smallerPrime(4));
    }

    @Test
    @DisplayName("Verify math optimization algorithms: QuickPow, GCD, Euler phi")
    void testMathOptimizations() {
        assertEquals(1024, OiMaths.OPTIMIZE.QuickPow(2, 10, 1000000007L));
        assertEquals(1, OiMaths.OPTIMIZE.QuickPow(5, 0, 1000000007L));
        assertEquals(0, OiMaths.OPTIMIZE.QuickPow(2, 5, 2));

        assertEquals(6, OiMaths.OPTIMIZE.__GCD(54, 24));
        assertEquals(1, OiMaths.OPTIMIZE.__GCD(17, 31));
        assertEquals(12, OiMaths.OPTIMIZE.__GCD(0, 12));

        assertEquals(1, OiMaths.OPTIMIZE.euler_phi(1));
        assertEquals(1, OiMaths.OPTIMIZE.euler_phi(2));
        assertEquals(4, OiMaths.OPTIMIZE.euler_phi(5));
        assertEquals(4, OiMaths.OPTIMIZE.euler_phi(10));
        assertEquals(12, OiMaths.OPTIMIZE.euler_phi(13));
    }

    @Test
    @DisplayName("Verify Fibonacci calculation via 2x2 matrix exponentiation")
    void testFibonacciMatrix() {
        assertEquals(0, OiMaths.Mat.Fibonacci(0));
        assertEquals(1, OiMaths.Mat.Fibonacci(1));
        assertEquals(1, OiMaths.Mat.Fibonacci(2));
        assertEquals(2, OiMaths.Mat.Fibonacci(3));
        assertEquals(3, OiMaths.Mat.Fibonacci(4));
        assertEquals(5, OiMaths.Mat.Fibonacci(5));
        assertEquals(8, OiMaths.Mat.Fibonacci(6));
        assertEquals(13, OiMaths.Mat.Fibonacci(7));
        assertEquals(21, OiMaths.Mat.Fibonacci(8));
        assertEquals(55, OiMaths.Mat.Fibonacci(10));
    }
}
