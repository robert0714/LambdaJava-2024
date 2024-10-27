package software.aws; 

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.lambda.waiters.LambdaWaiter;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;  
import static org.testcontainers.containers.localstack.LocalStackContainer.Service;
 

import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper; 

import software.amazon.awssdk.http.apache.ApacheHttpClient;  

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List; 

import static org.junit.jupiter.api.Assertions.*; 

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class WeatherServiceTest { 
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
    		.withServices(LocalStackContainer.Service.LAMBDA, 
                    LocalStackContainer.Service.API_GATEWAY, 
                    LocalStackContainer.Service.DYNAMODB);

    private static LambdaClient lambdaClient;
    private static ApiGatewayClient apiGatewayClient;
    private static DynamoDbClient dynamoDbClient;
    private static SdkHttpClient  httpClient;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
     
    private static final String TABLE_NAME = "LocationsTable";
    private static final String WEATHER_EVENT_FUNCTION = "WeatherEventLambda";
    private static final String WEATHER_QUERY_FUNCTION = "WeatherQueryLambda";
    
    private static String apiId;
    private static String apiEndpoint;

    @BeforeAll
    static void setUp() throws IOException {
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test")
        );

        Region region = Region.of(localStack.getRegion());

        System.out.println("--------------");
        System.out.println(region);
        System.out.println("--------------");
        // 初始化所有客戶端
        lambdaClient = LambdaClient.builder()
                .endpointOverride(localStack.getEndpointOverride(Service.LAMBDA))
                .credentialsProvider(credentialsProvider)
                .region(region)
                .httpClient(ApacheHttpClient.builder().build())
                .build();

        apiGatewayClient = ApiGatewayClient.builder()
                .endpointOverride(localStack.getEndpointOverride(Service.API_GATEWAY))
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();

        dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(localStack.getEndpointOverride(Service.DYNAMODB))
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();
        
        httpClient = UrlConnectionHttpClient.builder().build();
//        httpClient =  ApacheHttpClient.builder().build();
        setupInfrastructure();
    }
    @AfterAll
    static void tearDown() throws IOException {
    	cleanupTestData();
    }

    private static void setupInfrastructure() throws IOException {
        // 1. 創建DynamoDB表
        createDynamoDbTable();
        
        // 2. 創建Lambda函數
        byte[] zipBytes = LambdaCompilerHelper.getTestJarBytes();
        createLambdaFunction(WEATHER_EVENT_FUNCTION, zipBytes);
        createLambdaFunction(WEATHER_QUERY_FUNCTION, zipBytes);
        
         
        
        // 3. 創建和配置API Gateway
        setupApiGateway();
    }

    private static void createDynamoDbTable() {
        CreateTableRequest createTableRequest = CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder()
                        .attributeName("locationName")
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName("locationName")
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        dynamoDbClient.createTable(createTableRequest);
    }

     

     

    private static String createLambdaFunction(String functionName, byte[] zipBytes) {
       try {
    	   LambdaWaiter waiter = lambdaClient.waiter();
    	   
           CreateFunctionRequest createFunctionRequest = CreateFunctionRequest.builder()
                .functionName(functionName)
                .runtime("java17")
                .role("arn:aws:iam::000000000000:role/lambda-role")
                .handler("software.aws." + functionName + "::handleRequest")
                .code(FunctionCode.builder()
                        .zipFile(SdkBytes.fromByteArray(zipBytes))
                        .build())
                .environment(Environment.builder()
                        .variables(Map.of("LOCATIONS_TABLE", TABLE_NAME))
                        .build())
                .memorySize(512)
                .timeout(25)
                .build();

          // Create a Lambda function using a waiter
           CreateFunctionResponse functionResponse = lambdaClient.createFunction(createFunctionRequest);
           GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
                   .functionName(functionName)
                   .build(); 
           
           WaiterResponse<GetFunctionResponse> waiterResponse = waiter.waitUntilFunctionExists(getFunctionRequest);
           waiterResponse.matched().response().ifPresent(System.out::println);
//         SDKv2LambdaUtils. waitForFunctionActive(lambdaClient , functionName);
           
           return  functionResponse.functionArn();
    	 }catch (Exception e) {
             throw new RuntimeException("創建 Lambda 函數失敗", e);
         } 
    }

    private static void setupApiGateway() {
        // 創建API
        CreateRestApiRequest createApiRequest = CreateRestApiRequest.builder()
                .name("WeatherAPI")
                .description("Weather API")
                .build();

        CreateRestApiResponse createApiResponse = apiGatewayClient.createRestApi(createApiRequest);
        String apiId = createApiResponse.id();

        // 獲取根資源ID
        String rootResourceId = apiGatewayClient.getResources(GetResourcesRequest.builder()
                .restApiId(apiId)
                .build())
                .items()
                .get(0)
                .id();

        // 創建/events資源和方法
        setupApiResource(apiId, rootResourceId, "events", WEATHER_EVENT_FUNCTION, "POST");
        
        // 創建/locations資源和方法
        setupApiResource(apiId, rootResourceId, "locations", WEATHER_QUERY_FUNCTION, "GET");

        // 部署API
        CreateDeploymentRequest deployRequest = CreateDeploymentRequest.builder()
                .restApiId(apiId)
                .stageName("prod")
                .build();

        CreateDeploymentResponse createDeploymentResponse=  apiGatewayClient.createDeployment(deployRequest);
        
        Map<String, Map<String, MethodSnapshot>> map = createDeploymentResponse.apiSummary();
        
       // Set API endpoint
        apiEndpoint = String.format("http://%s:%d/restapis/%s/local/_user_request_",
                localStack.getHost(),
                localStack.getMappedPort(4566),
                apiId);
    }

    private static void setupApiResource(String apiId, String parentId, String pathPart, 
                                       String lambdaFunction, String httpMethod) {
        // 創建資源
        CreateResourceRequest createResourceRequest = CreateResourceRequest.builder()
                .restApiId(apiId)
                .parentId(parentId)
                .pathPart(pathPart)
                .build();

        CreateResourceResponse createResourceResponse = apiGatewayClient.createResource(createResourceRequest);
        String resourceId = createResourceResponse.id();

        // 創建方法
        PutMethodRequest putMethodRequest = PutMethodRequest.builder()
                .restApiId(apiId)
                .resourceId(resourceId)
                .httpMethod(httpMethod)
                .authorizationType("NONE")
                .build();

        apiGatewayClient.putMethod(putMethodRequest);

        // 設置Lambda集成
        String functionArn = String.format("arn:aws:lambda:%s:000000000000:function:%s",
                localStack.getRegion(), lambdaFunction);

        PutIntegrationRequest putIntegrationRequest = PutIntegrationRequest.builder()
                .restApiId(apiId)
                .resourceId(resourceId)
                .httpMethod(httpMethod)
                .type(IntegrationType.AWS_PROXY)
                .integrationHttpMethod("POST")
                .uri(String.format("arn:aws:apigateway:%s:lambda:path/2015-03-31/functions/%s/invocations",
                        localStack.getRegion(), functionArn))
                .build();

        apiGatewayClient.putIntegration(putIntegrationRequest);
    }

    @Test
    void testWeatherApi() {
        // 測試插入位置數據
        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(Map.of(
                        "locationName", AttributeValue.builder().s("Tokyo").build(),
                        "temperature", AttributeValue.builder().n("25.5").build()
                ))
                .build();

        dynamoDbClient.putItem(putItemRequest);

        // 測試Weather Event Lambda
        InvokeRequest eventInvokeRequest = InvokeRequest.builder()
                .functionName(WEATHER_EVENT_FUNCTION)
                .payload(SdkBytes.fromUtf8String(
           "{\"httpMethod\":\"POST\",\"body\":\"{\\\"locationName\\\":\\\"Brooklyn, NY\\\" , \\\"temperature\\\":91 , \\\"timestamp\\\":1564428897 , \\\"latitude\\\": 40.70, \\\"longitude\\\": -73.99 }\"  }"
                		
                		))
                .build();

        InvokeResponse eventResponse = lambdaClient.invoke(eventInvokeRequest);
        assertEquals(200, eventResponse.statusCode());
        String result1 = new String(eventResponse.payload().asByteArray(), StandardCharsets.UTF_8);
        assertEquals("{\"statusCode\":200,\"body\":\"Weather event processed\"}", result1);
        
        // 測試Weather Query Lambda
        InvokeRequest queryInvokeRequest = InvokeRequest.builder()
                .functionName(WEATHER_QUERY_FUNCTION)
                .payload(SdkBytes.fromUtf8String("{\"httpMethod\":\"GET\"}"))
                .build();

        InvokeResponse queryResponse = lambdaClient.invoke(queryInvokeRequest);
        assertEquals(200, queryResponse.statusCode());
        String result2 = new String(queryResponse.payload().asByteArray(), StandardCharsets.UTF_8);
        assertTrue(result2.contains("\\\"statusCode\\\":200") || result2.contains("Brooklyn, NY"));
    } 
    @Test
    @Order(1)
    void testDynamoDBTableCreation() {
        DescribeTableResponse response = dynamoDbClient.describeTable(
            DescribeTableRequest.builder()
                .tableName(TABLE_NAME)
                .build()
        );
        
        assertEquals(TABLE_NAME, response.table().tableName());
        assertEquals("locationName", response.table().keySchema().get(0).attributeName());
    }

    @Test
    @Order(2)
    void testLambdaFunctionsCreation() {
        // 測試 WeatherEventLambda
        GetFunctionResponse eventLambdaResponse = lambdaClient.getFunction(
            GetFunctionRequest.builder()
                .functionName(WEATHER_EVENT_FUNCTION)
                .build()
        );
        assertEquals(WEATHER_EVENT_FUNCTION, eventLambdaResponse.configuration().functionName());
        
        // 測試 WeatherQueryLambda
        GetFunctionResponse queryLambdaResponse = lambdaClient.getFunction(
            GetFunctionRequest.builder()
                .functionName(WEATHER_QUERY_FUNCTION)
                .build()
        );
        assertEquals(WEATHER_QUERY_FUNCTION, queryLambdaResponse.configuration().functionName());
    }

    @Test
    @Order(3)
    void testWeatherEventEndToEnd() throws Exception {
        // 準備測試數據
        Map<String, Object> weatherEvent = new HashMap<>();
        weatherEvent.put("locationName", "Tokyo");
        weatherEvent.put("temperature", 25.5);
        weatherEvent.put("timestamp", System.currentTimeMillis());
        
        // 通過 API Gateway 發送請求
        String response = makeHttpRequest("/events", "POST", weatherEvent);
        assertNotNull(response);
        
        // 驗證數據已存入 DynamoDB
        GetItemResponse getItemResponse = dynamoDbClient.getItem(
            GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of("locationName", AttributeValue.builder().s("Tokyo").build()))
                .build()
        );
        
        Map<String, AttributeValue> item = getItemResponse.item();
        assertNotNull(item);        
        assertEquals("Tokyo", item.get("locationName").s());
    }

    @Test
    @Order(4)
    void testWeatherQueryEndToEnd() throws Exception {
        // 先插入一些測試數據
        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(Map.of(
                    "locationName", AttributeValue.builder().s("Osaka").build(),
                    "temperature", AttributeValue.builder().n("23.5").build(),
                    "timestamp", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build()
                ))
                .build();
        PutItemResponse putItemRresponse =   dynamoDbClient.putItem(putItemRequest);
        System.out.println(TABLE_NAME + " was successfully updated. The request id is "
                + putItemRresponse.responseMetadata().requestId());
        
        
        // 通過 API Gateway 查詢數據
        String response = makeHttpRequest("/locations", "GET", null);
        assertNotNull(response);
        
        // 驗證返回的數據
        List<?> locations = objectMapper.readValue(response, List.class);
        assertFalse(locations.isEmpty());
    }
    
    @Test
    @Order(5)
    void testErrorHandling() throws Exception {
        // 測試無效的請求
        Map<String, Object> invalidEvent = new HashMap<>();
        invalidEvent.put("invalidField", "invalidValue");
        
        String response = makeHttpRequest("/events", "POST", invalidEvent);
        assertTrue(response.contains("error") || response.contains("Error"));
    }

    private static String makeHttpRequest(String path, String method, Object body) throws Exception  {
    	
    	SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder()
                .uri(URI.create(apiEndpoint.toString() + path))
                .method(SdkHttpMethod.fromValue(method));
                
        if (body != null) {
            requestBuilder.putHeader("Content-Type", "application/json"); 
            
            final byte[] bytes = objectMapper.writeValueAsBytes(body) ; 
            
            requestBuilder.contentStreamProvider(() -> 
                new ByteArrayInputStream(bytes));
        } 
        SdkHttpRequest request = requestBuilder.build() ;
        
        // Create executable request
        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                .request(request)
                .contentStreamProvider(requestBuilder.contentStreamProvider() )
                .build();
         
        // Call request 
        HttpExecuteResponse executeResponse = httpClient.prepareRequest(executeRequest).call();

        boolean ok = executeResponse.httpResponse().statusCode() == 200;
        if(ok) {
            String response = IoUtils.toUtf8String(executeResponse.responseBody().orElse(AbortableInputStream.createEmpty()));
            return response ;
        }else {
            System.out.println("------------------------");
           	System.out.println(executeResponse.httpResponse().statusCode());
            System.out.println(executeResponse.httpResponse().statusText());
            String response = new String(executeResponse.responseBody().get().readAllBytes());
            System.out.println(response);
            return response ;
        } 
    }

    private static void cleanupTestData() {
        try {
            // 清理 DynamoDB 表中的測試數據
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(TABLE_NAME)
                    .build();
            
            dynamoDbClient.scan(scanRequest)
                    .items()
                    .forEach(item -> {
                        DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                                .tableName(TABLE_NAME)
                                .key(Map.of("locationName", item.get("locationName")))
                                .build();
                        dynamoDbClient.deleteItem(deleteRequest);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}