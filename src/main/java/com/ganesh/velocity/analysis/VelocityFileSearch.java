package com.ganesh.velocity.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VelocityFileSearch {
    static Set<Match> matchesSet = new HashSet<>();
    static Set<String> foundMethodNamesInVMs = new HashSet<>();
    static Set<String> allowedMethodSet = new HashSet<>();

    static String directoryPath = "/Users/ggautam/Work/source/atlassian/atlassian-oauth";
    static String confluenceDirPath = "/Users/ggautam/Work/source/atlassian/confluence";
    static String outputFileName = "matches-all.csv";
    static String outputFile = outputFileName + "-" + directoryPath.split("/")[(directoryPath.split("/").length - 1)];

    static String onlyMethodNamesFileName = "matches-method-names.csv";
    static String onlyMethodNamesFile = onlyMethodNamesFileName + "-" + directoryPath.split("/")[(directoryPath.split("/").length - 1)];

    static String regex = "\\$[a-zA-Z_$][a-zA-Z\\d_$]*\\s*\\.([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*";
    static String methodAllowlistRegex = "<method>.+#([a-zA-Z_$][a-zA-Z\\d_$]*)";

    static String[] skipObjects = new String[] {
            "$htmlUtil",
            "$velocityUtil",
            "$generalUtil",
            "$stringUtils"
    };

    public static void main(String[] args) throws IOException {
        System.out.println("Populating VM data set for methods");
        startSearchAndPopulateSets(regex, directoryPath);

        System.out.println("Scanning plugin XMLs");
        loadMethodNamesFromPluginXMLs(methodAllowlistRegex, directoryPath);

        System.out.println("Scanning core plugin XMLs");
        loadMethodNamesFromPluginXMLs(methodAllowlistRegex, confluenceDirPath);

        System.out.println("Scanning velocity-default.properties XMLs");
        loadMethodNamesFromVelocityProperties();

        System.out.println("Cleaning up method file mappings");
        cleanUpMatchesSet();

        System.out.println("Cleaning up methods list");
        cleanUpFoundMethodNamesInVMsSet();

        System.out.println("Publishing output files");
        publishFiles(outputFile, onlyMethodNamesFile);

        System.out.println("Search completed. Results written to " + outputFile);
    }

    private static void startSearchAndPopulateSets(String regex, String directoryPath) throws IOException {
        Pattern pattern = Pattern.compile(regex);
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".vm") || path.toString().endsWith(".vmd"))
                .forEach(file -> {
                    try {
                        searchInFilesForMethodNames(file, pattern);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void loadMethodNamesFromPluginXMLs(String regex, String directoryPath) throws IOException {
        Pattern pattern = Pattern.compile(regex);
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".xml") && path.toString().contains("resources"))
                .forEach(file -> {
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(file.toFile()));
                        searchAndPrepareMatchingAllowedSets(file, pattern, reader);
                        reader.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        System.out.println(allowedMethodSet);
    }

    private static void loadMethodNamesFromVelocityProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream("/Users/ggautam/Work/source/atlassian/confluence/confluence-core/confluence/src/main/resources/velocity-default.properties")) {
            properties.load(inputStream);

            String[] methods = properties.getProperty("introspector.proper.allowlist.methods").split(",");

            Set<String> methodSet = extractMethodNamesFromAllowlist(methods);
            System.out.println(methodSet);

            allowedMethodSet.addAll(methodSet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void cleanUpMatchesSet() {
        Arrays.stream(skipObjects).forEach(o -> matchesSet.removeIf(s -> s.line.toLowerCase().startsWith(o.toLowerCase())));

        allowedMethodSet.forEach(m -> {
            matchesSet.removeIf(s -> s.method.equalsIgnoreCase(m));
            if(m.startsWith("get") || m.startsWith("set")) {
                matchesSet.removeIf(s -> s.method.equalsIgnoreCase(m.substring(3)));
            } else if(m.startsWith("is")) {
                matchesSet.removeIf(s -> s.method.equalsIgnoreCase(m.substring(2)));
            }
        });
    }

    private static void cleanUpFoundMethodNamesInVMsSet() {
        foundMethodNamesInVMs.removeAll(allowedMethodSet);
        allowedMethodSet.forEach(m -> {
            foundMethodNamesInVMs.removeIf(s -> s.equalsIgnoreCase(m));
            if(m.startsWith("get") || m.startsWith("set")) {
                foundMethodNamesInVMs.removeIf(s -> s.equalsIgnoreCase(m.substring(3)));
            } else if(m.startsWith("is")) {
                foundMethodNamesInVMs.removeIf(s -> s.equalsIgnoreCase(m.substring(2)));
            }
        });
    }

    private static void publishFiles(String outputFile, String onlyMethodNamesFile) throws IOException {
        FileWriter fileWriter = new FileWriter(outputFile);
        FileWriter onlyMethodNamesFileWriter = new FileWriter(onlyMethodNamesFile);

        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT.withHeader("Match", "Line", "Filename"));
        CSVPrinter onlyMethodNamesCsvPrinter = new CSVPrinter(onlyMethodNamesFileWriter, CSVFormat.DEFAULT.withHeader("Match"));

        printMethodAndLines(csvPrinter, matchesSet);
        printMethodNamesOnly(onlyMethodNamesCsvPrinter, foundMethodNamesInVMs);

        csvPrinter.close();
        onlyMethodNamesCsvPrinter.close();
    }

    private static Set<String> extractMethodNamesFromAllowlist(String[] methods) {
        Pattern pattern = Pattern.compile("#([a-zA-Z_$][a-zA-Z\\d_$]*)");
        return Arrays.stream(methods)
                .map(m -> {
                    Matcher matcher = pattern.matcher(m);
                    if(matcher.find()) {
                        return matcher.group(1);
                    }
                    return m;
                })
                .collect(Collectors.toSet());
    }


    private static void searchInFilesForMethodNames(Path file, Pattern pattern) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file.toFile()));

        searchAndPrepareMatchingSets(file, pattern, reader);

        reader.close();
    }

    private static void searchAndPrepareMatchingSets(Path file, Pattern pattern, BufferedReader reader) throws IOException {
        String line;

        while ((line = reader.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String match = matcher.group();
                String capturedMethod = matcher.group(1); // Capture group within the regex
                if(match.contains("i18n") ||
                        (match.contains("action.")
//                                && (capturedMethod.startsWith("get") || capturedMethod.startsWith("is"))
                        )) {
                    continue;
                }
                matchesSet.add(new Match(capturedMethod, match, file.getFileName().toString()));
                foundMethodNamesInVMs.add(capturedMethod);
            }
        }
    }

    private static void searchAndPrepareMatchingAllowedSets(Path file, Pattern pattern, BufferedReader reader) throws IOException {
        String line;

        while ((line = reader.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String match = matcher.group();
                String capturedMethod = matcher.group(1); // Capture group within the regex
                allowedMethodSet.add(capturedMethod);
            }
        }
    }

    private static void printMethodNamesOnly(CSVPrinter onlyMethodNamesCsvPrinter, Set<String> onlyMethodNamesSet) {
        onlyMethodNamesSet.forEach(methodName -> {
            try {
                onlyMethodNamesCsvPrinter.printRecord(methodName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void printMethodAndLines(CSVPrinter csvPrinter, Set<Match> matchesSet) {
        matchesSet.forEach(match -> {
            try {
                csvPrinter.printRecord(match.method, match.line, match.filename);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    record Match(String method, String line, String filename) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Match match = (Match) obj;
            return method.equals(match.method) && filename.equals(match.filename);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, filename);
        }
    }
}
