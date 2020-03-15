package com.creemama.swingconsole;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for an interactive console.
 */
public class ConsoleConfig {
	public static class EvalFileStartupCommand implements StartupCommand {
		final private File file;

		EvalFileStartupCommand(File file) {
			this.file = file;
		}

		@Override
		public void accept(StartupCommandVisitor visitor) {
			visitor.visit(this);
		}

		public File getFile() {
			return file;
		}
	}

	public static class PutStartupCommand implements StartupCommand {
		final private Object value;

		final private String variableName;

		PutStartupCommand(Object value, String variableName) {
			this.value = value;
			this.variableName = variableName;
		}

		@Override
		public void accept(StartupCommandVisitor visitor) {
			visitor.visit(this);
		}

		public Object getValue() {
			return value;
		}

		public String getVariableName() {
			return variableName;
		}
	}

	public interface StartupCommand {
		void accept(StartupCommandVisitor visitor);
	}

	public interface StartupCommandVisitor {
		void visit(EvalFileStartupCommand command);

		void visit(PutStartupCommand command);
	}

	private String banner;

	private File historyFile;

	final private List<StartupCommand> startupCommands;

	public ConsoleConfig() {
		this.startupCommands = new LinkedList<>();
	}

	public ConsoleConfig banner(String string) {
		this.banner = string;
		return this;
	}

	public ConsoleConfig eval(File file) {
		startupCommands.add(new EvalFileStartupCommand(Objects.requireNonNull(file)));
		return this;
	}

	public ConsoleConfig evalFile(String path) {
		return eval(new File(path));
	}

	public Optional<String> getBanner() {
		return Optional.ofNullable(banner);
	}

	public Optional<File> getHistoryFile() {
		return Optional.ofNullable(historyFile);
	}

	public List<StartupCommand> getStartupCommands() {
		return Collections.unmodifiableList(startupCommands);
	}

	public ConsoleConfig historyFile(File file) {
		this.historyFile = file;
		return this;
	}

	public ConsoleConfig put(String variableName, Object value) {
		startupCommands.add(new PutStartupCommand(value, Objects.requireNonNull(variableName)));
		return this;
	}
}
