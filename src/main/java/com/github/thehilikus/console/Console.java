/**
 * 
 */
package com.github.thehilikus.console;

import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.github.thehilikus.console.api.Command;

/**
 * The console core
 *
 * @author hilikus
 */
public class Console {

    private List<Command> commands;

    private Reflections reflections;

    private Scanner scanner;

    private InputStreamReader input;

    private PrintWriter output;

    private PrintStream error;

    private List<String> commandsPaths;

    /**
     * @param args the paths to scan for commands
     */
    public static void main(String[] args) {
	List<String> paths = Arrays.asList(args);

	Console mainConsole = new Console(paths);

	mainConsole.start();
    }

    /**
     * Constructs the console with commands in the given paths
     * 
     * @param commandsPaths a list of paths with commands to load to present as options
     */
    public Console(List<String> commandsPaths) {

	this.commandsPaths = commandsPaths;

	commands = new ArrayList<>();
	input = new InputStreamReader(System.in);
	output = new PrintWriter(System.out, true);
	error = System.err;
	scanner = new Scanner(input);

    }

    /**
     * Starts execution
     */
    public void start() {
	output.println("Starting console\n");

	while (runOnce());

	output.println("Terminating console");
    }

    private boolean runOnce() {
	populateAvailableCommands();

	output.println("Please choose one of the available commands or press ENTER to exit\n");

	printCommands();

	String selectionString = scanner.nextLine().trim();

	if (!selectionString.isEmpty()) {
	    Command toRun = getCommandFromSelection(selectionString);

	    if (toRun != null) {
		try {
		    output.println();
		    toRun.execute(input, output);
		} catch (RuntimeException exc) {
		    error.println("[ERROR] Error running command <" + toRun + ">. Exception was");
		    exc.printStackTrace(error);
		}
	    } else {
		error.println("[ERROR] Selection <" + selectionString + "> is not a valid option, try again");
	    }

	} else {
	    return false;
	}

	output.println("\n##############################################\n\n");
	
	return true;
    }

    private void populateAvailableCommands() {
	commands.clear();

	ConfigurationBuilder configBuilder = createReflectionsConfigBuilder();

	reflections = new Reflections(configBuilder);
	Set<Class<? extends Command>> commandsClasses = reflections.getSubTypesOf(Command.class);

	if (commandsClasses.isEmpty()) {
	    throw new IllegalStateException("No commands found");
	}

	for (Class<? extends Command> commandClass : commandsClasses) {
	    try {
		commands.add(commandClass.newInstance());
		Collections.sort(commands, (comm1, comm2) -> comm1.getDescription().compareTo(comm2.getDescription()));
	    } catch (InstantiationException | IllegalAccessException exc) {
		error.println("[ERROR] (populateCommands) error instantiating command " + commandClass + ":\n" + exc);
	    }
	}
    }

    private ConfigurationBuilder createReflectionsConfigBuilder() {
	URLClassLoader classloader = createClassLoader();
	ConfigurationBuilder result = new ConfigurationBuilder();

	result.addClassLoader(classloader);;
	result.setScanners(new SubTypesScanner(true));
	result.setUrls(ClasspathHelper.forClassLoader(classloader));

	return result;
    }

    private URLClassLoader createClassLoader() {
	URL[] paths = commandsPaths.stream().map(this::stringPathToUrl).toArray(URL[]::new);

	URLClassLoader result = new URLClassLoader(paths);
	return result;
    }

    private URL stringPathToUrl(String path) {
	URL result;
	try {
	    result = new File(path).toURI().toURL();
	} catch (MalformedURLException exc) {
	    throw new RuntimeException(exc);
	}

	return result;
    }

    private void printCommands() {
	for (int pos = 0; pos < commands.size(); pos++) {
	    output.println((pos + 1) + ") " + commands.get(pos).getDescription());
	}
    }

    private Command getCommandFromSelection(String selectionString) {
	Command result = null;

	if (isValidSelection(selectionString)) {
	    int selection = Integer.parseInt(selectionString) - 1;
	    result = commands.get(selection);
	    assert result != null; // selection already validated
	}

	return result;
    }

    private boolean isValidSelection(String selection) {
	if (selection.matches("\\d+")) {
	    int selectionInt = Integer.parseInt(selection) - 1;
	    if (selectionInt <= commands.size()) {
		return true;
	    }
	}
	return false;
    }

}
