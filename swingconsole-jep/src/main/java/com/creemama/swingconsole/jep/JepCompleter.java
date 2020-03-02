package com.creemama.swingconsole.jep;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.script.ScriptException;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;

/**
 * The mechanism to resolve tab-completion candidates.
 */
class JepCompleter implements jline.console.completer.Completer, org.jline.reader.Completer {
	static Optional<JepCompleter> create(JepEngine engine) {
		JepCompleter completer = new JepCompleter(engine);
		if (completer.setUp())
			return Optional.of(completer);
		return Optional.empty();
	}

	final private JepEngine engine;

	private JepCompleter(JepEngine engine) {
		this.engine = engine;
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		List<CharSequence> tempCandidates = new ArrayList<>();
		complete(tempCandidates, line.wordCursor(), line.word());
		tempCandidates.stream().map(CharSequence::toString).map(Candidate::new).forEach(candidates::add);
	}

	private void complete(List<CharSequence> candidates, int cursor, String word) {
		try {
			int i = 0;
			while (true) {
				Object result = engine
						.eval("readline.get_completer()(\"" + word.replaceAll("\"", "\\\"") + "\", " + i + ")");
				if (result == null)
					return;
				candidates.add(result.toString());
				i++;
			}
		} catch (ScriptException e) {
			return;
		}
	}

	@Override
	public int complete(String buffer, int cursor, List<CharSequence> candidates) {
		ParsedLine line = new DefaultParser().parse(buffer, cursor);
		complete(candidates, line.wordCursor(), line.word());
		return line.cursor() - line.wordCursor();
	}

	private boolean setUp() {
		try {
			// See https://github.com/ninia/jep/issues/235 for a discussion about readline.
			// Compare this with
			// https://github.com/ninia/jep/blob/master/src/main/python/jep/console.py.
			engine.eval("import readline");
		} catch (ScriptException readlineException) {
			try {
				engine.eval("import pyreadline as readline");
			} catch (ScriptException pyreadlineException) {
				return false;
			}
		}
		try {
			engine.eval("import rlcompleter");
			engine.eval("readline.set_completer(rlcompleter.Completer(locals()).complete)");
		} catch (ScriptException e) {
			return false;
		}
		return true;
	}
}
