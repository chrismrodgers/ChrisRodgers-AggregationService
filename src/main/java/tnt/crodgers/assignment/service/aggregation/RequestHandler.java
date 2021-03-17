package tnt.crodgers.assignment.service.aggregation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import tnt.crodgers.assignment.client.QueueBackedClient;

/**
 * Handler for decomposing aggregation requests into their individual calls,
 * waiting on each to complete and returning the aggregated responses
 */
@Slf4j
@Component
public class RequestHandler {

	protected static QueueBackedClient shipmentsClient = new QueueBackedClient("shipments");

	protected static QueueBackedClient pricingClient = new QueueBackedClient("pricing");

	protected static QueueBackedClient trackClient = new QueueBackedClient("track");

	/** 
	 * Prematurely respond to any pending requests after 30 seconds.
	 * This is a defensive measure vs something we should expect to have happen,
	 * as the individual component calls should return in a timely manner.
	 */ 
	private static final int TIMEOUT_MS = 30000;
	
	
	public Mono<ServerResponse> get(ServerRequest request) {
		String uri = request.uri().toString();
		log.info("REQUEST: " + uri);
		AggregationResponse response = get(request.queryParams().toSingleValueMap());
		log.info("RESPONDING: " + uri);
		
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(response));
	}

	protected AggregationResponse get(Map<String, String> params) {
		Set<String> shipmentsParams = dedup(params.get("shipments"));
		Set<String> pricingParams   = dedup(params.get("pricing"));
		Set<String> trackParams     = dedup(params.get("track"));

		// Create a response instance to gather the individual responses
		final AggregationResponse response = new AggregationResponse(shipmentsParams, pricingParams, trackParams);
		synchronized (response) {
			if (!shipmentsParams.isEmpty()) {
				shipmentsClient.request(shipmentsParams, response);
			}
			if (!pricingParams.isEmpty()) {
				pricingClient.request(pricingParams, response);
			}
			if (!trackParams.isEmpty()) {
				trackClient.request(trackParams, response);
			}
			
			// Wait for the component calls to complete and their responses to become available.
			// If we timeout first, we send whatever is available.
			try {
				response.wait(TIMEOUT_MS);
			} catch (InterruptedException e) {
				log.error("Interrupted while waiting for responses", e);
			}
		}
		
		return response;
	}

	protected HashSet<String> dedup(String paramVal) {
		if (paramVal == null || paramVal.isEmpty()) {
			return new HashSet<>();
		}
		return new HashSet<>(Arrays.asList(paramVal.split(",")));
	}
}
