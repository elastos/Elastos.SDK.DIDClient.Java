package org.elastos.service.ela;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class OutSideConfig {
    private static Properties properties;
    private static Logger logger = LoggerFactory.getLogger(OutSideConfig.class);

    public static boolean readOutSide() {
        try {
            properties = new Properties();
            //读取系统外配置文件 (即Jar包外文件) --- 外部工程引用该Jar包时需要在工程下创建config目录存放配置文件
            String filePath = System.getProperty("user.dir")
                    + "/conf/ela.did.properties";
            InputStream in = new BufferedInputStream(new FileInputStream(filePath));
            properties.load(in);
        } catch (IOException e) {
            logger.info("Ela DID无法读取外部配置文件 ela.did.properties 信息");
            return false;
        }
        return true;
    }

    public static String getObject(String prepKey) {
        return properties.getProperty(prepKey);
    }

}
