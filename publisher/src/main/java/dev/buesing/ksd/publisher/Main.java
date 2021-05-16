package dev.buesing.ksd.publisher;

import dev.buesing.ksd.common.config.OptionsUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

	public static void main(String[] args) throws Exception{

		final Options options = OptionsUtil.parse(Options.class, args);

		if (options == null) {
			return;
		}

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			new Producer(options).start();
		});
	}

}

