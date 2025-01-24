import models.Bind;

import groovy.lang.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

public class Controller {

    private final Object model;
    private double[] lata;
    private final Map<String, double[]> variables;
    Binding binding = new Binding();
    ArrayList<String> boundFieldNames = new ArrayList<>();

    public Controller(String modelName) {
        try {
            String className = "models." + modelName;
            Class<?> modelClass = Class.forName(className);

            model = modelClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Model class not found: " + modelName, e);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing model: " + modelName, e);
        }
        variables = new LinkedHashMap<>();
    }

    public void readDataFrom(String fName) {
        Map<String, double[]> data = readDataFromFile(fName);
        populateModelFields(data);
    }

    private Map<String, double[]> readDataFromFile(String fName) {
        Map<String, double[]> data = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fName))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    parseLine(data, line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading data from file: " + fName, e);
        }
        return data;
    }

    private void parseLine(Map<String, double[]> data, String line) {
        String[] parts = line.split("\\s+");
        String key = parts[0];
        double[] parsedValues = Arrays.stream(Arrays.copyOfRange(parts, 1, parts.length))
                .mapToDouble(Double::parseDouble)
                .toArray();
        data.put(key, parsedValues);
    }

    private void populateModelFields(Map<String, double[]> data) {
        Field[] fields = model.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Bind.class)) {
                setFieldValue(field, data);
            }
        }
    }

    private void setFieldValue(Field field, Map<String, double[]> data) {
        String fieldName = field.getName();
        try {
            field.setAccessible(true);
            if (fieldName.equals("LL")) {
                field.set(model, data.get("LATA").length);
                lata = data.get("LATA");
            } else if (data.containsKey(fieldName)) {
                fillFieldWithData(data.get(fieldName), field);
            } else if (variables.containsKey(fieldName)) {
                fillFieldWithData(variables.get(fieldName), field);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error setting field value for: " + fieldName, e);
        }
    }

    private void fillFieldWithData(double[] data, Field field) {
        double[] fieldData = new double[lata.length];
        for (int i = 0, lastIndex = 0; i < lata.length; i++) {
            if(data.length > i) {
                fieldData[i] = data[i];
                lastIndex = i;
            }
            else
                fieldData[i] = data[lastIndex];
        }

        field.setAccessible(true);
        try {
            field.set(model, fieldData);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void runModel(){
        try {
            Method runMethod = model.getClass().getMethod("run");
            runMethod.invoke(model);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void runScriptFromFile(String fName){
        Path filePath = Path.of(fName);
        String script;
        try {
            script = Files.readString(filePath);

        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
            return;
        }
        runScript(script);
    }

    public void runScript(String script){
        bindFields();
        bindScriptFields();
        createAndRunScript(script);
    }

    public void bindFields(){
        Field[] fields = model.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Bind.class)) {
                field.setAccessible(true);
                try {
                    binding.setVariable(field.getName(), field.get(model));
                    boundFieldNames.add(field.getName());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void bindScriptFields() {
        for (Map.Entry<String, double[]> entry : variables.entrySet()){
            binding.setVariable(entry.getKey(), entry.getValue());
        }
    }

    public void createAndRunScript(String script) {
        GroovyShell shell = new GroovyShell(binding);
        try {
            shell.evaluate(script);
        } catch (groovy.lang.MissingPropertyException e) {
            Frame.missingPropertyFrame(e.toString());
            throw new RuntimeException(e);
        }

        for (Object obj : binding.getVariables().entrySet()) {
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) obj;

            if(entry.getKey().length() < 2 && entry.getKey().matches("[a-z]"))
                continue;

            if(boundFieldNames.contains(entry.getKey()))
                continue;

            variables.put(entry.getKey(), (double[])entry.getValue());
        }
    }

    public String getResultsAsTsv() {
        StringBuilder fieldDataSB = new StringBuilder();

        try {
            //header
            fieldDataSB.append("\t");
            for (double year : lata) {
                fieldDataSB.append((int) year).append("\t");
            }
            fieldDataSB.setLength(fieldDataSB.length() - 1); //remove last tab
            fieldDataSB.append("\n");

            //row for each object
            Field[] fields = model.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.getName().equals("LL") && field.isAnnotationPresent(Bind.class)) {
                    field.setAccessible(true);
                    fieldDataSB.append(field.getName()).append("\t");
                    double[] values = (double[]) field.get(model);
                    for (double value : values) {
                        if(field.getName().startsWith("tw")){
                            fieldDataSB.append(String.format("%.2f", value)).append("\t");
                        } else {
                            fieldDataSB.append(String.format("%.1f", value)).append("\t");
                        }
                    }
                    fieldDataSB.setLength(fieldDataSB.length() - 1); //remove last tab
                    fieldDataSB.append("\n");
                }
            }

            //additional variables
            for (Map.Entry<String, double[]> entry : variables.entrySet()) {
                fieldDataSB.append(entry.getKey()).append("\t");
                for (double value : entry.getValue()) {
                    fieldDataSB.append(String.format("%.3f", value)).append("\t");
                }
                fieldDataSB.setLength(fieldDataSB.length() - 1); //remove last tab
                fieldDataSB.append("\n");
            }

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return fieldDataSB.toString();
    }
}