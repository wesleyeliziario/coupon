package br.com.technicalchallenge.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import br.com.technicalchallenge.coupon.domain.config.PersistenceConfig;

@SpringBootApplication(scanBasePackages = "br.com.technicalchallenge.coupon")
@Import(PersistenceConfig.class)
public class CouponApplication {

	public static void main(String[] args) {
		SpringApplication.run(CouponApplication.class, args);
	}
}
