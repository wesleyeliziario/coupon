package br.com.technicalchallenge.coupon.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class CouponWebTest {

	@Autowired
	private MockMvc mockMvc;

	private final JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

	@Test
	void multipleViolationsReturnFixedTitleAndErrorDetailsTest() throws Exception {
		String body = """
				{
				  "code": "AB",
				  "description": "   ",
				  "discountValue": 0.49,
				  "expirationDate": "%s"
				}
				""".formatted(LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1));

		mockMvc.perform(post("/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.message").value("Erro de validação dos dados do cupom."))
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
				.andExpect(jsonPath("$.errors[?(@.code=='CODE_LENGTH')]").isNotEmpty())
				.andExpect(jsonPath("$.errors[?(@.code=='DESCRIPTION_REQUIRED')]").isNotEmpty());
	}

	@Test
	void unsupportedContentTypeReturns415Test() throws Exception {
		mockMvc.perform(post("/coupons")
						.contentType(MediaType.TEXT_PLAIN)
						.content("hello"))
				.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void invalidUuidReturns400Test() throws Exception {
		mockMvc.perform(delete("/coupons/not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_IDENTIFIER"))
				.andExpect(jsonPath("$.message").value("Identificador inválido."));
	}

	@Test
	void createResponseHasSanitizedCodeAndNoRawCodeTest() throws Exception {
		String body = """
				{
				  "code": "cd-34#e5",
				  "description": "Resposta",
				  "discountValue": 10.50,
				  "expirationDate": "%s",
				  "published": true
				}
				""".formatted(LocalDate.now(java.time.ZoneOffset.UTC).plusDays(10));

		MvcResult result = mockMvc.perform(post("/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("CD34E5"))
				.andReturn();

		String json = result.getResponse().getContentAsString();
		assertThat(json).doesNotContain("rawCode");
		assertThat(objectMapper.readTree(json).has("rawCode")).isFalse();
	}

	@Test
	void createReturnsLocationHeaderTest() throws Exception {
		String body = """
				{
				  "code": "%s",
				  "description": "Location",
				  "discountValue": 1.00,
				  "expirationDate": "%s"
				}
				""".formatted(uniqueCode(), LocalDate.now(java.time.ZoneOffset.UTC).plusDays(10));

		MvcResult result = mockMvc.perform(post("/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/coupon/coupons/.+")))
				.andReturn();

		String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
		assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/coupon/coupons/" + id);
	}

	@Test
	void openApiDocumentsExactlyTwoEndpointsTest() throws Exception {
		MvcResult result = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn();

		String docs = result.getResponse().getContentAsString();
		JsonNode root = objectMapper.readTree(docs);
		JsonNode paths = root.get("paths");
		assertThat(paths.propertyNames()).containsExactlyInAnyOrder("/coupons", "/coupons/{id}");

		Set<String> operations = new HashSet<>();
		paths.properties().forEach(entry -> entry.getValue().propertyNames()
				.forEach(op -> operations.add(entry.getKey() + " " + op)));
		assertThat(operations).containsExactlyInAnyOrder("/coupons post", "/coupons/{id} delete");

		assertThat(docs).contains("Catálogo de erros");
		assertThat(docs).contains("VALIDATION_ERROR");
		assertThat(docs).contains("CODE_DUPLICATED");
		assertThat(docs).contains("INTERNAL_ERROR");
		assertThat(docs).doesNotContain("\"415\"");
		assertThat(root.get("components").get("schemas").has("CouponErrorCatalog")).isTrue();

		JsonNode postResponses = paths.get("/coupons").get("post").get("responses");
		assertThat(postResponses.propertyNames()).containsExactlyInAnyOrder("201", "400", "409", "500");
		assertThat(postResponses.get("400").get("content").get("application/json").get("examples")
				.propertyNames()).contains("VALIDATION_ERROR");
		assertThat(postResponses.get("409").get("content").get("application/json").get("examples")
				.propertyNames()).contains("CODE_DUPLICATED");
		assertThat(postResponses.get("500").get("content").get("application/json").get("examples")
				.propertyNames()).contains("INTERNAL_ERROR");

		JsonNode deleteResponses = paths.get("/coupons/{id}").get("delete").get("responses");
		assertThat(deleteResponses.propertyNames()).containsExactlyInAnyOrder("204", "400", "404", "409", "500");
		assertThat(deleteResponses.get("400").get("content").get("application/json").get("examples")
				.propertyNames()).contains("INVALID_IDENTIFIER");
		assertThat(deleteResponses.get("404").get("content").get("application/json").get("examples")
				.propertyNames()).contains("COUPON_NOT_FOUND");
		assertThat(deleteResponses.get("409").get("content").get("application/json").get("examples")
				.propertyNames()).contains("COUPON_ALREADY_DELETED");
	}

	private static String uniqueCode() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
	}
}
