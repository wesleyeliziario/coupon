package br.com.technicalchallenge.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "coupon")
public class CouponEntity {

	@Id
	private UUID id;

	@Column(name = "raw_code", nullable = false)
	private String rawCode;

	@Column(name = "code", nullable = false, length = 6)
	private String code;

	@Column(name = "description", nullable = false, length = 255)
	private String description;

	@Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
	private BigDecimal discountValue;

	@Column(name = "expiration_date", nullable = false)
	private LocalDate expirationDate;

	@Column(name = "published", nullable = false)
	private boolean published;

	@Column(name = "deleted", nullable = false)
	private boolean deleted;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public boolean isExpired(java.time.Clock clock) {
		return LocalDate.now(clock).isAfter(expirationDate);
	}
}
