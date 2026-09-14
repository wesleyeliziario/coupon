package br.com.technicalchallenge.coupon.domain;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catálogo estático de todos os erros da API (código + mensagem + status HTTP).
 * Fonte única para runtime e documentação Swagger/OpenAPI.
 */
@Getter
@RequiredArgsConstructor
public enum CouponError {

	VALIDATION_ERROR(
			"VALIDATION_ERROR",
			"Erro de validação dos dados do cupom.",
			400),
	CODE_REQUIRED("CODE_REQUIRED", "O código do cupom é obrigatório.", 400),
	CODE_LENGTH(
			"CODE_LENGTH",
			"O código deve conter 6 caracteres alfanuméricos após a remoção de caracteres especiais.",
			400),
	CODE_EMPTY_AFTER_SANITIZE(
			"CODE_EMPTY_AFTER_SANITIZE",
			"O código não contém caracteres alfanuméricos.",
			400),
	CODE_DUPLICATED("CODE_DUPLICATED", "Já existe um cupom ativo com este código.", 409),
	DESCRIPTION_REQUIRED("DESCRIPTION_REQUIRED", "A descrição é obrigatória.", 400),
	DESCRIPTION_TOO_LONG(
			"DESCRIPTION_TOO_LONG",
			"A descrição deve ter no máximo 255 caracteres.",
			400),
	DISCOUNT_REQUIRED("DISCOUNT_REQUIRED", "O valor de desconto é obrigatório.", 400),
	DISCOUNT_BELOW_MIN(
			"DISCOUNT_BELOW_MIN",
			"O valor de desconto deve ser maior ou igual a 0,5.",
			400),
	DISCOUNT_SCALE(
			"DISCOUNT_SCALE",
			"O valor de desconto deve ter no máximo 2 casas decimais.",
			400),
	EXPIRATION_REQUIRED("EXPIRATION_REQUIRED", "A data de expiração é obrigatória.", 400),
	EXPIRATION_IN_PAST(
			"EXPIRATION_IN_PAST",
			"A data de expiração não pode estar no passado.",
			400),
	INVALID_IDENTIFIER("INVALID_IDENTIFIER", "Identificador inválido.", 400),
	COUPON_NOT_FOUND("COUPON_NOT_FOUND", "Cupom não encontrado.", 404),
	COUPON_ALREADY_DELETED("COUPON_ALREADY_DELETED", "Este cupom já foi deletado.", 409),
	INTERNAL_ERROR(
			"INTERNAL_ERROR",
			"Ocorreu um erro inesperado. Tente novamente mais tarde.",
			500);

	private final String code;
	private final String message;
	private final int httpStatus;

	public CouponErrorDetail toDetail() {
		return new CouponErrorDetail(code, message);
	}

	public CouponException toException() {
		return new CouponException(code, message, httpStatus);
	}

	public CouponException toException(List<CouponErrorDetail> details) {
		return new CouponException(code, message, httpStatus, details);
	}

	public static List<CouponError> all() {
		return List.of(values());
	}

	/** Texto para descrição OpenAPI/Swagger listando todos os erros. */
	public static String openApiDocumentation() {
		return Arrays.stream(values())
				.map(e -> "- **" + e.code + "** (HTTP " + e.httpStatus + "): " + e.message)
				.collect(Collectors.joining(
						"\n",
						"### Catálogo de erros\n\n",
						"\n\nEm validações do cupom, a resposta usa `VALIDATION_ERROR` no topo "
								+ "e detalha cada violação em `errors[]`."));
	}
}
