package com.creemama.swingconsole;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JTextPane;

import jline.console.completer.Completer;
import jline.console.history.History;

/**
 * A text component displaying an interactive console with possible readline
 * support like tab completion and command history.
 */
public class SwingConsolePane extends JTextPane {
	final private static long serialVersionUID = 1L;

	final private TextAreaReadline tar;

	public SwingConsolePane(String message) {
		setBackground(new Color(0xf2, 0xf2, 0xf2));
		setCaretColor(new Color(0xa4, 0x00, 0x00));
		Font font = SwingConsoleUtil.findFont("Monospaced", Font.PLAIN, 14, new String[] { "Monaco", "Andale Mono" });
		setFont(font);
		setForeground(new Color(0xa4, 0x00, 0x00));
		setMargin(new Insets(8, 8, 8, 8));
		tar = new TextAreaReadline(this, message);
	}

	public InputStream getInputStream() {
		return tar.getInputStream();
	}

	public OutputStream getOutputStream() {
		return tar.getOutputStream();
	}

	public void inject(Completer newCompleter, History newHistory) {
		tar.inject(newCompleter, newHistory);
	}

	public String readLine(String prompt) {
		return tar.readLine(prompt);
	}

	public void shutDown() {
		tar.shutdown();
	}
}
