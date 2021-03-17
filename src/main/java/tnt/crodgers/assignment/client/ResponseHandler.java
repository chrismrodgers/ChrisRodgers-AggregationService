package tnt.crodgers.assignment.client;

import java.util.UUID;

/**
 * Callback interface to handle async/responsive lifecycle between client request and response 
 */
public interface ResponseHandler {
	void handleResponse(UUID callId, String ids, String responseJson);
}
