package org;

import java.nio.charset.Charset;

public interface Constants {

    public static final String JXVS_HOME = "jxvsHome";

    public static final String JXVS_LIB_DIR = "jxvs.lib.dir";

    public static final String JXVS_LOGS_DIR = "jxvs.logs.dir";

    public static final String JXVS_DEVMODE = "jxvs.devMode";

    public static final String ENCODING = "UTF-8";

    public static final Charset CHARSET = Charset.forName(ENCODING);
    
    public static final String GET_NODE_CAPABILITY_COMMAND = "capability";
    
    public static final String GET_NODE_POOLNUM_COMMAND = "poolNum";
    
    /*节点扫描周期*/
    public static final String NODE_FIND_INTERVAL = "node.find.interval";

    /*后台管理开关*/
    public static final String BACK_MANAGER_ENABLED = "back.manager.enabled";
    
    /*后台管理端口*/
    public static final String ROUTE_MANAGER_PORT = "route.manager.port";
    
    /*系统信息统计间隔*/
    public static final String OS_INFO_NOUN_SPACE = "os.info.noun.space";
    
    /*远程节点管理端口*/
    public static final String REMOTE_NODE_MANAGER_PORT = "remote.node.manager.port";
    
    /*节点列表*/
    public static final String NODELIST = "nodeList";
    
    /*节点列表分隔符*/
    public static final String NODELIST_SEPARATOR = ",";
    
    /*节点信息统计间隔*/
    public static final String NODE_INFO_NOUN_SPACE = "node.info.noun.space";

    /*节点选择算法*/
    public static final String ROUTE_OUTGIVING_MODE = "route.outgiving.mode";
    
    /*IM域名*/
    public static final String XMPP_DOMAIN = "xmpp.domain";
    
    /*支持的第三方列表*/
    public static final String XMPP_GATEWAY_LIST = "xmpp.gateway.list";
    
    /*IMP域名*/
    public static final String XMPP_COOPERATION_DOMAIN = "xmpp.cooperation.domain";
    
    
    /*命名空间：登录*/
    public static final String RCS_REGISTER_USERNAME = "rcs:register:username";
    
    /*命名空间：登出*/
    public static final String RCS_REGISTER = "rcs:register";

    public static final String XMPP_AUTH_IP = "xmpp.auth.ip";
    
    public static final String XMPP_IGNORE_PACKET = "xmpp.ignore.packet";
    /**
	 * The 'XMPP Ping' namespace
	 * 
	 * @see <a href="http://xmpp.org/extensions/xep-0199.html">XEP-0199</a>
	 */
	public static final String NAMESPACE_XMPP_PING = "urn:xmpp:ping";

    
}
