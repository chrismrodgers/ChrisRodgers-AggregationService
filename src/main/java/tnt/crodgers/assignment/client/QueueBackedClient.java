package tnt.crodgers.assignment.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import tnt.crodgers.assignment.service.aggregation.AggregationResponse;

/**
 * Client wrapper that buffers incoming requests, making a single client request when:
 * 	- BUFFERED_REQUEST_SIZE different IDs are pending
 *  or 
 *  - MAX_REQUEST_DELAY_MS has passed since the buffer was initially (re)populated 
 */
@Slf4j
public class QueueBackedClient implements ResponseHandler {

	/** The resource of the client we manage/buffer */
	private final String resource;
	
	/** The Client to be called subject to meeting buffering requirements */
	protected final Client client;

	/** Pending requests */
	private RequestQueue queue = new RequestQueue();

	/**
	 * The response targets for requests we have made and are awaiting responses for,
	 * keyed by the buffer's UUID to allow response matching. 
	 */
	private final Map<UUID, Map<String, Collection<AggregationResponse>>> inFlight = new HashMap<>();

	/** Issue a request upon one or more requests being buffered this long */
	private static final int MAX_REQUEST_DELAY_MS = 5000;

	/** Used to ensure buffered requests don't languish indefinitely */
	private Timer timer;
	
	public QueueBackedClient(String resource) {
		this.resource = resource;
		this.client = new Client(resource);
	}

	/**
	 * @param qValues The IDs to be requested
	 * @param response The response instance to populate with responses
	 */
	public void request(Set<String> qValues, AggregationResponse response) {
		// Treat this as a single logical operation to ensure atomic timer and buffer maintenance
		synchronized (this) {
			queue.enqueue(qValues, response);

			log.debug(resource + " now pending: " + queue.size());
			if (queue.hasReachedThreshold()) {
				sendQueuedRequests();
			}

			if (timer == null && queue.size() > 0) {
				// No timer instance means no unmade requests were previously queued,
				// so kick off a fresh timer to process the queued IDs if it doesn't fill in time.
				final QueueBackedClient callback = this;
				timer = new Timer(resource);
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						log.info(resource + " being requested due to time out");
						synchronized (callback) {
							callback.sendQueuedRequests();
						}
					}
				}, MAX_REQUEST_DELAY_MS);
				log.info(resource + " timer started");
			}
		}
	}
	
	private void sendQueuedRequests() {
		Map<String, Collection<AggregationResponse>> queued = queue.dequeue();
		final UUID uuid = UUID.randomUUID();
		inFlight.put(uuid, queued);
		client.call(String.join(",", queued.keySet()), this, uuid);
		
		// Now that we've issued a request for some/all pending IDs, consider starting a fresh timer.
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * Callback function that handles received responses
	 * 
	 * @param callId the correlation ID for the outgoing request given via {@link Client#call(String, ResponseHandler, UUID)}
	 * @param ids the IDs for which data was requested, and is now (except in error cases) available
	 * @param responseJson the response data from the client
	 */
	public void handleResponse(UUID callId, String ids, String responseJson) {
		// Treat this as a single logical operation to ensure atomic inFlight maintenance
		synchronized (this) {
			Map<String, Collection<AggregationResponse>> waitingReponses = inFlight.get(callId);
			if (waitingReponses == null) {
				log.error(resource + " received response to unrecognised caller ID " + callId + "; ignoring");
				return;
			}
			if (responseJson == null) {
				setNullValue(waitingReponses, ids);
				return;
			}

			try {
				Map<String, Object> responseData = 
						new ObjectMapper().readValue(responseJson, new TypeReference<Map<String, Object>>(){});

				for (Map.Entry<String, Object> entry : responseData.entrySet()) {
					for (AggregationResponse response : waitingReponses.get(entry.getKey())) {
						response.set(resource, entry.getKey(), entry.getValue());
					}
				}
			} catch (Exception e) {
				log.error(resource + " - couldn't parse response [" + responseJson + "], returning null data", e);
				setNullValue(waitingReponses, ids);
			}
			inFlight.remove(callId);
		}
	}

	private void setNullValue(Map<String, Collection<AggregationResponse>> waitingResponses, String ids) {
		for (String key : ids.split(",")) {
			for (AggregationResponse response : waitingResponses.get(key)) {
				response.reset(resource);
			}
		}
	}
}
