package br.com.technicalchallenge.coupon.api;

import br.com.technicalchallenge.coupon.domain.CouponError;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI couponOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Coupon API")
						.version("1.0")
						.description(apiDescription()));
	}

	@Bean
	OpenApiCustomizer couponErrorCatalogCustomizer() {
		return openApi -> {
			if (openApi.getInfo() != null) {
				openApi.getInfo().setDescription(apiDescription());
			}
			if (openApi.getComponents() == null) {
				openApi.setComponents(new io.swagger.v3.oas.models.Components());
			}
			openApi.getComponents().addSchemas("CouponErrorCatalog", errorCatalogSchema());

			// Evita um único example genérico no schema ErrorResponse —
			// cada status code traz o próprio ExampleObject na operação.
			Schema<?> errorResponse = openApi.getComponents().getSchemas().get("ErrorResponse");
			if (errorResponse != null) {
				errorResponse.setExample(null);
				errorResponse.setExamples(null);
			}
		};
	}

	private static String apiDescription() {
		return """
				API de cadastro e remoção lógica de cupons.

				**Base path:** `/api/coupon` · **Recurso:** `/coupons`

				Cada resposta de erro HTTP documenta o código específico na operação
				(POST/DELETE). O catálogo abaixo é a referência completa.

				"""
				+ CouponError.openApiDocumentation();
	}

	private static Schema<?> errorCatalogSchema() {
		ObjectSchema catalog = new ObjectSchema();
		catalog.setDescription(
				"Referência dos códigos de erro (enum CouponError). "
						+ "Não é um payload de resposta — veja os examples por status em cada operação.");

		Map<String, Schema> properties = new LinkedHashMap<>();
		Arrays.stream(CouponError.values()).forEach(error -> {
			ObjectSchema entry = new ObjectSchema();
			entry.addProperty("code", new StringSchema().example(error.getCode()));
			entry.addProperty("message", new StringSchema().example(error.getMessage()));
			entry.addProperty("httpStatus", new IntegerSchema().example(error.getHttpStatus()));
			entry.setDescription(error.getMessage());
			properties.put(error.getCode(), entry);
		});
		catalog.setProperties(properties);
		return catalog;
	}
}
