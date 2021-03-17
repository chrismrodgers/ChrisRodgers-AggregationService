package tnt.crodgers.assignment.client;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Integration class for invoking a component API.
 * The relative path is parameterised and the base URL can be overridden via the System Property client.url
 */
@Slf4j
public class Client {
	
	/** Timeout in-flight requests after 20 seconds to ensure eventual cleanup */
	private static final int RESPONSE_TIMEOUT_MS = 20000;

	private final HttpClient httpClient = HttpClient.create()
			  .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
			  .responseTimeout(Duration.ofMillis(RESPONSE_TIMEOUT_MS));
	
	private final WebClient client;

	private final String resource;

	/** System property key for overriding the high traffic alert threshold with: <pre>-Dalert_threshold={avg num requests per sec}</pre> */
	private static final String SYS_PROP_CLIENT_URL = "client.url";
	private static final String SYS_PROP_CLIENT_URL_DEFAULT = "http://localhost:8090";

	
	/**
	 * @return The absolute path to the log file to consume
	 */
	private static String getClientUrl() {
		Properties props = System.getProperties();
		return props.getProperty(SYS_PROP_CLIENT_URL, SYS_PROP_CLIENT_URL_DEFAULT);
	}
	
	public Client(String resource) {
		this.resource = resource;
		client = WebClient.builder()
				  .clientConnector(new ReactorClientHttpConnector(httpClient))
				  .baseUrl(getClientUrl())
				  .build();
	}
	
	protected Mono<String> getCaller(String qValue) {
		@SuppressWarnings("rawtypes")
		RequestHeadersSpec spec = client.get().uri(uriBuilder -> uriBuilder.path("/"+resource)
				.queryParam("q", qValue).build())
				.accept(MediaType.APPLICATION_JSON);
		return spec.retrieve().bodyToMono(String.class);
	}

	/**
	 * Makes an async call to the relevant component API. 
	 * @param qValues the "q" parameter value to use for the API call
	 * @param handler the callback to invoke when a response is received
	 * @param callId The correlation ID used to match the response with its aggregation request
	 */
	public void call(String qValues, ResponseHandler handler, UUID callId) {
		Mono<String> caller = getCaller(qValues);

        log.info("Querying " + resource + "?q=" + qValues);
        caller.onErrorResume(e -> {
            log.warn(resource + " error " + e);
            handler.handleResponse(callId, qValues, null);
            return Mono.empty();
        }).subscribe(new Consumer<String>() {
			@Override
			public void accept(String body) {
				log.info(resource + " rxd " + body);
				handler.handleResponse(callId, qValues, body);
			}
		});
	}
}