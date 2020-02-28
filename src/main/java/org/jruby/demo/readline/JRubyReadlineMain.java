package org.jruby.demo.readline;

/**
 * An application entry point that starts up a JRuby Interactive Ruby (IRB)
 * console as a Swing GUI.
 */
public class JRubyReadlineMain {
	public static void main(String[] args) {
		IRBConsole console = new IRBConsole("JRuby IRB Console");
		console.run(args, new JRubyReadlineFrameModel(false));
	}
}
