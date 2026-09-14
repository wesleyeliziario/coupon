package br.com.technicalchallenge.coupon.api;

/** Exemplos JSON usados na documentação OpenAPI/Swagger (um por status/código). */
final class ErrorExamples {

	static final String VALIDATION_ERROR = """
			{
			  "code": "VALIDATION_ERROR",
			  "message": "Erro de validação dos dados do cupom.",
			  "errors": [
			    {
			      "code": "CODE_LENGTH",
			      "message": "O código deve conter 6 caracteres alfanuméricos após a remoção de caracteres especiais."
			    },
			    {
			      "code": "DESCRIPTION_REQUIRED",
			      "message": "A descrição é obrigatória."
			    },
			    {
			      "code": "DISCOUNT_BELOW_MIN",
			      "message": "O valor de desconto deve ser maior ou igual a 0,5."
			    },
			    {
			      "code": "EXPIRATION_IN_PAST",
			      "message": "A data de expiração não pode estar no passado."
			    }
			  ]
			}
			""";

	static final String CODE_DUPLICATED = """
			{
			  "code": "CODE_DUPLICATED",
			  "message": "Já existe um cupom ativo com este código."
			}
			""";

	static final String INVALID_IDENTIFIER = """
			{
			  "code": "INVALID_IDENTIFIER",
			  "message": "Identificador inválido."
			}
			""";

	static final String COUPON_NOT_FOUND = """
			{
			  "code": "COUPON_NOT_FOUND",
			  "message": "Cupom não encontrado."
			}
			""";

	static final String COUPON_ALREADY_DELETED = """
			{
			  "code": "COUPON_ALREADY_DELETED",
			  "message": "Este cupom já foi deletado."
			}
			""";

	static final String INTERNAL_ERROR = """
			{
			  "code": "INTERNAL_ERROR",
			  "message": "Ocorreu um erro inesperado. Tente novamente mais tarde."
			}
			""";

	private ErrorExamples() {}
}
