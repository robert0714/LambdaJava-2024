package software.aws;

import org.apache.commons.lang3.StringUtils;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.lambda.model.Runtime;

import java.nio.charset.StandardCharsets; 
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.LAMBDA;

class StringProcessorTest {

    private static LocalStackContainer localstack;
    private static LambdaClient lambdaClient;
    private static final String FUNCTION_NAME = "string-processor";

    @BeforeAll
    static void setUp() {
        // 啟動LocalStack容器
        localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(LAMBDA);
        localstack.start();

        // 配置Lambda客戶端
         
        lambdaClient = LambdaClient.builder()
	       		 .endpointOverride(localstack.getEndpoint())
	       		 .credentialsProvider(
	       		        StaticCredentialsProvider.create(
	       		            AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
	       		        )
	       		    )
	       		 .region(Region.of(localstack.getRegion())) 
	             .build();
        
    }

    @Test
    protected void testStringProcessing() throws Exception{
    	// 創建函數
        createLambdaFunction();
    	
        // 準備測試輸入
        String input = "\"World\"";
        
        // 調用Lambda函數
        InvokeRequest invokeRequest = InvokeRequest.builder()
            .functionName(FUNCTION_NAME)
            .payload(SdkBytes.fromString(input, StandardCharsets.UTF_8))
            .build();

         
        System.out.println("4. Invoke the Lambda function.");
        System.out.println("*** Sleep for 1 min to get Lambda function ready.");       
        
        InvokeResponse response = lambdaClient.invoke(invokeRequest);
        
        // 解析響應
        String result = new String(response.payload().asByteArray(), StandardCharsets.UTF_8);
        
        // 驗證結果
        String expected = StringUtils.reverse(StringUtils.upperCase(input));
//        String expected = input;
        assertEquals(expected, result);
    }
   protected static void createLambdaFunction() {
        try {
        	// 創建Lambda函數配置
            CreateFunctionRequest request = CreateFunctionRequest.builder()
                .functionName(FUNCTION_NAME)
                .runtime(Runtime.JAVA17)
                .role("arn:aws:iam::000000000000:role/lambda-role")
                .handler("software.aws.StringProcessor::handleRequest")
                .code(FunctionCode.builder()
                    .zipFile(SdkBytes.fromByteArray(LambdaCompilerHelper.getTestJarBytes()))
                    .build())
                .build();

            lambdaClient.createFunction(request);
			SDKv2LambdaUtils. waitForFunctionActive(lambdaClient , FUNCTION_NAME);
		}catch (Exception e) {
            throw new RuntimeException("創建 Lambda 函數失敗", e);
        } 
        
    }
}