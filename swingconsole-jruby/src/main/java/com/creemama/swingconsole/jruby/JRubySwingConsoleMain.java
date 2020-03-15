package com.creemama.swingconsole.jruby;

import java.io.File;

import com.creemama.swingconsole.ConsoleConfig;
import com.creemama.swingconsole.SwingConsoleFrame;

/**
 * An application entry point that starts up a JRuby Interactive Ruby (IRB)
 * console as a Swing GUI.
 */
public class JRubySwingConsoleMain {
	public static void main(String[] args) {
		ConsoleConfig config = new ConsoleConfig() //
				// .evalFile("/path/to/script.rb") //
				// .put("$x", new StringBuilder("Hello, World!")) //
				// .banner("Welcome!") //
				.historyFile(new File(System.getProperty("user.home"), ".jruby"));
		SwingConsoleFrame console = new SwingConsoleFrame("JRuby IRB Console");
		console.run(new JRubySwingConsoleRunnable(config));
	}
}
