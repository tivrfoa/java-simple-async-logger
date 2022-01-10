/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package org.tivrfoa;

class Bar {
    private static final Logger logger = new Logger(Level.DEBUG, Bar.class.getName());

    public void doIt() {
        logger.debug("doing my job");
        // logger.info("doing info");
        // logger.error("doing error");
    }

    public static void main(String[] args) {
        new Bar().doIt();
    }
}