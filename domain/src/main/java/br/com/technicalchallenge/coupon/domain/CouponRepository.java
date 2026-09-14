package br.com.technicalchallenge.coupon.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<CouponEntity, UUID> {

	boolean existsByCodeAndDeletedFalse(String code);

	@Modifying(clearAutomatically = true)
	@Query(
			"""
			UPDATE CouponEntity c
			SET c.deleted = true, c.deletedAt = :deletedAt
			WHERE c.id = :id AND c.deleted = false
			""")
	int softDelete(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);
}
