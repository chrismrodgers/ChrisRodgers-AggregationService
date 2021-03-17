package tnt.crodgers.assignment.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tnt.crodgers.assignment.service.aggregation.AggregationResponse;

class RequestQueueTest {

	private Set<String> setify(String commaSeparatedString) {
		return new HashSet<>(Arrays.asList(commaSeparatedString.split(",")));
	}
	
	@Test
	void testEnqueueing() {
		final RequestQueue queue = new RequestQueue();
		assertEquals(0, queue.size());
		assertFalse(queue.hasReachedThreshold());

		AggregationResponse response = new AggregationResponse(new HashSet<>(), new HashSet<>(), new HashSet<>());
		queue.enqueue(setify("A,B,C,C,C"), response);
		assertEquals(3, queue.size());
		assertFalse(queue.hasReachedThreshold());
		
		queue.enqueue(setify("C,D,D,E"), response);
		assertEquals(5, queue.size());
		assertTrue(queue.hasReachedThreshold());
	}
	
	@Test
	void testFifo() {	
		final RequestQueue queue = new RequestQueue();
		AggregationResponse response = new AggregationResponse(new HashSet<>(), new HashSet<>(), new HashSet<>());
		queue.enqueue(setify("A"), response);
		queue.enqueue(setify("B"), response);
		queue.enqueue(setify("C,D"), response);
		queue.enqueue(setify("E"), response);
		queue.enqueue(setify("F"), response);
		assertEquals(6, queue.size());
		assertTrue(queue.hasReachedThreshold());

		// Dequeue A-E
		Map<String, Collection<AggregationResponse>> dequeued = queue.dequeue();
		assertEquals(5, dequeued.size());
		assertEquals(1, queue.size());
		
		// Confirm that F is the remainder
		assertNotNull(queue.dequeue().get("F"));
	}

}
