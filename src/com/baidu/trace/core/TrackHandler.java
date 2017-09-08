package com.baidu.trace.core;

import java.util.List;

import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.api.track.AddPointRequest;
import com.baidu.trace.api.track.AddPointsRequest;
import com.baidu.trace.api.track.IllegalTrackArgumentException;
import com.baidu.trace.api.track.UploadResponse;
import com.baidu.trace.model.BaseRequest;
import com.baidu.trace.model.BaseResponse;
import com.baidu.trace.model.OnUploadListener;
import com.baidu.trace.model.StatusCodes;
import com.baidu.trace.model.TrackPoint;
import com.baidu.trace.util.CommonUtils;
import com.baidu.trace.util.HttpUtils;
import com.baidu.trace.util.TrackUtils;

import net.sf.json.JSONObject;

/**
 * Track处理器
 * 
 * @author baidu
 *
 */
public class TrackHandler {

    /**
     * 添加单个轨迹点
     * 
     * @param request
     */
    public static void addPoint(AddPointRequest request) {
        StringBuilder parameters = new StringBuilder();
        packRequest(request, parameters);
        AsyncRequestClient.getInstance().submitTask(request.getRequestID(), UrlDomain.ACTION_ADD_POINT,
                parameters.toString(), HttpClient.METHOD_POST);
    }

    /**
     * 批量添加轨迹点
     * 
     * @param request
     */
    public static void addPoints(AddPointsRequest request) {
        StringBuilder parameters = new StringBuilder();
        packRequest(request, parameters);
        AsyncRequestClient.getInstance().submitTask(request.getRequestID(), UrlDomain.ACTION_ADD_POINTS,
                parameters.toString(), HttpClient.METHOD_POST);
    }

    /**
     * 组装请求
     * 
     * @return
     */
    public static void packRequest(BaseRequest request, StringBuilder parameters) {
        if (null == request) {
            throw new IllegalTrackArgumentException("request can not be null.");
        }

        packCommonRequest(request, parameters);

        if (request instanceof AddPointRequest) {
            AddPointRequest addPointRequest = (AddPointRequest) request;
            parameters.append("&entity_name=").append(HttpUtils.urlEncode(addPointRequest.getEntityName()));
            TrackPoint trackPoint = addPointRequest.getTrackPoint();
            if (null == trackPoint) {
                throw new IllegalTrackArgumentException("trackPoint can not be null.");
            }
            TrackUtils.packPoint(trackPoint, parameters);
        } else {
            AddPointsRequest addPointsRequest = (AddPointsRequest) request;
            TrackUtils.packPoints(addPointsRequest.getTrackPoints(), parameters);
        }
    }

    /**
     * 组装公共请求
     * 
     * @param request
     * @param parameters
     */
    private static void packCommonRequest(BaseRequest request, StringBuilder parameters) {
        if (CommonUtils.isNullOrEmpty(request.getAk())) {
            throw new IllegalTrackArgumentException("ak can not be null or empty string.");
        }

        if (request.getServiceId() <= 0) {
            throw new IllegalTrackArgumentException("serviceId is lower than 0.");
        }
        parameters.append("ak=").append(request.getAk());
        parameters.append("&service_id=").append(request.getServiceId());
    }

    /**
     * 解析响应
     * 
     * @param requestId 响应对应的请求ID
     * @param action 响应对应的请求action
     * @param result 响应结果
     */
    public static void parseResponse(long requestId, String action, String result) {
        List<OnUploadListener> listeners = LBSTraceClient.getInstance().uploadListeners;
        UploadResponse response = new UploadResponse(requestId, StatusCodes.SUCCESS, StatusCodes.MSG_SUCCESS);
        parseCommonResponse(response, result);
        for (OnUploadListener listener : listeners) {
            if (response.getStatus() == StatusCodes.SUCCESS) {
                listener.onSuccess(requestId);
            } else {
                listener.onFailed(response);
            }
        }
    }

    /**
     * 解析通用响应
     * 
     * @param response
     * @param result
     */
    private static void parseCommonResponse(BaseResponse response, String result) {
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.fromObject(result);
            if (jsonObject.has("status")) {
                response.setStatus(jsonObject.getInt("status"));
            }
            if (jsonObject.has("message")) {
                response.setMessage(jsonObject.getString("message"));
            }
        } catch (Exception e) {
            response.setStatus(StatusCodes.PARSE_FAILED);
            response.setMessage(StatusCodes.MSG_PARSE_FAILED);
        }
    }

}
