package com.creemama.swingreadline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public class JReadlineFrame extends JFrame {
	public JReadlineFrame(String title) {
		super(title);
	}

	public void run(String[] args, JReadlineFrameModel model) {
		final ArrayList<String> list = new ArrayList(Arrays.asList(args));

		getContentPane().setLayout(new BorderLayout());
		setSize(700, 600);

		JEditorPane text = new JTextPane();

		text.setMargin(new Insets(8, 8, 8, 8));
		text.setCaretColor(new Color(0xa4, 0x00, 0x00));
		text.setBackground(new Color(0xf2, 0xf2, 0xf2));
		text.setForeground(new Color(0xa4, 0x00, 0x00));
		Font font = findFont("Monospaced", Font.PLAIN, 14, new String[] { "Monaco", "Andale Mono" });

		text.setFont(font);
		JScrollPane pane = new JScrollPane();
		pane.setViewportView(text);
		pane.setBorder(BorderFactory.createLineBorder(Color.darkGray));
		getContentPane().add(pane);
		validate();

		final TextAreaReadline tar = new TextAreaReadline(text, " Welcome to the " + getTitle() + " \n\n");
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				tar.shutdown();
			}
		});

		model.setUp(list, tar);

		Thread t2 = new Thread() {
			@Override
			public void run() {
				setVisible(true);
				model.run(tar);
			}
		};
		t2.start();

		try {
			t2.join();
		} catch (InterruptedException ie) {
			// ignore
		}

		System.exit(0);
	}

	private Font findFont(String otherwise, int style, int size, String[] families) {
		String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		Arrays.sort(fonts);
		Font font = null;
		for (int i = 0; i < families.length; i++) {
			if (Arrays.binarySearch(fonts, families[i]) >= 0) {
				font = new Font(families[i], style, size);
				break;
			}
		}
		if (font == null) {
			font = new Font(otherwise, style, size);
		}
		return font;
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 3746242973444417387L;

}
