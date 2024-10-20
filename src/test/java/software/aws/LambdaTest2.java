//package  software.aws;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.testcontainers.containers.localstack.LocalStackContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.shaded.com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
//import org.testcontainers.utility.DockerImageName;
////  https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_lambda_code_examples.html
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.lambda.LambdaClient;
//import software.amazon.awssdk.services.lambda.model.LambdaException;
//import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;
//import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
//import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
//import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
//import software.amazon.awssdk.core.SdkBytes;
//import software.amazon.awssdk.core.waiters.WaiterResponse;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.lambda.LambdaClient;
//import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
//import software.amazon.awssdk.services.lambda.model.FunctionCode;
//import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;
//import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
//import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
//import software.amazon.awssdk.services.lambda.model.LambdaException;
//import software.amazon.awssdk.services.lambda.model.Runtime;
//import software.amazon.awssdk.services.lambda.waiters.LambdaWaiter;
//
////import org.json.JSONObject;
//import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
//import software.amazon.awssdk.services.lambda.LambdaClient;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.lambda.model.InvokeRequest;
//import software.amazon.awssdk.core.SdkBytes;
//import software.amazon.awssdk.services.lambda.model.InvokeResponse;
//import software.amazon.awssdk.services.lambda.model.LambdaException;
//
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//
////import com.amazonaws.services.lambda.AWSLambda;
////import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
////import com.amazonaws.services.lambda.model.*;
////import com.amazonaws.services.lambda.model.Runtime;
//
//import java.nio.ByteBuffer;
//import java.nio.charset.StandardCharsets;
//import java.time.Duration;
//import java.util.concurrent.TimeoutException;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.testcontainers.containers.localstack.LocalStackContainer.Service.LAMBDA;
//
//@Testcontainers
//public class LambdaTest2 {
//
//    @Container
//    public static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
//            .withServices(LAMBDA);
//
//    private static LambdaClient  lambdaClient;
//
//    @BeforeAll
//    public static void setup() {
//    	  
//        lambdaClient = LambdaClient.builder()
//        		 .endpointOverride(localStack.getEndpoint())
//        		    .credentialsProvider(
//        		        StaticCredentialsProvider.create(
//        		            AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())
//        		        )
//        		    )
//        		    .region(Region.of(localStack.getRegion())) 
//                .build();
//    }
//
//    @Test
//    public void testSimpleLambdaFunction() throws Exception {
//        // 驗證結果
//        String result = createLambdaFunction(lambdaClient);
//        assertEquals("\"Hello, World\"", result);
//    }
//    
////  https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_lambda_code_examples.html
//    public static String createLambdaFunction(LambdaClient awsLambda) {
//        try {
//            LambdaWaiter waiter = awsLambda.waiter();
//            String functionName = "test-function";
//            
//            // Lambda 函數代碼
//            String functionCode = 
//                "import com.amazonaws.services.lambda.runtime.Context;\n" +
//                "import com.amazonaws.services.lambda.runtime.RequestHandler;\n" +
//                "\n" +
//                "public class Handler implements RequestHandler<String, String> {\n" +
//                "    @Override\n" +
//                "    public String handleRequest(String input, Context context) {\n" +
//                "        return \"Hello, \" + input;\n" +
//                "    }\n" +
//                "}";
//
//            // 編譯並打包 Lambda 函數
//            ByteBuffer zippedCode = LambdaCompilerHelper.compileAndZip("Handler", functionCode);
//            ByteBufferBackedInputStream is = new ByteBufferBackedInputStream(zippedCode);
//            SdkBytes fileToUpload = SdkBytes.fromInputStream(is);
//            FunctionCode code = FunctionCode.builder()
//                    .zipFile(fileToUpload)
//                    .build();
//
//            CreateFunctionRequest functionRequest =  CreateFunctionRequest.builder()
//                    .functionName(functionName)
//                    .runtime(Runtime.JAVA17)
//                    .role("arn:aws:iam::000000000000:role/lambda-role")
//                    .handler("Handler::handleRequest")
//                    .code(code)
//                    .build();
//
//            // Create a Lambda function using a waiter
//            CreateFunctionResponse functionResponse = awsLambda.createFunction(functionRequest);
//            GetFunctionRequest getFunctionRequest = GetFunctionRequest.builder()
//                    .functionName(functionName)
//                    .build();
//            WaiterResponse<GetFunctionResponse> waiterResponse = waiter.waitUntilFunctionExists(getFunctionRequest);
//            waiterResponse.matched().response().ifPresent(System.out::println);
//            return functionResponse.functionArn();
//
//        } catch (LambdaException  | IOException e  ) {
//            System.err.println(e.getMessage());
//            System.exit(1);
//        }
//        return "";
//    }
//    
//} 

