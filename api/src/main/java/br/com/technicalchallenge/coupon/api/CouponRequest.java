package br.com.technicalchallenge.coupon.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {

	private String code;
	private String description;
	private BigDecimal discountValue;
	private LocalDate expirationDate;
	private Boolean published;
}
