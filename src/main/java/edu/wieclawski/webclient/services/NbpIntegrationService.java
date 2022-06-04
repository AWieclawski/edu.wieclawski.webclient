package edu.wieclawski.webclient.services;

import java.time.LocalDate;
import java.util.List;

import edu.wieclawski.webclient.dtos.NbpARateDto;

public interface NbpIntegrationService {

	List<NbpARateDto> getATypeRateByDateAndSymbol(LocalDate publicatiopnDate, String currencySymbol);

	List<NbpARateDto> getATypeRatesByDatesRangeAndSymbol(LocalDate startDate, LocalDate endDate,
			String currencySymbol);

}
