package software.aws;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
 
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

//https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_dynamodb_code_examples.html
public class WeatherQueryLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static DynamoDbClient dynamoDbClient= DynamoDbClient.builder().build(); 
    private final String tableName = System.getenv("LOCATIONS_TABLE");

    private static final String DEFAULT_LIMIT = "50";

	
	@Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    	LambdaLogger logger = context.getLogger();
    	logger.log(event.getBody(),LogLevel.INFO);
    	final String limitParam = DEFAULT_LIMIT;
        final int limit = Integer.parseInt(limitParam);

        final ScanRequest scanRequest = ScanRequest.builder()
                .tableName(tableName)
                .limit(limit) 
                .build();
        final ScanResponse  scanResult = dynamoDbClient.scan(scanRequest);

        final List<WeatherEvent> events = scanResult.items().stream()
                .map(item -> new WeatherEvent(
                        item.get("locationName").s(),
                        Double.parseDouble(item.get("temperature").n()),
                        null,
                        null,
                        null
                ))
                .collect(Collectors.toList());
//        if(events==null || events.size()==0) {
//        	return new APIGatewayProxyResponseEvent()
//                    .withStatusCode(200)
//                    .withBody("Weather query processed");
//        }
        try {
			final String json = objectMapper.writeValueAsString(events);
			return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(json);
		} catch (JsonProcessingException e) { 
			e.printStackTrace();
		}
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
//                .withBody("Weather query processed");
        .withBody("[]");
        
    }
}
