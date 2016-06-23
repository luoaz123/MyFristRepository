/*
 * Copyright 2012 The Sainfy Open Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sainfy.jxvs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.sainfy.jxvs.utils.FtpUtil;
import org.sainfy.jxvs.utils.JXVSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * 
 * @author luoaz <luoanzhu@gmail.com>
 */
public class JXVSServer {

    private static final Logger LOG = LoggerFactory.getLogger(JXVSServer.class);
    private static final String PATH = "jxvs.properties";
//    private AtomicBoolean runing = new AtomicBoolean();
//    private static JXVSServer server;
    
	//private static final String CONFIG_FILE_NAME = "config.properties";
	private static final String DEFAULT_DRIVER = "org.postgresql.Driver";
	private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/postgres";
	private static final String SYSTEM_TYPE_ONLINELEARN = "onlineLearn";//在线学习接口
	private static final String SYSTEM_TYPE_EMPREWARDS = "empRewards"; //员工奖惩接口
	private static final String SYSTEM_TYPE_DYNAMIC = "dynamic"; //取动态配置
	private static final String SQL_DUTYDEAL = "select * from dutydeal";
	private static final String SQL_DUTYMAN = "select * from dutyman";
	private static final String SQL_SYSORGANIZATION = "select * from sysorganization";
	private String contextPath = null;//上下文目录
	private String tempPath = null;//临时文件存放目录
	private String tempChildPath = null; //临时文件子目录
	private String ftpFilePath = null;
	private String systemType = null;
	private FTPClient client;
	private String ftpType = "FTP";
	private ChannelSftp sftp = null;
	private String dateStr = null; //日期字符串
	private String dynamicTables = null;

    public JXVSServer() {
//        if (runing.get()) {
//            throw new IllegalStateException("A JXVSServer is already running");
//        }
        //server = this;
        start();
//        runing.set(true);
    }

//    public static JXVSServer getInstance() {
//        return server;
//    }

    private void start() {
        initialize();
        
        
        tempPath = System.getProperty("temp.file.path", contextPath);
		tempChildPath = tempPath + "/tempFile";
		File path = new File(tempChildPath);
		if (!path.exists()) {
			path.mkdir();
		}
		systemType = System.getProperty("system.type", "empRewards");
		ftpFilePath = System.getProperty("ftp.file.path", "/root");
		ftpType = System.getProperty("ftp.type", "SFTP");
		//取数据模板
		dynamicTables = System.getProperty("data.crawl.tables.pattern");
		
		dateStr = getFormatDate("yyyyMMdd");
		
		// 查询数据，并存入临时文件
		queryData2File();
		System.out.println("Data file create success!");
		
		// 上传文件
		uploadHandler();
		System.out.println("Data file upload success!");
		System.out.println("DataCrawl Server Closed!");
		System.exit(0);
    }

    public void initialize() {
        try {
            loadProperties(PATH);
        } catch (IOException ex) {
            LOG.error("load config file fail : " + ex.getMessage());
        }
    }

    /**
     * 读取系统配置文件。
     * 
     * @param path 配置文件路径
     * @throws IOException
     */
    public void loadProperties(String path) throws IOException {
        Properties p = new Properties();
        InputStream in = getClass().getClassLoader().getResourceAsStream(path);
        if (in == null) {
            System.out.println("CLASSPATH don't not exist " + path + " resource");
            return;
        }
        p.load(in);
        in.close();
        System.getProperties().putAll(p);
    }
    
    /**
	 * 上传逻辑处理
	 */
	private void uploadHandler() {
		if (ftpType.equalsIgnoreCase("SFTP") && loginSFTP()) {
			put(tempPath + "/EAS_" + dateStr + ".tar", ftpFilePath + "/EAS_" + dateStr + ".tar");
			put(tempPath + "/EAS_END_" + dateStr, ftpFilePath + "/EAS_END_" + dateStr);
			disconnect();
		} else if (ftpType.equalsIgnoreCase("FTP")) {
		    ftpTrans();
		}
		
		if (JXVSUtils.getBoolean("temp.file.delete")) {
		    deleteFile(tempPath + "/EAS_" + dateStr + ".tar");
		    deleteFile(tempPath + "/EAS_END_" + dateStr);
		}
	}
	
	private void ftpTrans() {
	    String hostname = System.getProperty("ftp.address", "127.0.0.1");
        int port = Integer.parseInt(System.getProperty("ftp.port", "21"));
        String username = System.getProperty("ftp.username", "root");
        String password = System.getProperty("ftp.password", "root");
	    try {
	        FileInputStream in = new FileInputStream(new File(tempPath + "/EAS_" + dateStr + ".tar"));
	        
	        FtpUtil ftp = new FtpUtil(hostname, port, username, password, "/");
	        ftp.ftpLogin();
	        ftp.uploadFile(in, ftpFilePath, "EAS_" + dateStr + ".tar");
	        ftp.uploadFile(in, ftpFilePath, "EAS_END_" + dateStr);
        } catch (Exception e) {
            LOG.error("文件上传失败", e);
            e.printStackTrace();
        } finally {
        }
	}
	
	private void put(String localFile, String remoteFile) {
        try {
            File file = new File(localFile);
            
            if(file.isFile()){
                System.out.println("localFile : " + file.getAbsolutePath());
                System.out.println("remotePath:" + remoteFile);
                File rfile = new File(remoteFile);
                String rpath = rfile.getParent();
                rpath = rpath.replace("\\", "/");
                try {
                    createDir(rpath, sftp);
                } catch (Exception e) {
                    System.out.println("*******create path failed" + rpath);
                }

                this.sftp.put(new FileInputStream(file), file.getName());
                System.out.println("=========upload down for " + localFile);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        }
	}
	
	/**
     * create Directory
     * @param filepath
     * @param sftp
     */
    private void createDir(String filepath, ChannelSftp sftp){
        boolean bcreated = false;
        boolean bparent = false;
        File file = new File(filepath);
        String ppath = file.getParent();
        ppath = ppath.replace("\\", "/");
        try {
            this.sftp.cd(ppath);
            bparent = true;
        } catch (SftpException e1) {
            bparent = false;
        }
        try {
            if(bparent){
                try {
                    this.sftp.cd(filepath);
                    bcreated = true;
                } catch (Exception e) {
                    bcreated = false;
                }
                if(!bcreated){
                    this.sftp.mkdir(filepath);
                    bcreated = true;
                }
                return;
            }else{
                createDir(ppath,sftp);
                this.sftp.cd(ppath);
                this.sftp.mkdir(filepath);
            }
        } catch (SftpException e) {
            System.out.println("mkdir failed :" + filepath);
            e.printStackTrace();
        }
        
        try {
            this.sftp.cd(filepath);
        } catch (SftpException e) {
            e.printStackTrace();
            System.out.println("can not cd into :" + filepath);
        }
        
    }
	
	/**
	 * 登陆SFTP服务器
	 * @return
	 * @throws JSchException
	 */
	private boolean loginSFTP() {
		String hostname = System.getProperty("ftp.address", "127.0.0.1");
		int port = Integer.parseInt(System.getProperty("ftp.port", "21"));
		String username = System.getProperty("ftp.username", "root");
		String password = System.getProperty("ftp.password", "root");
		
		JSch jsch = new JSch();
        try {
			jsch.getSession(username, hostname, port);
			Session sshSession = jsch.getSession(username, hostname, port);
			System.out.println("Session created.");
			sshSession.setPassword(password);
			Properties sshConfig = new Properties();
			sshConfig.put("StrictHostKeyChecking", "no");
			sshSession.setConfig(sshConfig);
			sshSession.connect();
			System.out.println("Session connected.");
			System.out.println("Opening Channel.");
			Channel channel = sshSession.openChannel("sftp");
			channel.connect();
			sftp = (ChannelSftp) channel;
			System.out.println("Connected to " + hostname + ".");
			return true;
		} catch (JSchException e) {
			e.printStackTrace();
		}
        return false;
	}
	
    /**
     * Disconnect with server
     */
    public void disconnect() {
		if (this.sftp != null) {
			if (this.sftp.isConnected()) {
				this.sftp.disconnect();
			} else if (this.sftp.isClosed()) {
				System.out.println("sftp is closed already");
			}
		}
    }
	

	/**
	 * 查询数据，并写入文件
	 * 
	 * @author LUOAZ
	 * @date 2014年9月9日
	 */
	private void queryData2File() {
		Connection conn = getConnection();
		Pattern p = Pattern.compile("\\\r|\n");
		if (conn == null) {
			LOG.error("不能连接到数据库");
			throw new RuntimeException("不能连接到数据库");
		}
		try {
			if (systemType.equalsIgnoreCase(SYSTEM_TYPE_EMPREWARDS)) {
				int dutydealCount = 0;
				int dutymanCount = 0;
				int sysorganizationCount = 0;
				//员工奖惩系统数据提取
				FileOutputStream outputByDeal = new FileOutputStream(new File(tempChildPath + "/EAS_DUTYDEAL_"+dateStr+".txt"));
				BufferedWriter bwByDeal = new BufferedWriter(new OutputStreamWriter(outputByDeal));
				PreparedStatement psByDeal = conn.prepareStatement(SQL_DUTYDEAL);
				ResultSet rsByDeal = psByDeal.executeQuery();
				while(rsByDeal.next()) {
					StringBuilder sb = new StringBuilder();
					sb.append(nullToStr(rsByDeal.getString("id"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("cash"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("cltime"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("code"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("dtime"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("dutymanid"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("freedtime"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("handlequalityid"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("handletypeid"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("inputorgid"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("inputuserid"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("inspectid"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("orgid"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("outorg"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("problemid"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("remark"))).append("@|@");
					sb.append(nullToStr(rsByDeal.getString("userid")));
					bwByDeal.write(sb.toString());
					bwByDeal.newLine();
					dutydealCount++;
				}
				bwByDeal.flush();
				bwByDeal.close();
				
				FileOutputStream outputByMan = new FileOutputStream(new File(tempChildPath + "/EAS_DUTYMAN_" + dateStr + ".txt"));
				BufferedWriter bwByMan = new BufferedWriter(new OutputStreamWriter(outputByMan));
				PreparedStatement psByMan = conn.prepareStatement(SQL_DUTYMAN);
				ResultSet rsByMan = psByMan.executeQuery();
				while(rsByMan.next()) {
					StringBuilder sb = new StringBuilder();
					sb.append(nullToStr(rsByMan.getString("id"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("content"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("dtime"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("inputorgid"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("inputuserid"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("inspectid"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("job"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("orgid"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("problemid"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("remark"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("state"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("type"))).append("@|@");
					sb.append(nullToStr(rsByMan.getString("userid")));
					bwByMan.write(sb.toString());
					bwByMan.newLine();
					dutymanCount++;
				}
				bwByMan.flush();
				bwByMan.close();
				
				FileOutputStream outputByOrg = new FileOutputStream(new File(tempChildPath + "/EAS_SYSORGANIZATION_" + dateStr + ".txt"));
				BufferedWriter bwByOrg = new BufferedWriter(new OutputStreamWriter(outputByOrg));
                PreparedStatement psByOrg = conn.prepareStatement(SQL_SYSORGANIZATION);
                ResultSet rsByOrg = psByOrg.executeQuery();
                while(rsByOrg.next()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(nullToStr(rsByOrg.getString("id"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("name"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("nodeimage"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("orderno"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("orgtypecode"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("orgtypename"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("organizationnumber"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("remark"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("shortname"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("sysorganization_id"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("orgflagcode"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("sysflag"))).append("@|@");
                    sb.append(nullToStr(rsByOrg.getString("syncdate")));
                    bwByOrg.write(sb.toString());
                    bwByOrg.newLine();
                    sysorganizationCount++;
                }
                bwByOrg.flush();
                bwByOrg.close();
				
				
				// 生成CHECK文件
				FileOutputStream outputByCheck = new FileOutputStream(new File(tempChildPath + "/EAS_CHECK_"+dateStr+".txt"));
				BufferedWriter bwByCheck = new BufferedWriter(new OutputStreamWriter(outputByCheck));
				bwByCheck.write(toCheckStr("dutydeal",dutydealCount));
				bwByCheck.newLine();
				bwByCheck.write(toCheckStr("dutyman", dutymanCount));
				bwByCheck.newLine();
				bwByCheck.write(toCheckStr("sysorganization", sysorganizationCount));
				bwByCheck.flush();
				bwByCheck.close();
				
				//封装TAR包
				fileTar(tempChildPath, tempPath + "/EAS_" + dateStr + ".tar", dateStr);
			    File endFile = new File(tempPath + "/EAS_END_"+dateStr);
			    if (!endFile.exists()) {
			    	endFile.createNewFile();
			    }
			} else if (systemType.equalsIgnoreCase(SYSTEM_TYPE_ONLINELEARN)) {
				//在线学习接口数据提取
			} else if (systemType.equalsIgnoreCase(SYSTEM_TYPE_DYNAMIC)) {
			    //根据动态配置参数生成ODS接口数据
			    if (dynamicTables == null || dynamicTables.trim().equals("")) {
			        LOG.error("data.crawl.tables.pattern 配置不能为空");
			        System.exit(0);
			    }
			    
			    JSONObject json = null;
			    try {
			        json = JSONObject.parseObject(dynamicTables);
                } catch (Exception e) {
                    LOG.error("data.crawl.tables.pattern 配置格式不合法");
                }
			    List<Integer> rowcountList = new ArrayList<Integer>();
			    
                FileOutputStream outputByCheck = new FileOutputStream(new File(tempChildPath + "/EAS_CHECK_"+dateStr+".txt"));
                BufferedWriter bwByCheck = new BufferedWriter(new OutputStreamWriter(outputByCheck));
                
			    for (String key : json.keySet()) {
		            StringBuilder sqlStr = new StringBuilder();
		            sqlStr.append("select * from ").append(key);
		            JSONObject table = json.getJSONObject(key);
		            String prefix = table.getString("PREFIX");
		            JSONArray fields = table.getJSONArray("FIELDS");
		            
		            FileOutputStream outputByOrg = new FileOutputStream(new File(tempChildPath + "/EAS_" + prefix + "_" + dateStr + ".txt"));
	                BufferedWriter bwByOrg = new BufferedWriter(new OutputStreamWriter(outputByOrg));
	                PreparedStatement psByOrg = conn.prepareStatement(sqlStr.toString());
	                ResultSet rsByOrg = psByOrg.executeQuery();
	                int rowcount = 0;
	                while(rsByOrg.next()) {
	                    StringBuilder sb = new StringBuilder();
	                    for (int index = 0; index < fields.size(); index++) {
	                        if (index == fields.size() - 1) {
	                            sb.append(nullToStr(rsByOrg.getString(fields.getString(index))));
	                        } else {
	                            sb.append(nullToStr(rsByOrg.getString(fields.getString(index)))).append("@|@");
	                        }
                        }
	                    bwByOrg.write(sb.toString());
	                    bwByOrg.newLine();
	                    rowcount ++;
	                }
	                bwByOrg.flush();
	                bwByOrg.close();
	                rowcountList.add(rowcount);
	                
	                //生成CHECK文件
	                bwByCheck.write(toCheckStr(prefix,rowcount));
                    bwByCheck.newLine();
		        }
			    bwByCheck.flush();
			    bwByCheck.close();
			    

                //封装TAR包
                fileTar(tempChildPath, tempPath + "/EAS_" + dateStr + ".tar", dateStr);
                File endFile = new File(tempPath + "/EAS_END_"+dateStr);
                if (!endFile.exists()) {
                    endFile.createNewFile();
                }
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			LOG.error("sql异常>>{}", e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private String nullToStr(String str) {
		return str == null ? "" : str;
	}

	/**
	 * 生成CHECK记录
	 * 
	 * @author LUOAZ
	 * @date 2014年9月9日
	 * @param tableName
	 * @param count
	 */
	private String toCheckStr(String tableName, int count) {
		StringBuilder sb = new StringBuilder();
		sb.append(dateStr).append("@|@");
		sb.append(tableName).append("@|@");
		sb.append("0").append("@|@");
		sb.append("0").append("@|@");
		sb.append(count).append("@|@");
		sb.append("0");
		return sb.toString();
	}

	/**
	 * 获取数据库连接对象
	 * 
	 * @return 数据库连接成功时返回{@link Connection}, 否则返回null
	 */
	private Connection getConnection() {
		Connection conn = null;
		try {
			Class.forName(System.getProperty("database.driver.class", DEFAULT_DRIVER));
			String url = System.getProperty("database.url", DEFAULT_URL);
			String username = System.getProperty("database.username");
			String password = System.getProperty("database.password");
			conn = DriverManager.getConnection(url, username, password);
		} catch (ClassNotFoundException e) {
			LOG.error("数据库驱动加载失败", e);
		} catch (SQLException e) {
			LOG.error("", e);
		}
		return conn;
	}

	
	/**
	 * 压缩文件为TAR包。并删除之前文件
	 * 
	 * @author LUOAZ
	 * @param filesPath
	 *            文件路径
	 * @param tarPath
	 *            tar 路径
	 * @param suffix TODO
	 */
	public static void fileTar(String filesPath, String tarPath, String suffix) {
		List<String> list = file2tar(filesPath, tarPath, suffix + ".txt");

		/*// 循环遍历删除备份日志文件
		if (list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				deleteFile(list.get(i)); // 备份后进行日志文件的删除
			}
		}*/
		//删除一周前的备份日志文件
		Calendar now = Calendar.getInstance();
		now.add(Calendar.DAY_OF_MONTH, -7);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String dateStr = sdf.format(now.getTime());
		String oldSuffix = dateStr + ".txt";
		File fileDirectory = new File(filesPath);
		if (fileDirectory.isDirectory()) {
		    int length = fileDirectory.listFiles().length;
		    File[] files = fileDirectory.listFiles();
		    
		    for (int i = 0; i < length; i++) {
                String filename = fileDirectory.getPath() + File.separator + files[i].getName();
                if (filename.endsWith(oldSuffix)) {
                    deleteFile(filename);
                }
            }
        }
	}

	/**
	 * TAR 文件压缩
	 * 
	 * @param filesPath 文件路径
	 * @param tarPath TAR路径
	 * @param excludeSuffix 不包含的文件
	 * @return
	 */
	private static List<String> file2tar(String filesPath, String tarPath, String excludeSuffix) {

		List<String> list = new ArrayList<String>();

		File fileDirectory = new File(filesPath);
		if (!fileDirectory.isDirectory()) {
			return null;
		}
		int length = fileDirectory.listFiles().length;
		File[] files = fileDirectory.listFiles();
		TarArchiveEntry tarEn = null;
		TarArchiveOutputStream tout = null;
		FileOutputStream fout = null;
		FileInputStream in = null;
		try {
			File tarFile = new File(tarPath);
			if (!tarFile.exists()) {
				tarFile.createNewFile();
			}
			fout = new FileOutputStream(tarFile);
			tout = new TarArchiveOutputStream(fout);
			// 循环压缩文本文件到tar格式文件
			for (int i = 0; i < length; i++) {
				String filename = fileDirectory.getPath() + File.separator + files[i].getName();
				if (!filename.endsWith(excludeSuffix)) {
                    continue;//
                }
				in = new FileInputStream(filename);
				tarEn = new TarArchiveEntry(files[i]);
				tarEn.setName(files[i].getName());
				tout.putArchiveEntry(tarEn);
				byte[] B_ARRAY = new byte[1024];
				int num;
				while ((num = in.read(B_ARRAY)) != -1) {
					tout.write(B_ARRAY, 0, num);
				}

				tout.closeArchiveEntry();
				in.close();
				list.add(filename);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				tout.close();
				fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	/** 
     * 删除单个文件 
     * @param fileName 要删除的文件的文件名 
     * @return boolean 单个文件删除成功返回true，否则返回false 
     */ 
    public static boolean deleteFile(String fileName) { 
        File file = new File(fileName); 
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除 
        if ( file.exists() && file.isFile() ) { 
            if ( file.delete() ) { 
                return true; 
            } 
            else { 
                return false; 
            } 
        } 
        else { 
            return false; 
        } 
    } 
    
    /**
     * 获取一个指定格式的日期字符串
     * 
     * @author LUOAZ
     * @date 2014年9月9日
     * @param pattern 日期格式， 默认yyyyMMdd
     * @return 返回一个被格式化为字符串的日期
     */
    public static String getFormatDate(String pattern) {
    	if (pattern == null || pattern.trim().equals("")) {
    		pattern = "yyyyMMdd";
    	}
    	SimpleDateFormat sdf = new SimpleDateFormat(pattern);
    	return sdf.format(new Date());
    }

}
