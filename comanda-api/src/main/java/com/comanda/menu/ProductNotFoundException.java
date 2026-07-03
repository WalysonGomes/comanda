package com.comanda.menu;

/** Product missing or belonging to another tenant — surfaced as 404, never leaking existence. */
public class ProductNotFoundException extends RuntimeException {
}
