package com.quantinfotech.exam02;

import sun.misc.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

/**
 * 读取单个文件，并且统计字符数量
 */
public class TargetFile{

  /**
   * 文件地址
   */
  private Path filePath = null;

  /**
   * 分析结果
   */
  private LinkedHashMap<String,Long> analysisMap = null;

  /**
   * 小说内容
   */
  private StringBuffer context = new StringBuffer();

  /**
   * 不允许外部调用无参构造方法
   */
  private TargetFile(){
    analysisMap = new LinkedHashMap<String,Long>();
  }
  /**
   * 自定义有参构造方法
   * @param fullFilePath
   */
  public TargetFile(String fullFilePath){
    filePath = Paths.get(fullFilePath);
    analysisMap = new LinkedHashMap<String,Long>();
  }

  /**
   * 自定义有参构造方法
   * @param path
   */
  public TargetFile(Path path){
    filePath = path;
    analysisMap = new LinkedHashMap<String,Long>();
  }

  /**
   * 返回filePath内部参数
   * @return
   */
  public Path getFilePath(){
    return filePath;
  }

  /**
   * 加载文件内容
   * loadContextByBufferReader 和 loadContextByByteStream 任选其一，速度差不多
   */
  public void loadContext(){
    loadContextByBufferReader();
    // loadContextByByteStream();
  }

  /**
   * 加载文件内容
   * 使用BufferReader高速读取
   * loadContextByBufferReader 和 loadContextByByteStream速度相差无几，效率上是nio相对高速的实现方法
   */
  public void loadContextByBufferReader(){

    // 读取文件
    Path path = null;
    path = Paths.get(filePath.toUri());

    try(BufferedReader reader = Files.newBufferedReader(path, Charset.forName("ISO-8859-1"))){
      String currentLine = null;
      // 按照行读取数据
      while((currentLine = reader.readLine()) != null){
        // 过滤字符串
        currentLine = currentLine
                // 去除除英文以外其他的字符串
                .replaceAll("[^a-zA-Z]", " ")
                // 2个以上空格 换成单空格
                .replaceAll("[\\s]+", " ")
                // 大写变成小写
                .toLowerCase()
                // 行位统一加空格
                + " ";
        context.append(currentLine);
      }
      // System.out.println(context.toString().replaceAll("[\\s]+", " "));
    }catch(IOException ex){
      ex.printStackTrace(); //handle an exception here
    }
  }

  /**
   * 加载文件内容
   * 使用ByteArrayStream读取
   * loadContextByBufferReader 和 loadContextByByteStream速度相差无几，效率上是nio相对高速的实现方法
   */
  public void loadContextByByteStream(){
    int bufferSize = 2 * 1024 * 1024;
    Path srcPath = filePath ;
    ReadableByteChannel srcFc = null;
    // 输入流
    ByteArrayInputStream bis = null;
    // 输出流
    ByteArrayOutputStream bos = null;
    try {
      srcFc = FileChannel.open(srcPath, StandardOpenOption.READ);
      // Better to set this to a higher number
      byte[] barray = new byte[bufferSize];
      bos = new ByteArrayOutputStream();
      ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
      int bytesRead = 0;

      while ((bytesRead = srcFc.read(buffer)) != -1) {
        buffer.flip();
        buffer.get(barray, 0, bytesRead);
        bos.write(barray, 0, bytesRead);
        buffer.clear();
      }

      String text = new String(bos.toByteArray(), StandardCharsets.ISO_8859_1);
      text = text
              // 去除除英文以外其他的字符串
              .replaceAll("[^a-zA-Z]", " ")
              // 2个以上空格 换成单空格
              .replaceAll("[\\s]+", " ")
              // 大写变成小写
              .toLowerCase()
              // 行位统一加空格
              + " ";
      context.append(text);

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if(srcFc != null) {
          srcFc.close();
        }
        if (bos != null) {
          bos.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

  /**
   * 分析文件
   * 使用StringTokenizer快速拆分，效率高于String.Split方法
   */
  public void analysis(){

    // 如果context为空，调用loadContext方法
    if("".equals(context.toString())){
      loadContext();
    }
    StringTokenizer tokenizer = new StringTokenizer(context.toString().replaceAll("[\\s]+", " ")," ");
//    String[] words = new String[tokenizer.countTokens()];
//    int i = 0;

    while (tokenizer.hasMoreTokens()) {
      String word = tokenizer.nextToken();

      // 统计结果中不包含的单词，追加一个
      if(!analysisMap.containsKey(word)){
        analysisMap.put(word,0L);
      }
      // 计数加1
      analysisMap.put(word, analysisMap.get(word) + 1L);
//      words[i] = word;
//      i++;
    }
  }

  /**
   *
   * @return
   */
  public LinkedHashMap<String,Long> getAnalysisMap(){
    // 如果统计字符串为空，调用analysis方法
    if(analysisMap.keySet().size() == 0){
      analysis();
    }
    return this.analysisMap;
  }

}