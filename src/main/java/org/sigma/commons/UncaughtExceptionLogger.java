package org.sigma.commons;
/*
 * Copyright (C) 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uncaught exception handler the uses {@link Logger} to record all uncaught exceptions.
 *
 * <p>This ensures that if the structured logging is being used the exception will be logged with
 * correct severity.
 */
public final class UncaughtExceptionLogger implements UncaughtExceptionHandler {
    private static final UncaughtExceptionLogger INSTANCE = new UncaughtExceptionLogger();

    private static final Logger LOG = LoggerFactory.getLogger(UncaughtExceptionLogger.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LOG.error("The template launch failed.", e);

        // Still print crashes to stderr
        e.printStackTrace();
    }

    public static void register() {
        Thread.setDefaultUncaughtExceptionHandler(INSTANCE);
    }
}
