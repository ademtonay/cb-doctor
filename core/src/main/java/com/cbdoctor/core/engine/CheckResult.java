package com.cbdoctor.core.engine;

import com.cbdoctor.core.model.Finding;

public record CheckResult(Status status, Finding finding) {

    public enum Status { PASS, SKIPPED, FINDING }

    public static CheckResult pass() {
        return new CheckResult(Status.PASS, null);
    }

    public static CheckResult skipped(String checkId, String title, String reason) {
        return new CheckResult(Status.SKIPPED, Finding.skipped(checkId, title, reason));
    }

    public static CheckResult finding(Finding finding) {
        return new CheckResult(Status.FINDING, finding);
    }
}