package com.uagrm.erp.backend.exception;

import java.time.LocalDateTime;

public class AccountLockedException extends RuntimeException {

    private final LocalDateTime bloqueadoHasta;

    public AccountLockedException(String message, LocalDateTime bloqueadoHasta) {
        super(message);
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
