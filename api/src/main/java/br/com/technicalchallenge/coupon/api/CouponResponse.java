package br.com.technicalchallenge.coupon.api;

import br.com.technicalchallenge.coupon.domain.CouponEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {

	private UUID id;
	private String code;
	private String description;
	private BigDecimal discountValue;
	private LocalDate expirationDate;
	private boolean published;
	private boolean deleted;
	private Instant createdAt;

	public static CouponResponse from(CouponEntity entity) {
		return CouponResponse.builder()
				.id(entity.getId())
				.code(entity.getCode())
				.description(entity.getDescription())
				.discountValue(entity.getDiscountValue())
				.expirationDate(entity.getExpirationDate())
				.published(entity.isPublished())
				.deleted(entity.isDeleted())
				.createdAt(entity.getCreatedAt())
				.build();
	}
}
