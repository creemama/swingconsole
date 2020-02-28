package org.jruby.demo.readline;

import com.creemama.swingreadline.JReadlineFrame;

/**
 * An application entry point that starts up a JRuby Interactive Ruby (IRB)
 * console as a Swing GUI.
 */
public class JRubyReadlineMain {
	public static void main(String[] args) {
		JReadlineFrame console = new JReadlineFrame("JRuby IRB Console");
		console.run(args, new JRubyReadlineFrameModel(false));
	}
}
