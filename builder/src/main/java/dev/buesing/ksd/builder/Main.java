package dev.buesing.ksd.builder;

import dev.buesing.ksd.tools.config.OptionsUtil;

public class Main {

    public static void main(String[] args) throws Exception {

        final Options options = OptionsUtil.parse(Options.class, args);

        if (options == null) {
            return;
        }

        new BuildSystem(options).start();

    }

}

