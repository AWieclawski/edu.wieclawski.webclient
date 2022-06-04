package edu.wieclawski.webclient.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import edu.wieclawski.webclient.dtos.NbpARateDto;

public interface NbpARateService {
	String CONVERSION_RATES_PATH = "rates";
	String RATES_TABLE_TYPE = "a";
	DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	String RATES_DATA_FORMAT = "json";

	List<NbpARateDto> getATypeRateByDateAndSymbol(LocalDate publicatiopnDate, String currencySymbol);

}
