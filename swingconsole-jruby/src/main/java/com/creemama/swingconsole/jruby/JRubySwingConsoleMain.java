package com.creemama.swingconsole.jruby;

import java.util.function.Consumer;

import org.jruby.embed.ScriptingContainer;

import com.creemama.swingconsole.SwingConsoleFrame;

/**
 * An application entry point that starts up a JRuby Interactive Ruby (IRB)
 * console as a Swing GUI.
 */
public class JRubySwingConsoleMain {
	public static void main(String[] args) {
		// Read more about ScriptingContainer at
		// https://github.com/jruby/jruby/wiki/RedBridge.
		Consumer<ScriptingContainer> runAfterContainerInitialization = container -> {

			// Evaluate a script before starting the console:

			// File script = new File("/path/to/script.rb");
			// try (Reader reader = new FileReader(script, Charset.forName("UTF-8"))) {
			// container.runScriptlet(reader, script.getPath());
			// } catch (IOException e) {
			// e.printStackTrace();
			// }

			// Assign variables before starting the console:

			// container.put("$x", "Hello, World!");
		};

		SwingConsoleFrame console = new SwingConsoleFrame("JRuby IRB Console");
		console.run(new JRubySwingConsoleRunnable(false, runAfterContainerInitialization));
	}
}
