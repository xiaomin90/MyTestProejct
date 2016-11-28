package cn.flyingwings.robot.utils;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import cn.flyingwings.robot.RobotDebug;

public class MD5Util {
	private static String TAG = "MD5Util";
	private static final char ARR_CHAR_MD5[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	
	/**
	 * 功能说明：计算字符串MD5值。
	 * @param str
	 * @return
	 */
	public static String md5(String str) {
	
		// 用于加密的字符
        try {
        	MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] btInput = str.getBytes("UTF-8");
            
            messageDigest.update(btInput);
            byte[] btMd5 = messageDigest.digest();

            return byteArray2Str(btMd5);
            
        } catch (NoSuchAlgorithmException e) {
        	RobotDebug.d(TAG,"MD5加密字符串失败。");
        } catch (UnsupportedEncodingException e) {
        	RobotDebug.d(TAG,"MD5加密字符串失败。");
		}
        
        return null;
	}
	
	/**
	 * 功能说明：把字节数组转换成字符串。
	 * @param btMd5
	 * @return
	 */
	private static String byteArray2Str(byte[] btMd5) {
		
		// 把密文转换成十六进制的字符串形式
        int md5Len = btMd5.length;
        
        char arrChar[] = new char[md5Len * 2];
        int k = 0;
        for (int i = 0; i < md5Len; i++) { // i = 0
            byte bt = btMd5[i]; // 95
            arrChar[k++] = ARR_CHAR_MD5[bt >>> 4 & 0xf]; // 5
            arrChar[k++] = ARR_CHAR_MD5[bt & 0xf]; // F
        }
        return new String(arrChar);
	}
	
	/**
	 * 功能说明：计算文件MD5值。
	 * @param inputFile
	 * @return
	 * @throws IOException
	 */
	public static String fileMD5(String inputFile) {
		long startTime = System.currentTimeMillis();
		
		int bufferSize = 256 * 1024; //缓冲区大小
		
		FileInputStream fileInputStream = null;
		DigestInputStream digestInputStream = null;
		try {
			// 拿到一个MD5转换器
			MessageDigest messageDigest = null;
			try {
				messageDigest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //SHA-1、SHA-256
			
			// 使用DigestInputStream
			fileInputStream = new FileInputStream(inputFile);
			digestInputStream = new DigestInputStream(fileInputStream, messageDigest);
			
			// read的过程中进行MD5处理，直到读完文件
			byte[] buffer = new byte[bufferSize];
			while (digestInputStream.read(buffer) > 0)
				;
			
			// 获取最终的MessageDigest
			messageDigest = digestInputStream.getMessageDigest();
			// 拿到结果，也是字节数组，包含16个元素
			byte[] btResult = messageDigest.digest();
			
			// 把字节数组转换成字符串
			String md5Str = byteArray2Str(btResult);
			
			RobotDebug.d(TAG,"计算文件MD5值，耗时: " + (System.currentTimeMillis() - startTime) + " ms, 文件: " + inputFile + " , md5: " + md5Str);
			
			return md5Str;
			
		} catch (IOException e) {
			
			return null;
		} finally {
			try {
				digestInputStream.close();
			} catch (Exception e) {
			}
			
			try {
				fileInputStream.close();
			} catch (Exception e) {
			}
		}
	}
}
