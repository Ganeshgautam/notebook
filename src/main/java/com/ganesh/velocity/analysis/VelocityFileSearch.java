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
    static Set<Match> fullMatchesFoundInVMs = new HashSet<>();
    static Set<String> foundMethodNamesInVMs = new HashSet<>();
    static Set<String> coreAllowedMethodSet = new HashSet<>();
    static Set<String> pluginAllowedMethodSet = new HashSet<>();

    static String[] directoryPaths = {
//            "/Users/ggautam/Work/data/temp/atlassian/application-links",
//            "/Users/ggautam/Work/data/temp/atlassian/atlassian-analytics",
//            "/Users/ggautam/Work/data/temp/atlassian/atlassian-bot-killer-plugin",
//            "/Users/ggautam/Work/data/temp/atlassian/atlassian-keyboard-shortcuts",
//            "/Users/ggautam/Work/data/temp/atlassian/atlassian-mail",
//            "/Users/ggautam/Work/data/temp/atlassian/atlassian-nav-links",
//            "/Users/ggautam/Work/data/temp/atlassian/atlassian-oauth",
//            "/Users/ggautam/Work/data/temp/atlassian/atlassian-plugins-osgi-testrunner-parent",
//            "/Users/ggautam/Work/data/temp/atlassian/atlassian-plugins-viewer",
//            "/Users/ggautam/Work/data/temp/atlassian/atlassian-streams",
//            "/Users/ggautam/Work/data/temp/atlassian/functest-plugin",
//            "/Users/ggautam/Work/data/temp/atlassian/personal-access-tokens",
//            "/Users/ggautam/Work/data/temp/atlassian/rest-api-browser",
//            "/Users/ggautam/Work/data/temp/atlassian/confluence-toc-plugin",
//            "/Users/ggautam/Work/data/temp/confluence-questions",
            "/Users/ggautam/Work/data/temp/confluence-ancillary-plugins",
            "/Users/ggautam/Work/data/temp/confluence-content-plugins",
            "/Users/ggautam/Work/data/temp/confluence-frontend-plugins",
            "/Users/ggautam/Work/data/temp/confluence-jira-integration-plugins",
            "/Users/ggautam/Work/data/temp/confluence-open-plugins",
            "/Users/ggautam/Work/data/temp/confluence-public-plugins",
            "/Users/ggautam/Work/data/temp/confluence",
    };

    static String confluenceDirPath = "/Users/ggautam/Work/source/atlassian/confluence";
    static String outputFileName = "matches-all";

    static String onlyMethodNamesFileName = "matches-method-names";

    // Main regex for covering normal cases, minimum false positives
    static String regex = "\\$!?[a-zA-Z_$][a-zA-Z\\d_$]*\\s*\\.([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*";

    //Regex to cover nested cases, can cause false positives
//    static String regex = "\\$!?[a-zA-Z_$][a-zA-Z\\d_$]*\\s*\\..*\\.([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*";

    // Regex to cover non-unique method scan algo, can cause false positives
//    static String regex = "\\$!?[a-zA-Z_$][a-zA-Z\\d_$]*\\s*\\.([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*";

    static String methodAllowlistRegex = "<method>.+#([a-zA-Z_$][a-zA-Z\\d_$]*)";

    static String[] skipObjects = new String[] {
            "$htmlUtil",
            "$velocityUtil",
            "$generalUtil",
            "$stringUtils",
            "$!htmlUtil",
            "$!velocityUtil",
            "$!generalUtil",
            "$!stringUtils"
    };

    public static void main(String[] args) throws IOException {
        System.out.println("Scanning velocity-default.properties XMLs");
        loadMethodNamesFromVelocityProperties();

        System.out.println("Scanning core plugin XMLs");
        loadMethodNamesFromPluginXMLs(methodAllowlistRegex, confluenceDirPath, coreAllowedMethodSet);

        for (String directoryPath : directoryPaths) {
            System.out.println("Scanning " + directoryPath);
            resetContainers();

            System.out.println("Scanning plugin XMLs - " +  directoryPath);
            loadMethodNamesFromPluginXMLs(methodAllowlistRegex, directoryPath, pluginAllowedMethodSet);

            System.out.println("Populating VM data set for methods - " +  directoryPath);
            crawlVMFilesAndPopulateJavaMethodCalls(regex, directoryPath);

            System.out.println("Cleaning up method file mappings - " +  directoryPath);
            filterUpMatchesInVMSet();

            System.out.println("Cleaning up methods list - " +  directoryPath);
            filterUpJustMethodMatchesInVMsSet();

            String outputFile = outputFileName + "-" + directoryPath.split("/")[(directoryPath.split("/").length - 1)] + ".csv";
            String onlyMethodNamesFile = onlyMethodNamesFileName + "-" + directoryPath.split("/")[(directoryPath.split("/").length - 1)] + ".csv";
            System.out.println("Publishing output files - " +  directoryPath);
            publishFiles(outputFile, onlyMethodNamesFile);

            System.out.println("Search completed. Results written to " + outputFile);
        }
        System.out.println("Search finished. Results written to ");
    }

    private static void resetContainers() {
        fullMatchesFoundInVMs.clear();
        foundMethodNamesInVMs.clear();
        pluginAllowedMethodSet.clear();
        pluginAllowedMethodSet.addAll(coreAllowedMethodSet);
    }

    private static void crawlVMFilesAndPopulateJavaMethodCalls(String regex, String directoryPath) throws IOException {
        Pattern pattern = Pattern.compile(regex);
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".vm") || path.toString().endsWith(".vmd"))
                .forEach(velocityFile -> {
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(velocityFile.toFile()));
                        searchAndPrepareMatchingSets(velocityFile, pattern, reader);
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void loadMethodNamesFromPluginXMLs(String regex, String directoryPath, Set<String> allowedMethodSet) throws IOException {
        Pattern pattern = Pattern.compile(regex);
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".xml") && path.toString().contains("resources"))
                .forEach(file -> {
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(file.toFile()));
                        searchAndPrepareMatchingAllowedSets(file, pattern, reader, allowedMethodSet);
                        reader.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        System.out.println(pluginAllowedMethodSet);
    }

    private static void loadMethodNamesFromVelocityProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream("/Users/ggautam/Work/source/atlassian/confluence/confluence-core/confluence/src/main/resources/velocity-default.properties")) {
            properties.load(inputStream);

            String[] methods = properties.getProperty("introspector.proper.allowlist.methods").split(",");

            Set<String> methodSet = extractMethodNamesFromAllowlist(methods);
            System.out.println(methodSet);

            coreAllowedMethodSet.addAll(methodSet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void filterUpMatchesInVMSet() {
        Arrays.stream(skipObjects).forEach(o -> fullMatchesFoundInVMs.removeIf(s -> s.line.toLowerCase().startsWith(o.toLowerCase())));

        pluginAllowedMethodSet.forEach(m -> {
            fullMatchesFoundInVMs.removeIf(s -> s.method.equalsIgnoreCase(m));
            if(m.startsWith("get") || m.startsWith("set")) {
                fullMatchesFoundInVMs.removeIf(s -> s.method.equalsIgnoreCase(m.substring(3)));
            } else if(m.startsWith("is")) {
                fullMatchesFoundInVMs.removeIf(s -> s.method.equalsIgnoreCase(m.substring(2)));
            }
        });
    }

    private static void filterUpJustMethodMatchesInVMsSet() {
        foundMethodNamesInVMs.removeAll(pluginAllowedMethodSet);
        pluginAllowedMethodSet.forEach(m -> {
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

        printMethodAndLines(csvPrinter, fullMatchesFoundInVMs);
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
                fullMatchesFoundInVMs.add(new Match(capturedMethod, match, file.getFileName().toString()));
                foundMethodNamesInVMs.add(capturedMethod);
            }
        }
    }

    private static void searchAndPrepareMatchingAllowedSets(Path file, Pattern pattern, BufferedReader reader, Set<String> allowedMethodSet) throws IOException {
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
