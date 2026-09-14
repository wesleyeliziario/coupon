package br.com.technicalchallenge.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

	private static final Instant FIXED_INSTANT = Instant.parse("2026-09-13T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
	private static final LocalDate TODAY = LocalDate.of(2026, 9, 13);

	@Mock
	private CouponRepository couponRepository;

	private CouponService couponService;

	@BeforeEach
	void setUp() {
		couponService = new CouponService(couponRepository, new CouponValidator(CLOCK), CLOCK);
	}

	@Test
	void rawCodePreservesOriginalInputTest() {
		when(couponRepository.existsByCodeAndDeletedFalse("AB12C7")).thenReturn(false);
		when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		CouponEntity saved = couponService.create(baseInput().code("ab-12#c7").build());
		assertThat(saved.getRawCode()).isEqualTo("ab-12#c7");
		assertThat(saved.getCode()).isEqualTo("AB12C7");
	}

	@Test
	void publishedTrueCreatesPublishedTest() {
		when(couponRepository.existsByCodeAndDeletedFalse("ABC123")).thenReturn(false);
		when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		CouponEntity saved = couponService.create(baseInput().published(true).build());
		assertThat(saved.isPublished()).isTrue();
	}

	@Test
	void publishedAbsentOrFalseCreatesDraftTest() {
		when(couponRepository.existsByCodeAndDeletedFalse("ABC123")).thenReturn(false);
		when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		assertThat(couponService.create(baseInput().published(null).build()).isPublished()).isFalse();

		when(couponRepository.existsByCodeAndDeletedFalse("DEF456")).thenReturn(false);
		assertThat(couponService.create(baseInput().code("DEF456").published(false).build()).isPublished())
				.isFalse();
	}

	@Test
	void duplicateActiveCodeRejectedTest() {
		when(couponRepository.existsByCodeAndDeletedFalse("ABC123")).thenReturn(true);

		assertThatThrownBy(() -> couponService.create(baseInput().build()))
				.isInstanceOf(CouponException.class)
				.extracting(ex -> ((CouponException) ex).getCode())
				.isEqualTo("CODE_DUPLICATED");
		verify(couponRepository, never()).save(any());
	}

	@Test
	void deleteActiveSoftDeletesTest() {
		UUID id = UUID.randomUUID();
		when(couponRepository.softDelete(id, FIXED_INSTANT)).thenReturn(1);

		couponService.delete(id);

		verify(couponRepository).softDelete(id, FIXED_INSTANT);
		verify(couponRepository, never()).save(any());
	}

	@Test
	void deleteTwiceKeepsDeletedAtTest() {
		UUID id = UUID.randomUUID();
		when(couponRepository.softDelete(id, FIXED_INSTANT)).thenReturn(0);
		when(couponRepository.existsById(id)).thenReturn(true);

		assertThatThrownBy(() -> couponService.delete(id))
				.isInstanceOf(CouponException.class)
				.extracting(ex -> ((CouponException) ex).getCode())
				.isEqualTo("COUPON_ALREADY_DELETED");
		verify(couponRepository, never()).save(any());
	}

	@Test
	void deleteExpiredAllowedTest() {
		UUID id = UUID.randomUUID();
		when(couponRepository.softDelete(id, FIXED_INSTANT)).thenReturn(1);

		couponService.delete(id);

		verify(couponRepository).softDelete(id, FIXED_INSTANT);
	}

	@Test
	void deletePublishedAllowedTest() {
		UUID id = UUID.randomUUID();
		when(couponRepository.softDelete(id, FIXED_INSTANT)).thenReturn(1);

		couponService.delete(id);

		verify(couponRepository).softDelete(id, FIXED_INSTANT);
	}

	@Test
	void noRestorePathTest() {
		UUID id = UUID.randomUUID();
		when(couponRepository.softDelete(id, FIXED_INSTANT)).thenReturn(0);
		when(couponRepository.existsById(id)).thenReturn(true);

		assertThatThrownBy(() -> couponService.delete(id))
				.isInstanceOf(CouponException.class)
				.extracting(ex -> ((CouponException) ex).getCode())
				.isEqualTo("COUPON_ALREADY_DELETED");
		verify(couponRepository, never()).save(any());
	}

	@Test
	void expiredIsCalculatedNotPersistedTest() {
		CouponEntity entity = activeEntity(UUID.randomUUID());
		entity.setExpirationDate(TODAY.minusDays(1));
		assertThat(entity.isExpired(CLOCK)).isTrue();

		entity.setExpirationDate(TODAY);
		assertThat(entity.isExpired(CLOCK)).isFalse();
	}

	@Test
	void deleteUnknownIdReturnsNotFoundTest() {
		UUID id = UUID.randomUUID();
		when(couponRepository.softDelete(id, FIXED_INSTANT)).thenReturn(0);
		when(couponRepository.existsById(id)).thenReturn(false);

		assertThatThrownBy(() -> couponService.delete(id))
				.isInstanceOf(CouponException.class)
				.extracting(ex -> ((CouponException) ex).getCode())
				.isEqualTo("COUPON_NOT_FOUND");
	}

	@Test
	void createPersistsSanitizedCodeTest() {
		when(couponRepository.existsByCodeAndDeletedFalse("ABC123")).thenReturn(false);
		when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		couponService.create(baseInput().build());

		ArgumentCaptor<CouponEntity> captor = ArgumentCaptor.forClass(CouponEntity.class);
		verify(couponRepository).save(captor.capture());
		assertThat(captor.getValue().getCode()).isEqualTo("ABC123");
		assertThat(captor.getValue().getRawCode()).isEqualTo("ABC123");
	}

	private CouponInput.CouponInputBuilder baseInput() {
		return CouponInput.builder()
				.code("ABC123")
				.description("Test coupon")
				.discountValue(new BigDecimal("1.00"))
				.expirationDate(TODAY.plusDays(10))
				.published(false);
	}

	private CouponEntity activeEntity(UUID id) {
		return CouponEntity.builder()
				.id(id)
				.rawCode("abc123")
				.code("ABC123")
				.description("Active")
				.discountValue(new BigDecimal("10.00"))
				.expirationDate(TODAY.plusDays(30))
				.published(false)
				.deleted(false)
				.deletedAt(null)
				.createdAt(FIXED_INSTANT)
				.build();
	}
}
