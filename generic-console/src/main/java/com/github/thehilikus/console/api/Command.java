package com.github.thehilikus.console.api;

import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * An action to run. Each action is responsible to request its inputs and print its outputs
 *
 * @author hilikus
 */
public interface Command {
    /**
     * Runs the command
     * 
     * @param input the stream to request user input
     * @param output the stream to write output to
     */
    public void execute(InputStreamReader input, PrintWriter output);

    /**
     * @return a user-friendly explanation of what the command does
     */
    public String getDescription();
}
