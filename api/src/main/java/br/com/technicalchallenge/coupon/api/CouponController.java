package br.com.technicalchallenge.coupon.api;

import br.com.technicalchallenge.coupon.domain.CouponEntity;
import br.com.technicalchallenge.coupon.domain.CouponError;
import br.com.technicalchallenge.coupon.domain.CouponException;
import br.com.technicalchallenge.coupon.domain.CouponInput;
import br.com.technicalchallenge.coupon.domain.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupon", description = "Cadastro e remoção lógica de cupons")
public class CouponController {

	static final String RESOURCE_BASE = "/api/coupon/coupons";

	private final CouponService couponService;

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "Criar cupom",
			description = "Cria um cupom. Falhas de validação retornam HTTP 400 com "
					+ "`VALIDATION_ERROR` e detalhes em `errors[]`.")
	@ApiResponse(
			responseCode = "201",
			description = "Cupom criado",
			headers = @Header(name = "Location", description = "URI do cupom criado (`/api/coupon/coupons/{id}`)"),
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = CouponResponse.class)))
	@ApiResponse(
			responseCode = "400",
			description = "Validação de negócio (`VALIDATION_ERROR`). "
					+ "Códigos possíveis em `errors[]`: CODE_REQUIRED, CODE_LENGTH, "
					+ "CODE_EMPTY_AFTER_SANITIZE, DESCRIPTION_REQUIRED, DESCRIPTION_TOO_LONG, "
					+ "DISCOUNT_REQUIRED, DISCOUNT_BELOW_MIN, DISCOUNT_SCALE, "
					+ "EXPIRATION_REQUIRED, EXPIRATION_IN_PAST.",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ErrorResponse.class),
					examples = @ExampleObject(
							name = "VALIDATION_ERROR",
							summary = "VALIDATION_ERROR",
							value = ErrorExamples.VALIDATION_ERROR)))
	@ApiResponse(
			responseCode = "409",
			description = "Já existe cupom ativo com o mesmo código (`CODE_DUPLICATED`)",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ErrorResponse.class),
					examples = @ExampleObject(
							name = "CODE_DUPLICATED",
							summary = "CODE_DUPLICATED",
							value = ErrorExamples.CODE_DUPLICATED)))
	@ApiResponse(
			responseCode = "500",
			description = "Falha inesperada no processamento (`INTERNAL_ERROR`)",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ErrorResponse.class),
					examples = @ExampleObject(
							name = "INTERNAL_ERROR",
							summary = "INTERNAL_ERROR",
							value = ErrorExamples.INTERNAL_ERROR)))
	public ResponseEntity<?> create(@RequestBody CouponRequest request) {
		try {
			CouponInput input = CouponInput.builder()
					.code(request.getCode())
					.description(request.getDescription())
					.discountValue(request.getDiscountValue())
					.expirationDate(request.getExpirationDate())
					.published(request.getPublished())
					.build();
			CouponEntity created = couponService.create(input);
			CouponResponse body = CouponResponse.from(created);
			return ResponseEntity.created(URI.create(RESOURCE_BASE + "/" + created.getId())).body(body);
		} catch (CouponException ex) {
			return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.from(ex));
		} catch (Exception ex) {
			log.error("Unexpected error creating coupon", ex);
			return ResponseEntity.internalServerError()
					.body(ErrorResponse.of(CouponError.INTERNAL_ERROR));
		}
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Deletar cupom (soft delete)")
	@ApiResponse(responseCode = "204", description = "Cupom deletado")
	@ApiResponse(
			responseCode = "400",
			description = "Identificador inválido (`INVALID_IDENTIFIER`)",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ErrorResponse.class),
					examples = @ExampleObject(
							name = "INVALID_IDENTIFIER",
							summary = "INVALID_IDENTIFIER",
							value = ErrorExamples.INVALID_IDENTIFIER)))
	@ApiResponse(
			responseCode = "404",
			description = "Cupom não encontrado (`COUPON_NOT_FOUND`)",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ErrorResponse.class),
					examples = @ExampleObject(
							name = "COUPON_NOT_FOUND",
							summary = "COUPON_NOT_FOUND",
							value = ErrorExamples.COUPON_NOT_FOUND)))
	@ApiResponse(
			responseCode = "409",
			description = "Cupom já deletado (`COUPON_ALREADY_DELETED`)",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ErrorResponse.class),
					examples = @ExampleObject(
							name = "COUPON_ALREADY_DELETED",
							summary = "COUPON_ALREADY_DELETED",
							value = ErrorExamples.COUPON_ALREADY_DELETED)))
	@ApiResponse(
			responseCode = "500",
			description = "Falha inesperada no processamento (`INTERNAL_ERROR`)",
			content = @Content(
					mediaType = MediaType.APPLICATION_JSON_VALUE,
					schema = @Schema(implementation = ErrorResponse.class),
					examples = @ExampleObject(
							name = "INTERNAL_ERROR",
							summary = "INTERNAL_ERROR",
							value = ErrorExamples.INTERNAL_ERROR)))
	public ResponseEntity<?> delete(@PathVariable("id") String id) {
		try {
			UUID uuid = UUID.fromString(id);
			couponService.delete(uuid);
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest()
					.body(ErrorResponse.of(CouponError.INVALID_IDENTIFIER));
		} catch (CouponException ex) {
			return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.from(ex));
		} catch (Exception ex) {
			log.error("Unexpected error deleting coupon id={}", id, ex);
			return ResponseEntity.internalServerError()
					.body(ErrorResponse.of(CouponError.INTERNAL_ERROR));
		}
	}
}
