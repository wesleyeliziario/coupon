package br.com.technicalchallenge.coupon.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponValidator {

	private static final BigDecimal MIN_DISCOUNT = new BigDecimal("0.5");
	private static final int CODE_LENGTH = 6;
	private static final int DESCRIPTION_MAX = 255;
	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]");

	private final Clock clock;

	public String validate(CouponInput input) {
		List<CouponError> violations = new ArrayList<>();
		String sanitized = validateCode(input.getCode(), violations);
		validateDescription(input.getDescription(), violations);
		validateDiscount(input.getDiscountValue(), violations);
		validateExpiration(input.getExpirationDate(), violations);

		if (!violations.isEmpty()) {
			throw CouponError.VALIDATION_ERROR.toException(
					violations.stream().map(CouponError::toDetail).toList());
		}
		return sanitized;
	}

	static String sanitize(String code) {
		return NON_ALPHANUMERIC.matcher(code).replaceAll("").toUpperCase(Locale.ROOT);
	}

	private String validateCode(String code, List<CouponError> violations) {
		if (code == null || code.isBlank()) {
			violations.add(CouponError.CODE_REQUIRED);
			return null;
		}

		String sanitized = sanitize(code);
		if (sanitized.isEmpty()) {
			violations.add(CouponError.CODE_EMPTY_AFTER_SANITIZE);
			return sanitized;
		}
		if (sanitized.length() != CODE_LENGTH) {
			violations.add(CouponError.CODE_LENGTH);
		}
		return sanitized;
	}

	private void validateDescription(String description, List<CouponError> violations) {
		if (description == null || description.isBlank()) {
			violations.add(CouponError.DESCRIPTION_REQUIRED);
			return;
		}
		if (description.length() > DESCRIPTION_MAX) {
			violations.add(CouponError.DESCRIPTION_TOO_LONG);
		}
	}

	private void validateDiscount(BigDecimal discountValue, List<CouponError> violations) {
		if (discountValue == null) {
			violations.add(CouponError.DISCOUNT_REQUIRED);
			return;
		}
		if (discountValue.scale() > 2) {
			violations.add(CouponError.DISCOUNT_SCALE);
		}
		if (discountValue.compareTo(MIN_DISCOUNT) < 0) {
			violations.add(CouponError.DISCOUNT_BELOW_MIN);
		}
	}

	private void validateExpiration(LocalDate expirationDate, List<CouponError> violations) {
		if (expirationDate == null) {
			violations.add(CouponError.EXPIRATION_REQUIRED);
			return;
		}
		LocalDate todayUtc = LocalDate.now(clock);
		if (expirationDate.isBefore(todayUtc)) {
			violations.add(CouponError.EXPIRATION_IN_PAST);
		}
	}
}
