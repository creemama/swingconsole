package com.creemama.swingconsole.jruby;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.jruby.Ruby;
import org.jruby.ext.readline.Readline;

import jline.console.history.FileHistory;
import jline.console.history.History;

/**
 * Manager of the persistence of command history (if given a non-{@code null}
 * history file).
 */
class JRubyConsoleHistory {
	final private File historyFile;

	JRubyConsoleHistory(File historyFile) {
		this.historyFile = historyFile;
	}

	History setUpHistory(PrintStream err, Ruby runtime) {
		runtime.getLoadService().require("readline");
		History history = Readline.getHistory(Readline.getHolder(runtime));

		if (historyFile == null)
			return history;

		try {
			FileHistory fileHistory = new FileHistory(historyFile);

			fileHistory.forEach(entry -> history.add(entry.value()));

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					fileHistory.clear();
					history.forEach(entry -> fileHistory.add(entry.value()));
					fileHistory.flush();
				} catch (IOException e) {
					e.printStackTrace(err);
				}
			}, "JRuby History Writer"));

		} catch (IOException | RuntimeException e) {
			e.printStackTrace(err);
		}

		return history;
	}
}
