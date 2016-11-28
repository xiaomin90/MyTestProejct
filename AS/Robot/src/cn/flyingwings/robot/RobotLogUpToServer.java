package cn.flyingwings.robot;

import android.content.Context;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;

public class RobotLogUpToServer {
	private OSS oss;
    // 运行sample前需要配置以下字段为有效的值
    private static final String endpoint = "http://oss-cn-hangzhou.aliyuncs.com";
    private static final String accessKeyId =  null;
    private static final String accessKeySecret = null;
    private static final String uploadFilePath  = null;
    private static final String testBucket   = "<bucket_name>";
    private static final String uploadObject = "sampleObject";
    private Context context = null;
    private String securityToken = null;
    
    public void upload() {
    	OSSStsTokenCredentialProvider ossststokenProvider = new OSSStsTokenCredentialProvider(accessKeyId, accessKeySecret,securityToken);
	    //OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret);
	    ClientConfiguration conf = new ClientConfiguration();
	    conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
	    conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
	    conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
	    conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
	    OSSLog.enableLog();
//	    oss = new OSSClient(context , endpoint, credentialProvider, conf);
	    oss = new OSSClient(context , endpoint, ossststokenProvider, conf);
	    new Thread(new Runnable() {
            @Override
            public void run() {
                new PutObjectSamples(oss, testBucket, uploadObject, uploadFilePath).asyncPutObjectFromLocalFile();
            }
        }).start();
    }
}
