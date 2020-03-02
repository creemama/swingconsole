package com.creemama.swingconsole.jep;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.ScriptException;

import jep.Jep;
import jep.JepException;
import jep.SharedInterpreter;

/**
 * The model for a <a href="https://github.com/ninia/jep">Java Embedded Python
 * (JEP)</a> console or {@link com.creemama.swingconsole.SwingConsoleFrame}.
 */
public class JepEngine {
	private static class QueueCommand {
		private static QueueCommand createEvalFileCommand(String script) {
			return new QueueCommand(script, QueueCommandType.EVAL_FILE, null);
		}

		private static QueueCommand createEvalStringCommand(String code) {
			return new QueueCommand(code, QueueCommandType.EVAL_STRING, null);
		}

		private static QueueCommand createPutCommand(String variableName, Object value) {
			return new QueueCommand(variableName, QueueCommandType.PUT, value);
		}

		private static QueueCommand createShutDownCommand() {
			return new QueueCommand(null, QueueCommandType.SHUT_DOWN, null);
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
		EVAL_FILE, EVAL_STRING, PUT, SHUT_DOWN
	}

	private static final AtomicInteger THREAD_COUNT = new AtomicInteger();

	private Entry<BlockingQueue<QueueCommand>, Throwable> commandQueue;

	public Object eval(File file) throws ScriptException {
		try {
			QueueCommand command = QueueCommand.createEvalFileCommand(file.getAbsolutePath());
			getCommandQueue().add(command);
			return command.future.get();
		} catch (ExecutionException | InterruptedException | RuntimeException e) {
			throw new ScriptException(e);
		}
	}

	public Object eval(String script) throws ScriptException {
		try {
			QueueCommand command = QueueCommand.createEvalStringCommand(script);
			getCommandQueue().add(command);
			return command.future.get();
		} catch (ExecutionException | InterruptedException | RuntimeException e) {
			throw new ScriptException(e);
		}
	}

	public synchronized BlockingQueue<QueueCommand> getCommandQueue() {
		if (commandQueue == null) {
			CompletableFuture<Void> interpreterCreated = new CompletableFuture<>();
			BlockingQueue<QueueCommand> newCommandQueue = new LinkedBlockingQueue<>();
			Thread interpreterThread = new Thread(() -> processCommands(interpreterCreated),
					"JEP SubInterpreter " + THREAD_COUNT.getAndAdd(1));
			interpreterThread.setDaemon(true);
			interpreterThread.start();
			try {
				interpreterCreated.get();
				commandQueue = new SimpleEntry<>(newCommandQueue, null);
			} catch (ExecutionException | InterruptedException e) {
				commandQueue = new SimpleEntry<>(null, e);
			}
		}
		if (commandQueue.getKey() != null)
			return commandQueue.getKey();
		throw new RuntimeException(commandQueue.getValue());
	}

	private void processCommands(CompletableFuture<Void> interpreterCreated) {
		// According to https://github.com/ninia/jep/wiki/Numpy-Usage, "Numpy does not
		// support Python sub-interpreters. It is recommended to use Jep's
		// SharedInterpreter for applications that import numpy."
		try (SharedInterpreter interp = new SharedInterpreter()) {
			interpreterCreated.complete(null);
			while (true) {
				QueueCommand command = getCommandQueue().take();
				switch (command.type) {
				case EVAL_FILE:
					processEvalFileCommand(interp, command);
					break;
				case EVAL_STRING:
					processEvalStringCommand(interp, command);
					break;
				case PUT:
					processPutCommand(interp, command);
					break;
				case SHUT_DOWN:
					command.future.complete(null);
					return;
				default:
					throw new IllegalStateException("Handle " + command.type + ".");
				}
			}
		} catch (InterruptedException e) {
			// Ignore.
		} catch (JepException e) {
			interpreterCreated.completeExceptionally(e);
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
			interpreterCreated.completeExceptionally(e);
		}
	}

	private void processEvalFileCommand(Jep interp, QueueCommand command) {
		try {
			interp.runScript(command.code);
			command.future.complete(null);
		} catch (JepException e) {
			ScriptException scriptEx = new ScriptException("jep.Jep.runScript threw " + e.getMessage());
			scriptEx.initCause(e);
			command.future.completeExceptionally(scriptEx);
		}
	}

	private void processEvalStringCommand(Jep interp, QueueCommand command) {
		try {
			Object result = interp.getValue(command.code);
			command.future.complete(result == null ? null : result.toString());
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

				ScriptException scriptEx = new ScriptException(errors.toString());
				command.future.completeExceptionally(scriptEx);
			}
		}
	}

	private void processPutCommand(Jep interp, QueueCommand command) {
		try {
			interp.set(command.code, command.value);
			command.future.complete(null);
		} catch (JepException e) {
			ScriptException scriptEx = new ScriptException("jep.Jep.set threw " + e.getMessage());
			scriptEx.initCause(e);
			command.future.completeExceptionally(scriptEx);
		}
	}

	public void put(String variableName, Object value) {
		try {
			QueueCommand command = QueueCommand.createPutCommand(variableName, value);
			getCommandQueue().add(command);
			command.future.get();
		} catch (ExecutionException | InterruptedException | RuntimeException e) {
			throw new RuntimeException(e);
		}
	}

	public void shutDown() {
		try {
			QueueCommand command = QueueCommand.createShutDownCommand();
			getCommandQueue().add(command);
			command.future.get();
		} catch (ExecutionException | InterruptedException | RuntimeException e) {
			throw new RuntimeException(e);
		}
	}
}
