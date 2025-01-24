import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.util.*;

public class Frame {
    private Controller controller;
    private JList<String> dataList;
    private JList<String> modelList;
    private JTable table;
    private JFrame frame;

    public Frame() {
        initializeGUI();
    }

    public void display() {
        frame.setVisible(true);
    }

    private void initializeGUI() {
        frame = new JFrame("Modelling Framework");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLayout(new BorderLayout());

        frame.add(setupSelectionPanel(), BorderLayout.WEST);
        frame.add(setupResultPanel(), BorderLayout.CENTER);
    }

    private JPanel setupSelectionPanel() {
        JPanel selectionPanel = new JPanel(new BorderLayout());
        selectionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        selectionPanel.add(new JLabel("Select model and data"), BorderLayout.NORTH);
        selectionPanel.add(new JScrollPane(setupModelList()), BorderLayout.WEST);
        selectionPanel.add(new JScrollPane(setupDataList()), BorderLayout.EAST);

        JButton runModelButton = new JButton("Run Model");
        runModelButton.addActionListener(e -> runModel());
        selectionPanel.add(runModelButton, BorderLayout.SOUTH);

        return selectionPanel;
    }

    private JList<String> setupModelList() {
        DefaultListModel<String> modelListModel = new DefaultListModel<>();
        loadDataIntoModel(modelListModel, "src/main/java/models");
        modelList = new JList<>(modelListModel);
        modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modelList.setPreferredSize(new Dimension(75, 500));
        return modelList;
    }

    private JList<String> setupDataList() {
        DefaultListModel<String> dataListModel = new DefaultListModel<>();
        loadDataIntoModel(dataListModel, "src/main/resources/data");
        dataList = new JList<>(dataListModel);
        dataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dataList.setPreferredSize(new Dimension(75, 500));
        return dataList;
    }

    private JPanel setupResultPanel() {
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        resultPanel.add(new JLabel("Results"), BorderLayout.NORTH);

        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        resultPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        resultPanel.add(getButtonPanel(), BorderLayout.SOUTH);
        return resultPanel;
    }


    private void runModel() {
        if (isModelAndDataSelected()) {
            initializeController();
            readDataAndRunModel();
            updateTable();
        }
    }

    private boolean isModelAndDataSelected() {
        return modelList.getSelectedValue() != null && dataList.getSelectedValue() != null;
    }

    private void initializeController() {
        controller = new Controller(modelList.getSelectedValue());
    }

    private void readDataAndRunModel() {
        controller.readDataFrom("src/main/resources/data/" + dataList.getSelectedValue());
        controller.runModel();
    }

    private JPanel getButtonPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        JButton runScriptFromFileButton = new JButton("Run Script from File");
        runScriptFromFileButton.addActionListener(e -> runScriptFromFile());

        JButton createAndRunAdHocScriptButton = new JButton("Create and Run Ad Hoc Script");
        createAndRunAdHocScriptButton.addActionListener(e -> createAndRunAdHocScript());

        buttonPanel.add(runScriptFromFileButton);
        buttonPanel.add(createAndRunAdHocScriptButton);
        return buttonPanel;
    }

    private void runScriptFromFile() {
        if (controller != null && table.getModel().getRowCount() > 0) {
            JFileChooser fileChooser = new JFileChooser("src/main/resources/scripts");
            fileChooser.setDialogTitle("Select Script File");
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                controller.runScriptFromFile(fileChooser.getSelectedFile().getAbsolutePath());
                updateTable();
            }
        }
    }

    private void createAndRunAdHocScript() {
        if (controller != null) {
            JFrame customScriptFrame = new JFrame("Enter Your Script Here");
            customScriptFrame.setSize(500, 400);
            customScriptFrame.setLayout(new BorderLayout());

            JTextArea textArea = new JTextArea();
            customScriptFrame.add(new JScrollPane(textArea), BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okButton = new JButton("Ok");
            JButton cancelButton = new JButton("Cancel");
            buttons.add(okButton);
            buttons.add(cancelButton);

            okButton.addActionListener(e -> {
                controller.runScript(textArea.getText());
                updateTable();
                customScriptFrame.dispose();
            });

            cancelButton.addActionListener(e -> customScriptFrame.dispose());
            customScriptFrame.add(buttons, BorderLayout.SOUTH);
            customScriptFrame.setVisible(true);
        }
    }

    public static void missingPropertyFrame(String error){
        String trimmedError = error.substring(error.indexOf("No such property: "));
        JOptionPane.showMessageDialog(new JFrame(), trimmedError);
    }

    private void loadDataIntoModel(DefaultListModel<String> modelListModel, String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            Arrays.stream(Objects.requireNonNull(directory.listFiles()))
                    .filter(file -> file.isFile() && !file.getName().equals("Bind.java"))
                    .map(file -> file.getName().replace(".java", ""))
                    .forEach(modelListModel::addElement);
        } else {
            JOptionPane.showMessageDialog(null, "Directory not found: " + directoryPath, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTable() {
        if (controller != null) {
            Object[][] tableData = parseTSVString(controller.getResultsAsTsv());
            Object[] columnNames = tableData[0];
            Object[][] rows = new Object[tableData.length - 1][tableData[0].length - 1];
            System.arraycopy(tableData, 1, rows, 0, tableData.length - 1);
            table.setModel(new DefaultTableModel(rows, columnNames));

            DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
            leftRenderer.setHorizontalAlignment(JLabel.LEFT);
            table.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);
        }
    }


    private Object[][] parseTSVString(String tsvString) {
        ArrayList<String[]> rows = new ArrayList<>();
        for (String line : tsvString.split("\n")) {
            rows.add(line.split("\t"));
        }
        int columnCount = rows.get(0).length;
        Object[][] data = new Object[rows.size()][columnCount];
        for (int i = 0; i < rows.size(); i++) {
            data[i] = rows.get(i);
        }
        return data;
    }
}
