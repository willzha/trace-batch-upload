package com.baidu.trace.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.baidu.trace.core.NetConstants;
import com.baidu.trace.util.TimeUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * 异步请求客户端
 * 
 * 1、将待执行的任务存储在阻塞队列中，等到调度；
 * 
 * 2、获取任务线程不断循环阻塞队列获取任务；
 * 
 * 3、获取任务时，先判断是否超并发，若超并发线程休眠1s；
 * 
 * 4、从线程池中调度空闲线程执行取出来的任务
 * 
 * @author baidu
 *
 */
public class AsyncRequestClient {

    private static AsyncRequestClient instance = null;

    /**
     * 等待队列：所有待处理的任务均存入该队列中，等到线程池调度
     */
    private static BlockingQueue<TaskCallable<String>> waitingQueue = new LinkedBlockingQueue<TaskCallable<String>>();

    /**
     * 每个请求中轨迹点数量
     */
    private static int pointSize = 200;

    /**
     * 每分钟并发数
     */
    private static long minutesConcurrency = 5000;

    /**
     * 每分钟并发控制器
     */
    private static LoadingCache<Long, AtomicInteger> minutesCounter = null;

    /**
     * 每秒并发控制器
     */
    private static LoadingCache<Long, AtomicInteger> secondsCounter = null;

    /**
     * 线程池service
     */
    private static ExecutorService executorService = null;

    /**
     * 任务调度线程
     */
    private static TaskScheduleThread taskScheduleThread = null;

    /**
     * 是否执行任务
     */
    private static boolean isRunning = false;

    private AsyncRequestClient() {
    }

    public static AsyncRequestClient getInstance() {
        if (null == instance) {
            synchronized (AsyncRequestClient.class) {
                if (null == instance) {
                    instance = new AsyncRequestClient();
                    instance.init();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化
     */
    public void init() {
        executorService = new ThreadPoolExecutor(NetConstants.CORE_POOL_SIZE, NetConstants.MAX_POOL_SIZE,
                NetConstants.KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        minutesCounter = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<Long, AtomicInteger>() {
                    @Override
                    public AtomicInteger load(Long arg0) throws Exception {
                        return new AtomicInteger(0);
                    }
                });

        secondsCounter = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS)
                .build(new CacheLoader<Long, AtomicInteger>() {
                    @Override
                    public AtomicInteger load(Long arg0) throws Exception {
                        return new AtomicInteger(0);
                    }
                });
    }

    /**
     * 设置每个请求轨迹点数量
     * 
     * @param pointSize
     */
    public void setRequestPointSize(int pointSize) {
        AsyncRequestClient.pointSize = pointSize;
    }

    /**
     * 设置每分钟并发数
     * 
     * @param concurrencyRequest
     */
    public void setConcurrency(int concurrency) {
        AsyncRequestClient.minutesConcurrency = concurrency;
    }

    /**
     * 开启任务调度
     */
    public void start() {
        if (!isRunning) {
            isRunning = true;
            taskScheduleThread = new TaskScheduleThread();
            taskScheduleThread.start();
        }
    }

    /**
     * 停止任务调度
     */
    public void stop() {
        isRunning = false;
        if (null != taskScheduleThread) {
            taskScheduleThread.interrupt();
        }
    }

    /**
     * 销毁client
     */
    public void destroy() {
        isRunning = false;
        if (null != taskScheduleThread) {
            taskScheduleThread.interrupt();
        }
    }

    /**
     * 提交任务
     * 
     * @param requestId
     * @param action
     * @param parameters
     * @param method
     */
    public void submitTask(long requestId, String action, String parameters, String method) {
        // 构造待执行任务
        TaskRunable taskRunnable = new TaskRunable(requestId, action, parameters, method);
        TaskCallable<String> taskCallable = new TaskCallable<String>(taskRunnable, "SUCCESS");
        // 任务存放到待执行队列
        try {
            waitingQueue.put(taskCallable);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行任务
     */
    private static void doTask() {
        TaskCallable<String> taskCallable = null;
        while (isRunning) {
            try {
                taskCallable = waitingQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            // 并发控制
            if (concurrencyControl()) {
                // 若未超并发，执行任务
                executorService.submit(taskCallable);
            } else {
                // 若超并发，休眠1s再执行
                System.err.println("超并发，休眠1s, waitingQueue size : " + waitingQueue.size());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                executorService.submit(taskCallable);
            }

        }
    }

    /**
     * 并发控制
     * 
     * @return
     */
    private static boolean concurrencyControl() {
        long currentMinutes = TimeUtils.getCurrentTimeOfMinutes();
        long currentSeconds = System.currentTimeMillis() / 1000;
        int currentMinutesCounts = 0;
        int currentSecondsCounts = 0;
        try {
            currentMinutesCounts = minutesCounter.get(currentMinutes).incrementAndGet();
            currentSecondsCounts = secondsCounter.get(currentSeconds).incrementAndGet();
        } catch (ExecutionException e1) {
            e1.printStackTrace();
            return true;
        }

        System.out.println("currentSecondsCounts * pointSize : " + currentSecondsCounts * pointSize);

        if (currentMinutesCounts * pointSize > minutesConcurrency
                || currentSecondsCounts * pointSize > minutesConcurrency / 60) {
            return false;
        }
        return true;
    }

    /**
     * 任务封装类，作为参数传给TaskCallable
     * 
     * @author baidu
     *
     */
    public static final class TaskRunable implements Runnable {

        private long requestId;

        private String action;

        private String parameters;

        private String method;

        public TaskRunable(long requestId, String action, String parameters, String method) {
            super();
            this.requestId = requestId;
            this.action = action;
            this.parameters = parameters;
            this.method = method;
        }

        @Override
        public void run() {
            // 发送请求
            String result = HttpClient.sendRequest(action, parameters, method);
            // 解析响应
            TrackHandler.parseResponse(requestId, action, result);
        }
    }

    /**
     * 任务封装类，作为参数传给线程池
     * 
     * @author baidu
     *
     * @param <T>
     */
    public static final class TaskCallable<T> implements Callable<T> {

        private Runnable task;

        private T result;

        public TaskCallable(Runnable task, T result) {
            super();
            this.task = task;
            this.result = result;
        }

        @Override
        public T call() throws Exception {
            task.run();
            return result;
        }

    }

    /**
     * 任务调度线程，不断循环阻塞队列获取任务
     * 
     * @author baidu
     *
     */
    public static final class TaskScheduleThread extends Thread {

        public void run() {
            doTask();
        }
    }

}
