package br.com.technicalchallenge.coupon.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CouponInput {

	String code;
	String description;
	BigDecimal discountValue;
	LocalDate expirationDate;
	Boolean published;
}
