package com.comanda.orders;

/** Cancellation reason missing or under 10 characters (PRD 3.2 / spec `order-operation`). */
public class InvalidCancellationReasonException extends RuntimeException {
}
