/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.osgi.jmx;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.kernel.utils.CarbonServerInfo;
import org.wso2.carbon.osgi.utils.OSGiTestUtils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * JMX test case
 *
 * @since 5.1.0
 */
@Listeners(org.ops4j.pax.exam.testng.listener.PaxExam.class)
@ExamReactorStrategy(org.ops4j.pax.exam.spi.reactors.PerClass.class)
public class JMXOSGiTest {

    private static final Logger logger = LoggerFactory.getLogger(JMXOSGiTest.class);

    @Configuration
    public Option[] createConfiguration() {
        OSGiTestUtils.setupOSGiTestEnvironment();
        copyCarbonYAML();
        return OSGiTestUtils.getDefaultPaxOptions();
    }

//    @Inject
//    private CarbonServerInfo carbonServerInfo;

    @Test
    public void testMBeanRegistration() throws Exception {
        JMXSample test = new JMXSample();
        ObjectName mbeanName = new ObjectName("org.wso2.carbon.osgi.jmx:type=JMXSample");
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(test, mbeanName);

        Assert.assertTrue(mBeanServer.isRegistered(mbeanName), "MBean is not registered");
    }

    @Test(dependsOnMethods = {"testMBeanRegistration"})
    public void testAccessMBean() throws Exception {

        String hostname = InetAddress.getLocalHost().getHostAddress();

        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://127.0.0.1:11111/jndi/rmi://127.0.0.1:9999/jmxrmi");
//        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://" + hostname + ":11111/jndi/rmi://" + hostname + ":9999/jmxrmi");
        Map<String, Object> environment = new HashMap<>();
        String[] credentials = {"admin", "password"};
        environment.put(JMXConnector.CREDENTIALS, credentials);
        JMXConnector jmxc = JMXConnectorFactory.connect(url, environment);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        ObjectName mbeanName = new ObjectName("org.wso2.carbon.osgi.jmx:type=JMXSample");
        JMXSampleMBean mbeanProxy = JMX.newMBeanProxy(mbsc, mbeanName, JMXSampleMBean.class, true);

        Assert.assertEquals(mbeanProxy.getCount(), 0, "Count is not zero");

        mbeanProxy.setCount(500);
        Assert.assertEquals(mbeanProxy.getCount(), 500, "Count is not 500");

        mbeanProxy.reset();
        Assert.assertEquals(mbeanProxy.getCount(), 0, "Count is not reset");
    }

    /**
     * Replace the existing carbon.yml file with populated carbon.yml file.
     */
    private static void copyCarbonYAML() {
        Path carbonYmlFilePath;

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = Paths.get(".").toString();
        }
        try {
            carbonYmlFilePath = Paths.get(basedir, "src", "test", "resources", "runtime", "carbon.yml");
            Files.copy(carbonYmlFilePath, Paths.get(System.getProperty("carbon.home"), "conf", "carbon.yml"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Unable to copy the carbon.yml file", e);
        }
    }
}
