package br.com.technicalchallenge.coupon.domain;

import java.util.List;
import lombok.Getter;

@Getter
public class CouponException extends RuntimeException {

	private final String code;
	private final String message;
	private final int status;
	private final List<CouponErrorDetail> errors;

	public CouponException(String code, String message, int status) {
		this(code, message, status, List.of());
	}

	public CouponException(String code, String message, int status, List<CouponErrorDetail> errors) {
		super(message);
		this.code = code;
		this.message = message;
		this.status = status;
		this.errors = errors == null || errors.isEmpty() ? List.of() : List.copyOf(errors);
	}
}
