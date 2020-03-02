package com.creemama.swingconsole.jep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import com.creemama.swingconsole.SwingConsoleModel;
import com.creemama.swingconsole.TextAreaReadline;

import jep.JepException;
import jep.SharedInterpreter;
import jline.console.history.MemoryHistory;

/**
 * The model for a <a href="https://github.com/ninia/jep">Java Embedded Python
 * (JEP)</a> {@link com.creemama.swingconsole.SwingConsoleFrame}.
 */
public class JepConsoleModel implements SwingConsoleModel {
	private static class QueueCommand {
		private static QueueCommand createEvalCommand(String code) {
			return new QueueCommand(code, QueueCommandType.EVAL, null);
		}

		private static QueueCommand createPutVariableCommand(String variableName, Object value) {
			return new QueueCommand(variableName, QueueCommandType.PUT_VARIABLE, value);
		}

		private final String code;

		private final CompletableFuture<Object> future = new CompletableFuture<>();

		private final QueueCommandType type;

		private final Object value;

		private QueueCommand(String code, QueueCommandType type, Object value) {
			this.code = code;
			this.type = type;
			this.value = value;
		}
	}

	private enum QueueCommandType {
		EVAL, PUT_VARIABLE
	}

	private final LinkedBlockingQueue<QueueCommand> commandQueue = new LinkedBlockingQueue<>();

	private TextAreaReadline tar;

	void processCommands() {
		try (SharedInterpreter interp = new SharedInterpreter()) {

			while (true) {
				QueueCommand command = commandQueue.take();
				switch (command.type) {
				case EVAL:
					processEvalCommand(interp, command);
					break;
				case PUT_VARIABLE:
					processPutVariableCommand(interp, command);
					break;
				default:
					break;
				}
			}
		} catch (InterruptedException e) {
			// Ignore.
		} catch (JepException e) {
			throw new RuntimeException(e);
		} catch (UnsatisfiedLinkError e) {
			/**
			 * We get this exception if there is "no jep in java.library.path". We call
			 * tar.shutdown() so that the while loop in #run ends.
			 * 
			 * <pre>
			 *Exception in thread "JEP Shared Interpreter" java.lang.UnsatisfiedLinkError: no jep in java.library.path: [/Users/user/Library/Java/Extensions, /Library/Java/Extensions, /Network/Library/Java/Extensions, /System/Library/Java/Extensions, /usr/lib/java, .]
			 *	at java.base/java.lang.ClassLoader.loadLibrary(ClassLoader.java:2660)
			 *	at java.base/java.lang.Runtime.loadLibrary0(Runtime.java:829)
			 *	at java.base/java.lang.System.loadLibrary(System.java:1870)
			 *	at jep.MainInterpreter.initialize(MainInterpreter.java:128)
			 *	at jep.MainInterpreter.getMainInterpreter(MainInterpreter.java:101)
			 *	at jep.Jep.<init>(Jep.java:256)
			 *	at jep.SharedInterpreter.<init>(SharedInterpreter.java:56)
			 *	at com.creemama.swingconsole.jep.JepConsoleModel.processCommands(JepConsoleModel.java:58)
			 *	at java.base/java.lang.Thread.run(Thread.java:834)
			 * </pre>
			 */
			tar.shutdown();
			throw e;
		}
	}

	private void processEvalCommand(SharedInterpreter interp, QueueCommand command) {
		try {
			Object result = interp.getValue(command.code);
			if (result != null)
				command.future.complete(result.toString() + "\n");
			else
				command.future.complete(null);
		} catch (JepException getValueException) {
			try {
				interp.exec(command.code);
				command.future.complete(null);
			} catch (JepException execException) {
				StringBuilder errors = new StringBuilder();

				errors.append("jep.Jep.getValue threw " + getValueException.getMessage());
				errors.append("\n\n");
				errors.append("jep.Jep.exec threw " + execException.getMessage());
				errors.append("\n\n");

				command.future.complete(errors.toString());
			}
		}
	}

	private void processPutVariableCommand(SharedInterpreter interp, QueueCommand command) {
		try {
			interp.set(command.code, command.value);
			command.future.complete(null);
		} catch (JepException e) {
			command.future.complete("jep.Jep.set threw " + e.getMessage());
		}
	}

	@Override
	public void putVariable(String variableName, Object value) {
		commandQueue.add(QueueCommand.createPutVariableCommand(variableName, value));
	}

	@Override
	public void run(TextAreaReadline tar) {
		this.tar = tar;
		MemoryHistory history = new MemoryHistory();
		new Thread(this::processCommands, "JEP Shared Interpreter").start();
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(tar.getOutputStream()))) {

			tar.inject(null, history);

			while (true) {
				String line = tar.readLine(">>> ");
				if (!line.isEmpty()) {
					history.add(line);
					try {
						QueueCommand command = QueueCommand.createEvalCommand(line);
						commandQueue.add(command);
						Object result = command.future.get();
						if (result != null) {
							writer.write(result.toString());
							writer.flush();
						}
					} catch (ExecutionException | InterruptedException e) {
						// Ignore.
					}
				}
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void runScript(File script) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setUp(List<String> args, TextAreaReadline tar) {
		// Do nothing.
	}
}
