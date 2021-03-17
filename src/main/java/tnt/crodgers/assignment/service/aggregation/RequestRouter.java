package tnt.crodgers.assignment.service.aggregation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Routes for our Service's supported paths and HTTP methods - currently only GET /aggregation
 */
@Configuration
public class RequestRouter {

	/**
	 * @param handler The Service's request handler
	 * @return Route mapping from our Service resource to its request handler 
	 */
	@Bean
	public RouterFunction<ServerResponse> route(RequestHandler handler) {
		return RouterFunctions.route(
				RequestPredicates.GET("/aggregation").and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
				handler::get);
	}

}