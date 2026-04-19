package com.bigproject.core.exception;

public class BadRequestException extends RuntimeException {

	private final String errorCode;

	public BadRequestException(String errorCode) {
		super(errorCode);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}

}
