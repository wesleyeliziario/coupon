package br.com.technicalchallenge.coupon.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

	private final CouponRepository couponRepository;
	private final CouponValidator couponValidator;
	private final Clock clock;

	@Transactional
	public CouponEntity create(CouponInput input) {
		String sanitized = couponValidator.validate(input);

		if (couponRepository.existsByCodeAndDeletedFalse(sanitized)) {
			throw CouponError.CODE_DUPLICATED.toException();
		}

		boolean published = input.getPublished() != null && input.getPublished();
		Instant now = Instant.now(clock);

		CouponEntity entity = CouponEntity.builder()
				.id(UUID.randomUUID())
				.rawCode(input.getCode())
				.code(sanitized)
				.description(input.getDescription())
				.discountValue(input.getDiscountValue())
				.expirationDate(input.getExpirationDate())
				.published(published)
				.deleted(false)
				.deletedAt(null)
				.createdAt(now)
				.build();

		return couponRepository.save(entity);
	}

	@Transactional
	public void delete(UUID id) {
		Instant now = Instant.now(clock);
		int updated = couponRepository.softDelete(id, now);
		if (updated == 1) {
			return;
		}
		if (!couponRepository.existsById(id)) {
			throw CouponError.COUPON_NOT_FOUND.toException();
		}
		throw CouponError.COUPON_ALREADY_DELETED.toException();
	}
}