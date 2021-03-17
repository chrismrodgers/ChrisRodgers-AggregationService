package tnt.crodgers.assignment.service.aggregation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import tnt.crodgers.assignment.client.QueueBackedClient;

class RequestHandlerTest {
	@BeforeEach
    void initService() {
        MockitoAnnotations.openMocks(this);
    }
	 
	@Mock
	private QueueBackedClient mockTrackClient, mockShipmentsClient, mockPricingClient;
	
	@Test
	void testGetNoParams() {
		final RequestHandler handler = new RequestHandler();
		
		setField(handler, "shipmentsClient", mockShipmentsClient);
		setField(handler, "pricingClient", mockPricingClient);
		setField(handler, "trackClient", mockTrackClient);
		
		handler.get(new HashMap<String, String>());
		verifyNoInteractions(mockShipmentsClient);
		verifyNoInteractions(mockPricingClient);
		verifyNoInteractions(mockTrackClient);
	}
	
	@Test
	void testGetShipments() {
		final RequestHandler handler = new RequestHandler();
		
		setField(handler, "shipmentsClient", mockShipmentsClient);
		setField(handler, "pricingClient", mockPricingClient);
		setField(handler, "trackClient", mockTrackClient);
		
		HashMap<String, String> params = new HashMap<String, String>();
		String qVal = "a,b,c";
		params.put("shipments", qVal);
		handler.get(params);
		verifyNoInteractions(mockPricingClient);
		verifyNoInteractions(mockTrackClient);
		verify(mockShipmentsClient, times(1)).request(eq(handler.dedup(qVal)), any(AggregationResponse.class));
	}
	
	@Test
	void testGetTrack() {
		final RequestHandler handler = new RequestHandler();
		
		setField(handler, "shipmentsClient", mockShipmentsClient);
		setField(handler, "pricingClient", mockPricingClient);
		setField(handler, "trackClient", mockTrackClient);
		
		HashMap<String, String> params = new HashMap<String, String>();
		String qVal = "a,b,c";
		params.put("track", qVal);
		handler.get(params);
		verifyNoInteractions(mockPricingClient);
		verifyNoInteractions(mockShipmentsClient);
		verify(mockTrackClient, times(1)).request(eq(handler.dedup(qVal)), any(AggregationResponse.class));
	}
	
	@Test
	void testGetPricing() {
		final RequestHandler handler = new RequestHandler();
		
		setField(handler, "shipmentsClient", mockShipmentsClient);
		setField(handler, "pricingClient", mockPricingClient);
		setField(handler, "trackClient", mockTrackClient);
		
		HashMap<String, String> params = new HashMap<String, String>();
		String qVal = "a,b,c";
		params.put("pricing", qVal);
		handler.get(params);
		verifyNoInteractions(mockShipmentsClient);
		verifyNoInteractions(mockTrackClient);
		verify(mockPricingClient, times(1)).request(eq(handler.dedup(qVal)), any(AggregationResponse.class));
	}
	
	@Test
	void testGetAll() {
		final RequestHandler handler = new RequestHandler();
		
		setField(handler, "shipmentsClient", mockShipmentsClient);
		setField(handler, "pricingClient", mockPricingClient);
		setField(handler, "trackClient", mockTrackClient);
		
		HashMap<String, String> params = new HashMap<String, String>();
		String qPricing = "a,b,c";
		params.put("pricing", qPricing);
		String qTrack = "D,E,F";
		params.put("track", qTrack);
		String qShipments = "123,456,789";
		params.put("shipments", qShipments);
		handler.get(params);
		verify(mockPricingClient,   times(1)).request(eq(handler.dedup(qPricing)),   any(AggregationResponse.class));
		verify(mockTrackClient,     times(1)).request(eq(handler.dedup(qTrack)),     any(AggregationResponse.class));
		verify(mockShipmentsClient, times(1)).request(eq(handler.dedup(qShipments)), any(AggregationResponse.class));
	}

}
