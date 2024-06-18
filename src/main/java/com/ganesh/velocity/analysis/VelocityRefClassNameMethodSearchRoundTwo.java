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
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class gets around ~90% false positives.
 * <br>
 * It relies on reference names and matching class names of references with contains clause along with exact method
 * and hence the list produced from this logic is just Round 2 with high false positives.
 * Example: if a VM contains "$action.getSomething"
 * and a class is marked as allowed with "SomeAction#getSomething()", then the tooling skips the VM expression.
 * Here in this example "SomeAction".containsIgnoreCase("action") is true and hence the VM expression is skipped.
 * <br>
 * Compliment this with round one to have more comprehensive object plus method combo.
 */
public class VelocityRefClassNameMethodSearchRoundTwo {
    static Set<Match> unMatchedFoundInVMs = new HashSet<>();
    static Set<String> foundMethodNamesInVMs = new HashSet<>();
    static Set<Match> unFitleredRawVMsoriginalSet = new HashSet<>();
    static Set<Method> coreAllowedMethodSet = new HashSet<>();
    static Set<Method> pluginAllowedMethodSet = new HashSet<>();
    static Set<VTLMetaItem> vmVtlMetaMapping = new HashSet<>();
    public static final String CONFLUENCE_VELOCITY_PROPERTIES = "/Users/ggautam/Work/source/atlassian/confluence/confluence-core/confluence/src/main/resources/velocity-default.properties";
    private static final String CROWD_VELOCITY_PROPERTIES = "/Users/ggautam/Work/source/atlassian/atlassian-embedded-crowd/embedded-crowd-admin-plugin/src/main/resources/embedded-crowd-admin-velocity.properties";

    public static final String CONFLUENCE_IMPLICIT_VTL = "/Users/ggautam/Work/source/atlassian/confluence/confluence-core/confluence-webapp/src/main/resources";
    static String confluenceDirPath = "/Users/ggautam/Work/source/atlassian/confluence";
    static String[] directoryPathsToBeScanned = {
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
//            "/Users/ggautam/Work/data/temp/confluence-ancillary-plugins",
            "/Users/ggautam/Work/data/temp/confluence-content-plugins",
//            "/Users/ggautam/Work/data/temp/confluence-frontend-plugins",
//            "/Users/ggautam/Work/data/temp/confluence-jira-integration-plugins",
//            "/Users/ggautam/Work/data/temp/confluence-open-plugins",
//            "/Users/ggautam/Work/data/temp/confluence-public-plugins",
//            "/Users/ggautam/Work/source/atlassian/confluence",
//            "/Users/ggautam/Work/data/temp/atlassian-embedded-crowd",
    };
    static String filteredOutputFileName = "matches-filtered";
    static String allOutputFileName = "matches-unfiltered";
    static String onlyMethodNamesFileName = "matches-filtered-method-names";

    // Main regex for covering normal cases, minimum false positives
//    static String regex = "\\$!?[a-zA-Z_$][a-zA-Z\\d_$]*\\s*\\.([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*";
    static String regex = "\\$!?([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*\\.([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*";

    //Regex to cover nested cases, can cause false positives
//    static String regex = "\\$!?[a-zA-Z_$][a-zA-Z\\d_$]*\\s*\\..*\\.([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*";

    // Regex to cover non-unique method scan algo, can cause false positives
//    static String regex = "\\$!?[a-zA-Z_$][a-zA-Z\\d_$]*\\s*\\.([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*";

    static String vtlMetaLineRegex = "vtlvariable name=\"([a-zA-Z_$][a-zA-Z\\d_$]+)\".*type=\".*\\.([a-zA-Z_$][a-zA-Z\\d_$]+)";


    static final String methodAllowlistRegexInVelocityProperties = ".*\\.([a-zA-Z_$][a-zA-Z\\d_$]+)#([a-zA-Z_$][a-zA-Z\\d_$]*)";
    static String methodAllowlistRegex = "<method>.+#([a-zA-Z_$][a-zA-Z\\d_$]*)";
    static String classMethodAllowlistRegex = "<method>.*\\.([a-zA-Z_$][a-zA-Z\\d_$]+)#([a-zA-Z_$][a-zA-Z\\d_$]*)";

    static String[] skipObjects = new String[] {
            "$htmlUtil",
            "$velocityUtil",
            "$generalUtil",
            "$stringUtils",
            "$!htmlUtil",
            "$!velocityUtil",
            "$!generalUtil",
            "$!stringUtils",
            "$soyTemplateRendererHelper"
    };

    public static void main(String[] args) throws IOException {
        System.out.println("Scanning velocity-default.properties XMLs");
        loadMethodNamesFromVelocityProperties(CONFLUENCE_VELOCITY_PROPERTIES);
//        loadMethodNamesFromVelocityProperties(CROWD_VELOCITY_PROPERTIES);

        System.out.println("Scanning core plugin XMLs");
        loadMethodNamesFromPluginXMLs(classMethodAllowlistRegex, confluenceDirPath, coreAllowedMethodSet);

        for (String directoryPath : directoryPathsToBeScanned) {
            System.out.println("Scanning " + directoryPath);
            resetContainers();

            System.out.println("Scanning plugin XMLs - " +  directoryPath);
            loadMethodNamesFromPluginXMLs(classMethodAllowlistRegex, directoryPath, pluginAllowedMethodSet);

            System.out.println("Populating VM data set for methods - " +  directoryPath);
            crawlVMFilesAndPopulateJavaMethodCalls(regex, vtlMetaLineRegex, directoryPath);
            crawlVMFilesAndPopulateJavaMethodCalls(regex, vtlMetaLineRegex, CONFLUENCE_IMPLICIT_VTL);

            System.out.println("Cleaning up method file mappings - " +  directoryPath);
            filterUpMatchesInVMSet();

            System.out.println("Cleaning up methods list - " +  directoryPath);
            filterUpJustMethodMatchesInVMsSet();

            System.out.println("Publishing output files - " +  directoryPath);
            publishFiles(directoryPath, onlyMethodNamesFileName,  filteredOutputFileName, allOutputFileName);

            System.out.println("Search completed. Results written to outputfiles");
        }
        System.out.println("Search finished. Results written to ");
    }

    private static void resetContainers() {
        unMatchedFoundInVMs.clear();
        foundMethodNamesInVMs.clear();
        pluginAllowedMethodSet.clear();
        pluginAllowedMethodSet.addAll(coreAllowedMethodSet);
    }

    private static void crawlVMFilesAndPopulateJavaMethodCalls(String regex, String vtlMetaLineRegex, String directoryPath) throws IOException {
        Pattern pattern = Pattern.compile(regex);
        Pattern vtlMetaLinePattern = Pattern.compile(vtlMetaLineRegex);
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".vm") || path.toString().endsWith(".vmd"))
                .forEach(velocityFile -> {
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(velocityFile.toFile()));
                        searchAndPrepareMatchingSets(velocityFile, vtlMetaLinePattern, pattern, reader);
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void loadMethodNamesFromPluginXMLs(String regex, String directoryPath, Set<Method> allowedMethodSet) throws IOException {
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

        ActionFileParser.findActionGetIsMethods(directoryPath).forEach((k, v) -> {
            v.forEach(m -> allowedMethodSet.add(new Method(k, m)));
        });
        System.out.println(pluginAllowedMethodSet);
    }

    private static void loadMethodNamesFromVelocityProperties(String propertiesFile) {
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(propertiesFile)) {
            properties.load(inputStream);

            String[] methods = properties.getProperty("introspector.proper.allowlist.methods").split(",");

            Set<Method> methodSet = extractMethodNamesFromAllowlist(methods);
            System.out.println(methodSet);

            coreAllowedMethodSet.addAll(methodSet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void filterUpMatchesInVMSet() {
        Arrays.stream(skipObjects).forEach(o -> unMatchedFoundInVMs.removeIf(s -> s.line.toLowerCase().startsWith(o.toLowerCase())));

        pluginAllowedMethodSet.forEach(allowedItems -> {
            unMatchedFoundInVMs.removeIf(vmLineEntry ->
                    vmLineEntry.method.equalsIgnoreCase(allowedItems.vtlMethod)
                            && allowedItems.vtlClass.toLowerCase().contains(vmLineEntry.vtlReference.toLowerCase()));
            if(allowedItems.vtlMethod.startsWith("get") || allowedItems.vtlMethod.startsWith("set")) {
                unMatchedFoundInVMs.removeIf(vmLineEntry ->
                        vmLineEntry.method.equalsIgnoreCase(allowedItems.vtlMethod.substring(3))
                                && allowedItems.vtlClass.toLowerCase().contains(vmLineEntry.vtlReference.toLowerCase()));
            } else if(allowedItems.vtlMethod.startsWith("is")) {
                unMatchedFoundInVMs.removeIf(vmLineEntry -> vmLineEntry.method.equalsIgnoreCase(allowedItems.vtlMethod.substring(2))
                        && allowedItems.vtlClass.toLowerCase().contains(vmLineEntry.vtlReference.toLowerCase()));
            }
        });

        vmVtlMetaMapping.forEach(vtlMeta -> {
            unMatchedFoundInVMs.removeIf(vmLineEntry ->
                    (vtlMeta.filename.equals(vmLineEntry.filename) || vtlMeta.filename.contains("velocity_implicit"))
                            && vtlMeta.vtlVar.equalsIgnoreCase(vmLineEntry.vtlReference)
                            && pluginAllowedMethodSet.stream()
                            .anyMatch(plugXMLAllowedEntry -> plugXMLAllowedEntry.vtlClass.equalsIgnoreCase(vtlMeta.vtlClass)
                                    && (plugXMLAllowedEntry.vtlMethod.equalsIgnoreCase(vmLineEntry.method)
                                    || (plugXMLAllowedEntry.vtlMethod.startsWith("get") && plugXMLAllowedEntry.vtlMethod.substring(3).equalsIgnoreCase(vmLineEntry.method))
                                    || (plugXMLAllowedEntry.vtlMethod.startsWith("is") && plugXMLAllowedEntry.vtlMethod.substring(2).equalsIgnoreCase(vmLineEntry.method)))));
        });
    }

    private static void filterUpJustMethodMatchesInVMsSet() {
        foundMethodNamesInVMs.removeAll(pluginAllowedMethodSet);
        pluginAllowedMethodSet.forEach(m -> {
            foundMethodNamesInVMs.removeIf(s -> s.equalsIgnoreCase(m.vtlMethod));
            if(m.vtlMethod.startsWith("get") || m.vtlMethod.startsWith("set")) {
                foundMethodNamesInVMs.removeIf(s -> s.equalsIgnoreCase(m.vtlMethod.substring(3)));
            } else if(m.vtlMethod.startsWith("is")) {
                foundMethodNamesInVMs.removeIf(s -> s.equalsIgnoreCase(m.vtlMethod.substring(2)));
            }
        });
    }

    private static void publishFiles(String directoryPath, String onlyMethodNamesFileName, String filteredOutputFileName, String allOutputFileName) throws IOException {
        String outputFile = filteredOutputFileName + "-" + directoryPath.split("/")[(directoryPath.split("/").length - 1)] + ".csv";
        String onlyMethodNamesFile = onlyMethodNamesFileName + "-" + directoryPath.split("/")[(directoryPath.split("/").length - 1)] + ".csv";
        String fullDumpFile = allOutputFileName + "-" + directoryPath.split("/")[(directoryPath.split("/").length - 1)] + ".csv";

        FileWriter filteredFileWriter = new FileWriter(outputFile);
        FileWriter onlyMethodNamesFileWriter = new FileWriter(onlyMethodNamesFile);
        FileWriter fullMethodsDumpInFileWriter = new FileWriter(fullDumpFile);

        CSVPrinter csvPrinter = new CSVPrinter(filteredFileWriter, CSVFormat.DEFAULT.withHeader("Match", "Line", "Filename"));
        CSVPrinter fullMethodsDumpInFilePrinter = new CSVPrinter(fullMethodsDumpInFileWriter, CSVFormat.DEFAULT.withHeader("Match", "Line", "Filename"));
        CSVPrinter onlyMethodNamesCsvPrinter = new CSVPrinter(onlyMethodNamesFileWriter, CSVFormat.DEFAULT.withHeader("Match"));

        printMethodAndLines(csvPrinter, unMatchedFoundInVMs);
        printMethodAndLines(fullMethodsDumpInFilePrinter, unFitleredRawVMsoriginalSet);
        printMethodNamesOnly(onlyMethodNamesCsvPrinter, foundMethodNamesInVMs);

        csvPrinter.close();
        fullMethodsDumpInFilePrinter.close();
        onlyMethodNamesCsvPrinter.close();
    }

    private static Set<Method> extractMethodNamesFromAllowlist(String[] methods) {
        Pattern pattern = Pattern.compile(methodAllowlistRegexInVelocityProperties);
        return Arrays.stream(methods)
                .map(m -> {
                    Matcher matcher = pattern.matcher(m);
                    if(matcher.find()) {
                        return new Method(matcher.group(1), matcher.group(2));
                    }
                    return null;
                })
                .collect(Collectors.toSet());
    }

    private static void searchAndPrepareMatchingSets(Path file, Pattern vtlMetaLinePattern, Pattern vmLinePattern, BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher matcher = vmLinePattern.matcher(line);
            if(!matcher.matches()) {
                vtlMetaLinePattern.matcher(line).results().forEach(m -> {
                    vmVtlMetaMapping.add(new VTLMetaItem(file.getFileName().toString(), m.group(1), m.group(2)));
                });
            }
            while (matcher.find()) {
                String match = matcher.group();
                String vtlReference = matcher.group(1); // Capture group within the regex
                String capturedMethod = matcher.group(2); // Capture group within the regex
                if((match.contains("action.") && (capturedMethod.toLowerCase().startsWith("get") || capturedMethod.toLowerCase().startsWith("is")))) {
                    continue;
                }
                unMatchedFoundInVMs.add(new Match(vtlReference, capturedMethod, match, file.getFileName().toString()));
                foundMethodNamesInVMs.add(capturedMethod);
            }
        }
        unFitleredRawVMsoriginalSet.addAll(unMatchedFoundInVMs);
    }

    private static void searchAndPrepareMatchingAllowedSets(Path file, Pattern pattern, BufferedReader reader, Set<Method> allowedMethodSet) throws IOException {
        String line;

        while ((line = reader.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String match = matcher.group();
                String capturedClass = matcher.group(1);
                String capturedMethod = matcher.group(2);
                allowedMethodSet.add(new Method(capturedClass, capturedMethod));
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

    record Match(String vtlReference, String method, String line, String filename) {
    }

    record VTLMetaItem(String filename, String vtlVar, String vtlClass) {
    }

    record Method(String vtlClass, String vtlMethod) {
    }


}
