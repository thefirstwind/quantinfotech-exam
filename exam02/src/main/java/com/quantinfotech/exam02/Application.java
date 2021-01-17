package com.quantinfotech.exam02;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.LinkedHashMap;

@SpringBootApplication
public class Application {

    public static void main(String[] args){
        SpringApplication.run(Application.class, args);


//        loadFiles01();
//        loadFiles02();
//        loadFiles03();
        loadFiles04();
    }

    /**
     * 测试123M文件 执行时间32秒
     */
    public static void loadFiles01(){
        DateTime dt1 = new DateTime();
        FilesLoader fl = new FilesLoader("exam02/articleFiles/files_100M");
        fl.simpleLoadFiles();
        LinkedHashMap<String,Long> result = fl.getTotalAnalysisMap();
        System.out.println(result);

        DateTime dt2 = new DateTime();
        Duration d = new Duration(dt1, dt2);
        long time = d.getMillis();
        System.out.println("执行时间: " + time);
    }

    /**
     * 测试7.7M文件 执行时间 2.166秒
     */
    public static void loadFiles02(){
        DateTime dt1 = new DateTime();
        FilesLoader fl = new FilesLoader("exam02/articleFiles/files");
        fl.simpleLoadFiles();
        LinkedHashMap<String,Long> result = fl.getTotalAnalysisMap();
        System.out.println(result);

        DateTime dt2 = new DateTime();
        Duration d = new Duration(dt1, dt2);
        long time = d.getMillis();
        System.out.println("执行时间: " + time);
    }

    /**
     * 测试7.7M文件 执行时间 1.3秒
     */
    public static void loadFiles03(){
        DateTime dt1 = new DateTime();
        FilesLoader fl = new FilesLoader("exam02/articleFiles/files");
        fl.multiThreadLoadFiles();
        LinkedHashMap<String,Long> result = fl.getTotalAnalysisMap();

        //等待所有子线程执行完
        while (!fl.getFixedThreadPool().isTerminated()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("loadFiles03执行结束");
        System.out.println(result);

        DateTime dt2 = new DateTime();
        Duration d = new Duration(dt1, dt2);
        long time = d.getMillis();
        System.out.println("执行时间: " + time);
    }

    /**
     * 测试123M文件 执行时间 16秒
     *
     * 预计2T文件执行时间要75小时左右
     */
    public static void loadFiles04(){
        DateTime dt1 = new DateTime();
        FilesLoader fl = new FilesLoader("exam02/articleFiles/files_100M");
        fl.multiThreadLoadFiles();
        LinkedHashMap<String,Long> result = fl.getTotalAnalysisMap();

        //等待所有子线程执行完
        while (!fl.getFixedThreadPool().isTerminated()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("loadFiles03执行结束");
        System.out.println(result);

        DateTime dt2 = new DateTime();
        Duration d = new Duration(dt1, dt2);
        long time = d.getMillis();
        System.out.println("执行时间: " + time);
    }

}
