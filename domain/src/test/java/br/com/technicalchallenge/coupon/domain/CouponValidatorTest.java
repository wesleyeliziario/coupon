package br.com.technicalchallenge.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CouponValidatorTest {

	private static final Instant FIXED_INSTANT = Instant.parse("2026-09-13T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
	private static final LocalDate TODAY = LocalDate.of(2026, 9, 13);

	private CouponValidator validator;

	@BeforeEach
	void setUp() {
		validator = new CouponValidator(CLOCK);
	}

	@Test
	void normalizedCodeAcceptedTest() {
		assertThat(validator.validate(baseInput().code("ABC123").build())).isEqualTo("ABC123");
	}

	@Test
	void lowercaseNormalizedToUppercaseTest() {
		assertThat(validator.validate(baseInput().code("abc123").build())).isEqualTo("ABC123");
	}

	@Test
	void specialCharactersRemovedTest() {
		assertThat(validator.validate(baseInput().code("a@b#c1d2").build())).isEqualTo("ABC1D2");
	}

	@Test
	void codeTooShortAfterSanitizeRejectedTest() {
		assertValidationError(baseInput().code("AB-12").build());
	}

	@Test
	void codeTooLongRejectedTest() {
		assertValidationError(baseInput().code("ABC1234").build());
	}

	@Test
	void codeTooLongAfterSanitizeRejectedTest() {
		assertValidationError(baseInput().code("A-B-C-1-2-3-4").build());
	}

	@Test
	void emptyAfterSanitizeRejectedTest() {
		assertValidationError(baseInput().code("######").build());
	}

	@Test
	void accentsRemovedLeavingShortCodeTest() {
		assertValidationError(baseInput().code("ÁBÇ123").build());
	}

	@Test
	void underscoreRemovedLeavingShortCodeTest() {
		assertValidationError(baseInput().code("AB_123").build());
	}

	@Test
	void unicodeDigitsRemovedLeavingShortCodeTest() {
		assertValidationError(baseInput().code("１２３ABC").build());
	}

	@Test
	void nullOrBlankCodeRejectedTest() {
		assertValidationError(baseInput().code(null).build());
		assertValidationError(baseInput().code("").build());
		assertValidationError(baseInput().code("   ").build());
	}

	@Test
	void discountMinInclusiveAcceptedTest() {
		assertThat(validator.validate(baseInput().discountValue(new BigDecimal("0.5")).build()))
				.isEqualTo("ABC123");
	}

	@Test
	void discountBelowMinRejectedTest() {
		assertValidationError(baseInput().discountValue(new BigDecimal("0.49")).build());
	}

	@Test
	void discountZeroRejectedTest() {
		assertValidationError(baseInput().discountValue(BigDecimal.ZERO).build());
	}

	@Test
	void discountNegativeRejectedTest() {
		assertValidationError(baseInput().discountValue(new BigDecimal("-10")).build());
	}

	@Test
	void largeDiscountAcceptedTest() {
		assertThat(validator.validate(baseInput().discountValue(new BigDecimal("999999999.99")).build()))
				.isEqualTo("ABC123");
	}

	@Test
	void discountScaleRejectedTest() {
		assertValidationError(baseInput().discountValue(new BigDecimal("0.499")).build());
	}

	@Test
	void nullDiscountRejectedTest() {
		assertValidationError(baseInput().discountValue(null).build());
	}

	@Test
	void expirationYesterdayRejectedTest() {
		assertValidationError(baseInput().expirationDate(TODAY.minusDays(1)).build());
	}

	@Test
	void expirationTodayAcceptedTest() {
		assertThat(validator.validate(baseInput().expirationDate(TODAY).build())).isEqualTo("ABC123");
	}

	@Test
	void expirationTomorrowAcceptedTest() {
		assertThat(validator.validate(baseInput().expirationDate(TODAY.plusDays(1)).build()))
				.isEqualTo("ABC123");
	}

	@Test
	void nullExpirationRejectedTest() {
		assertValidationError(baseInput().expirationDate(null).build());
	}

	@Test
	void clockInjectedBoundaryAtMidnightUtcTest() {
		Clock midnight = Clock.fixed(Instant.parse("2026-09-13T00:00:00Z"), ZoneOffset.UTC);
		CouponValidator midnightValidator = new CouponValidator(midnight);

		assertThat(midnightValidator.validate(
						baseInput().expirationDate(LocalDate.of(2026, 9, 13)).build()))
				.isEqualTo("ABC123");

		assertValidationError(
				midnightValidator,
				baseInput().code("DEF456").expirationDate(LocalDate.of(2026, 9, 12)).build());
	}

	@Test
	void nullDescriptionRejectedTest() {
		assertValidationError(baseInput().description(null).build());
	}

	@Test
	void emptyDescriptionRejectedTest() {
		assertValidationError(baseInput().description("").build());
	}

	@Test
	void blankDescriptionRejectedTest() {
		assertValidationError(baseInput().description("   ").build());
	}

	@Test
	void descriptionTooLongRejectedTest() {
		assertValidationError(baseInput().description("x".repeat(256)).build());
	}

	@Test
	void descriptionMaxLengthAcceptedTest() {
		assertThat(validator.validate(baseInput().description("x".repeat(255)).build()))
				.isEqualTo("ABC123");
	}

	private void assertValidationError(CouponInput input) {
		assertValidationError(validator, input);
	}

	private void assertValidationError(CouponValidator target, CouponInput input) {
		assertThatThrownBy(() -> target.validate(input))
				.isInstanceOf(CouponException.class)
				.satisfies(ex -> {
					CouponException ce = (CouponException) ex;
					assertThat(ce.getCode()).isEqualTo("VALIDATION_ERROR");
					assertThat(ce.getMessage()).isEqualTo(CouponError.VALIDATION_ERROR.getMessage());
					assertThat(ce.getStatus()).isEqualTo(400);
				});
	}

	private CouponInput.CouponInputBuilder baseInput() {
		return CouponInput.builder()
				.code("ABC123")
				.description("Test coupon")
				.discountValue(new BigDecimal("1.00"))
				.expirationDate(TODAY.plusDays(10))
				.published(false);
	}
}
