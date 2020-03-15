package com.creemama.swingconsole.jruby;

import java.io.File;

import org.jruby.embed.ScriptingContainer;

/**
 * An application entry point that starts up a JRuby Interactive Ruby (IRB)
 * console.
 */
public class JRubyConsoleMain {
	public static void main(String[] args) {
		// Read more about ScriptingContainer at
		// https://github.com/jruby/jruby/wiki/RedBridge.
		ScriptingContainer container = new ScriptingContainer();

		// Evaluate a script before starting the console:

		// File script = new File("/path/to/script.rb");
		// try (Reader reader = new FileReader(script, Charset.forName("UTF-8"))) {
		// container.runScriptlet(reader, script.getPath());
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		// Assign variables before starting the console:

		// container.put("$x", "Hello, World!");

		File historyFile = new File(System.getProperty("user.home"), ".jruby");

		new JRubyConsole().run(container, historyFile);
	}
}
