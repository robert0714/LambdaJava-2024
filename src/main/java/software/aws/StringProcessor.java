package software.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;


import org.apache.commons.lang3.StringUtils;

public class StringProcessor implements RequestHandler<String, String> {
    
    @Override
    public String handleRequest(String input, Context context) {
        // 使用Apache Commons Lang處理字串
        if (StringUtils.isEmpty(input)) {
            return "輸入不能為空";
        }
        
        // 轉換成大寫並反轉字串
        String upperCase = StringUtils.upperCase(input);
        String reversed = StringUtils.reverse(upperCase);
        
        return reversed;
//    	return input ;
    }
}