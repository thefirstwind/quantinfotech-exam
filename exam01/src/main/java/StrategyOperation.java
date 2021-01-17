import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qit.lp.automation.function.test.util.CheckWebResponseResult;
import com.qit.lp.automation.function.test.util.ConfigUtil;
import com.qit.lp.automation.function.test.util.Context;
import com.qit.lp.automation.function.test.util.HttpClientResult;
import com.qit.lp.automation.function.test.util.HttpClientUtil2;

import javax.print.URIException;

/**
 * 策略操作：登录、登出、启动、停止、更新、添加报价、查找
 *
 * 具体业务应该是证券相关的策略操作相关的业务。
 * 业务流程应该是 登陆 -> 启动 -> 更新/添加报价/查找 -> 停止 -> 登出
 *
 * @author xxxx
 * @date Aug 13, 2019 2:53:04 PM
 *
 * codeview的建议，烦请查看 TODO 标注
 */
public class StrategyOperation {

  private static final Logger logger = LoggerFactory.getLogger(StrategyOperation.class);

  private HttpClientUtil2 httpClient;


  private Configuration config;


  /**
   * 无参构造器 在class最初，读取 config/strategy.properties 文件
   *   strategy.login.path
   *   strategy.logout.path
   *   strategy.start.path
   *   strategy.stop.path
   *   strategy.update.path
   *   strategy.add.path
   *   strategy.find.path
   *   strategy.upLoad.path
   *   manual.placeOrder.path
   *   manual.cancelOrder.path
   *   strategy.risk.path
   *   risk.find.path
   *   strategy.findAlgo.path
   * 将读取到的 配置信息保存在 config 对象中, config对象中有如下对象，用于保存 cookie
   *   caseMap保存当前登陆用户
   *   cookieMap 保存cookie中的信息
   *   globalMap 保存的信息有：instanceId，risk
   *
   */
  public StrategyOperation() {
    httpClient = new HttpClientUtil2();
    // config/strategy.properties 文件中保存
    // 各个测试方法用的url地址 如strategy.login.path
    config = ConfigUtil.getConfig("config/strategy.properties");
  }
  
  
  /**
   * 登录
   * @param user
   * @param pass
   */
  public Boolean login(String user, String pass, String link) {
    Boolean flag = false;
    if (StringUtils.isBlank(user) || StringUtils.isBlank(pass)) {
      logger.error("Username and Password are required!");
      return flag;
    }

    try {
      String url = "";
      if (StringUtils.isNotBlank(link)) {
        url = link;
      } else {
        url = config.getString("strategy.login.path");
      }

      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> params = new HashMap<>();
      Map<String, Object> params = new HashMap<>(4);

      params.put("username", user);
      params.put("password", pass);
      // Context 保存 业务上下文信息，
      Context.getContext().getCaseMap().put("currentLoginName", user);

      // 提交post请求
      HttpClientResult result = httpClient.doPost(url, null, params);
      // CheckWebResponseResult 检查登陆结果是否成功
      flag = CheckWebResponseResult.check(result.getMessage());
      logger.info("Strategy Login Result is: {}", result.toString());
      //登录后保持cookie，为后续ws时使用
      // 将用户信息 保存为cookie需要的格式，供http客户端使用
      httpClient.saveCookie(user);
    }
    // TODO 使用excption拦截例外，很不友好
    catch (URIException e) {
      logger.error("连接地址不正确 URIException " + e.toString());
    } catch (NoHttpResponseException e) {
      logger.error("无http返回信息 NoHttpResponseException " + e.toString());
    } catch (ConnectTimeoutException e) {
      logger.error("连接超时 ConnectTimeoutException " + e.toString());
    }
    // TODO 还有其他exception，具体实现请参考以下地址
    // https://hc.apache.org/httpclient-3.x/exception-handling.html
    catch (Exception e) {
      // TODO 将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    }
    return flag;
  }
  
  /**
   * 登出
   */
  public Boolean logout(String link) {
    Boolean flag = false;
    try {
      String url = "";
      if (StringUtils.isNotBlank(link)) {
        url = link;
      } else {
        url = config.getString("strategy.logout.path");
      }
      HttpClientResult result = httpClient.doGet(url, null, null);
      flag = CheckWebResponseResult.check(result.getMessage());
      logger.info("Strategy Logout Result is: {}", result.toString());
    }
    // TODO 使用excption拦截例外，很不友好 具体做法参考login方法
    catch (Exception e) {
      // TODO 请将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    } finally {
      //登出后释放资源
      httpClient.release();
    }
    return flag;
  }
  
  /**
   * 启动
   * @param instanceId
   */
  public Boolean start(String instanceId, String userName, String link) {
    Boolean flag = false;
    if (StringUtils.isBlank(instanceId)) {
      logger.error("instanceId is required!");
      return flag;
    }

    try {
      String url = "";
      if (StringUtils.isNotBlank(link)) {
        url = link;
      } else {
        url = config.getString("strategy.start.path");
      }

      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> params = new HashMap<>();
      Map<String, Object> params = new HashMap<>(2);
      params.put("instanceId", instanceId);

      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> header = new HashMap<>();
      Map<String, Object> header = new HashMap<>(2);
      header.put("Cookie", "JSESSIONID=" + Context.getContext().getCookieMap().get(userName).toString());
      HttpClientResult result = httpClient.doPost(url, header, params);
      flag = CheckWebResponseResult.check(result.getMessage());
      logger.info("Strategy Start Result is: {}", result.toString());
    }
    // TODO 使用excption拦截例外，很不友好 具体做法参考login方法
    catch (Exception e) {
      // TODO 请将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    }
    return flag;
  }
  
  /**
   * 停止
   * @param instanceId
   */
  public Boolean stop(String instanceId, String link) {
    Boolean flag = false;
    if (StringUtils.isBlank(instanceId)) {
      logger.error("instanceId is required!");
      return flag;
    }

    try {
      String url = "";
      if (StringUtils.isNotBlank(link)) {
        url = link;
      } else {
        url = config.getString("strategy.stop.path");
      }

      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> params = new HashMap<>();
      Map<String, Object> params = new HashMap<>(2);
      params.put("instanceId", instanceId);

      HttpClientResult result = httpClient.doPost(url, null, params);
      flag = CheckWebResponseResult.check(result.getMessage());
      logger.info("Strategy Stop Result is: {}", result.toString());
    } catch (Exception e) {
      // TODO 请将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    }
    return flag;
  }
  
  /**
   * 更新
   * 
   * @param map
   */
  public Boolean update(Map<String, Object> map, String link) {
    Boolean flag = false;
    try {
      String url = "";
      if (StringUtils.isNotBlank(link)) {
        url = link;
      } else {
        url = config.getString("strategy.update.path");
      }

      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(map);
      logger.info("Strategy Update JSON is: {}", json);

      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> params = new HashMap<>();
      Map<String, Object> params = new HashMap<>(2);
      params.put("input", json);

      HttpClientResult result = httpClient.doPost(url, null, params);
      flag = CheckWebResponseResult.check(result.getMessage());
      logger.info("Strategy Update Result is: {}", result.toString());

    } catch (Exception e) {
      // TODO 请将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    }
    return flag;
  }

  /**
   * 添加
   * 
   * @param map
   */
  public Boolean add(List<Map<String, Object>> list, String link, String user) {
    Boolean flag = false;
    try {
      String url = "";
      if (StringUtils.isNotBlank(link)) {
        url = link;
      } else {
        url = config.getString("strategy.add.path");
      }

      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(list);
      logger.info("Strategy Add JSON is: {}", json);

      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> params = new HashMap<>();
      Map<String, Object> params = new HashMap<>(2);
      params.put("input", json);

      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> header = new HashMap<>();
      Map<String, Object> header = new HashMap<>(2);

      header.put("Cookie", "JSESSIONID=" + Context.getContext().getCookieMap().get(user).toString());
      HttpClientResult result = httpClient.doPost(url, header, params);
      logger.info("Strategy Add Result is: {}", result.toString());
      flag = CheckWebResponseResult.check(result.getMessage());
    } catch (Exception e) {
      // TODO 请将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    }
    return flag;
  }
  
  /**
   * 查找
   * @param user
   * @param link
   */
  public void findInstance(String user, String link) {

    try {
      String url = "";
      if (StringUtils.isNotBlank(link)) {
        url = link;
      } else {
        url = config.getString("strategy.find.path");
      }

      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> params = new HashMap<>();
      Map<String, Object> params = new HashMap<>(2);
      params.put("username", user);

      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> header = new HashMap<>();
      Map<String, Object> header = new HashMap<>(2);

      header.put("Cookie", "JSESSIONID=" + Context.getContext().getCookieMap().get(user).toString());
      logger.info("Strategy find Cookie is: {}", "JSESSIONID=" + Context.getContext().getCookieMap().get(user).toString());
      HttpClientResult result = httpClient.doGet(url, header, params);
      logger.info("Strategy find Result is: {}", result.toString());
      //message={"data":[{"mtype":null,"instanceId":"BondMarketMaking.20190812175239538","symbol":"180212","code":"180212@CFETS","marketIndicator":"4","marketBidOfferPrices":[0.0,0.0],"marketSpread":0.0,"marketBp":null,"quoteBidOfferPrice":[0.0,0.0],"quoteSpread":0.0,"quoteSkew":0.0,"time":"19:11:39","quota":0.0,"pnl":0.0,"position":0.0,"algo":"Manual","status":"STARTED","flag":null,"settlType":"1","bookId":0,"bookIdMap":{"TOP":0,"Manual":0},"ownerId":"system","avgPxLong":0.0,"avgPxShort":0.0,"positionShort":0.0,"positionLong":0.0,"amtShort":0.0,"amtLong":0.0,"strategyScriptParams":{"bidPricesStep":["0"],"bidQuantities":["10000000"],"askQuantities":["10000000"],"askPricesStep":["0"]},"strategyScriptNames":["Manual","TOP"],"strategyScriptParamsMap":{"TOP":{"bid_qty":"1","ask_qty":"1"},"Manual":{"bidPricesStep":["0"],"bidQuantities":["10000000"],"askQuantities":["10000000"],"askPricesStep":["0"]}},"mktPxConfigId":6,"pricingListId":22,"instanceName":null,"duration":null,"lastPrice":null,"lastSide":null,"lastQty":null,"transTimesUnit":null,"transTimesTotal":null,"retraceMax":null,"volatility":null,"groupId":null,"groupName":""},{"mtype":null,"instanceId":"BondMarketMaking.20190813084133518","symbol":"180406","code":"180406@CFETS","marketIndicator":"4","marketBidOfferPrices":[101.8869,101.8896],"marketSpread":0.0,"marketBp":null,"quoteBidOfferPrice":[101.8869,101.8896],"quoteSpread":0.0,"quoteSkew":0.0,"time":"19:11:40","quota":0.0,"pnl":0.0,"position":0.0,"algo":"Manual","status":"STARTED","flag":null,"settlType":"2","bookId":0,"bookIdMap":{"Manual":0},"ownerId":"system","avgPxLong":0.0,"avgPxShort":0.0,"positionShort":0.0,"positionLong":0.0,"amtShort":0.0,"amtLong":0.0,"strategyScriptParams":{"bidPricesStep":["0"],"bidQuantities":["10000000"],"askQuantities":["10000000"],"askPricesStep":["0"]},"strategyScriptNames":["Manual"],"strategyScriptParamsMap":{"Manual":{"bidPricesStep":["0"],"bidQuantities":["10000000"],"askQuantities":["10000000"],"askPricesStep":["0"]}},"mktPxConfigId":3,"pricingListId":23,"instanceName":null,"duration":null,"lastPrice":null,"lastSide":null,"lastQty":null,"transTimesUnit":null,"transTimesTotal":null,"retraceMax":null,"volatility":null,"groupId":null,"groupName":""},{"mtype":null,"instanceId":"BondMarketMaking.20190821183643852","symbol":"190205","code":"190205@CFETS","marketIndicator":"4","marketBidOfferPrices":[101.8869,101.8896],"marketSpread":0.0,"marketBp":null,"quoteBidOfferPrice":[101.8869,101.8896],"quoteSpread":0.0,"quoteSkew":0.0,"time":"19:11:41","quota":0.0,"pnl":0.0,"position":0.0,"algo":"Manual","status":"STARTED","flag":null,"settlType":"2","bookId":0,"bookIdMap":{"Manual":0},"ownerId":"system","avgPxLong":0.0,"avgPxShort":0.0,"positionShort":0.0,"positionLong":0.0,"amtShort":0.0,"amtLong":0.0,"strategyScriptParams":{"bidPricesStep":["0"],"bidQuantities":["10000000"],"askQuantities":["10000000"],"askPricesStep":["0"]},"strategyScriptNames":["Manual"],"strategyScriptParamsMap":{"Manual":{"bidPricesStep":["0"],"bidQuantities":["10000000"],"askQuantities":["10000000"],"askPricesStep":["0"]}},"mktPxConfigId":12,"pricingListId":24,"instanceName":null,"duration":null,"lastPrice":null,"lastSide":null,"lastQty":null,"transTimesUnit":null,"transTimesTotal":null,"retraceMax":null,"volatility":null,"groupId":null,"groupName":""}],"messageList":[],"success":true}]

      // TODO 添加 getDataListByJson 方法 避免重复代码，
      // String message = result.getMessage();
      // ObjectMapper om = new ObjectMapper();
      // Map<String, Object> map = (Map<String, Object>) om.readValue(message, HashMap.class);
      // List<Map<String, Object>> dataList = (List<Map<String, Object>>) map.get("data");
      List<Map<String,Object>> dataList = getDataListByJson(result.getMessage());

      String instanceId = null;
      // TODO 在这里没有必要使用 Long，用long就可以
      // Long time = 1L;
      long time = 1L;
      for (Map<String, Object> each : dataList) {
        String tempInstanceId = ((String) each.get("instanceId"));
        logger.info("tempInstanceId=" + tempInstanceId);
        // TODO 在这里没有必要使用 Long，用long就可以
        // Long tempTime = Long.parseLong(tempInstanceId.substring(tempInstanceId.length() - 17, tempInstanceId.length()));
        long tempTime = Long.parseLong(tempInstanceId.substring(tempInstanceId.length() - 17, tempInstanceId.length()));
        logger.info("tempTime=" + tempTime);
        // if (tempTime > time)
        //  instanceId = tempInstanceId;
        // TODO 代码规范 if 后要跟上完整的{}
        if (tempTime > time) {
          instanceId = tempInstanceId;
        }
      }
      Map<String, Object> globalMap = Context.getContext().getGlobalMap();
      globalMap.put("instanceId", instanceId);
      setAllInstance(dataList, user);
    } catch (Exception e) {
      // TODO 请将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    }
  }
  /**
   * 上传
   * @param filePath
   * @param link
   */
  public void upLoad(Map<String, Object> map, String filePath, String fileName, String link) {
    String url = "";
    if (StringUtils.isNotBlank(link)) {
      url = link;
    } else {
      url = config.getString("strategy.upLoad.path");
    }
    //从ftp服务器中获取指定文件
    FileItemFactory factory = new DiskFileItemFactory(16, null);
    // filePath = "D:\\备份文件\\"+fileName[i];

    FileItem item = factory.createItem(fileName, "text/plain", true, fileName);
    File newfile = new File(filePath + File.separator + fileName);
    logger.info("系统找到指定的文件--" + filePath + File.separator + fileName);

    int bytesRead = 0;

    // TODO 8192 是魔术值，不推荐使用
    // byte[] buffer = new byte[8192];
    int byteLength = 8192;
    byte[] buffer = new byte[byteLength];
    try {
      FileInputStream fis = new FileInputStream(newfile);
      OutputStream os = item.getOutputStream();

      // while ((bytesRead = fis.read(buffer, 0, 8192)) != -1) {
      //  os.write(buffer, 0, bytesRead);
      //}
      while ((bytesRead = fis.read(buffer, 0, byteLength)) != -1) {
        os.write(buffer, 0, bytesRead);
      }
      os.close();
      fis.close();
    } catch (IOException e) {
      logger.info(e.getMessage());
      logger.error("系统找不到指定的文件--" + filePath + File.separator + fileName);
    }
    MultipartFile multipartFile = new CommonsMultipartFile(item);
    ObjectMapper mapper = new ObjectMapper();
    //@RequestParam("code") MultipartFile file, @RequestParam("name") String name,
    //@RequestParam("template") String template, @RequestParam(name = "params", required = false) String sparams,

    // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
    // Map<String, Object> params = new HashMap<>();
    Map<String, Object> params = new HashMap<>(16);
    params.put("code", multipartFile);
    params.put("template", map.get("template"));
    params.put("name", map.get("name"));
    params.putAll(map);
    /*
    Map<String,String> head = new HashMap<>();
    //head.put("Content-Type", "multipart/form-data");
    head.put("Content-Type", "multipart/form-data;boundary=----WebKitFormBoundary8jpGCXYJ5qUkGzpg");
    //head.put("enctype", "multipart/form-data");
    HttpClientResult result = null;
    try {
      result = httpClient.doPost(url, head, params);
    } catch (Exception e) {
      e.printStackTrace();
    }
    logger.info("Strategy UoLoad Result is: {}", result.toString());
    */

    httpClientUploadFile(multipartFile, params, url);

  }

  public void setAllInstance(List<Map<String, Object>> list, String user) {
    Map<String, Object> globalMap = Context.getContext().getGlobalMap();
    for (Map<String, Object> each : list) {
      String symbol = each.get("symbol") + "";

      // TODO equals条件判断，左侧不推荐用变量，
      // String settlType = (each.get("settlType") + "").equals("1") ? "T+0" : "T+1";
      String settlType = "1".equals(each.get("settlType") + "") ? "T+0" : "T+1";

      String key = user.concat(symbol).concat(",".concat(settlType));
      globalMap.put(key, each.get("instanceId"));
    }
  }

  public void setAllRisk(List<Map<String, Object>> list, String user) {
    Map<String, Object> globalMap = Context.getContext().getGlobalMap();
    for (Map<String, Object> each : list) {
      String symbol = each.get("code") + "";

      // TODO equals条件判断，左侧不推荐用变量，
      //String settlType = (each.get("settlType") + "").equals("1") ? "T+0" : "T+1";
      String settlType = "1".equals(each.get("settlType") + "") ? "T+0" : "T+1";
      String key = "risk,".concat(user).concat(symbol.concat(",".concat(settlType)));
      globalMap.put(key, each.get("id"));
    }
  }
  
  /**
   * 中转文件
   * 
   * @param file
   *            上传的文件
   * @return 响应结果
   */
  public String httpClientUploadFile(MultipartFile file, Map<String, Object> map, String remoteUrl) {
    // TODO remote_url不符合驼峰命名规则: remote_url 改为 remoteUrl

    CloseableHttpClient httpClient = HttpClients.createDefault();
    String result = "";
    try {
      String fileName = file.getOriginalFilename();
      HttpPost httpPost = new HttpPost(remoteUrl);
      String userName = Context.getContext().getCaseMap().get("currentLoginName").toString();
      if (userName != null) {
        httpPost.setHeader("Cookie", "JSESSIONID=" + Context.getContext().getCookieMap().get(userName).toString());
        logger.info("username:" + userName);
        logger.info("sessionid:" + Context.getContext().getCookieMap().get(userName).toString());
      }

      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      // TODO 代码注释格式不规范
      //builder.addBinaryBody("code", file.getInputStream(), ContentType.MULTIPART_FORM_DATA, fileName);// 文件流
      // 改成
      // 文件流
      builder.addBinaryBody("code", file.getInputStream(), ContentType.MULTIPART_FORM_DATA, fileName);

      // TODO 代码注释格式不规范
      // builder.addTextBody("filename", fileName);// 类似浏览器表单提交，对应input的name和value
      // 改成
      // 类似浏览器表单提交，对应input的name和value
      builder.addTextBody("filename", fileName);


      builder.addTextBody("name", (String) map.get("name"));
      builder.addTextBody("template", (String) map.get("template"));
      builder.addTextBody("params", map.get("params").toString());


      HttpEntity entity = builder.build();
      httpPost.setEntity(entity);

      // TODO 代码注释格式不规范
      // HttpResponse response = httpClient.execute(httpPost);// 执行提交
      // 改成
      // 执行提交
      HttpResponse response = httpClient.execute(httpPost);
      HttpEntity responseEntity = response.getEntity();
      if (responseEntity != null) {
        // 将响应内容转换为字符串
        result = EntityUtils.toString(responseEntity, Charset.forName("UTF-8"));
        logger.info("Strategy Login Result is: {}", result.toString());
      }
    } catch (IOException e) {
      // TODO 请将错误信息保存到日志中
      logger.error("IOException " + e.toString());
      e.printStackTrace();
    } catch (Exception e) {
      // TODO 请将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    } finally {
      try {
        httpClient.close();
      } catch (IOException e) {
        // TODO 请将错误信息保存到日志中
        logger.error("IOException " + e.toString());
        e.printStackTrace();
      }
    }
    return result;
  }
  
  /**
   * 手工下单/撤单
   * @param map 订单参数
   * @param type 类型
   * @param userName 用户
   * @param link 链接
   */
  public void placeOrder(Map<String, Object> map, String type, String userName, String link) {
    /*
    try {
      String url = "";
      if (StringUtils.isNotBlank(url)) {
        url = link;
      } else {
        url = config.getString("manual.placeOrder.path");
      }
      HttpEntity entity = new StringEntity(JSON.toJSONString(params),"utf-8");
      post.setEntity(entity);
      //HttpClientResult result = httpClient.doPost(url, null, map);
      logger.info("Manual PlaceOrder Result is: {}", result.toString());
      } catch (Exception e) {
        e.printStackTrace();
      }
    */
    String url = "";
    if (StringUtils.isNotBlank(link)) {
      url = link;
    } else {
      if ("placeOrder".equals(type)) {
        url = config.getString("manual.placeOrder.path");
      } else if ("cancelOrder".equals(type)) {
        url = config.getString("manual.cancelOrder.path");
      }
    }
    if ("cancelOrder".equals(type)) {
      HttpClientResult result = null;
      try {
        // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
        // Map<String, Object> header = new HashMap<>();
        Map<String, Object> header = new HashMap<>(2);
        header.put("Cookie", "JSESSIONID=" + Context.getContext().getCookieMap().get(userName).toString());
        result = httpClient.doPost(url, header, map);
      } catch (Exception e) {
        // TODO 请将错误信息保存到日志中
        logger.error("Exception " + e.toString());
        e.printStackTrace();
      }
      System.out.println("cancelOrder Result is:" + result.toString());
      logger.info("cancelOrder Result is: {}", result.toString());
      return;
    }
    // TODO 命名规则不符合驼峰原则： TimeOutTime 改为 timeOutTime
    int timeOutTime = 200000;
    long time = System.currentTimeMillis();
    CloseableHttpResponse httpResponse = null;
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpContext httpContext = new BasicHttpContext();
    try {
      HttpPost post = new HttpPost(url);
      // TODO 代码注释格式不规范
      // RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOutTime)
      //        .setConnectTimeout(timeOutTime).build();// 设置请求和传输超时时间
      // 改成
      // 设置请求和传输超时时间
      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOutTime)
              .setConnectTimeout(timeOutTime).build();

      post.setHeader(new BasicHeader("Content-Type", "application/json;charset=UTF-8"));
      //设置期望服务端返回的编码

      // TODO params 没有被使用的地方，删除
      // Map<String, Object> params = new HashMap<String, Object>();

      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(map);
      HttpEntity entity = new StringEntity(json, "utf-8");
      post.setEntity(entity);
      post.setConfig(requestConfig);
      System.out.println(type + ":" + "JSESSIONID=" + Context.getContext().getCookieMap().get(userName));
      post.addHeader("Cookie", "JSESSIONID=" + Context.getContext().getCookieMap().get(userName).toString());
      //Cookie: JSESSIONID=AA5AC2D6E3BDAC24FC1C56B93DB443CD
      logger.info("执行post请求..." + post.getURI());
      // 执行请求
      httpResponse = httpClient.execute(post, httpContext);
      HttpEntity entity2 = httpResponse.getEntity();

      // TODO entity 不可能为空，以下if判断条件可以去掉
      // if (null != entity) {
      String content = new String(EntityUtils.toString(entity2));
      System.out.println(content);
      //}
    } catch (Exception e) {
      // TODO 请将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    } finally {
      try {
        httpResponse.close();
        if (httpClient != null) {
          httpClient.close();
        }
      } catch (Exception e) {
        // TODO 请将错误信息保存到日志中
        logger.error("Exception " + e.toString());
        e.printStackTrace();
      }
      logger.info("http请求时间：" + (System.currentTimeMillis() - time) / 1000d + "s");
    }
  }

  public Boolean setRisk(Map<String, Object> map, String userName, String link) {
    Boolean flag = false;
    String url = "";
    if (StringUtils.isNotBlank(link)) {
      url = link;
    } else {
      url = config.getString("strategy.risk.path");
    }
    // TODO 命名规则不符合驼峰原则： TimeOutTime 改为 timeOutTime
    int timeOutTime = 200000;
    long time = System.currentTimeMillis();
    CloseableHttpResponse httpResponse = null;
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpContext httpContext = new BasicHttpContext();
    try {
      HttpPost post = new HttpPost(url);

      // TODO 代码注释格式不规范
      //RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOutTime)
      //        .setConnectTimeout(timeOutTime).build();// 设置请求和传输超时时间
      // 改成
      // 设置请求和传输超时时间
      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOutTime)
              .setConnectTimeout(timeOutTime).build();

      post.setHeader(new BasicHeader("Content-Type", "application/json;charset=UTF-8"));
      //设置期望服务端返回的编码

      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> header = new HashMap<String, Object>();
      Map<String, Object> header = new HashMap<String, Object>(2);
      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(map);
      HttpEntity entity = new StringEntity(json, "utf-8");
      post.setEntity(entity);
      post.setConfig(requestConfig);
      post.addHeader("Cookie", "JSESSIONID=" + Context.getContext().getCookieMap().get(userName).toString());
      logger.info("执行post请求..." + post.getURI());
      // 执行请求
      httpResponse = httpClient.execute(post, httpContext);
      HttpEntity entity2 = httpResponse.getEntity();
      // TODO entity 不可能为null，去掉if条件
      //if (null != entity) {
        String content = new String(EntityUtils.toString(entity2));
        flag = CheckWebResponseResult.check(content);
        System.out.println(content);
      //}
    } catch (Exception e) {
      // TODO 请将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    } finally {
      try {
        httpResponse.close();
        if (httpClient != null) {
          httpClient.close();
        }
      } catch (Exception e) {
        // TODO 请将错误信息保存到日志中
        logger.error("Exception " + e.toString());
        e.printStackTrace();
      }
      logger.info("http请求时间：" + (System.currentTimeMillis() - time) / 1000d + "s");
    }
    return flag;
  }

  public void findRisk(String user, String link) {
    String url = "";
    if (StringUtils.isNotBlank(link)) {
      url = link;
    } else {
      url = config.getString("risk.find.path");
    }
    // TODO 命名规则不符合驼峰原则： TimeOutTime 改为 timeOutTime
    int timeOutTime = 200000;
    long time = System.currentTimeMillis();
    CloseableHttpResponse httpResponse = null;
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpContext httpContext = new BasicHttpContext();
    try {
      HttpPost post = new HttpPost(url);
      // 设置请求和传输超时时间
      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOutTime)
              .setConnectTimeout(timeOutTime).build();// 设置请求和传输超时时间
      // 改成
      // 设置请求和传输超时时间
      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeOutTime)
              .setConnectTimeout(timeOutTime).build();

      post.setHeader(new BasicHeader("Content-Type", "application/json;charset=UTF-8"));
      //设置期望服务端返回的编码
      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> params = new HashMap<String, Object>();
      Map<String, Object> params = new HashMap<String, Object>(2);

      ObjectMapper mapper = new ObjectMapper();
      // TODO 无意义的params，转成无意义的json
      String json = mapper.writeValueAsString(params);
      HttpEntity entity = new StringEntity(json, "utf-8");
      post.setEntity(entity);
      post.setConfig(requestConfig);
      post.addHeader("Cookie", "JSESSIONID=" + Context.getContext().getCookieMap().get(user).toString());
      logger.info("执行post请求..." + post.getURI());
      // 执行请求
      httpResponse = httpClient.execute(post, httpContext);
      HttpEntity entity2 = httpResponse.getEntity();
      // TODO entity 不可能为null，去掉if条件
      // if (null != entity) {
        String content = new String(EntityUtils.toString(entity2));

        // TODO 重复代码
        // ObjectMapper om = new ObjectMapper();
        // Map<String, Object> map = (Map<String, Object>) om.readValue(content, HashMap.class);
        // List<Map<String, Object>> dataList = (List<Map<String, Object>>) map.get("data");
        List<Map<String,Object>> dataList = getDataListByJson(content);
        setAllRisk(dataList, user);
      //}
    } catch (Exception e) {
      // TODO 请将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    } finally {
      try {
        httpResponse.close();
        if (httpClient != null) {
          httpClient.close();
        }
      } catch (Exception e) {
        // TODO 请将错误信息保存到日志中
        logger.error("Exception " + e.toString());
        e.printStackTrace();
      }
      logger.info("http请求时间：" + (System.currentTimeMillis() - time) / 1000d + "s");
    }
  }

  public String findStrategyByName(String name, String user, String link) {
    String algo = null;
    try {
      String url = "";
      if (StringUtils.isNotBlank(link)) {
        url = link;
      } else {
        url = config.getString("strategy.findAlgo.path");
      }
      user = Context.getContext().getCaseMap().get("currentLoginName").toString();
      // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
      // Map<String, Object> header = new HashMap<String, Object>();
      Map<String, Object> header = new HashMap<String, Object>(2);

      header.put("Cookie", "JSESSIONID=" + Context.getContext().getCookieMap().get(user).toString());
      logger.info("algo find cookie is: {}", "JSESSIONID=" + Context.getContext().getCookieMap().get(user).toString());
      // TODO 添加 getDataListByJson方法
      // HttpClientResult result = httpClient.doGet(url, header, null);
      HttpClientResult result = httpClient.doGet(url, header, null);
      logger.info("algo find result is: {}", result.toString());

      // TODO 重复代码
      // String message = result.getMessage();
      // ObjectMapper om = new ObjectMapper();
      // Map<String,Object> map = (Map<String, Object>) om.readValue(message, HashMap.class);
      // List<Map<String, Object>> dataList = (List<Map<String, Object>>) map.get("data");
      List<Map<String, Object>> dataList = getDataListByJson(result.getMessage());

      for (Map<String, Object> each : dataList) {
        algo = ((String) each.get("algo"));
        //if (name.equals(algo))
        //  break;
        // TODO 代码规范 if 后要跟上完整的{}
        if (name.equals(algo)){
          break;
        }
      }
    } catch (Exception e) {
      // TODO 请将错误信息保存到日志中
      logger.error("Exception " + e.toString());
      e.printStackTrace();
    }
    return algo;
  }


  public static void main(String[] args) throws InterruptedException {
    StrategyOperation s = new StrategyOperation();

    System.out.println(s.login("cc", "123", "http://127.0.0.1:8080/finone-quantexecutor-spdb/jcgn/yhyz/dl.json"));
    // TODO hashMap初始化时，应提供size大小，推荐2的次幂，以提高运行效率
    // Map map = new HashMap<>();
    Map<String, Object> map = new HashMap<String, Object>(8);
    map.put("code", "180211@CFETS");
    map.put("settlType", "2");
    map.put("shortPositionMax", "20000000");
    map.put("shortPositionAlarmMethod", "ALARM_ONLY");
    map.put("longPositionMax", "20000000");
    map.put("longPositionAlarmMethod", "ALARM_ONLY");
    map.put("pnlMin", 0);
    map.put("pnlAlarmMethod", "ALARM_ONLY");
    System.out.println(s.setRisk(map, "cc", "http://127.0.0.1:8080/finone-quantexecutor-spdb/risk/fi/riskTradingParam/createOrUpdate.json"));
    // s.findRisk("cc","http://127.0.0.1:8080/finone-quantexecutor-spdb/risk/fi/riskTradingParam/find.json");
    // Map<String,Object> globalMap = Context.getContext().getGlobalMap();
    // Integer id = (Integer) globalMap.get("risk,".concat("180211@CFETS".concat(",".concat("T+1"))));
    // System.out.print(id);
    /*
    String algo = s.findStrategyByName("test", "cc", "http://52.83.59.179/finone-quantexecutor-spdb/strategy/fi/marketMaking/findAlgo.json");
    logger.info(algo);
    */
    /*
    Map<String, Object> uMap = new HashMap<>();
    uMap.put("strategyScriptName", "Manual");
    uMap.put("instanceId", "BondMarketMaking.20190917105556459");
    uMap.put("quoteSpread", 0);
    uMap.put("quoteSkew", 0);
    uMap.put("strategyScriptParams", "bidQuantities=10000000;askQuantities=10000000;bidPricesStep=0;askPricesStep=0");
    uMap.put("quoteSpreadFixed", true);
    uMap.put("bookId", 0);
    System.out.println(s.update(uMap, null));
    Thread.sleep(2000L);
    System.out.println(s.start("BondMarketMaking.20190917105556459", "cc", null));
    Thread.sleep(2000L);
    System.out.println(s.stop("BondMarketMaking.20190917105556459", null));
    Thread.sleep(2000L);
    System.out.println(s.logout(null));
    */
    // Map<String, Object> aMap = new HashMap<>();
    // List<Map<String, Object>> aList = new ArrayList<>();
    // aMap.put("settlType", "2");
    // aMap.put("marPxCfgId", 3);
    // aMap.put("groupId", "");
    // aMap.put("displayName", "180406@CFETS");
    // aMap.put("groupName", "");
    // aMap.put("symbol", "180406@CFETS");
    // aList.add(aMap);
    // s.add(aList, "libo");
    // s.findInstance("libo", null);
    // Map<String,Object> globalMap = Context.getContext().getGlobalMap();
    // String instanceId = (String) globalMap.get("instanceId");
    // logger.info(instanceId);
    
    /*
    s.findInstance("cc", null);
    Map<String,Object> globalMap = Context.getContext().getGlobalMap();
    String instanceId = (String) globalMap.get("instanceId");
    String symbol = "190401";
    String settlType="1";
    settlType = settlType.endsWith("1")?"T+1":"T+2";
    Object instanceIdObj =  globalMap.get(symbol.concat(","+settlType));
    if(null==instanceIdObj){
    logger.info(instanceIdObj+"--------error");
    return;
    }
    instanceId =  (String)globalMap.get(symbol.concat(","+settlType));
    logger.info(instanceId);*/
    /*
    Map<String,Object> map = new HashMap<>();
    String params = "1=1;2=2;3=3";
    map.put("name", "test");
    map.put("template", "123");
    map.put("params", params);
    String filePath = "D:/Alog";
    //    String filePath = "/data/fitnesseTestCases/FitNesseRoot/files/";
    String fileName = "X-Bond可交易债券信息_20190827.xml";
    s.upLoad(map, filePath, fileName, null);*/
    /*
    Map<String,Object> orderMap = new HashMap<>();
    Map<String,Object> extraParams = new HashMap<>();
    extraParams.put("marketIndicator",4);
    orderMap.put("id","cc-20190910161714817");
    */
    /*
    orderMap.put("code", "180211@CFETS");
    orderMap.put("symbol", 180211);
    orderMap.put("side","SELL");
    orderMap.put("type", "LIMIT");
    orderMap.put("tradeType", "SPECULATION");
    orderMap.put("tradingType", "ODM");
    orderMap.put("price", 100.1032);
    orderMap.put("tradingAccount", "tradingAccount");
    orderMap.put("settlType", "1");
    //	orderMap.put("extraParams", extraParams);
    orderMap.put("generateSource", "PM");
    orderMap.put("quantity", 50);
    orderMap.put("bookId", 0);
    orderMap.put("owner", "cc");
    orderMap.put("clearMethod", 13);
    */
    //StrategyOperation so = new StrategyOperation();
    //s.placeOrder(orderMap, "placeOrder", "http://52.82.34.230:8080/finone-quantexecutor-spdb/fi/trading/manualTrading/placeOrder.json");
    //s.placeOrder(orderMap, "cancelOrder", "cc", "http://192.168.2.136:8080/finone-quantexecutor-spdb/fi/trading/manualTrading/cancelOrder.json");
  }

  // TODO 添加getDataListByJson方法
  public List<Map<String, Object>> getDataListByJson(String message){
    ObjectMapper om = new ObjectMapper();
    Map<String,Object> map = (Map<String, Object>) om.readValue(message, HashMap.class);
    List<Map<String, Object>> dataList = (List<Map<String, Object>>) map.get("data");
    return dataList();
  }
}



