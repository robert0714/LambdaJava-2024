package software.aws;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
 
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class WeatherEventLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	 private static final String TABLE_NAME = "LocationsTable";
	 private final ObjectMapper objectMapper =
	            new ObjectMapper()
	                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    	LambdaLogger logger = context.getLogger();
    	logger.log(event.getBody(),LogLevel.INFO);
    	
    	WeatherEvent weatherEvent=null;
		try {
			weatherEvent = this.objectMapper.readValue(event.getBody(), WeatherEvent.class);
		} catch (JsonProcessingException e) {
			logger.log(e.getMessage(),LogLevel.INFO);
			e.printStackTrace();
		} 
    	
    	Region region =   Region.US_EAST_1  ;
    	DynamoDbClient ddb = DynamoDbClient.builder()
                 .region(region)
                 .build();

         // Create a DynamoDbEnhancedClient and use the DynamoDbClient object.
         DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                 .dynamoDbClient(ddb)
                 .build();
      // Create a DynamoDbTable object based on Employee.
        DynamoDbTable<WeatherEvent> table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(WeatherEvent.class));
        
        table.putItem(weatherEvent);
        
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withBody("Weather event processed");
    }
}