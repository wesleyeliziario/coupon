package br.com.technicalchallenge.coupon.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class CouponIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

	@Test
	void flywayCreatesSchemaWithDdlAutoNoneTest() {
		Integer count = jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
				WHERE TABLE_NAME = 'COUPON'
				""",
				Integer.class);
		assertThat(count).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM coupon", Integer.class)).isNotNull();
	}

	@Test
	void postPersistsSanitizedCodeAndRawCodeTest() throws Exception {
		String rawCode = "ab-12#c7";
		String body = createBody(rawCode, "Integração sanitize", "10.50", futureDate(), false);

		MvcResult result = mockMvc.perform(post("/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();

		UUID id = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
				.get("id")
				.asText());

		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM coupon WHERE id = ?", id);
		assertThat(row.get("CODE").toString()).isEqualTo("AB12C7");
		assertThat(row.get("RAW_CODE").toString()).isEqualTo(rawCode);
		assertThat(row.get("DELETED")).isEqualTo(false);
	}

	@Test
	void deleteSoftDeletesAndPreservesRowTest() throws Exception {
		UUID id = createCoupon("t41xxx", "Soft delete", "12.34", false);

		mockMvc.perform(delete("/coupons/" + id)).andExpect(status().isNoContent());

		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM coupon WHERE id = ?", id);
		assertThat(row.get("DELETED")).isEqualTo(true);
		assertThat(row.get("DELETED_AT")).isNotNull();
		assertThat(row.get("RAW_CODE").toString()).isEqualTo("t41xxx");
		assertThat(row.get("CODE").toString()).isEqualTo("T41XXX");
		assertThat(row.get("DESCRIPTION").toString()).isEqualTo("Soft delete");
		assertThat(new BigDecimal(row.get("DISCOUNT_VALUE").toString())).isEqualByComparingTo("12.34");
	}

	@Test
	void repeatedDeleteReturns409AndKeepsDeletedAtTest() throws Exception {
		UUID id = createCoupon("t42xxx", "Dup delete", "1.00", false);
		mockMvc.perform(delete("/coupons/" + id)).andExpect(status().isNoContent());

		Object deletedAt = jdbcTemplate.queryForObject(
				"SELECT deleted_at FROM coupon WHERE id = ?", Object.class, id);

		mockMvc.perform(delete("/coupons/" + id))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("COUPON_ALREADY_DELETED"));

		Object deletedAtAfter = jdbcTemplate.queryForObject(
				"SELECT deleted_at FROM coupon WHERE id = ?", Object.class, id);
		assertThat(deletedAtAfter).isEqualTo(deletedAt);
	}

	@Test
	void duplicateActiveCodeReturns409Test() throws Exception {
		String code = "t43" + uniqueSuffix(3);
		createCoupon(code, "First", "1.00", false);
		long before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM coupon", Long.class);

		mockMvc.perform(post("/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody(code, "Second", "2.00", futureDate(), false)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CODE_DUPLICATED"));

		long after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM coupon", Long.class);
		assertThat(after).isEqualTo(before);
	}

	@Test
	void codeFromDeletedCouponCanBeReusedTest() throws Exception {
		String code = "t44" + uniqueSuffix(3);
		UUID firstId = createCoupon(code, "First", "1.00", false);
		mockMvc.perform(delete("/coupons/" + firstId)).andExpect(status().isNoContent());

		mockMvc.perform(post("/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody(code, "Reused", "2.00", futureDate(), false)))
				.andExpect(status().isCreated());

		Integer rows = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM coupon WHERE code = ?", Integer.class, code.toUpperCase());
		assertThat(rows).isEqualTo(2);
	}

	@Test
	void persistenceLayerBlocksActiveDuplicateTest() throws Exception {
		String code = "t45" + uniqueSuffix(3);
		createCoupon(code, "Active", "1.00", false);

		mockMvc.perform(post("/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody(code, "Dup", "1.00", futureDate(), false)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CODE_DUPLICATED"));
	}

	@Test
	void deleteUnknownUuidReturns404Test() throws Exception {
		mockMvc.perform(delete("/coupons/" + UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("COUPON_NOT_FOUND"));
	}

	@Test
	void discountValueRoundTripPreservesScaleTest() throws Exception {
		UUID id = createCoupon("t47xxx", "Precision", "10.50", false);
		BigDecimal value = jdbcTemplate.queryForObject(
				"SELECT discount_value FROM coupon WHERE id = ?", BigDecimal.class, id);
		assertThat(value).isEqualByComparingTo("10.50");
	}

	@Test
	void expirationDatePreservedWithoutTimezoneShiftTest() throws Exception {
		LocalDate date = LocalDate.of(2026, 12, 31);
		UUID id = createCoupon("t48xxx", "Date", "1.00", false, date);
		LocalDate stored = jdbcTemplate.queryForObject(
				"SELECT expiration_date FROM coupon WHERE id = ?", LocalDate.class, id);
		assertThat(stored).isEqualTo(date);
	}

	private UUID createCoupon(String code, String description, String discount, boolean published)
			throws Exception {
		return createCoupon(code, description, discount, published, futureDate());
	}

	private UUID createCoupon(
			String code, String description, String discount, boolean published, LocalDate expiration)
			throws Exception {
		MvcResult result = mockMvc.perform(post("/coupons")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createBody(code, description, discount, expiration, published)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/coupon/coupons/")))
				.andReturn();
		JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
		return UUID.fromString(json.get("id").asText());
	}

	private static String createBody(
			String code, String description, String discount, LocalDate expiration, boolean published) {
		return """
				{
				  "code": "%s",
				  "description": "%s",
				  "discountValue": %s,
				  "expirationDate": "%s",
				  "published": %s
				}
				""".formatted(code, description, discount, expiration, published);
	}

	private static LocalDate futureDate() {
		return LocalDate.now(java.time.ZoneOffset.UTC).plusDays(30);
	}

	private static String uniqueSuffix(int length) {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		return uuid.substring(0, length);
	}
}
