package com.limonanarchy.anticheat.reports;

public class Report {

    private final int id;
    private final String reporterName;
    private final String reportedName;
    private final String reason;
    private final long timestamp;

    public Report(int id, String reporterName, String reportedName, String reason, long timestamp) {
        this.id = id;
        this.reporterName = reporterName;
        this.reportedName = reportedName;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getReportedName() {
        return reportedName;
    }

    public String getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
