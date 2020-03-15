package com.creemama.swingconsole.jruby;

import java.io.File;
import java.io.PrintStream;
import java.util.function.Consumer;

import org.jruby.Ruby;
import org.jruby.RubyIO;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.embed.ScriptingContainer;
import org.jruby.ext.readline.Readline;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import com.creemama.swingconsole.SwingConsolePane;
import com.creemama.swingconsole.SwingConsoleRunnable;

import jline.console.completer.Completer;
import jline.console.history.History;

/**
 * A task that hooks up and runs a JRuby interactive Ruby (IRB) shell within a
 * {@link com.creemama.swingconsole.SwingConsoleDialog} or
 * {@link com.creemama.swingconsole.SwingConsoleFrame}.
 * <p>
 * The original code for this class comes from JRuby-Readline's <a href=
 * "https://github.com/jruby/jruby-readline/blob/80c8a97cc595837ec8e89154395d3f4a6ed2eee7/src/main/java/org/jruby/demo/readline/TextAreaReadline.java">TextAreaReadline</a>
 * and <a href=
 * "https://github.com/jruby/jruby-readline/blob/80c8a97cc595837ec8e89154395d3f4a6ed2eee7/src/main/java/org/jruby/demo/readline/IRBConsole.java">IRBConsole</a>.
 * </p>
 * 
 * @see SwingConsoleRunnable
 */
public class JRubySwingConsoleRunnable implements SwingConsoleRunnable {

	final private JRubyConsoleHistory history;

	final private boolean redefineStandardIOStreams;

	final private Consumer<ScriptingContainer> runAfterContainerInitialization;

	/**
	 * Constructs a new {@link JRubySwingConsoleRunnable} instance.
	 * 
	 * @param history                         command-history file
	 * @param redefineStandardIOStreams       whether to redefine JRuby's
	 *                                        {@code $stdin}, {@code $stdout}, and
	 *                                        {@code $stderr} streams
	 * @param runAfterContainerInitialization method used to set up the
	 *                                        {@code ScriptingContainer} after
	 *                                        container initialization (e.g.,
	 *                                        assigning variables or evaluating
	 *                                        scripts)
	 */
	public JRubySwingConsoleRunnable(File history, boolean redefineStandardIOStreams,
			Consumer<ScriptingContainer> runAfterContainerInitialization) {
		this.history = new JRubyConsoleHistory(history);
		this.redefineStandardIOStreams = redefineStandardIOStreams;
		this.runAfterContainerInitialization = runAfterContainerInitialization;
	}

	/**
	 * Hooks the <code>SwingConsolePane</code> instance into the runtime, redefining
	 * the <code>Readline</code> module so that it uses <code>tar</code>. This
	 * method does not redefine the standard input-output streams. If you need that,
	 * use {@link #hookIntoRuntimeWithStreams(Ruby, SwingConsolePane)}.
	 *
	 * @param runtime the Ruby runtime
	 * @param console a text component displaying an interactive console
	 * @see #hookIntoRuntimeWithStreams(Ruby, SwingConsolePane)
	 */
	private void hookIntoRuntime(Ruby runtime, SwingConsolePane console) {
		// Hack in to replace the usual readline with this.
		runtime.getLoadService().require("readline");
		RubyModule readlineM = runtime.getModule("Readline");

		DynamicMethod readlineMethod = new JavaMethod.JavaMethodTwo(readlineM, Visibility.PUBLIC, "readline") {
			@Override
			public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name,
					IRubyObject arg0, IRubyObject arg1) {
				String line = console.readLine(arg0.toString());
				if (line != null) {
					return RubyString.newUnicodeString(runtime, line);
				} else {
					return runtime.getNil();
				}
			}
		};
		readlineM.addMethod("readline", readlineMethod);
		readlineM.getSingletonClass().addMethod("readline", readlineMethod);

		History hist = history.setUpHistory(new PrintStream(console.getOutputStream()), runtime);

		runtime.evalScriptlet(
				"ARGV << '--readline' << '--prompt' << 'inf-ruby';" + "require 'irb'; require 'irb/completion';");

		Completer completer = Readline.getCompletor(Readline.getHolder(runtime));

		console.inject(completer, hist);
	}

	/**
	 * Hooks the <code>SwingConsolePane</code> instance into the runtime, redefining
	 * the <code>Readline</code> module so that it uses <code>tar</code>. This
	 * method also redefines the standard input-output streams accordingly.
	 *
	 * @param runtime the Ruby runtime
	 * @see #hookIntoRuntime(Ruby, SwingConsolePane)
	 */
	private void hookIntoRuntimeWithStreams(Ruby runtime, SwingConsolePane console) {
		hookIntoRuntime(runtime, console);

		RubyIO in = new RubyIO(runtime, console.getInputStream());
		runtime.getGlobalVariables().set("$stdin", in);

		RubyIO out = new RubyIO(runtime, console.getOutputStream());
		out.sync_set(runtime.getTrue());
		runtime.getGlobalVariables().set("$stdout", out);
		runtime.getGlobalVariables().set("$stderr", out);
	}

	@Override
	public void run(SwingConsolePane console) {
		ScriptingContainer container = new ScriptingContainer();
		container.setInput(console.getInputStream());
		container.setOutput(new PrintStream(console.getOutputStream()));
		container.setError(new PrintStream(console.getOutputStream()));

		Ruby runtime = container.getProvider().getRuntime();

		runtime.getGlobalVariables().defineReadonly("$$",
				new ValueAccessor(runtime.newFixnum(System.identityHashCode(runtime))), GlobalVariable.Scope.GLOBAL);

		if (redefineStandardIOStreams)
			hookIntoRuntimeWithStreams(runtime, console);
		else
			hookIntoRuntime(runtime, console);

		if (runAfterContainerInitialization != null)
			runAfterContainerInitialization.accept(container);

		runtime.evalScriptlet("IRB.start");
	}
}
