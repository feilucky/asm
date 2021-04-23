package com.mxtech.demo.plugin;

class AopInteceptor {
    public static long startTime;

    public static void start() {
        startTime = System.currentTimeMillis();
        System.out.println("=====startTime=====" + startTime);
    }

    public static void end() {
        long costTime = System.currentTimeMillis() - startTime;
        System.out.println("=====endTime=====" + System.currentTimeMillis());
        System.out.println("=====costTime=====" + costTime);
    }
}
