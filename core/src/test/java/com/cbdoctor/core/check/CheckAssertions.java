package com.cbdoctor.core.check;

import com.cbdoctor.core.engine.CheckResult;
import com.cbdoctor.core.model.Finding;
import com.cbdoctor.core.model.Severity;
import org.junit.jupiter.api.Assertions;

/**
 * Test assertions for CheckResult.
 */
public final class CheckAssertions {

    private CheckAssertions() {
    }

    public static void assertPass(CheckResult result) {
        Assertions.assertNotNull(result, "result must not be null");
        Assertions.assertEquals(CheckResult.Status.PASS, result.status(), "expected PASS");
        Assertions.assertNull(result.finding(), "PASS must not produce a Finding");
    }

    public static Finding assertFinding(CheckResult result, Severity expectedSeverity) {
        Assertions.assertNotNull(result, "result must not be null");
        Assertions.assertEquals(CheckResult.Status.FINDING, result.status(), "expected FINDING");
        Assertions.assertNotNull(result.finding(), "FINDING must include a Finding");
        Assertions.assertEquals(expectedSeverity, result.finding().severity(), "unexpected severity");
        return result.finding();
    }

    public static Finding assertSkipped(CheckResult result) {
        Assertions.assertNotNull(result, "result must not be null");
        Assertions.assertEquals(CheckResult.Status.SKIPPED, result.status(), "expected SKIPPED");
        Assertions.assertNotNull(result.finding(), "SKIPPED must include a Finding");
        Assertions.assertEquals(Severity.SKIPPED, result.finding().severity(), "SKIPPED must have SKIPPED severity");
        return result.finding();
    }
}
