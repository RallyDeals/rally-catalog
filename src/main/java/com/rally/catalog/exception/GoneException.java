package com.rally.catalog.exception;

import com.rally.common.exceptions.base.BaseException;
import org.springframework.http.HttpStatus;

public class GoneException extends BaseException {
    public GoneException(String message) {
        super(HttpStatus.GONE, "Gone", message);
    }
}
