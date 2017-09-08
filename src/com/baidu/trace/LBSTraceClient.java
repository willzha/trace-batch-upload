package com.baidu.trace;

import java.util.ArrayList;
import java.util.List;

import com.baidu.trace.api.track.AddPointRequest;
import com.baidu.trace.api.track.AddPointsRequest;
import com.baidu.trace.core.AsyncRequestClient;
import com.baidu.trace.core.HttpClient;
import com.baidu.trace.core.TrackHandler;
import com.baidu.trace.model.OnUploadListener;

/**
 * 轨迹客户端
 * 
 * @author baidu
 *
 */
public class LBSTraceClient {

	private static LBSTraceClient instance = new LBSTraceClient();

	public List<OnUploadListener> uploadListeners = null;

	private LBSTraceClient() {
	}

	/**
	 * 初始化Client
	 */
	public void init() {
		AsyncRequestClient.getInstance().init();
		uploadListeners = new ArrayList<OnUploadListener>();
	}

	/**
	 * 开启Client
	 */
	public void start() {
		AsyncRequestClient.getInstance().start();
	}

	/**
	 * 停止Client
	 */
	public void stop() {
		AsyncRequestClient.getInstance().stop();
	}

	/**
	 * 销毁Client
	 */
	public void destory() {
		AsyncRequestClient.getInstance().destroy();
	}

	/**
	 * 注册上传监听器，接收上传结果回调
	 * 
	 * @param listener
	 */
	public void registerUploadListener(OnUploadListener listener) {
		uploadListeners.add(listener);
	}

	/**
	 * 取消已注册的上传监听器
	 * 
	 * @param listener
	 */
	public void unRegisterUploadListener(OnUploadListener listener) {
		if (uploadListeners.contains(listener)) {
			uploadListeners.remove(listener);
		}
	}

	/**
	 * 设置并发数，默认5000（单位：次/每分钟）
	 */
	public void setConcurrency(int concurrency) {
		AsyncRequestClient.getInstance().setConcurrency(concurrency);
	}

	/**
	 * 开启HTTPS
	 */
	public void enableHttps(boolean enable) {
		HttpClient.isEnableHttps = enable;
	}

	/**
	 * 获取轨迹客户端实例
	 * 
	 * @return
	 */
	public static LBSTraceClient getInstance() {
		if (null == instance) {
			synchronized (LBSTraceClient.class) {
				if (null == instance) {
					instance = new LBSTraceClient();
					instance.init();
				}
			}
		}
		return instance;
	}

	/**
	 * 添加单个轨迹点
	 * 
	 * @param trackPoint
	 */
	public void addPoint(AddPointRequest request) {
		TrackHandler.addPoint(request);
	}

	/**
	 * 添加多个轨迹点
	 * 
	 * @param trackPoints
	 */
	public void addPoints(AddPointsRequest request) {
		TrackHandler.addPoints(request);
	}

}
