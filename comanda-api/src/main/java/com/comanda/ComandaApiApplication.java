package com.comanda;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ComandaApiApplication {

	static {
		// PRD Secao 13: timezone fixo America/Fortaleza, limitacao conhecida ate a Fase 4.
		TimeZone.setDefault(TimeZone.getTimeZone("America/Fortaleza"));
	}

	public static void main(String[] args) {
		SpringApplication.run(ComandaApiApplication.class, args);
	}

}
