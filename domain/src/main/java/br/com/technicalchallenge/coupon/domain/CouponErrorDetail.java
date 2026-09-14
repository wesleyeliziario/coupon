package br.com.technicalchallenge.coupon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponErrorDetail {

	private String code;
	private String message;
}
