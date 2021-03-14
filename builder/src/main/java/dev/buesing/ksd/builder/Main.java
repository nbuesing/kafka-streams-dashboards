/*
 * Copyright (c) 2020.
 *
 * This code is provided as-is w/out warranty.
 *
 */

package dev.buesing.ksd.builder;

import dev.buesing.ksd.common.config.OptionsUtil;

public class Main {

    public static void main(String[] args) throws Exception {

        final Options options = OptionsUtil.parse(Options.class, args);

        if (options == null) {
            return;
        }

        new BuildSystem(options).start();

    }

}

