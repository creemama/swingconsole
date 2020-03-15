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
				// .evalFile("/path/to/startup/script.rb") //
				// .put("$java_variable", new StringBuilder("Console-Accessible Variable")) //
				.banner("JRuby {{VERSION}} Java " + System.getProperty("java.version") + "\n") //
				.historyFile(new File(System.getProperty("user.home"), ".jruby"));
		SwingConsoleFrame console = new SwingConsoleFrame("JRuby IRB Console");
		console.run(new JRubySwingConsole(config));
	}
}
