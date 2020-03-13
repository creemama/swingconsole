# JRuby SwingConsole

> A Java Swing interactive console for [JRuby](https://github.com/jruby/jruby) with readline-esque support

<img alt="JRuby SwingConsole - Hello, World!" src="https://raw.githubusercontent.com/creemama/swingconsole/master/swingconsole-jruby/JRuby-SwingConsole.gif" width="500">

Try it out by executing the following:

```sh
mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.2:copy \
  -Dartifact=com.creemama.swingconsole:swingconsole-jruby:9.2.10.0.0.0.1-SNAPSHOT:jar:jar-with-dependencies \
  -DoutputDirectory=. \
&& java -jar swingconsole-jruby-9.2.10.0.0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

Use the following code snippet as a guide to embed a `JRuby SwingConsole` into your application:

```java
public static void main(String[] args) {
	// Read more about ScriptingContainer at
	// https://github.com/jruby/jruby/wiki/RedBridge.
	Consumer<ScriptingContainer> runAfterContainerInitialization = container -> {

		// Evaluate a script before starting the console:

		File script = new File("/path/to/script.rb");
		try (Reader reader = new FileReader(script, Charset.forName("UTF-8"))) {
			container.runScriptlet(reader, script.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Assign variables before starting the console:

		container.put("$x", "Hello, World!");
	};

	SwingConsoleFrame console = new SwingConsoleFrame("JRuby IRB Console");
	File historyFile = new File(System.getProperty("user.home"), ".jruby");
	console.run(new JRubySwingConsoleRunnable(historyFile, false, runAfterContainerInitialization));
}
```