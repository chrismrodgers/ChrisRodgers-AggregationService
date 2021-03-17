package tnt.crodgers.assignment.service.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

class AggregationResponseTest {
		
	@Test
	void testRejectUnexpected() {
		AggregationResponse response = new AggregationResponse(new HashSet<>(Arrays.asList("A,B,C")),
				new HashSet<>(Arrays.asList("A,B,C")),
				new HashSet<>(Arrays.asList("A,B,C")));
		assertEquals(null, response.getShipments());		
		response.set("shipments", "D", 1);
		assertEquals(null, response.getShipments());
		assertFalse(response.allResponsesReceived());
	}

	@Test
	void testReset() {
		AggregationResponse response = new AggregationResponse(new HashSet<>(Arrays.asList("A,B,C")),
				new HashSet<>(Arrays.asList("A,B,C")),
				new HashSet<>(Arrays.asList("A,B,C")));
		assertEquals(null, response.getShipments());		
		assertFalse(response.allResponsesReceived());

		response.reset("shipments");
		response.reset("track");
		response.reset("pricing");
		assertTrue(response.allResponsesReceived());
	}

	@Test
	void testAllResponsesReceived() {
		AggregationResponse response = new AggregationResponse(new HashSet<>(Arrays.asList("A")),
				new HashSet<>(Arrays.asList("B")),
				new HashSet<>(Arrays.asList("C")));
		assertEquals(null, response.getShipments());		
		assertFalse(response.allResponsesReceived());

		response.set("shipments", "A", 1);
		response.set("pricing",   "B", 1);
		response.set("track",     "C", 1);
		assertTrue(response.allResponsesReceived());
	}

}
