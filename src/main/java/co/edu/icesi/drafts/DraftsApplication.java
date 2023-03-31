package co.edu.icesi.drafts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"co.edu.icesi.drafts.service.IcesiDocumentService"})
public class DraftsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DraftsApplication.class, args);
	}

}
