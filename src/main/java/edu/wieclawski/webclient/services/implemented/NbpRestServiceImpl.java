package edu.wieclawski.webclient.services.implemented;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.wieclawski.webclient.dtos.NbpARateDto;
import edu.wieclawski.webclient.dtos.NbpResponseDto;
import edu.wieclawski.webclient.services.NbpRestService;

/**
 * http://api.nbp.pl/#kursySingle
 * 
 * @author awieclawski according to https://www.baeldung.com/rest-template
 */
@Service
public class NbpRestServiceImpl implements NbpRestService {
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	@Value("${nbp-api.rates.dir}")
	private String RATES_DIR;

	@Value("${nbp-api.rates.range}")
	private String SCOPE_DIR;

	@Value("${nbp-api.rates.type}")
	private String TYPE_DIR;

	@Value("${nbp-api.rates.date-pattern}")
	private String DATE_PATTERN;

	@Value("${nbp-api.rates.format}")
	private String FORMAT_DATA;

	@Value("${nbp-api.host}")
	private String NBP_API_URL;

	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public List<NbpARateDto> getATypeRateByDateAndSymbol(LocalDate publicationDate, String currencySymbol) {
		final Map<String, ?> params = Map.of("format", FORMAT_DATA);
		final URI fullUri = getARatePathWithDateAndSymbol(publicationDate, currencySymbol);

		return getATypeRates(params, fullUri);
	}

	@Override
	public List<NbpARateDto> getATypeRatesByDatesRangeAndSymbol(LocalDate startDate, LocalDate endDate,
			String currencySymbol) {
		final Map<String, ?> params = Map.of("format", FORMAT_DATA);
		final URI fullUri = getARatePathWithDatesRangeAndSymbol(startDate, endDate, currencySymbol);

		return getATypeRates(params, fullUri);
	}

	private List<NbpARateDto> getATypeRates(Map<String, ?> params, URI fullUri) {
		final ParameterizedTypeReference<NbpResponseDto<List<NbpARateDto>>> parameterizedTypeReference =
				new ParameterizedTypeReference<NbpResponseDto<List<NbpARateDto>>>() {
				};
		ResponseEntity<NbpResponseDto<List<NbpARateDto>>> responseEntity = this.restTemplate.exchange(
				fullUri.toString(),
				HttpMethod.GET,
				null,
				parameterizedTypeReference,
				params);
		NbpResponseDto<List<NbpARateDto>> nbpResponseDto = responseEntity.getBody();

		return nbpResponseDto != null
				? nbpResponseDto.getRates()
				: null;
	}

	private URI getSafeUriPath(List<String> pathVariables) {
		URIBuilder builder = new URIBuilder();
		builder.setScheme("http");
		builder.setHost(NBP_API_URL.toLowerCase());
		builder.setPathSegments(pathVariables);
		try {
			return builder.build();
		} catch (URISyntaxException e) {
			LOG.error("Safe URI build problem! {}", e.getMessage());
		}

		throw new RuntimeException("NBP url building problem!");
	}

	// http://api.nbp.pl/api/exchangerates/rates/{table}/{code}/{date}/
	private URI getARatePathWithDateAndSymbol(LocalDate publicationDate, String currencySymbol) {
		final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

		return getSafeUriPath(List.of(
				RATES_DIR.toLowerCase(),
				SCOPE_DIR.toLowerCase(),
				TYPE_DIR.toLowerCase(),
				currencySymbol.toLowerCase(),
				publicationDate.format(DATE_TIME_FORMATTER)));
	}

	// http://api.nbp.pl/api/exchangerates/rates/{table}/{code}/{startDate}/{endDate}/
	private URI getARatePathWithDatesRangeAndSymbol(LocalDate startDate, LocalDate endDate, String currencySymbol) {
		final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

		return getSafeUriPath(List.of(
				RATES_DIR.toLowerCase(),
				SCOPE_DIR.toLowerCase(),
				TYPE_DIR.toLowerCase(),
				currencySymbol.toLowerCase(),
				startDate.format(DATE_TIME_FORMATTER),
				endDate.format(DATE_TIME_FORMATTER)));
	}

}
