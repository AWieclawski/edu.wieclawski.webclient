package edu.wieclawski.webclient.services.implemented;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.wieclawski.webclient.services.NbpARateService;

@SpringBootTest
class NbpIntegrationServiceImplTest {

	@Autowired
	private NbpARateService nbpIntegrationService;

	@Test
	void testGetATypeRateByValidDateAndValidSymbolReturnOk() {
		LocalDate date = LocalDate.of(2022, 5, 27); // business day

		Assertions.assertDoesNotThrow(() -> nbpIntegrationService.getATypeRateByDateAndSymbol(date, "eur"));
		Assertions.assertEquals(nbpIntegrationService.getATypeRateByDateAndSymbol(date, "usd").size(), 1);
	}

	@Test
	void testGetATypeRateByNotValidDateAndValidSymbolThrowsException() {
		LocalDate date = LocalDate.of(2022, 5, 29); // non business day

		Assertions.assertThrows(RuntimeException.class,
				() -> nbpIntegrationService.getATypeRateByDateAndSymbol(date, "eur"));
	}

}
