package com.quantinfotech.exam02;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 读取文件目录下所有文件
 */
public class FilesLoader {


    /**
     * 原文件保存地址
     */
    private String folder = "exam02/articleFiles/files";

    /**
     * 序列化中间结果保存地址
     */
    private String serializedFolder = "exam02/articleFiles/serialized/";

    /**
     * 是否要序列化
     */
    private Boolean serializeFlag = false;

    /**
     * 最终统计结果
     */
    private LinkedHashMap<String, Long> totalAnalysisMap = null;

    /**
     * 无参构造方法
     */
    private FilesLoader(){

    }

    /**
     * 自定义有参构造方法
     * @param folder
     */
    public FilesLoader(String folder){
        this.folder = folder;
        this.totalAnalysisMap = new LinkedHashMap<>();
    }

    /**
     * 自定义有参构造方法
     * @param folder
     * @param serializeFlag
     */
    public FilesLoader(String folder, Boolean serializeFlag){
        this.folder = folder;
        this.serializeFlag = serializeFlag;
        this.totalAnalysisMap = new LinkedHashMap<>();
    }


    /**
     * 功能相对单一的加载文件夹下所有文件的方法
     */
    public void simpleLoadFiles(){

        Path path = Paths.get(folder);
        // path = Paths.get(ClassLoader.getSystemResource(this.folder).toURI());

        Stream<Path> list = null;
        try {
            list = Files.list(path);

            AtomicInteger i = new AtomicInteger(0);
            list.forEach( filePath -> {
                i.set(i.get() + 1);
                // System.out.println(i.get());
                TargetFile tf = new TargetFile(filePath);
                tf.loadContext();
                tf.analysis();
                LinkedHashMap<String, Long> analysisMap = tf.getAnalysisMap();
                // System.out.println(analysisMap);

                mergeAnalysisMap(analysisMap);

                // 序列化，将文件中间结果保存
                if(serializeFlag){
                    Path outpath = Paths.get(serializedFolder + filePath.getFileName().toString());
                    try {
                        Files.write(outpath, analysisMap.toString().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);
    public ExecutorService getFixedThreadPool(){
        return fixedThreadPool;
    }
    public void multiThreadLoadFiles(){

        Path path = Paths.get(folder);
        // path = Paths.get(ClassLoader.getSystemResource(this.folder).toURI());

        Stream<Path> list = null;
        try {
            list = Files.list(path);

            AtomicInteger i = new AtomicInteger(0);
            List<Path> paths = list.collect(Collectors.toList());
            for(Path filePath: paths){
                fixedThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        i.set(i.get() + 1);
                        // System.out.println(i.get());
                        TargetFile tf = new TargetFile(filePath);
                        tf.loadContext();
                        tf.analysis();
                        LinkedHashMap<String, Long> analysisMap = tf.getAnalysisMap();
//                        System.out.println(Thread.currentThread().getName() + "结束.");
                        mergeAnalysisMapSync(analysisMap);
                        // System.out.println(analysisMap);
                    }
                });

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        fixedThreadPool.shutdown();
    }

    /**
     * 讲中间结果合并到最终结果上
     * @param analysisMap
     */
    public void mergeAnalysisMap(LinkedHashMap<String, Long> analysisMap){
        analysisMap.keySet().forEach(
           key -> {
               if(!totalAnalysisMap.containsKey(key)){
                   totalAnalysisMap.put(key, 0L);
               }
               totalAnalysisMap.put(key, totalAnalysisMap.get(key) + analysisMap.get(key));
           }
        );

    }
    public synchronized void mergeAnalysisMapSync(LinkedHashMap<String, Long> analysisMap){
        analysisMap.keySet().forEach(
                key -> {
                    if(!totalAnalysisMap.containsKey(key)){
                        totalAnalysisMap.put(key, 0L);
                    }
                    totalAnalysisMap.put(key, totalAnalysisMap.get(key) + analysisMap.get(key));
                }
        );

    }

    /**
     * 返回最终结果的方法
     * @return
     */
    public LinkedHashMap<String, Long> getTotalAnalysisMap(){
        return totalAnalysisMap;
    }
}
