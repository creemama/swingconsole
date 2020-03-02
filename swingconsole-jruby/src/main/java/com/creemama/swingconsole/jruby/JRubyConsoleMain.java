package com.creemama.swingconsole.jruby;

import com.creemama.swingconsole.SwingConsoleFrame;

/**
 * An application entry point that starts up a JRuby Interactive Ruby (IRB)
 * console as a Swing GUI.
 */
public class JRubyConsoleMain {
	public static void main(String[] args) {
		SwingConsoleFrame console = new SwingConsoleFrame("JRuby IRB Console");
		console.run(args, new JRubyConsoleModel(false));
	}
}
