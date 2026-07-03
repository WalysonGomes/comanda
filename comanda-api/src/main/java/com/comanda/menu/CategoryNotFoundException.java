package com.comanda.menu;

/** Category missing or belonging to another tenant — surfaced as 404, never leaking existence. */
public class CategoryNotFoundException extends RuntimeException {
}
