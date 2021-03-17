package tnt.crodgers.assignment.service.aggregation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Response object used to collate component responses,
 * then signal the waiting handler to return the aggregation response.
 * 
 * Fields (those with getters) are structured to have the desired default JSON conversion.
 */
@Slf4j
@ToString
public class AggregationResponse {
	@Getter
	private Map<String, Object> shipments;
	@Getter
	private Map<String, Object> pricing;
	@Getter
	private Map<String, Object> track;

	private Set<String> shipmentsExpected, pricingExpected, trackExpected;
	
	public AggregationResponse(Set<String> shipmentsParams, Set<String> pricingParams, Set<String> trackParams) {
		shipmentsExpected = new HashSet<>(shipmentsParams);
		pricingExpected   = new HashSet<>(pricingParams);
		trackExpected     = new HashSet<>(trackParams);
	}

	public synchronized void set(String resource, String key, Object val) {
		synchronized (this) {
			if (resource.equalsIgnoreCase("pricing")) {
				pricing = set(resource, pricing, pricingExpected, key, val);
			}
			else if (resource.equalsIgnoreCase("track")) {
				track = set(resource, track, trackExpected, key, val);
			}
			else if (resource.equalsIgnoreCase("shipments")) {
				shipments = set(resource, shipments, shipmentsExpected, key, val);
			}	
			notifyIfPopulated();
		}
	}

	private Map<String, Object> set(String resource, Map<String, Object> data, Set<String> expected,
			String key, Object val) {
		if (!expected.remove(key)) {
			log.error(String.format("Ignoring unepxected response data: %s[%s]", resource, key));
		} else {
			if (data == null) {
				data = new HashMap<>();
			}
			log.debug(String.format("Storing response %s(%s)=%s", resource, key, val));
			data.put(key, val);
		}
		return data;
	}

	/**
	 * @param resource The resource for which we were unable to retrieve data and wish to return null
	 */
	public void reset(String resource) {
		synchronized (this) {
			if (resource.equalsIgnoreCase("pricing")) {
				pricing = null;
				pricingExpected.clear();
			}
			else if (resource.equalsIgnoreCase("track")) {
				track = null;
				trackExpected.clear();
			}
			else if (resource.equalsIgnoreCase("shipments")) {
				shipments = null;
				shipmentsExpected.clear();
			}
	
			notifyIfPopulated();
		}
	}

	protected boolean allResponsesReceived() {
		return shipmentsExpected.isEmpty() && pricingExpected.isEmpty() && trackExpected.isEmpty();
	}

	private void notifyIfPopulated() {
		if (allResponsesReceived()) {
			log.debug(String.format("Response now populated: [%s], notifying", this));
			this.notify();
		} else {
			log.debug(String.format("Response still waiting on %d track, %d shipment, %d pricing responses", 
					trackExpected.size(), shipmentsExpected.size(), pricingExpected.size()));
		}
	}
}
