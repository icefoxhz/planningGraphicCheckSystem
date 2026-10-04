package com.hz.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hz.constant.MyConstant;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author saber
 */
public class GeomCovertUtil {
    public static ConcurrentLinkedQueue<JSONObject> dataList2JsonQueue(List<Map<String, Object>> records) {
        ConcurrentLinkedQueue<JSONObject> resultQueue = new ConcurrentLinkedQueue<>();

        if (records.isEmpty()) {
            return resultQueue;
        }

        records.parallelStream().forEach(stringObjectMap -> {
            Object wkb = stringObjectMap.get(MyConstant.WKB_FIELD_NAME);
            try {
                JSONObject jsonObject = GeomCovertUtil.wkb2JsonObject(wkb);
                stringObjectMap.remove(MyConstant.WKB_FIELD_NAME);
                // 加入所有属性
                jsonObject.putAll(stringObjectMap);
//                jsonObject.put(MyConstant.ID_FIELD_NAME, stringObjectMap.get(MyConstant.ID_FIELD_NAME));

                resultQueue.offer(jsonObject);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return resultQueue;
    }

    public static JSONObject wkb2JsonObject(Object wkb) throws Exception {
        JSONObject jsonObject = new JSONObject();
        Geometry geom = new WKBReader().read((byte[]) wkb);
        String geometryType = geom.getGeometryType();
//        System.out.println("geometryType: " + geometryType);

//        WKBWriter writer = new WKBWriter();
//        byte[] my_wkb = writer.write(geom);

        switch (geometryType) {
            case "Point": {
                JSONArray coordinates = parsePoint2JsonArray((Point) geom);
                jsonObject.put("coordinates", coordinates);
            }
            break;
            case "MultiPoint": {
                JSONArray coordinates = parseMultiPoint2JsonArray((MultiPoint) geom);
                jsonObject.put("coordinates", coordinates);
            }
            break;
            case "LineString": {
                JSONArray coordinates = parseLine2JsonArray((LineString) geom);
                jsonObject.put("coordinates", coordinates);
            }
            break;
            case "MultiLineString": {
                JSONArray coordinates = parseMultiLine2JsonArray((MultiLineString) geom);
                jsonObject.put("coordinates", coordinates);
            }
            break;
            case "Polygon": {
                Polygon polygon = (Polygon) geom;
                JSONArray coordinates = parsePolygon2JsonArray(polygon);
                jsonObject.put("coordinates", coordinates);
                jsonObject.put("ringCount", polygon.getNumInteriorRing());
            }
            break;
            case "MultiPolygon": {
                MultiPolygon mPolygon = (MultiPolygon) geom;
                JSONArray coordinates = parseMultiPolygon2JsonArray(mPolygon);
                jsonObject.put("coordinates", coordinates);
                jsonObject.put("geomCount", mPolygon.getNumGeometries());
            }
            break;
            default:
                throw new Exception(String.format("当前实体类型为: %s, 暂不支持解析！", geometryType));
        }

        return jsonObject;
    }

    private static JSONArray coordinates2JsonArray(Coordinate[] coordinates) {
        JSONArray objects = new JSONArray();
        for (Coordinate coordinate : coordinates) {
            JSONArray coordinateObj = new JSONArray();
            coordinateObj.add(coordinate.getX());
            coordinateObj.add(coordinate.getY());
            objects.add(coordinateObj);
        }
        return objects;
    }

    private static JSONArray parsePoint2JsonArray(Point pt) {
        JSONArray retObjects = new JSONArray();
        JSONArray objects = new JSONArray();
        objects.add(pt.getCoordinate().getX());
        objects.add(pt.getCoordinate().getY());
        retObjects.add(objects);
        return retObjects;
    }

    private static JSONArray parseMultiPoint2JsonArray(MultiPoint mPt) {
        Coordinate[] coordinates = mPt.getCoordinates();
        return coordinates2JsonArray(coordinates);
    }

    private static JSONArray parseLine2JsonArray(LineString line) {
        JSONArray objects = new JSONArray();
        Coordinate[] coordinates = line.getCoordinates();
        objects.add(coordinates2JsonArray(coordinates));
        return objects;
    }

    private static JSONArray parseMultiLine2JsonArray(MultiLineString mLine) {
        JSONArray objects = new JSONArray();
        int numGeometries = mLine.getNumGeometries();
        for (int i = 0; i < numGeometries; i++) {
            Geometry line = mLine.getGeometryN(i);
            Coordinate[] coordinates = line.getCoordinates();
            JSONArray lineCoordinates = coordinates2JsonArray(coordinates);
            objects.add(lineCoordinates);
        }
        return objects;
    }

    private static JSONArray parsePolygon2JsonArray(Polygon polygon) {
        int numInteriorRing = polygon.getNumInteriorRing();
        if (numInteriorRing > 0) {
            JSONArray objects = new JSONArray();
            // 外圈
            LinearRing exteriorRing = polygon.getExteriorRing();
            objects.add(coordinates2JsonArray(exteriorRing.getCoordinates()));

            // 内孔
            for (int i = 0; i < numInteriorRing; i++) {
                LinearRing interiorRingN = polygon.getInteriorRingN(i);
                objects.add(coordinates2JsonArray(interiorRingN.getCoordinates()));
            }
//            objectResults.add(objects);
            return objects;
        } else {
            JSONArray objectResults = new JSONArray();
            Coordinate[] coordinates = polygon.getCoordinates();
            objectResults.add(coordinates2JsonArray(coordinates));
            return objectResults;
        }
    }

    private static JSONArray parseMultiPolygon2JsonArray(MultiPolygon mPolygon) {
        JSONArray objects = new JSONArray();
        int numGeometries = mPolygon.getNumGeometries();
        for (int i = 0; i < numGeometries; i++) {
            JSONObject jsonObject = new JSONObject();
            Geometry geom = mPolygon.getGeometryN(i);
            Polygon polygon = (Polygon) geom;
            JSONArray coordinates = parsePolygon2JsonArray(polygon);
            jsonObject.put("coordinates", coordinates);
            jsonObject.put("ringCount", polygon.getNumInteriorRing());
            objects.add(jsonObject);
        }
        return objects;
    }

    public static String createWkt(JSONArray pts){
        if (pts.isEmpty()){
            return null;
        }
//        String wkt = "POLYGON ((0 0, 0 0, 0 0, 0 0, 0 0, 0 0))";
        String wkt_start = "POLYGON ((";
        String wkt_end = "))";
        List<String> ptList = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) {
            JSONArray jsonArray = pts.getJSONArray(i);
            double x = jsonArray.getDouble(0);
            double y = jsonArray.getDouble(1);
            ptList.add(String.format("%f %f", x, y));
        }
        String wkt_mid = String.join(",", ptList);

        return wkt_start + wkt_mid + wkt_end;
    }

}
