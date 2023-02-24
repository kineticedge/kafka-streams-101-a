package io.kineticedge.ks101.consumer;

import io.kineticedge.ks101.common.config.OptionsUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    final static Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            System.err.println("Uncaught exception in thread '" + t.getName() + "': " + e.getMessage());
        }
    };

    public static void main(String[] args) throws Exception {

        final Options options = OptionsUtil.parse(Options.class, args);

        if (options == null) {
            return;
        }

        final Consumer consumer = new Consumer(options);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("PROPER SHUTDOWN");
            consumer.close();
        }));


        final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            final Thread t = Executors.defaultThreadFactory().newThread(r);
            //t.setDaemon(true);
            t.setUncaughtExceptionHandler(exceptionHandler);
            return t;
        });

        executor.submit(() -> {
            consumer.consume();
        });
    }

}

