package edu.wieclawski.webclient.services.implemented;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import edu.wieclawski.webclient.services.NbpARateService;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * http://api.nbp.pl/#kursySingle
 * 
 * @author awieclawski
 *
 */
@Service
public class NbpIntegrationServiceImpl implements NbpARateService {
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	private final String NBP_API_URL = "http://api.nbp.pl/api/exchangerates/";
	private final WebClient webclient;

	public NbpIntegrationServiceImpl() {
		this.webclient = WebClient.builder().filter(loggRequest()).baseUrl(NBP_API_URL)
				.clientConnector(createClientConnector()).build();
	}

	@SneakyThrows
	private ClientHttpConnector createClientConnector() {
		var sslContext = SslContextBuilder.forClient()
				.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		var httpClient = HttpClient.create().secure(con -> con.sslContext(sslContext));

		return new ReactorClientHttpConnector(httpClient);
	}

	private Function<ClientResponse, Mono<? extends Throwable>> handleErrorResponse() {

		return clientResponse -> clientResponse.bodyToMono(String.class).map(rawResponse -> {
			LOG.error(rawResponse);

			throw new RuntimeException("NBP integration problem!");
		});
	}

	@Override
	public List<NbpARateDto> getATypeRateByDateAndSymbol(LocalDate publicationDate,
			String currencySymbol) {
		final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.setAll(Map.of("format", RATES_DATA_FORMAT));
		final URI fullUri = getARatePathWithDateAndSymbol(publicationDate, currencySymbol);
		final ParameterizedTypeReference<
				NbpResponseDto<List<NbpARateDto>>> parameterizedTypeReference =
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

	private URI getARatePathWithDateAndSymbol(LocalDate publicationDate, String currencySymbol) {

		return buildPathFromVariablesList(List.of(
				CONVERSION_RATES_PATH.toLowerCase(),
				RATES_TABLE_TYPE.toLowerCase(), currencySymbol.toLowerCase(),
				publicationDate.format(DATE_TIME_FORMATTER)));
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
