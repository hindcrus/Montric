package org.eurekaj.manager.servlets;

import org.eurekaj.manager.berkeley.statistics.LiveStatistics;
import org.eurekaj.manager.json.BuildJsonObjectsUtil;
import org.eurekaj.manager.perst.alert.Alert;
import org.eurekaj.manager.perst.statistics.GroupedStatistics;
import org.eurekaj.manager.util.ChartUtil;
import org.jsflot.xydata.XYDataList;
import org.jsflot.xydata.XYDataSetCollection;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: joahaa
 * Date: 1/20/11
 * Time: 9:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class ChartServlet extends EurekaJGenericServlet {

    private int getChartTimeSpan(JSONObject jsonRequest) throws JSONException {
        int chartTimespan = 10;
        if (jsonRequest.has("chartTimespan")) {
            chartTimespan = jsonRequest.getInt("chartTimespan");
        }

        return chartTimespan;
    }

    private int getChartResolution(JSONObject jsonRequest) throws JSONException {
        int chartTimespan = 15;
        if (jsonRequest.has("chartResolution")) {
            chartTimespan = jsonRequest.getInt("chartResolution");
        }
        return chartTimespan;
    }

    private boolean isAlertChart(JSONObject jsonRequest) throws JSONException {
        return jsonRequest.has("path") && jsonRequest.getString("path").startsWith("_alert_:");
    }

    private Long getFromPeriod(int chartTimespan) {
        Calendar thenCal = Calendar.getInstance();
        thenCal.add(Calendar.MINUTE, chartTimespan * -1);

        Long fromPeriod = thenCal.getTime().getTime() / 15000;

        return fromPeriod;
    }

    private Long getToPeriod() {
        Calendar nowCal = Calendar.getInstance();
        Long toPeriod = nowCal.getTime().getTime() / 15000;
        return toPeriod;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String jsonResponse = "";

        try {
            JSONObject jsonObject = BuildJsonObjectsUtil.extractRequestJSONContents(request);
            System.out.println("Accepted JSON: \n" + jsonObject);

            if (jsonObject.has("getInstrumentationChartData")) {
                JSONObject keyObject = jsonObject.getJSONObject("getInstrumentationChartData");
                String chartId = keyObject.getString("id");
                String pathFromClient = keyObject.getString("path");
                String chartPath = null;

                int chartTimespan = getChartTimeSpan(keyObject);
                int chartResolution = getChartResolution(keyObject);
                Long fromPeriod = getFromPeriod(chartTimespan);
                Long toPeriod = getToPeriod();

                List<LiveStatistics> liveList = null;
                String seriesLabel = null;
                Alert alert = null;
                if (isAlertChart(keyObject)) {
                    String alertName = pathFromClient.substring(8, pathFromClient.length());
                     alert = getBerkeleyTreeMenuService().getAlert(alertName);
                    if (alert != null) {
                        chartPath = alert.getGuiPath();
                        seriesLabel = "Alert: " + alert.getAlertName();
                    }
                } else {
                    chartPath = pathFromClient;
                    seriesLabel = chartPath;
                    if (seriesLabel.contains(":")) {
                    seriesLabel = seriesLabel.substring(chartPath.lastIndexOf(":") + 1, chartPath.length());
                }
                }

                liveList = getBerkeleyTreeMenuService().getLiveStatistics(chartPath, fromPeriod, toPeriod);
                Collections.sort(liveList);

                XYDataSetCollection valueCollection = new XYDataSetCollection();

                valueCollection = ChartUtil.generateChart(liveList, seriesLabel, fromPeriod * 15000, toPeriod * 15000, chartResolution);

                if (alert != null) {
                    valueCollection.addDataList(ChartUtil.buildWarningList(alert, Alert.CRITICAL, fromPeriod * 15000, toPeriod * 15000));
                    valueCollection.addDataList(ChartUtil.buildWarningList(alert, Alert.WARNING, fromPeriod * 15000, toPeriod * 15000));
                }

                /*GroupedStatistics groupedStatistics = getBerkeleyTreeMenuService().getGroupedStatistics(path);
                if (groupedStatistics != null) {
                    //There are grouped statistics, all must be added to the chart
                    for (String gsPath : groupedStatistics.getGroupedPathList()) {
                        liveList = getBerkeleyTreeMenuService().getLiveStatistics(gsPath, fromPeriod, toPeriod);
                        Collections.sort(liveList);
                        String seriesLabel = gsPath;
                        if (gsPath.length() > path.length() + 1) {
                            seriesLabel = gsPath.substring(path.length() + 1, gsPath.length());
                        }
                        for (XYDataList dataList : ChartUtil.generateChart(liveList, seriesLabel, thenCal.getTime(), nowCal.getTime(), chartResolution).getDataList()) {
                            valueCollection.addDataList(dataList);
                        }
                    }
                } else {
                    Alert alert = getBerkeleyTreeMenuService().getAlert(path);


                }*/

                jsonResponse = BuildJsonObjectsUtil.generateChartData(chartId, chartPath, valueCollection);
                System.out.println("Got Chart Data:\n" + jsonResponse);
            }
        } catch (JSONException jsonException) {
            throw new IOException("Unable to process JSON Request", jsonException);
        }

        PrintWriter writer = response.getWriter();
        writer.write(jsonResponse.toString());
        response.flushBuffer();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}
