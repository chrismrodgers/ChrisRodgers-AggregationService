package tnt.crodgers.assignment.service.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import tnt.crodgers.assignment.client.Client;
import tnt.crodgers.assignment.client.QueueBackedClient;
import tnt.crodgers.assignment.client.ResponseHandler;

/**
 * Pseudo-integration tests that mock out the component clients and test the POJO
 * entry and exit points to the application.
 */
class RequestHandlerIntegrationTest {
	 
	@Mock
	private Client mockClient;
	
	@Captor 
	private ArgumentCaptor<ResponseHandler> callbackCaptor;

	@Captor
	private ArgumentCaptor<UUID> pricingUuidCaptor, trackUuidCaptor, shipmentsUuidCaptor;
	
	private RequestHandler handler = new RequestHandler();

	@BeforeEach
    void initService() throws Exception {
        MockitoAnnotations.openMocks(this);
		handler = new RequestHandler();
		mockClient(RequestHandler.shipmentsClient, mockClient);
		mockClient(RequestHandler.pricingClient,   mockClient);
		mockClient(RequestHandler.trackClient,     mockClient);
    }

	private void mockClient(QueueBackedClient clientWrapper, Client mockClient)
			throws Exception {
		Field privateStringField = QueueBackedClient.class.getDeclaredField("client");
		privateStringField.setAccessible(true);	
		privateStringField.set(clientWrapper, mockClient);
	}
	
	@Test
	void testGetShipmentsNoQueueing() {
		HashMap<String, String> params = new HashMap<String, String>();

		String ids = "A,B,C,D,E";
		params.put("shipments", ids);
		AggregationResponse aggregationResponse = handler.get(params);

		verify(mockClient).call(any(String.class), callbackCaptor.capture(), shipmentsUuidCaptor.capture());
		String responseJson = "{" +
				"\"A\": [\"box\", \"box\", \"pallet\"]," +
				"\"B\": [\"envelope\"]," +
				"\"C\": [\"pallet\"]," +
				"\"D\": [\"box\"]," +
				"\"E\": null" +
				"}";
		callbackCaptor.getValue().handleResponse(shipmentsUuidCaptor.getValue(), ids, responseJson);
		
		assertNull(aggregationResponse.getPricing());
		assertNull(aggregationResponse.getTrack());
		assertNotNull(aggregationResponse.getShipments());

		assertEquals(new HashSet<>(Arrays.asList(ids.split(","))), aggregationResponse.getShipments().keySet());
		assertEquals(Arrays.asList("box", "box", "pallet"), aggregationResponse.getShipments().get("A"));
		assertEquals(Arrays.asList("envelope"), aggregationResponse.getShipments().get("B"));
		assertEquals(Arrays.asList("pallet"), aggregationResponse.getShipments().get("C"));
		assertEquals(Arrays.asList("box"), aggregationResponse.getShipments().get("D"));
		assertNull(aggregationResponse.getShipments().get("E"));
	}
	
	@Test
	void testGetShipmentsWithQueueing() {
		HashMap<String, String> params = new HashMap<String, String>();

		String ids = "A";
		params.put("shipments", ids);
		AggregationResponse aggregationResponse = handler.get(params);

		verify(mockClient).call(any(String.class), callbackCaptor.capture(), shipmentsUuidCaptor.capture());
		String responseJson = "{" +
				"\"A\": [\"box\", \"box\", \"pallet\"]" +
				"}";
		callbackCaptor.getValue().handleResponse(shipmentsUuidCaptor.getValue(), ids, responseJson);
		
		assertNull(aggregationResponse.getPricing());
		assertNull(aggregationResponse.getTrack());
		assertNotNull(aggregationResponse.getShipments());

		assertEquals(new HashSet<>(Arrays.asList(ids.split(","))), aggregationResponse.getShipments().keySet());
		assertEquals(Arrays.asList("box", "box", "pallet"), aggregationResponse.getShipments().get("A"));
	}
	
	@Test
	void testGetTrackNoQueueing() {
		HashMap<String, String> params = new HashMap<String, String>();

		String ids = "1,2,3,4,5";
		params.put("track", ids);
		AggregationResponse aggregationResponse = handler.get(params);

		verify(mockClient).call(any(String.class), callbackCaptor.capture(), trackUuidCaptor.capture());
		String responseJson = "{" +
				"\"1\": \"NEW\"," +
				"\"2\": null," +
				"\"3\": \"NEW\"," +
				"\"4\": null," +
				"\"5\": \"COLLECTING\"" +
				"}";
		callbackCaptor.getValue().handleResponse(trackUuidCaptor.getValue(), ids, responseJson);
		
		assertNull(aggregationResponse.getPricing());
		assertNotNull(aggregationResponse.getTrack());
		assertNull(aggregationResponse.getShipments());

		assertEquals(new HashSet<>(Arrays.asList(ids.split(","))), aggregationResponse.getTrack().keySet());
		assertEquals("NEW", aggregationResponse.getTrack().get("1"));
		assertNull(aggregationResponse.getTrack().get("2"));
		assertEquals("NEW", aggregationResponse.getTrack().get("3"));
		assertNull(aggregationResponse.getTrack().get("4"));
		assertEquals("COLLECTING", aggregationResponse.getTrack().get("5"));
	}
	
	@Test
	void testGetTrackWithQueueing() {
		HashMap<String, String> params = new HashMap<String, String>();

		String ids = "1,2";
		params.put("track", ids);
		AggregationResponse aggregationResponse = handler.get(params);

		verify(mockClient).call(any(String.class), callbackCaptor.capture(), trackUuidCaptor.capture());
		String responseJson = "{" +
				"\"1\": \"NEW\"," +
				"\"2\": null" +
				"}";
		callbackCaptor.getValue().handleResponse(trackUuidCaptor.getValue(), ids, responseJson);
		
		assertNull(aggregationResponse.getPricing());
		assertNotNull(aggregationResponse.getTrack());
		assertNull(aggregationResponse.getShipments());

		assertEquals(new HashSet<>(Arrays.asList(ids.split(","))), aggregationResponse.getTrack().keySet());
		assertEquals("NEW", aggregationResponse.getTrack().get("1"));
		assertNull(aggregationResponse.getTrack().get("2"));
	}
	
	@Test
	void testGetPricingNoQueueing() {
		HashMap<String, String> params = new HashMap<String, String>();

		String ids = "NL,PT,AU,AG,CN";
		params.put("pricing", ids);
		AggregationResponse aggregationResponse = handler.get(params);

		verify(mockClient).call(any(String.class), callbackCaptor.capture(), pricingUuidCaptor.capture());
		String responseJson = "{" +
				"\"NL\": \"14.24209\"," +
				"\"PT\": null," +
				"\"AU\": null," +
				"\"AG\": null," +
				"\"CN\": \"20.5034\"" +
				"}";
		callbackCaptor.getValue().handleResponse(pricingUuidCaptor.getValue(), ids, responseJson);
		
		assertNotNull(aggregationResponse.getPricing());
		assertNull(aggregationResponse.getTrack());
		assertNull(aggregationResponse.getShipments());

		assertEquals(new HashSet<>(Arrays.asList(ids.split(","))), aggregationResponse.getPricing().keySet());
		assertEquals("14.24209", aggregationResponse.getPricing().get("NL"));
		assertNull(aggregationResponse.getPricing().get("PT"));
		assertNull(aggregationResponse.getPricing().get("AU"));
		assertNull(aggregationResponse.getPricing().get("AG"));
		assertEquals("20.5034", aggregationResponse.getPricing().get("CN"));
	}
	
	@Test
	void testGetPricingWithQueueing() {
		HashMap<String, String> params = new HashMap<String, String>();

		String ids = "NL,CN";
		params.put("pricing", ids);
		AggregationResponse aggregationResponse = handler.get(params);

		verify(mockClient).call(any(String.class), callbackCaptor.capture(), pricingUuidCaptor.capture());
		String responseJson = "{" +
				"\"NL\": \"14.24209\"," +
				"\"CN\": \"20.5034\"" +
				"}";
		callbackCaptor.getValue().handleResponse(pricingUuidCaptor.getValue(), ids, responseJson);
		
		assertNotNull(aggregationResponse.getPricing());
		assertNull(aggregationResponse.getTrack());
		assertNull(aggregationResponse.getShipments());

		assertEquals(new HashSet<>(Arrays.asList(ids.split(","))), aggregationResponse.getPricing().keySet());
		assertEquals("14.24209", aggregationResponse.getPricing().get("NL"));
		assertEquals("20.5034", aggregationResponse.getPricing().get("CN"));
	}

	@Test
	void testGetNoParams() {
		HashMap<String, String> params = new HashMap<String, String>();
		handler.get(params);

		verifyNoInteractions(mockClient);
	}

	@Test
	void testGetBlankParams() {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("shipments", "");
		params.put("track", "");
		params.put("pricing", "");
		handler.get(params);

		verifyNoInteractions(mockClient);
	}
}
