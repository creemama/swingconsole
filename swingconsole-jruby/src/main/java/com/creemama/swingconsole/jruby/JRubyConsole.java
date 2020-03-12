package com.creemama.swingconsole.jruby;

import java.io.File;

import org.jruby.Ruby;
import org.jruby.embed.ScriptingContainer;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.internal.runtime.ValueAccessor;

/**
 * A JRuby interactive Ruby (IRB) shell.
 */
public class JRubyConsole {
	final private ScriptingContainer container;

	final private JRubyConsoleHistory history;

	public JRubyConsole(ScriptingContainer container, File historyFile) {
		this.container = container;
		this.history = new JRubyConsoleHistory(historyFile);
	}

	public void run() {
		Ruby runtime = container.getProvider().getRuntime();
		history.setUpHistory(System.out, runtime);
		runtime.getGlobalVariables().defineReadonly("$$",
				new ValueAccessor(runtime.newFixnum(System.identityHashCode(runtime))), GlobalVariable.Scope.GLOBAL);
		runtime.evalScriptlet(
				"ARGV << '--readline' << '--prompt' << 'inf-ruby';" + "require 'irb'; require 'irb/completion';");
		runtime.evalScriptlet("IRB.start");
	}
}
