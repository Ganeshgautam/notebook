package com.ganesh.velocity.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionFileParser {

    public static Map<String, Set<String>> findActionGetIsMethods(String path) {
        // Initialize the map to store class vs methods
        Map<String, Set<String>> classMethodsMap = new HashMap<>();

        // Recursively traverse the directory
        traverseDirectory(new File(path), classMethodsMap);

        return classMethodsMap;
    }

    private static void traverseDirectory(File directory, Map<String, Set<String>> classMethodsMap) {
        // Check if the directory is valid
        if (directory.isDirectory()) {
            // List all files in the directory
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Recursive call for subdirectories
                        traverseDirectory(file, classMethodsMap);
                    } else if (file.isFile() && (file.getName().endsWith("Action.java") || file.getName().contains("ConfluenceActionSupport"))) {
                        // Process .java files
                        try {
                            extractClassMethods(file, classMethodsMap);
                        } catch (IOException e) {
                            System.err.println("Error reading file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    private static void extractClassMethods(File file, Map<String, Set<String>> classMethodsMap) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        String className = file.getName().replace(".java", "");
        Set<String> methods = new HashSet<>();

        for (String line : lines) {
            String regex = "((get|is)([a-zA-Z_$][a-zA-Z\\d_$]*))\\s*\\(.*\\)\\s*\\{";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(line);
            if(matcher.find()) {
                String method = matcher.group(1);
                methods.add(method);
            }
        }

        // Add class and methods to the map
        if (className != null && !methods.isEmpty()) {
            classMethodsMap.put(className, methods);
        }
    }
}
