package io.github.vcvitaly.k8cp.controller;

import io.github.vcvitaly.k8cp.TestUtil;

public abstract class TestFxTest {

    public TestFxTest() {
        TestUtil.cleanupContext();
    }
}
