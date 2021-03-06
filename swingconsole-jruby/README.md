# JRuby SwingConsole

> A Java Swing interactive console for [JRuby](https://github.com/jruby/jruby) with readline-esque support

<img alt="JRuby SwingConsole - Hello, World!" src="https://raw.githubusercontent.com/creemama/swingconsole/master/swingconsole-jruby/JRuby-SwingConsole.gif" width="500">

Try it out by executing the following:

```sh
mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.2:copy \
  -Dartifact=com.creemama.swingconsole:swingconsole-jruby:9.2.10.0.0.0.1:jar:jar-with-dependencies \
  -DoutputDirectory=. \
&& java -jar swingconsole-jruby-9.2.10.0.0.0.1-jar-with-dependencies.jar
```

Use the following code snippet as a guide to embed a `JRuby SwingConsole` into your application:

```java
public static void main(String[] args) {
	ConsoleConfig config = new ConsoleConfig()
			.evalFile("/path/to/startup/script.rb")
			.put("$java_variable", new StringBuilder("Console-Accessible Variable"))
			.banner("JRuby {{VERSION}} Java " + System.getProperty("java.version") + "\n")
			.historyFile(new File(System.getProperty("user.home"), ".jruby"));
	SwingConsoleFrame console = new SwingConsoleFrame("JRuby IRB Console");
	console.run(new JRubySwingConsole(config));
}
```

To add JRuby SwingConsole as a dependency in your Maven project, use the following:

```xml
<dependency>
  <groupId>com.creemama.swingconsole</groupId>
  <artifactId>swingconsole-jruby</artifactId>
  <version>9.2.10.0.0.0.1</version>
</dependency>
```
