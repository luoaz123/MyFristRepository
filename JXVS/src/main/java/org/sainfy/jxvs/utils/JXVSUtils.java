package org.sainfy.jxvs.utils;

import org.Constants;

public class JXVSUtils {
    
    private static final String DEFAULT_XMPP_GATEWAY_LIST = "msn,google";
    private static final String DEFAULT_DOMAIN = "connector.com";
    private static final String DEFAULT_NODELIST = "127.0.0.1:5269:0086:1";
    private static final int DEFAULT_REMOTE_NODE_MANAGER_PORT = 7869;
    private static final int DEFAULT_NODE_INFO_NOUN_SPACE = 1000;
    private static final String DEFAULT_ROUTE_OUTGIVING_MODE = "default";
    
    /**
     * Returns <code>true</code> if and only if the system property 
     * named by the argument exists and is equal to the string 
     * {@code "true"}. (Beginning with version 1.0.2 of the 
     * Java<small><sup>TM</sup></small> platform, the test of 
     * this string is case insensitive.) A system property is accessible 
     * through <code>getProperty</code>, a method defined by the 
     * <code>System</code> class.
     * <p>
     * If there is no property with the specified name, or if the specified
     * name is empty or null, then <code>false</code> is returned.
     *
     * @param   name   the system property name.
     * @return  the <code>boolean</code> value of the system property.
     * @see     java.lang.System#getProperty(java.lang.String)
     * @see     java.lang.System#getProperty(java.lang.String, java.lang.String)
     */
    public static boolean getBoolean(String name) {
        boolean result = true;
        try {
            result = toBoolean(System.getProperty(name, "true"));
        } catch (IllegalArgumentException e) {
        } catch (NullPointerException e) {
        }
        return result;
    }
    
    private static boolean toBoolean(String name) { 
        return ((name != null) && name.equalsIgnoreCase("true"));
    }
    
    /**
     * 获取支持的第三方列表
     * 
     * @return 返回gateway列表
     */
    public static String[] getGatewayList() {
        String gateway = System.getProperty(Constants.XMPP_GATEWAY_LIST, DEFAULT_XMPP_GATEWAY_LIST);
        return gateway.split(",");
    }
    
    /**
     * 获取Connector域名
     * @return connector域名
     */
    public static String getConnectorDomain() {
        return System.getProperty(Constants.XMPP_DOMAIN, DEFAULT_DOMAIN);
    }
    
    public static String getCooperationDomain() {
        return System.getProperty(Constants.XMPP_COOPERATION_DOMAIN, "rcs.com");
    }
    
    /**
     * 获取节点列表
     * 
     * @return 返回节点列表
     */
    public static String[] getNodeList() {
        String nodeList = System.getProperty(Constants.NODELIST, DEFAULT_NODELIST);
        return nodeList.split(Constants.NODELIST_SEPARATOR);
    }
    
    /**
     * 远程节点管理端口
     * @return 节点管理端口
     */
    public static int getRemoteNodeManagerPort() {
        return Integer.getInteger(Constants.REMOTE_NODE_MANAGER_PORT, DEFAULT_REMOTE_NODE_MANAGER_PORT);
    }
    
    /**
     * 节点信息统计周期
     * @return 周期
     */
    public static int getNodeInfoNounSpace() {
        return Integer.getInteger(Constants.NODE_INFO_NOUN_SPACE, DEFAULT_NODE_INFO_NOUN_SPACE);
    }
    
    /**
     * 节点选择算法
     * @return 算法
     */
    public static String getRouteOutgivingMode() {
        return System.getProperty(Constants.ROUTE_OUTGIVING_MODE, DEFAULT_ROUTE_OUTGIVING_MODE);
    }
    
    /**
     * 获取节点扫描周期
     * @return 扫描周期
     */
    public static int getNodeFindInterval() {
        return Integer.getInteger(Constants.NODE_FIND_INTERVAL, 15000);
    }
}
