package tnt.crodgers.assignment.service.aggregation;

import java.util.Collections;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages={"tnt.crodgers.assignment"})
public class AggregationService {

	/** System property key for overriding the high traffic alert threshold with: <pre>-Dalert_threshold={avg num requests per sec}</pre> */
	private static final String SYS_PROP_SERVER_PORT = "server.port";
	private static final String SYS_PROP_SERVER_PORT_DEFAULT = "8081";

	/**
	 * @return The absolute path to the log file to consume
	 */
	private static String getServerPort() {
		Properties props = System.getProperties();
		return props.getProperty(SYS_PROP_SERVER_PORT, SYS_PROP_SERVER_PORT_DEFAULT);
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(AggregationService.class);
        app.setDefaultProperties(Collections.singletonMap(SYS_PROP_SERVER_PORT, getServerPort()));
        app.run(args);
	}
}