package com.creemama.swingconsole.jruby;

import java.io.File;

import com.creemama.swingconsole.ConsoleConfig;

/**
 * An application entry point that starts up a JRuby Interactive Ruby (IRB)
 * console.
 */
public class JRubyConsoleMain {
	public static void main(String[] args) {
		ConsoleConfig config = new ConsoleConfig() //
				// .evalFile("/path/to/script.rb") //
				// .put("$x", new StringBuilder("Hello, World!")) //
				.banner("Welcome to the JRuby IRB Console") //
				.historyFile(new File(System.getProperty("user.home"), ".jruby"));
		new JRubyConsole().run(config);
	}
}
