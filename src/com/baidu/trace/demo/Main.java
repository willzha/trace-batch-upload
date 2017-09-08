package com.baidu.trace.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.api.track.AddPointsRequest;
import com.baidu.trace.api.track.UploadResponse;
import com.baidu.trace.model.CoordType;
import com.baidu.trace.model.LatLng;
import com.baidu.trace.model.OnUploadListener;
import com.baidu.trace.model.TrackPoint;

public class Main {

    private static AtomicLong mSequenceGenerator = new AtomicLong();

    private static AtomicInteger successCounter = new AtomicInteger();

    private static AtomicInteger failedCounter = new AtomicInteger();

    public static void main(String[] args) throws ExecutionException {
        LBSTraceClient client = LBSTraceClient.getInstance();
        client.init();
        client.start();

        client.registerUploadListener(new OnUploadListener() {

            @Override
            public void onSuccess(long responseId) {
                System.out.println("上传成功 : " + responseId + ", successCounter : " + successCounter.incrementAndGet());
            }

            @Override
            public void onFailed(UploadResponse response) {
                System.err.println("上传失败 : " + response.getResponseID() + ", failedCounter : "
                        + failedCounter.incrementAndGet() + ", " + response);
            }
        });

        List<TrackPoint> trackPoints = new ArrayList<TrackPoint>();
        for (int i = 1; i <= 50; ++i) {
            TrackPoint trackPoint = new TrackPoint(new LatLng(40.05 + i / 100.0, 116.31), CoordType.bd09ll, 30,
                    System.currentTimeMillis() / 1000 + i, 4, 20, 40, null, null);
            trackPoints.add(trackPoint);
        }

        for (int i = 0; i < 10000; i++) {
            Map<String, List<TrackPoint>> trackPointMap = new HashMap<String, List<TrackPoint>>();
            trackPointMap.put("batch_upload_" + i, trackPoints);
            AddPointsRequest request = new AddPointsRequest(mSequenceGenerator.incrementAndGet(),
                    "4NEAN17DpkroLCVwZPg21EIQ0KsxGt3E", 137062, trackPointMap);
            // 批量添加轨迹点
            client.addPoints(request);
        }

        // AddPointRequest request1 = new AddPointRequest(100,
        // "4NEAN17DpkroLCVwZPg21EIQ0KsxGt3E", 137062,
        // "batch_upload_1", trackPoints.get(0));
        // instance.addPoint(request1);
        //
        // AddPointsRequest request2 = new AddPointsRequest(101,
        // "4NEAN17DpkroLCVwZPg21EIQ0KsxGt3E", 137062,
        // trackPointMap);
        // instance.addPoints(request2);

    }

}
