package tnt.crodgers.assignment.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import tnt.crodgers.assignment.service.aggregation.AggregationResponse;

/**
 * This class uses a queue to manage the IDs for which client requests are to be chunked up,
 * and a Map to manage the response(s) waiting on that component data to assemble the overarching Aggregation response.
 * Opted for composition over inheritance ("has a" vs "is a") to provide a tighter interface. 
 */
public class RequestQueue {
	/** Maps IDs to the response(s) waiting on response data for those IDs */ 
	private final Map<String, Collection<AggregationResponse>> responseManager = new HashMap<>();

	/** FIFO queue of IDs waiting for a data request to be made */ 
	private final LinkedList<String> requestQueue = new LinkedList<>();
	
	/** The queue length at which we start issuing requests */
	private static final int QUEUE_THRESHOLD = 5;
	
	/**
	 * @param qValues the IDs to issue request(s) for;
	 * 					note that we don't grow the queue if the ID was already queued
	 * @param response the response collector to send the eventual response fragment to  
	 */
	public void enqueue(Set<String> qValues, AggregationResponse response) {
		for (String id : qValues) {
			if (!responseManager.containsKey(id)) {
				requestQueue.add(id);
				responseManager.put(id, new HashSet<>());
			}
			responseManager.get(id).add(response);
		}
	}

	public int size() {
		return requestQueue.size();
	}

	public boolean hasReachedThreshold() {
		return requestQueue.size() >= QUEUE_THRESHOLD;
	}

	/**
	 * @return The first n={@link #QUEUE_THRESHOLD} items in the queue,
	 * 			or the entire queue content if it wasn't that full,
	 * 			mapped to the pending response(s) interested in those IDs 
	 */
	public Map<String, Collection<AggregationResponse>> dequeue() {
		Map<String, Collection<AggregationResponse>> retVal = new HashMap<>();
		for (int i = 0; i < QUEUE_THRESHOLD && !requestQueue.isEmpty(); i++) {
			String id = requestQueue.remove();
			retVal.put(id, responseManager.remove(id));
		}
		return retVal;
	}
}
