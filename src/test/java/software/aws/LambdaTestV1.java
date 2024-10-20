package software.aws;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.lambda.model.Runtime;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.LAMBDA;

@Testcontainers
public class LambdaTestV1 {

    @Container
    public static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(LAMBDA);

    private static AWSLambda lambdaClient;

    @BeforeAll
    public static void setup() {
        lambdaClient = AWSLambdaClientBuilder.standard()
                .withEndpointConfiguration(localStack.getEndpointConfiguration(LAMBDA))
                .withCredentials(localStack.getDefaultCredentialsProvider())
                .build();
    }

    @Test
    public void testSimpleLambdaFunction() throws Exception {
        String functionName = "test-function";
        
        // Lambda 函數代碼
        String functionCode = 
            "import com.amazonaws.services.lambda.runtime.Context;\n" +
            "import com.amazonaws.services.lambda.runtime.RequestHandler;\n" +
            "\n" +
            "public class Handler implements RequestHandler<String, String> {\n" +
            "    @Override\n" +
            "    public String handleRequest(String input, Context context) {\n" +
            "        return \"Hello, \" + input;\n" +
            "    }\n" +
            "}";

        // 編譯並打包 Lambda 函數
        ByteBuffer zippedCode = LambdaCompilerHelper.compileAndZip("Handler", functionCode);

        // 創建函數
        CreateFunctionRequest createFunctionRequest = new CreateFunctionRequest()
                .withFunctionName(functionName)
                .withRuntime(Runtime.Java17)
                .withRole("arn:aws:iam::000000000000:role/lambda-role")
                .withHandler("Handler::handleRequest")
                .withCode(new FunctionCode().withZipFile(zippedCode));

        lambdaClient.createFunction(createFunctionRequest);

        // 等待函數變為活動狀態
        waitForFunctionActive(functionName);

        // 調用函數
        InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName(functionName)
                .withPayload("\"World\"");

        InvokeResult invokeResult = lambdaClient.invoke(invokeRequest);

        // 驗證結果
        String result = new String(invokeResult.getPayload().array(), StandardCharsets.UTF_8);
        assertEquals("\"Hello, World\"", result);
    }

    private void waitForFunctionActive(String functionName) throws TimeoutException, InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeout = Duration.ofMinutes(2).toMillis();  // 設置2分鐘超時
        
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                GetFunctionRequest getFunctionRequest = new GetFunctionRequest().withFunctionName(functionName);
                GetFunctionResult getFunctionResult = lambdaClient.getFunction(getFunctionRequest);
                
                if ("Active".equals(getFunctionResult.getConfiguration().getState())) {
                    return;  // 函數已經處於活動狀態
                }
            } catch (ResourceNotFoundException e) {
                // 函數尚未創建，繼續等待
            }
            
            Thread.sleep(5000);  // 等待5秒後再次檢查
        }
        
        throw new TimeoutException("Function did not become active within the timeout period");
    }
} 