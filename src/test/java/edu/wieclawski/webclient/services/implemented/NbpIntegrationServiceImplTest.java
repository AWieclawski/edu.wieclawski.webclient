package edu.wieclawski.webclient.services.implemented;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.wieclawski.webclient.dtos.NbpARateDto;
import edu.wieclawski.webclient.services.NbpIntegrationService;

@SpringBootTest
class NbpIntegrationServiceImplTest {

	@Autowired
	private NbpIntegrationService nbpIntegrationService;

	@Test
	void testGetATypeRateByValidDateAndValidSymbolDoesNotThrow() {
		LocalDate date = LocalDate.of(2022, 5, 27); // business day

		Assertions.assertDoesNotThrow(
				() -> nbpIntegrationService.getATypeRateByDateAndSymbol(date, "eur"));
	}

	@Test
	void testGetATypeRateByValidDateAndValidSymbolReturnProperList() {
		LocalDate date = LocalDate.of(2022, 5, 2); // business day
		List<NbpARateDto> nbpARateDtos =
				nbpIntegrationService.getATypeRateByDateAndSymbol(date, "usd");

		Assertions.assertEquals(nbpARateDtos.size(), 1);
		Assertions.assertEquals(nbpARateDtos.get(0).getEffectiveDate(), date);
	}

	@Test
	void testGetATypeRateByNotValidDateAndValidSymbolThrowsException() {
		LocalDate date = LocalDate.of(2022, 5, 29); // non business day

		Assertions.assertThrows(RuntimeException.class,
				() -> nbpIntegrationService.getATypeRateByDateAndSymbol(date, "eur"));
	}

}
