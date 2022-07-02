package edu.wieclawski.webclient.services.implemented;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import edu.wieclawski.webclient.dtos.NbpARateDto;
import edu.wieclawski.webclient.dtos.NbpResponseDto;
import edu.wieclawski.webclient.exceptions.NbpIntegrationException;
import edu.wieclawski.webclient.services.NbpReactService;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * http://api.nbp.pl/#kursySingle
 * 
 * @author awieclawski according to https://www.baeldung.com/spring-5-webclient
 *
 */
@Service
public class NbpReactServiceImpl implements NbpReactService {
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

	private final WebClient webclient;

	public NbpReactServiceImpl(
			@Value("${nbp-api.host}") String NBP_API_URL) {
		this.webclient = WebClient.builder().filter(loggRequest()).baseUrl(NBP_API_URL)
				.clientConnector(createClientConnector()).build();
	}

	@SneakyThrows
	private ClientHttpConnector createClientConnector() {
		var sslContext = SslContextBuilder.forClient()
				.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		var httpClient = HttpClient.create()
				.secure(con -> con.sslContext(sslContext))
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
				.doOnConnected(conn -> conn
						.addHandlerFirst(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
						.addHandlerFirst(new WriteTimeoutHandler(5)));;

		return new ReactorClientHttpConnector(httpClient);
	}

	private Function<ClientResponse, Mono<? extends Throwable>> handleErrorResponse() {

		return clientResponse -> clientResponse.bodyToMono(String.class).map(rawResponse -> {
			LOG.error(rawResponse);

			throw new NbpIntegrationException("NBP integration problem!");
		});
	}

	@Override
	public List<NbpARateDto> getATypeRateByDateAndSymbol(LocalDate publicationDate,
			String currencySymbol) {
		final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.setAll(Map.of("format", FORMAT_DATA));
		final URI fullUri = getARatePathWithDateAndSymbol(publicationDate, currencySymbol);

		return getATypeRates(params, fullUri);
	}

	@Override
	public List<NbpARateDto> getATypeRatesByDatesRangeAndSymbol(LocalDate startDate, LocalDate endDate,
			String currencySymbol) {
		final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.setAll(Map.of("format", FORMAT_DATA));
		final URI fullUri = getARatePathWithDatesRangeAndSymbol(startDate, endDate, currencySymbol);

		return getATypeRates(params, fullUri);
	}

	private List<NbpARateDto> getATypeRates(MultiValueMap<String, String> params, URI fullUri) {
		final ParameterizedTypeReference<NbpResponseDto<List<NbpARateDto>>> parameterizedTypeReference =
				new ParameterizedTypeReference<NbpResponseDto<List<NbpARateDto>>>() {
				};
		var response = this.webclient.get()
				.uri(uriBuilder -> uriBuilder.path(fullUri.toString())
						.queryParams(params)
						.build())
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.onStatus(httpStatus -> !httpStatus.is2xxSuccessful(), handleErrorResponse())
				.bodyToMono(parameterizedTypeReference)
				.block();

		return response != null ? response.getRates() : null;
	}

	// http://api.nbp.pl/api/exchangerates/rates/{table}/code}/{date}/
	private URI getARatePathWithDateAndSymbol(LocalDate publicationDate, String currencySymbol) {
		final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

		return buildPathFromVariablesList(List.of(
				RATES_DIR.toLowerCase(),
				SCOPE_DIR.toLowerCase(),
				TYPE_DIR.toLowerCase(),
				currencySymbol.toLowerCase(),
				publicationDate.format(DATE_TIME_FORMATTER)));
	}

	// http://api.nbp.pl/api/exchangerates/rates/{table}/{code}/{startDate}/{endDate}/
	private URI getARatePathWithDatesRangeAndSymbol(LocalDate startDate, LocalDate endDate, String currencySymbol) {
		final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

		return buildPathFromVariablesList(List.of(
				RATES_DIR.toLowerCase(),
				SCOPE_DIR.toLowerCase(),
				TYPE_DIR.toLowerCase(),
				currencySymbol.toLowerCase(),
				startDate.format(DATE_TIME_FORMATTER),
				endDate.format(DATE_TIME_FORMATTER)));
	}

	private URI buildPathFromVariablesList(List<String> pathVariables) {
		URIBuilder builder = new URIBuilder();
		builder.setPathSegments(pathVariables);
		try {
			return builder.build();
		} catch (URISyntaxException e) {
			LOG.error(e.getMessage());
		}

		throw new RuntimeException("NBP url building problem");
	}

	private ExchangeFilterFunction loggRequest() {

		return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
			LOG.info("Request method={}, url={}", clientRequest.method(), clientRequest.url());
			clientRequest.headers().forEach((name, values) -> values
					.forEach(value -> LOG.info("Header name={}, value={}", name, value)));

			return Mono.just(clientRequest);
		});
	}

}
