package io.kineticedge.ks101.builder;

import io.kineticedge.ks101.tools.config.OptionsUtil;

public class Main {

    public static void main(String[] args) throws Exception {

        final Options options = OptionsUtil.parse(Options.class, args);

        if (options == null) {
            return;
        }

        new BuildSystem(options).start();
    }
}

