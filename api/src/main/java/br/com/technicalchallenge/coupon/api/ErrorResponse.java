package br.com.technicalchallenge.coupon.api;

import br.com.technicalchallenge.coupon.domain.CouponError;
import br.com.technicalchallenge.coupon.domain.CouponErrorDetail;
import br.com.technicalchallenge.coupon.domain.CouponException;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
		name = "ErrorResponse",
		description = "Resposta de erro da API. Em VALIDATION_ERROR, os detalhes ficam em errors[].")
public class ErrorResponse {

	@Schema(description = "Código do erro de topo", example = "VALIDATION_ERROR")
	private String code;

	@Schema(description = "Mensagem (pt-BR)", example = "Erro de validação dos dados do cupom.")
	private String message;

	@ArraySchema(
			arraySchema = @Schema(
					description = "Presente apenas em VALIDATION_ERROR: uma entrada por violação"),
			schema = @Schema(implementation = CouponErrorDetail.class))
	private List<CouponErrorDetail> errors;

	public ErrorResponse(String code, String message) {
		this(code, message, null);
	}

	public static ErrorResponse from(CouponException ex) {
		List<CouponErrorDetail> details = ex.getErrors();
		return new ErrorResponse(
				ex.getCode(),
				ex.getMessage(),
				details.isEmpty() ? null : details);
	}

	public static ErrorResponse of(CouponError error) {
		return new ErrorResponse(error.getCode(), error.getMessage(), null);
	}
}
