package br.com.technicalchallenge.coupon.domain.config;

import java.time.Clock;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "br.com.technicalchallenge.coupon")
@EnableJpaRepositories(basePackages = "br.com.technicalchallenge.coupon")
public class PersistenceConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
