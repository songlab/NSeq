/*
 * NSeqView.java
 */

package nseq;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.FontMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.codec.Base64.OutputStream;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import javax.jws.soap.SOAPBinding.Style;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.tools.FileObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * The application's main frame.
 */
public class NSeqView extends FrameView {
    File lengthFile = null;
    File seqFile = null;
    String dirPath = null;
    Hashtable<String, Integer> chrLengths = null;
    javax.swing.text.Style blackFontStyle;
    javax.swing.text.Style redFontStyle;
    MedianUnbiasedEstimator MUE;
    DataFileReader br;
    HashMap<String,CandidateNucleosome[]> nucleosomes;
    JFreeChart readChart, nucleosomeChart;
    
    public NSeqView(SingleFrameApplication app) {
        super(app);
        initComponents();
        //Correct design
        jTabbedPane1.setEnabledAt(3,false);
        jTabbedPane1.setEnabledAt(2,false);
        this.getFrame().setSize(700, 700);
        this.getFrame().setResizable(true);
        blackFontStyle = runTextPane.addStyle("Black", null);
        StyleConstants.setForeground(blackFontStyle, Color.black);
        StyleConstants.setFontSize(blackFontStyle, 14);
        
        redFontStyle = runTextPane.addStyle("Red", null);
        StyleConstants.setForeground(redFontStyle, Color.red);
        StyleConstants.setFontSize(redFontStyle, 14);
        
        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        
        

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        getFrame().setResizable(false);
        DefaultCaret caret = (DefaultCaret)runTextPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        Runnable mue = new Runnable() {  
                public void run() {  
                     setMUE();
                }  
            };  
        Thread mueThread = new Thread(mue); 
        mueThread.start();  
    }
    
    public NSeqView(SingleFrameApplication app, String lengthFilename, String seqFilename, String fileType, int numberOfThreads, double fdrCutoff, int numberOfSims, int windowWidth, int centerWidth, double tsCutoff, String genome) {
        super(app);
        initComponents();
        //Correct design
        jTabbedPane1.setEnabledAt(3,false);
        this.getFrame().setSize(700, 700);
        this.getFrame().setResizable(true);
        blackFontStyle = runTextPane.addStyle("Black", null);
        StyleConstants.setForeground(blackFontStyle, Color.black);
        StyleConstants.setFontSize(blackFontStyle, 14);
        
        //Set options from command line and run
        if (lengthFilename != null) {
            lengthFile = new File(lengthFilename);
        }
        seqFile = new File(seqFilename);
        if (fileType == "bam" || fileType == "sam") {
            inputComboBox.setSelectedIndex(3);
        } else {
            inputComboBox.setSelectedIndex(0);
        }
        threadsTextField.setText(((Integer) numberOfThreads).toString());
        FDRTextField.setText(((Double) fdrCutoff).toString());
        nSimTextField.setText(((Integer) numberOfSims).toString());
        scanWidthTextField.setText(((Integer) windowWidth).toString());
        tsCutoffTextField.setText(((Double) tsCutoff).toString());

        redFontStyle = runTextPane.addStyle("Red", null);
        StyleConstants.setForeground(redFontStyle, Color.red);
        StyleConstants.setFontSize(redFontStyle, 14);
        
        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        
        

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        getFrame().setResizable(false);
        DefaultCaret caret = (DefaultCaret)runTextPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        Runnable mue = new Runnable() {  
                public void run() {  
                     setMUE();
                }  
            };  
        Thread mueThread = new Thread(mue); 
        mueThread.start();
        
        if (genome.toLowerCase().equals("hg19")){
            chrLengthButton.setEnabled(false);
            chrLengths = StandardGenomes.hg19();
            FileParser.setOrderedChromosomes(chrLengths);
            updateTextArea("HG19 genome chosen.");       
            alignmentButton.setEnabled(true);        
            updateTextArea("Total genome length =  "+computeGenomeLength()+"\n");
            lengthFile = null;
        } else if (genome.toLowerCase().equals("hg18")){
            chrLengthButton.setEnabled(false);
            chrLengths = StandardGenomes.hg18();
            FileParser.setOrderedChromosomes(chrLengths);
            updateTextArea("HG18 genome chosen.");       
            alignmentButton.setEnabled(true);        
            updateTextArea("Total genome length =  "+computeGenomeLength()+"\n");
            lengthFile = null;
        } 
        else if (genome.toLowerCase().equals("mm10")){
            chrLengthButton.setEnabled(false);
            chrLengths = StandardGenomes.mm10();
            FileParser.setOrderedChromosomes(chrLengths);
            updateTextArea("MM10 genome chosen.");       
            alignmentButton.setEnabled(true);        
            updateTextArea("Total genome length =  "+computeGenomeLength()+"\n");
            lengthFile = null;
        } 
        else if (genome.toLowerCase().equals("mm9")){
            chrLengthButton.setEnabled(false);
            chrLengths = StandardGenomes.mm9();
            FileParser.setOrderedChromosomes(chrLengths);
            updateTextArea("MM9 genome chosen.");       
            alignmentButton.setEnabled(true);        
            updateTextArea("Total genome length =  "+computeGenomeLength()+"\n");
            lengthFile = null;
        } 
        else if (genome.toLowerCase().equals("ce10")){
            chrLengthButton.setEnabled(false);
            chrLengths = StandardGenomes.ce10();
            FileParser.setOrderedChromosomes(chrLengths);
            updateTextArea("CE10 genome chosen.");       
            alignmentButton.setEnabled(true);        
            updateTextArea("Total genome length =  "+computeGenomeLength()+"\n");
            lengthFile = null;
        } else {
            updateTextArea("Chromosome length file: " + lengthFile);
            //updateTextArea("Dir Path: " + dirPath);
            alignmentButton.setEnabled(true);
            chrLengths = FileParser.getChromosomeLength(lengthFile.getPath());
            updateTextArea("Total genome length = "+computeGenomeLength()+"\n");
        }
        
        Runnable updateGUI = new Runnable() {  
                public void run() {  
                    updateRunTextArea("Using " +threadsTextField.getText() + " threads.\n"); 
                    updateRunTextArea("Reading data and constructing probability distributions for nucleosome centers...");  
                }  
            };  
        Thread t = new Thread(updateGUI); 
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();  
                
            Runnable read = new Runnable() {  
                public void run() {  
                     DataProcess();
                }  
            };  
            Thread readThread = new Thread(read); 
            readThread.start();
        
    }
    
    private void setMUE(){
        MUE = new MedianUnbiasedEstimator(1000);
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = NSeqApp.getApplication().getMainFrame();
            aboutBox = new NSeqAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        NSeqApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        mainPanel = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jPanel6 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        loadPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        chrLengthButton = new javax.swing.JButton();
        alignmentButton = new javax.swing.JButton();
        inputComboBox = new javax.swing.JComboBox();
        threadsTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        FDRTextField = new javax.swing.JTextField();
        scanWidthTextField = new javax.swing.JTextField();
        nSimTextField = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        centerWidthTextField = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        tsCutoffTextField = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        runButton = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextPane();
        jPanel2 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        displayButton = new javax.swing.JButton();
        NSeqLabel = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        runTextPane = new javax.swing.JTextPane();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        chrComboBox = new javax.swing.JComboBox();
        chrStartPosition = new javax.swing.JTextField();
        chrEndPosition = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        readChartPanelContainer = new javax.swing.JPanel();
        nucleosomeChartPanelContainer = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        right3Button = new javax.swing.JButton();
        left3Button = new javax.swing.JButton();
        left2Button = new javax.swing.JButton();
        left1Button = new javax.swing.JButton();
        right1Button = new javax.swing.JButton();
        right2Button = new javax.swing.JButton();
        coverageExportToPDF = new javax.swing.JButton();
        exportReadPlotToPDF = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        displayTextPane = new javax.swing.JTextPane();
        jFileChooser1 = new javax.swing.JFileChooser();
        jFileChooser2 = new javax.swing.JFileChooser();

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(nseq.NSeqApp.class).getContext().getResourceMap(NSeqView.class);
        menuBar.setBackground(resourceMap.getColor("menuBar.background")); // NOI18N
        menuBar.setFont(resourceMap.getFont("menuBar.font")); // NOI18N
        menuBar.setMaximumSize(new java.awt.Dimension(200, 32768));
        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(nseq.NSeqApp.class).getContext().getActionMap(NSeqView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setFont(resourceMap.getFont("exitMenuItem.font")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setFont(resourceMap.getFont("aboutMenuItem.font")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        jTabbedPane1.setBackground(resourceMap.getColor("jTabbedPane1.background")); // NOI18N
        jTabbedPane1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jTabbedPane1.setToolTipText(resourceMap.getString("jTabbedPane1.toolTipText")); // NOI18N
        jTabbedPane1.setAlignmentX(0.0F);
        jTabbedPane1.setAlignmentY(0.0F);
        jTabbedPane1.setFont(resourceMap.getFont("jTabbedPane1.font")); // NOI18N
        jTabbedPane1.setMaximumSize(new java.awt.Dimension(1000, 741));
        jTabbedPane1.setMinimumSize(new java.awt.Dimension(1000, 701));
        jTabbedPane1.setName("jTabbedPane1"); // NOI18N
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(1100, 730));

        jPanel1.setBackground(resourceMap.getColor("jPanel1.background")); // NOI18N
        jPanel1.setFont(resourceMap.getFont("jPanel1.font")); // NOI18N
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        mainPanel.setBackground(resourceMap.getColor("mainPanel.background")); // NOI18N
        mainPanel.setFont(resourceMap.getFont("mainPanel.font")); // NOI18N
        mainPanel.setMaximumSize(new java.awt.Dimension(687, 544));
        mainPanel.setMinimumSize(new java.awt.Dimension(2000, 610));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jButton2.setFont(resourceMap.getFont("jButton2.font")); // NOI18N
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        mainPanel.add(jButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 500, 380, 150));

        jScrollPane5.setBorder(null);
        jScrollPane5.setName("jScrollPane5"); // NOI18N

        jTextArea3.setColumns(20);
        jTextArea3.setEditable(false);
        jTextArea3.setFont(resourceMap.getFont("jTextArea3.font")); // NOI18N
        jTextArea3.setRows(5);
        jTextArea3.setText(resourceMap.getString("jTextArea3.text")); // NOI18N
        jTextArea3.setBorder(null);
        jTextArea3.setName("jTextArea3"); // NOI18N
        jScrollPane5.setViewportView(jTextArea3);

        mainPanel.add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 240, 820, 260));

        jPanel6.setBackground(resourceMap.getColor("jPanel6.background")); // NOI18N
        jPanel6.setFont(resourceMap.getFont("jPanel6.font")); // NOI18N
        jPanel6.setName("jPanel6"); // NOI18N

        jLabel13.setBackground(resourceMap.getColor("jLabel13.background")); // NOI18N
        jLabel13.setIcon(resourceMap.getIcon("jLabel13.icon")); // NOI18N
        jLabel13.setText(resourceMap.getString("jLabel13.text")); // NOI18N
        jLabel13.setName("jLabel13"); // NOI18N

        jLabel12.setBackground(resourceMap.getColor("jLabel12.background")); // NOI18N
        jLabel12.setFont(resourceMap.getFont("jLabel12.font")); // NOI18N
        jLabel12.setForeground(resourceMap.getColor("jLabel12.foreground")); // NOI18N
        jLabel12.setText(resourceMap.getString("jLabel12.text")); // NOI18N
        jLabel12.setName("jLabel12"); // NOI18N

        jLabel10.setFont(resourceMap.getFont("jLabel10.font")); // NOI18N
        jLabel10.setText(resourceMap.getString("jLabel10.text")); // NOI18N
        jLabel10.setName("jLabel10"); // NOI18N

        jLabel14.setFont(resourceMap.getFont("jLabel14.font")); // NOI18N
        jLabel14.setText(resourceMap.getString("jLabel14.text")); // NOI18N
        jLabel14.setName("jLabel14"); // NOI18N

        jLabel11.setFont(resourceMap.getFont("jLabel11.font")); // NOI18N
        jLabel11.setText(resourceMap.getString("jLabel11.text")); // NOI18N
        jLabel11.setName("jLabel11"); // NOI18N

        jLabel1.setIcon(resourceMap.getIcon("jLabel1.icon")); // NOI18N
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel6Layout.createSequentialGroup()
                .add(63, 63, 63)
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel10)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel12)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel6Layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 136, Short.MAX_VALUE)
                        .add(jLabel11)
                        .add(8, 8, 8))
                    .add(jPanel6Layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel14)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)))
                .add(jLabel13)
                .add(153, 153, 153))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(133, Short.MAX_VALUE)
                .add(jLabel11)
                .add(78, 78, 78))
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(91, Short.MAX_VALUE)
                .add(jLabel13)
                .addContainerGap())
            .add(jPanel6Layout.createSequentialGroup()
                .add(31, 31, 31)
                .add(jLabel1)
                .addContainerGap(92, Short.MAX_VALUE))
            .add(jPanel6Layout.createSequentialGroup()
                .add(59, 59, 59)
                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(jLabel12)
                    .add(jLabel14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 29, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(123, Short.MAX_VALUE))
        );

        mainPanel.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(-10, 0, 1170, 230));

        jPanel1.add(mainPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1150, 651));

        jTabbedPane1.addTab(resourceMap.getString("jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

        loadPanel.setBackground(resourceMap.getColor("loadPanel.background")); // NOI18N
        loadPanel.setFont(resourceMap.getFont("loadPanel.font")); // NOI18N
        loadPanel.setMaximumSize(new java.awt.Dimension(789, 699));
        loadPanel.setName("loadPanel"); // NOI18N

        jPanel3.setBackground(resourceMap.getColor("jPanel3.background")); // NOI18N
        jPanel3.setMaximumSize(new java.awt.Dimension(377, 417));
        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        chrLengthButton.setFont(resourceMap.getFont("chrLengthButton.font")); // NOI18N
        chrLengthButton.setText(resourceMap.getString("chrLengthButton.text")); // NOI18N
        chrLengthButton.setActionCommand(resourceMap.getString("chrLengthButton.actionCommand")); // NOI18N
        chrLengthButton.setName("chrLengthButton"); // NOI18N
        chrLengthButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chrLengthButtonActionPerformed(evt);
            }
        });
        jPanel3.add(chrLengthButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 0, -1, 40));

        alignmentButton.setFont(resourceMap.getFont("alignmentButton.font")); // NOI18N
        alignmentButton.setText(resourceMap.getString("alignmentButton.text")); // NOI18N
        alignmentButton.setEnabled(false);
        alignmentButton.setName("alignmentButton"); // NOI18N
        alignmentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                alignmentButtonActionPerformed(evt);
            }
        });
        jPanel3.add(alignmentButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 50, 230, 40));

        inputComboBox.setFont(resourceMap.getFont("inputComboBox.font")); // NOI18N
        inputComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "BED", "SAM", "BAM" }));
        inputComboBox.setName("inputComboBox"); // NOI18N
        jPanel3.add(inputComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 50, 170, 40));

        threadsTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        threadsTextField.setText(resourceMap.getString("threadsTextField.text")); // NOI18N
        threadsTextField.setName("threadsTextField"); // NOI18N
        jPanel3.add(threadsTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 100, 190, 40));

        jLabel4.setBackground(resourceMap.getColor("jLabel4.background")); // NOI18N
        jLabel4.setFont(resourceMap.getFont("jLabel15.font")); // NOI18N
        jLabel4.setForeground(resourceMap.getColor("jLabel4.foreground")); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setLabelFor(threadsTextField);
        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        jLabel4.setOpaque(true);
        jPanel3.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, 250, 40));

        jLabel5.setBackground(resourceMap.getColor("jLabel5.background")); // NOI18N
        jLabel5.setFont(resourceMap.getFont("jLabel15.font")); // NOI18N
        jLabel5.setForeground(resourceMap.getColor("jLabel16.foreground")); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setLabelFor(threadsTextField);
        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N
        jLabel5.setOpaque(true);
        jPanel3.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 160, 230, 40));

        jLabel8.setBackground(resourceMap.getColor("jLabel8.background")); // NOI18N
        jLabel8.setFont(resourceMap.getFont("jLabel15.font")); // NOI18N
        jLabel8.setForeground(resourceMap.getColor("jLabel16.foreground")); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setLabelFor(threadsTextField);
        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setName("jLabel8"); // NOI18N
        jLabel8.setOpaque(true);
        jPanel3.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 280, 240, 40));

        FDRTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        FDRTextField.setText(resourceMap.getString("FDRTextField.text")); // NOI18N
        FDRTextField.setName("FDRTextField"); // NOI18N
        jPanel3.add(FDRTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 160, 190, 40));

        scanWidthTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        scanWidthTextField.setText(resourceMap.getString("scanWidthTextField.text")); // NOI18N
        scanWidthTextField.setName("scanWidthTextField"); // NOI18N
        jPanel3.add(scanWidthTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 280, 190, 40));

        nSimTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        nSimTextField.setText(resourceMap.getString("nSimTextField.text")); // NOI18N
        nSimTextField.setName("nSimTextField"); // NOI18N
        jPanel3.add(nSimTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 220, 190, 40));

        jLabel16.setBackground(resourceMap.getColor("jLabel16.background")); // NOI18N
        jLabel16.setFont(resourceMap.getFont("jLabel15.font")); // NOI18N
        jLabel16.setForeground(resourceMap.getColor("jLabel16.foreground")); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel16.setLabelFor(threadsTextField);
        jLabel16.setText(resourceMap.getString("jLabel16.text")); // NOI18N
        jLabel16.setName("jLabel16"); // NOI18N
        jLabel16.setOpaque(true);
        jPanel3.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 220, 270, 40));

        centerWidthTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        centerWidthTextField.setText(resourceMap.getString("centerWidthTextField.text")); // NOI18N
        centerWidthTextField.setName("centerWidthTextField"); // NOI18N
        jPanel3.add(centerWidthTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 340, 190, 40));

        jLabel19.setBackground(resourceMap.getColor("jLabel19.background")); // NOI18N
        jLabel19.setFont(resourceMap.getFont("jLabel19.font")); // NOI18N
        jLabel19.setForeground(resourceMap.getColor("jLabel19.foreground")); // NOI18N
        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel19.setLabelFor(threadsTextField);
        jLabel19.setText(resourceMap.getString("jLabel19.text")); // NOI18N
        jLabel19.setName("jLabel19"); // NOI18N
        jLabel19.setOpaque(true);
        jPanel3.add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 340, 220, 40));

        tsCutoffTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tsCutoffTextField.setText(resourceMap.getString("tsCutoffTextField.text")); // NOI18N
        tsCutoffTextField.setName("tsCutoffTextField"); // NOI18N
        jPanel3.add(tsCutoffTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 400, 190, 40));

        jLabel15.setBackground(resourceMap.getColor("jLabel15.background")); // NOI18N
        jLabel15.setFont(resourceMap.getFont("jLabel15.font")); // NOI18N
        jLabel15.setForeground(resourceMap.getColor("jLabel16.foreground")); // NOI18N
        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel15.setLabelFor(threadsTextField);
        jLabel15.setText(resourceMap.getString("jLabel15.text")); // NOI18N
        jLabel15.setName("jLabel15"); // NOI18N
        jLabel15.setOpaque(true);
        jPanel3.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(1, 400, 270, 40));

        jComboBox1.setFont(resourceMap.getFont("jComboBox1.font")); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Custom", "human (hg19)", "human (hg18)", "mouse (mm10)", "mouse (mm9)", "worm (ce10)" }));
        jComboBox1.setName("jComboBox1"); // NOI18N
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });
        jPanel3.add(jComboBox1, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 5, 170, 40));

        jPanel7.setBackground(resourceMap.getColor("jPanel7.background")); // NOI18N
        jPanel7.setName("jPanel7"); // NOI18N

        jScrollPane2.setBorder(null);
        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jTextArea2.setBackground(resourceMap.getColor("jTextArea2.background")); // NOI18N
        jTextArea2.setColumns(20);
        jTextArea2.setEditable(false);
        jTextArea2.setFont(resourceMap.getFont("jTextArea2.font")); // NOI18N
        jTextArea2.setRows(5);
        jTextArea2.setText(resourceMap.getString("jTextArea2.text")); // NOI18N
        jTextArea2.setBorder(null);
        jTextArea2.setName("jTextArea2"); // NOI18N
        jScrollPane2.setViewportView(jTextArea2);

        jLabel6.setFont(resourceMap.getFont("jLabel6.font")); // NOI18N
        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        jLabel9.setFont(resourceMap.getFont("jLabel9.font")); // NOI18N
        jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
        jLabel9.setName("jLabel9"); // NOI18N

        jLabel17.setFont(resourceMap.getFont("jLabel17.font")); // NOI18N
        jLabel17.setForeground(resourceMap.getColor("jLabel17.foreground")); // NOI18N
        jLabel17.setText(resourceMap.getString("jLabel17.text")); // NOI18N
        jLabel17.setName("jLabel17"); // NOI18N

        jLabel18.setFont(resourceMap.getFont("jLabel18.font")); // NOI18N
        jLabel18.setText(resourceMap.getString("jLabel18.text")); // NOI18N
        jLabel18.setName("jLabel18"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel7Layout.createSequentialGroup()
                        .add(14, 14, 14)
                        .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel9)
                            .add(jPanel7Layout.createSequentialGroup()
                                .add(jLabel6)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel17)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                    .add(jPanel7Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 538, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(422, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel17)
                    .add(jLabel18)
                    .add(jLabel6))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel9)
                .add(18, 18, 18)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE)
                .addContainerGap())
        );

        runButton.setFont(resourceMap.getFont("runButton.font")); // NOI18N
        runButton.setText(resourceMap.getString("runButton.text")); // NOI18N
        runButton.setName("runButton"); // NOI18N
        runButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runButtonActionPerformed(evt);
            }
        });

        jPanel9.setBackground(resourceMap.getColor("jPanel9.background")); // NOI18N
        jPanel9.setName("jPanel9"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel9Layout = new org.jdesktop.layout.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 1, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 178, Short.MAX_VALUE)
        );

        jScrollPane7.setName("jScrollPane7"); // NOI18N

        jTextArea1.setBackground(resourceMap.getColor("jTextArea1.background")); // NOI18N
        jTextArea1.setEditable(false);
        jTextArea1.setFont(resourceMap.getFont("jTextArea1.font")); // NOI18N
        jTextArea1.setMaximumSize(new java.awt.Dimension(0, 16));
        jTextArea1.setName("jTextArea1"); // NOI18N
        jScrollPane7.setViewportView(jTextArea1);

        org.jdesktop.layout.GroupLayout loadPanelLayout = new org.jdesktop.layout.GroupLayout(loadPanel);
        loadPanel.setLayout(loadPanelLayout);
        loadPanelLayout.setHorizontalGroup(
            loadPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(loadPanelLayout.createSequentialGroup()
                .add(loadPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(loadPanelLayout.createSequentialGroup()
                        .add(36, 36, 36)
                        .add(jScrollPane7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 450, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(3, 3, 3)
                        .add(jPanel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(loadPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)))
                .add(loadPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(loadPanelLayout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(jPanel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(loadPanelLayout.createSequentialGroup()
                        .add(95, 95, 95)
                        .add(runButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 404, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
        );
        loadPanelLayout.setVerticalGroup(
            loadPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(loadPanelLayout.createSequentialGroup()
                .add(loadPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(loadPanelLayout.createSequentialGroup()
                        .add(jPanel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(runButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 150, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(loadPanelLayout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 450, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(loadPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jScrollPane7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE)
                            .add(jPanel9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );

        jTabbedPane1.addTab(resourceMap.getString("loadPanel.TabConstraints.tabTitle"), loadPanel); // NOI18N

        jPanel2.setBackground(resourceMap.getColor("jPanel2.background")); // NOI18N
        jPanel2.setFont(resourceMap.getFont("jPanel2.font")); // NOI18N
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel8.setBackground(resourceMap.getColor("jPanel8.background")); // NOI18N
        jPanel8.setName("jPanel8"); // NOI18N

        jLabel7.setIcon(resourceMap.getIcon("jLabel7.icon")); // NOI18N
        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        displayButton.setFont(resourceMap.getFont("displayButton.font")); // NOI18N
        displayButton.setText(resourceMap.getString("displayButton.text")); // NOI18N
        displayButton.setEnabled(false);
        displayButton.setName("displayButton"); // NOI18N
        displayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayButtonActionPerformed(evt);
            }
        });

        NSeqLabel.setBackground(resourceMap.getColor("NSeqLabel.background")); // NOI18N
        NSeqLabel.setFont(resourceMap.getFont("NSeqLabel.font")); // NOI18N
        NSeqLabel.setForeground(resourceMap.getColor("NSeqLabel.foreground")); // NOI18N
        NSeqLabel.setText(resourceMap.getString("NSeqLabel.text")); // NOI18N
        NSeqLabel.setName("NSeqLabel"); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel8Layout = new org.jdesktop.layout.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel8Layout.createSequentialGroup()
                .add(133, 133, 133)
                .add(jLabel7)
                .add(18, 18, 18)
                .add(NSeqLabel)
                .add(183, 183, 183)
                .add(displayButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 394, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(102, 102, 102))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel8Layout.createSequentialGroup()
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel8Layout.createSequentialGroup()
                        .add(47, 47, 47)
                        .add(jLabel7))
                    .add(jPanel8Layout.createSequentialGroup()
                        .add(89, 89, 89)
                        .add(NSeqLabel))
                    .add(jPanel8Layout.createSequentialGroup()
                        .add(32, 32, 32)
                        .add(displayButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 154, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(64, Short.MAX_VALUE))
        );

        jPanel2.add(jPanel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 460, 1080, 250));

        jPanel10.setBackground(resourceMap.getColor("jPanel10.background")); // NOI18N
        jPanel10.setName("jPanel10"); // NOI18N

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        runTextPane.setBackground(resourceMap.getColor("runTextPane.background")); // NOI18N
        runTextPane.setEditable(false);
        runTextPane.setFont(resourceMap.getFont("runTextPane.font")); // NOI18N
        runTextPane.setName("runTextPane"); // NOI18N
        jScrollPane3.setViewportView(runTextPane);

        org.jdesktop.layout.GroupLayout jPanel10Layout = new org.jdesktop.layout.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1000, Short.MAX_VALUE)
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
        );

        jPanel2.add(jPanel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(38, 20, 1000, 420));

        jTabbedPane1.addTab(resourceMap.getString("jPanel2.TabConstraints.tabTitle"), jPanel2); // NOI18N

        jPanel4.setFont(resourceMap.getFont("jPanel4.font")); // NOI18N
        jPanel4.setName("jPanel4"); // NOI18N

        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setPreferredSize(new java.awt.Dimension(1000, 631));
        jPanel5.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        chrComboBox.setFont(resourceMap.getFont("chrComboBox.font")); // NOI18N
        chrComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        chrComboBox.setName("chrComboBox"); // NOI18N
        chrComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chrComboBoxActionPerformed(evt);
            }
        });
        jPanel5.add(chrComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, 110, 50));

        chrStartPosition.setText(resourceMap.getString("chrStartPosition.text")); // NOI18N
        chrStartPosition.setName("chrStartPosition"); // NOI18N
        chrStartPosition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chrStartPositionActionPerformed(evt);
            }
        });
        jPanel5.add(chrStartPosition, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 10, 93, 50));

        chrEndPosition.setText(resourceMap.getString("chrEndPosition.text")); // NOI18N
        chrEndPosition.setName("chrEndPosition"); // NOI18N
        chrEndPosition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chrEndPositionActionPerformed(evt);
            }
        });
        jPanel5.add(chrEndPosition, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 10, 95, 50));

        jLabel3.setFont(resourceMap.getFont("jLabel3.font")); // NOI18N
        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        jPanel5.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 10, -1, 50));

        readChartPanelContainer.setName("readChartPanelContainer"); // NOI18N

        org.jdesktop.layout.GroupLayout readChartPanelContainerLayout = new org.jdesktop.layout.GroupLayout(readChartPanelContainer);
        readChartPanelContainer.setLayout(readChartPanelContainerLayout);
        readChartPanelContainerLayout.setHorizontalGroup(
            readChartPanelContainerLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 960, Short.MAX_VALUE)
        );
        readChartPanelContainerLayout.setVerticalGroup(
            readChartPanelContainerLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 200, Short.MAX_VALUE)
        );

        jPanel5.add(readChartPanelContainer, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 70, 960, 200));

        nucleosomeChartPanelContainer.setName("nucleosomeChartPanelContainer"); // NOI18N

        org.jdesktop.layout.GroupLayout nucleosomeChartPanelContainerLayout = new org.jdesktop.layout.GroupLayout(nucleosomeChartPanelContainer);
        nucleosomeChartPanelContainer.setLayout(nucleosomeChartPanelContainerLayout);
        nucleosomeChartPanelContainerLayout.setHorizontalGroup(
            nucleosomeChartPanelContainerLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 960, Short.MAX_VALUE)
        );
        nucleosomeChartPanelContainerLayout.setVerticalGroup(
            nucleosomeChartPanelContainerLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 190, Short.MAX_VALUE)
        );

        jPanel5.add(nucleosomeChartPanelContainer, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 320, 960, 190));

        jLabel2.setFont(resourceMap.getFont("jLabel2.font")); // NOI18N
        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jPanel5.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 10, -1, 50));

        right3Button.setFont(resourceMap.getFont("right1Button.font")); // NOI18N
        right3Button.setText(resourceMap.getString("right3Button.text")); // NOI18N
        right3Button.setName("right3Button"); // NOI18N
        right3Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                right3ButtonActionPerformed(evt);
            }
        });
        jPanel5.add(right3Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(920, 10, 61, 50));

        left3Button.setFont(resourceMap.getFont("right1Button.font")); // NOI18N
        left3Button.setText(resourceMap.getString("left3Button.text")); // NOI18N
        left3Button.setName("left3Button"); // NOI18N
        left3Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                left3ButtonActionPerformed(evt);
            }
        });
        jPanel5.add(left3Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(620, 10, 61, 50));

        left2Button.setFont(resourceMap.getFont("right1Button.font")); // NOI18N
        left2Button.setText(resourceMap.getString("left2Button.text")); // NOI18N
        left2Button.setName("left2Button"); // NOI18N
        left2Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                left2ButtonActionPerformed(evt);
            }
        });
        jPanel5.add(left2Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(680, 10, 61, 50));

        left1Button.setFont(resourceMap.getFont("right1Button.font")); // NOI18N
        left1Button.setText(resourceMap.getString("left1Button.text")); // NOI18N
        left1Button.setName("left1Button"); // NOI18N
        left1Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                left1ButtonActionPerformed(evt);
            }
        });
        jPanel5.add(left1Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 10, 61, 50));

        right1Button.setFont(resourceMap.getFont("right1Button.font")); // NOI18N
        right1Button.setText(resourceMap.getString("right1Button.text")); // NOI18N
        right1Button.setName("right1Button"); // NOI18N
        right1Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                right1ButtonActionPerformed(evt);
            }
        });
        jPanel5.add(right1Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(800, 10, 61, 50));

        right2Button.setFont(resourceMap.getFont("right1Button.font")); // NOI18N
        right2Button.setText(resourceMap.getString("right2Button.text")); // NOI18N
        right2Button.setName("right2Button"); // NOI18N
        right2Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                right2ButtonActionPerformed(evt);
            }
        });
        jPanel5.add(right2Button, new org.netbeans.lib.awtextra.AbsoluteConstraints(860, 10, 61, 50));

        coverageExportToPDF.setFont(resourceMap.getFont("exportReadPlotToPDF.font")); // NOI18N
        coverageExportToPDF.setText(resourceMap.getString("coverageExportToPDF.text")); // NOI18N
        coverageExportToPDF.setName("coverageExportToPDF"); // NOI18N
        coverageExportToPDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                coverageExportToPDFActionPerformed(evt);
            }
        });
        jPanel5.add(coverageExportToPDF, new org.netbeans.lib.awtextra.AbsoluteConstraints(860, 510, 130, -1));

        exportReadPlotToPDF.setFont(resourceMap.getFont("exportReadPlotToPDF.font")); // NOI18N
        exportReadPlotToPDF.setText(resourceMap.getString("exportReadPlotToPDF.text")); // NOI18N
        exportReadPlotToPDF.setName("exportReadPlotToPDF"); // NOI18N
        exportReadPlotToPDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportReadPlotToPDFActionPerformed(evt);
            }
        });
        jPanel5.add(exportReadPlotToPDF, new org.netbeans.lib.awtextra.AbsoluteConstraints(857, 270, 130, -1));

        jPanel11.setName("jPanel11"); // NOI18N

        jScrollPane6.setName("jScrollPane6"); // NOI18N

        displayTextPane.setEditable(false);
        displayTextPane.setFont(resourceMap.getFont("displayTextPane.font")); // NOI18N
        displayTextPane.setName("displayTextPane"); // NOI18N
        jScrollPane6.setViewportView(displayTextPane);

        org.jdesktop.layout.GroupLayout jPanel11Layout = new org.jdesktop.layout.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 960, Short.MAX_VALUE)
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11Layout.createSequentialGroup()
                .add(20, 20, 20)
                .add(jScrollPane6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE))
        );

        jPanel5.add(jPanel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 540, 960, 110));

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .add(31, 31, 31)
                .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(37, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanel4.TabConstraints.tabTitle"), jPanel4); // NOI18N

        jTabbedPane1.getAccessibleContext().setAccessibleName(resourceMap.getString("jTabbedPane1.AccessibleContext.accessibleName")); // NOI18N

        jFileChooser1.setName("jFileChooser1"); // NOI18N

        jFileChooser2.setName("jFileChooser2"); // NOI18N

        setComponent(jTabbedPane1);
        setMenuBar(menuBar);
    }// </editor-fold>//GEN-END:initComponents

private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    jTabbedPane1.setSelectedIndex(1);
   
}//GEN-LAST:event_jButton2ActionPerformed
private void updateTextArea(String a){
        StyledDocument doc = (StyledDocument)jTextArea1.getDocument();
        
        doc.setLogicalStyle(doc.getLength(), blackFontStyle);
        try {
            doc.insertString(doc.getLength(), a+"\n", null);
            jTextArea1.setCaretPosition(jTextArea1.getDocument().getLength());
        }catch (BadLocationException ble) {
            System.err.println("Couldn't insert initial text into text pane.");
        }
}

public void updateRunTextArea(String a){
    
        StyledDocument doc = (StyledDocument)runTextPane.getDocument();
        
        doc.setLogicalStyle(doc.getLength(), blackFontStyle);
        try {
            doc.insertString(doc.getLength(), a+"\n", null);
            runTextPane.setCaretPosition(runTextPane.getDocument().getLength());
        }catch (BadLocationException ble) {
            System.err.println("Couldn't insert initial text into text pane.");
        }
}

public void updateDisplayTextArea(String a){
    
        StyledDocument doc = (StyledDocument)displayTextPane.getDocument();
        
        doc.setLogicalStyle(doc.getLength(), blackFontStyle);
        try {
            doc.insertString(doc.getLength(), a+"\n", null);
            displayTextPane.setCaretPosition(displayTextPane.getDocument().getLength());
        }catch (BadLocationException ble) {
            System.err.println("Couldn't insert initial text into text pane.");
        }
}

private void warnDisplayTextArea(String a){
        StyledDocument doc = (StyledDocument)displayTextPane.getDocument();
        
        
        doc.setLogicalStyle(doc.getLength(), redFontStyle);
        try {
            doc.insertString(doc.getLength(), a+"\n", null);
            displayTextPane.setCaretPosition(displayTextPane.getDocument().getLength());
        }catch (BadLocationException ble) {
            System.err.println("Couldn't insert initial text into text pane.");
        }

}

public void updateRunTextAreaNoReturn(String a){
    
        StyledDocument doc = (StyledDocument)runTextPane.getDocument();
        
        doc.setLogicalStyle(doc.getLength(), blackFontStyle);
        try {
            doc.insertString(doc.getLength(), a, null);
            runTextPane.setCaretPosition(runTextPane.getDocument().getLength());
        }catch (BadLocationException ble) {
            System.err.println("Couldn't insert initial text into text pane.");
        }
}

private void warnRunTextArea(String a){
        StyledDocument doc = (StyledDocument)runTextPane.getDocument();
        
        
        doc.setLogicalStyle(doc.getLength(), redFontStyle);
        try {
            doc.insertString(doc.getLength(), a+"\n", null);
            runTextPane.setCaretPosition(runTextPane.getDocument().getLength());
        }catch (BadLocationException ble) {
            System.err.println("Couldn't insert initial text into text pane.");
        }

}


private void warnTextArea(String a){
        StyledDocument doc = (StyledDocument)jTextArea1.getDocument();
        
        
        doc.setLogicalStyle(doc.getLength(), redFontStyle);
        try {
            doc.insertString(doc.getLength(), a+"\n", null);
            java.awt.Rectangle visibleRect = jTextArea1.getVisibleRect();
            visibleRect.y = jTextArea1.getHeight() - visibleRect.height;
            jTextArea1.scrollRectToVisible(visibleRect);
        }catch (BadLocationException ble) {
            System.err.println("Couldn't insert initial text into text pane.");
        }

}
private long computeGenomeLength(){
    long genomeLength = 0;
    for (Enumeration e = chrLengths.keys() ; e.hasMoreElements() ;) {
            genomeLength += (Integer) chrLengths.get(e.nextElement());
    }
    return genomeLength;
}
private void chrLengthButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chrLengthButtonActionPerformed
// TODO add your handling code here:
try {
    int result = jFileChooser1.showOpenDialog(this.getFrame());
    switch (result) {
            case javax.swing.JFileChooser.APPROVE_OPTION:
                lengthFile = jFileChooser1.getSelectedFile();
                dirPath = lengthFile.getParent()+File.separator;
                
                updateTextArea("Chromosome length file: " + lengthFile);
                //updateTextArea("Dir Path: " + dirPath);
                alignmentButton.setEnabled(true);
                
                chrLengths = FileParser.getChromosomeLength(lengthFile.getPath());
                updateTextArea("Total genome length =  "+computeGenomeLength()+"\n");
            break;
        case javax.swing.JFileChooser.CANCEL_OPTION:
            return;
        case javax.swing.JFileChooser.ERROR_OPTION:
            // The selection process did not complete successfully
            break;
            
         
    }
  } catch (Exception e) {
      updateTextArea("Invalid chromosome length file. Please select a different file.");
  }
}//GEN-LAST:event_chrLengthButtonActionPerformed

private void alignmentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_alignmentButtonActionPerformed
// TODO add your handling code here:
    if (dirPath != null) jFileChooser2.setCurrentDirectory(new File(dirPath));
   
    int result = jFileChooser2.showOpenDialog(this.getFrame());
    switch (result) {
            case javax.swing.JFileChooser.APPROVE_OPTION:
                seqFile = jFileChooser2.getSelectedFile();
                String seqFileString = seqFile.getName();
                updateTextArea("Sequence File: " + seqFile);
                //updateTextArea("Dir Path: " + dirPath);
                if ("bam".equals(seqFileString.substring(seqFileString.lastIndexOf(".")+1).toLowerCase())) {
                    inputComboBox.setSelectedItem("BAM");
                } else if ("sam".equals(seqFileString.substring(seqFileString.lastIndexOf(".")+1).toLowerCase())) {
                    inputComboBox.setSelectedItem("SAM");
                } else if ("bed".equals(seqFileString.substring(seqFileString.lastIndexOf(".")+1).toLowerCase())) {
                    inputComboBox.setSelectedItem("BED");
                }
            break;
        case javax.swing.JFileChooser.CANCEL_OPTION:
            return;
        case javax.swing.JFileChooser.ERROR_OPTION:
            // The selection process did not complete successfully
            break;
    }
}//GEN-LAST:event_alignmentButtonActionPerformed

private void runButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runButtonActionPerformed
// TODO add your handling code here:
    if (lengthFile == null && chrLengths == null){
        if (seqFile == null ){
            warnTextArea("Missing chromosome length and sequencing data files.");
        } else warnTextArea("Missing chromosome length file.");   
    } else if (seqFile == null){
       warnTextArea("Missing sequencing data file."); 
    }
    else{
        jTabbedPane1.setSelectedIndex(2);
        Runnable updateGUI = new Runnable() {  
                public void run() {  
                    updateRunTextArea("Using " +threadsTextField.getText() + " threads.\n"); 
                    updateRunTextArea("Reading data and constructing probability distributions for nucleosome centers...");  
                }  
            };  
        Thread t = new Thread(updateGUI); 
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();  
                
            Runnable read = new Runnable() {  
                public void run() {  
                     DataProcess();
                }  
            };  
            Thread readThread = new Thread(read); 
            readThread.start();  
        
        
    }
}//GEN-LAST:event_runButtonActionPerformed

private void displayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayButtonActionPerformed
    jTabbedPane1.setSelectedIndex(3);
}//GEN-LAST:event_displayButtonActionPerformed

private void chrStartPositionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chrStartPositionActionPerformed
    int startPosition = Integer.parseInt(chrStartPosition.getText()), endPosition = Integer.parseInt(chrEndPosition.getText());
    displayResults(startPosition, endPosition);
}//GEN-LAST:event_chrStartPositionActionPerformed

private void chrEndPositionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chrEndPositionActionPerformed
    int startPosition = Integer.parseInt(chrStartPosition.getText()), endPosition = Integer.parseInt(chrEndPosition.getText());
    displayResults(startPosition, endPosition);
}//GEN-LAST:event_chrEndPositionActionPerformed

private void chrComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chrComboBoxActionPerformed
    int startPosition = Integer.parseInt(chrStartPosition.getText()), endPosition = Integer.parseInt(chrEndPosition.getText());
    displayResults(startPosition, endPosition);
}//GEN-LAST:event_chrComboBoxActionPerformed

private void left3ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_left3ButtonActionPerformed
    int startPosition = Integer.parseInt(chrStartPosition.getText()), endPosition = Integer.parseInt(chrEndPosition.getText());
    if (startPosition < 100000) {
        startPosition = 100000;
    }
    if (endPosition <= 100000) {
        endPosition = endPosition+100000;
    }
    displayResults(startPosition-100000, endPosition-100000);
}//GEN-LAST:event_left3ButtonActionPerformed

private void left2ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_left2ButtonActionPerformed
    int startPosition = Integer.parseInt(chrStartPosition.getText()), endPosition = Integer.parseInt(chrEndPosition.getText());
    if (startPosition < 10000) {
        startPosition = 1000;
    }
    if (endPosition <= 10000) {
        endPosition = endPosition+10000;
    }
    displayResults(startPosition-10000, endPosition-10000);
}//GEN-LAST:event_left2ButtonActionPerformed

private void left1ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_left1ButtonActionPerformed
    int startPosition = Integer.parseInt(chrStartPosition.getText()), endPosition = Integer.parseInt(chrEndPosition.getText());
    if (startPosition < 1000) {
        startPosition = 1000;
    }
    if (endPosition <= 1000) {
        endPosition = endPosition+1000;
    }
    displayResults(startPosition-1000, endPosition-1000);
}//GEN-LAST:event_left1ButtonActionPerformed

private void right1ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_right1ButtonActionPerformed
    int chrLength = br.getChrList().get((String) chrComboBox.getSelectedItem()).getLength();
    int startPosition = Integer.parseInt(chrStartPosition.getText()), endPosition = Integer.parseInt(chrEndPosition.getText());
    if (startPosition >= chrLength-1000) {
        startPosition = startPosition-1000;
    }
    if (endPosition >= chrLength-1000) {
        endPosition = chrLength-1000;
    }
    displayResults(startPosition+1000, endPosition+1000);
}//GEN-LAST:event_right1ButtonActionPerformed

private void right2ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_right2ButtonActionPerformed
    int chrLength = br.getChrList().get((String) chrComboBox.getSelectedItem()).getLength();
    int startPosition = Integer.parseInt(chrStartPosition.getText()), endPosition = Integer.parseInt(chrEndPosition.getText());
    if (startPosition >= chrLength-10000) {
        startPosition = startPosition-10000;
    }
    if (endPosition >= chrLength-10000) {
        endPosition = chrLength-10000;
    }
    displayResults(startPosition+10000, endPosition+10000);
}//GEN-LAST:event_right2ButtonActionPerformed

private void right3ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_right3ButtonActionPerformed
    int chrLength = br.getChrList().get((String) chrComboBox.getSelectedItem()).getLength();
    int startPosition = Integer.parseInt(chrStartPosition.getText()), endPosition = Integer.parseInt(chrEndPosition.getText());
    if (startPosition >= chrLength-100000) {
        startPosition = startPosition-100000;
    }
    if (endPosition >= chrLength-100000) {
        endPosition = chrLength-100000;
    }
    displayResults(startPosition+100000, endPosition+100000);
}//GEN-LAST:event_right3ButtonActionPerformed


//The next two methods are derived from the JFreeChart guide; they rely on iTextPDF
public static void writeChartAsPDF(BufferedOutputStream out, JFreeChart chart, int width, int height, FontMapper mapper) throws IOException, DocumentException {
    Rectangle pagesize = new Rectangle(width, height);
    Document document = new Document(pagesize, 50, 50, 50, 50);
    try {
        PdfWriter writer = PdfWriter.getInstance(document, out); document.addAuthor("JFreeChart"); document.addSubject("Demonstration");
        document.open();
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate tp = cb.createTemplate(width, height);
        Graphics2D g2 = tp.createGraphics(width, height, mapper); 
        Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
        chart.draw(g2, r2D);
        g2.dispose();
        cb.addTemplate(tp, 0, 0);
    } catch (DocumentException de) {
        System.err.println(de.getMessage());
    }
    document.close();
}

public static void saveChartAsPDF(File file, JFreeChart chart, int width, int height, FontMapper mapper) throws IOException, DocumentException {
    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
    writeChartAsPDF(out, chart, width, height, mapper);
    out.close();
}

private void exportReadPlotToPDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportReadPlotToPDFActionPerformed
    SimpleDateFormat df = new SimpleDateFormat("MM.dd.yyyy_HH.mm.ss");
    String outPDF = seqFile.getPath()+"_NSeq_Reads_"+df.format(new Date(System.currentTimeMillis()))+".pdf";
    try {
        saveChartAsPDF(new File(outPDF), readChart, 960, 200, new DefaultFontMapper());
        updateDisplayTextArea("Display coverage plot exported to " + outPDF + ".");
    } catch (IOException e) {
        warnDisplayTextArea("Could not write PDF: " + e.getMessage());
    } catch (DocumentException e) {
        warnDisplayTextArea("Could not write PDF: " + e.getMessage());
    }
}//GEN-LAST:event_exportReadPlotToPDFActionPerformed

private void coverageExportToPDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_coverageExportToPDFActionPerformed
    SimpleDateFormat df = new SimpleDateFormat("MM.dd.yyyy_HH.mm.ss");
    String outPDF = seqFile.getPath()+"_NSeq_Reads_"+df.format(new Date(System.currentTimeMillis()))+".pdf";
    try {
        saveChartAsPDF(new File(outPDF), nucleosomeChart, 960, 200, new DefaultFontMapper());
        updateDisplayTextArea("Nucleosome coverage plot exported to " + outPDF + ".");
    } catch (IOException e) {
        warnDisplayTextArea("Could not write PDF: " + e.getMessage());
    } catch (DocumentException e) {
        warnDisplayTextArea("Could not write PDF: " + e.getMessage());
    }
}//GEN-LAST:event_coverageExportToPDFActionPerformed

private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
// TODO add your handling code here:
    String genome = (String) jComboBox1.getSelectedItem();
    if (genome.equals("human (hg19)")){
        chrLengthButton.setEnabled(false);
        chrLengths = StandardGenomes.hg19();
        FileParser.setOrderedChromosomes(chrLengths);
        updateTextArea("HG19 genome chosen.");       
        alignmentButton.setEnabled(true);        
        updateTextArea("Total genome length =  "+computeGenomeLength()+"\n");
        lengthFile = null;
    } else if (genome.equals("human (hg18)")){
        chrLengthButton.setEnabled(false);
        chrLengths = StandardGenomes.hg18();
        FileParser.setOrderedChromosomes(chrLengths);
        updateTextArea("HG18 genome chosen.");       
        alignmentButton.setEnabled(true);        
        updateTextArea("Total genome length =  "+computeGenomeLength()+"\n");
        lengthFile = null;
    } 
    else if (genome.equals("mouse (mm10)")){
        chrLengthButton.setEnabled(false);
        chrLengths = StandardGenomes.mm10();
        FileParser.setOrderedChromosomes(chrLengths);
        updateTextArea("MM10 genome chosen.");       
        alignmentButton.setEnabled(true);        
        updateTextArea("Total genome length =  "+computeGenomeLength()+"\n");
        lengthFile = null;
    } 
    else if (genome.equals("mouse (mm9)")){
        chrLengthButton.setEnabled(false);
        chrLengths = StandardGenomes.mm9();
        FileParser.setOrderedChromosomes(chrLengths);
        updateTextArea("MM9 genome chosen.");       
        alignmentButton.setEnabled(true);        
        updateTextArea("Total genome length =  "+computeGenomeLength()+"\n");
        lengthFile = null;
    } 
    else if (genome.equals("worm (ce10)")){
        chrLengthButton.setEnabled(false);
        chrLengths = StandardGenomes.ce10();
        FileParser.setOrderedChromosomes(chrLengths);
        updateTextArea("CE10 genome chosen.");       
        alignmentButton.setEnabled(true);        
        updateTextArea("Total genome length =  "+computeGenomeLength()+"\n");
        lengthFile = null;
    } 
    else if (genome.equals("Custom")){
        chrLengthButton.setEnabled(true);
        updateTextArea("Choose a custom chromosome length file.");
        alignmentButton.setEnabled(false);
        lengthFile = null;
        chrLengths = null;
    }
    
}//GEN-LAST:event_jComboBox1ActionPerformed

private void DataProcess(){
    double oddsCutOff = Double.parseDouble(tsCutoffTextField.getText());
    long start = System.currentTimeMillis();
    int nThreads = Integer.parseInt(threadsTextField.getText());
    jTabbedPane1.setEnabled(false);
    int nSim = Integer.parseInt(nSimTextField.getText());
    NucleosomeDetector nd =null;
    
    NSeqLabel.setText("NSeq-ing...");
    displayButton.setEnabled(false);

    if (inputComboBox.getSelectedItem().equals("BED")){
        br = new BedFileReader(chrLengths, seqFile.getPath(), 68, NucleosomeCenter.getBetaBins(1.92038028 , 1.89366713, 11));
    } else if (inputComboBox.getSelectedItem().equals("SAM")){
        br = new BamFileReader(chrLengths, seqFile.getPath(), 68, NucleosomeCenter.getBetaBins(1.92038028 , 1.89366713, 11));
    }
    else if (inputComboBox.getSelectedItem().equals("BAM")) {
        br = new BamFileReader(chrLengths, seqFile.getPath(), 68, NucleosomeCenter.getBetaBins(1.92038028 , 1.89366713, 11));
    }
    else {
        br = new BedFileReader(chrLengths, seqFile.getPath(), 68, NucleosomeCenter.getBetaBins(1.92038028 , 1.89366713, 11));
    }
    updateRunTextArea("*** Finished reading. ***\n");
    int minChrLength = -1;
    for (String chr : chrLengths.keySet()) {
        if (br.getChrList().get(chr).getPosRawList().length == 0 && br.getChrList().get(chr).getPosRawList().length == 0) {
           br.removeChr(chr);
           FileParser.removeOrderedChromosome(chr);
        } else if (chrLengths.get(chr) < minChrLength || minChrLength < 0) {
            minChrLength = chrLengths.get(chr);
        }
    }
    updateRunTextArea("Detecting nucleosomes....");
    nd = new NucleosomeDetector(Integer.parseInt(scanWidthTextField.getText()), Integer.parseInt(centerWidthTextField.getText()), nThreads, nSim, br.getChrList(), this, MUE, true);
    
    //int intervalSize = Collections.max(chrLengths.values())/nThreadsChrInteger;
    int intervalSize = Math.max(minChrLength/nThreads,100000);
    //intervalSize = 50000;
    nucleosomes = nd.computeIntervalWise(oddsCutOff, intervalSize);
    updateRunTextArea("\nComputing FDR using " +nSim + " simulations...");
    nd.computeFDR(oddsCutOff, nucleosomes, intervalSize);
    
    SimpleDateFormat df = new SimpleDateFormat("MM.dd.yyyy_HH.mm.ss");
    
    String formattedDate = df.format(new Date(System.currentTimeMillis()));
    String tmp = seqFile.getPath();
    if (tmp.lastIndexOf(".txt")== tmp.length()-4){
        tmp = tmp.substring(0, tmp.lastIndexOf(".txt"));
    }
    String outFile = tmp+"_NSeq_"+formattedDate+".txt";
    String outWig = tmp+"_NSeq_"+formattedDate+".wig";
    String info = "# Sequence file: "+seqFile.getPath()+"\n";
    if(lengthFile != null) info += "# Chromosome length file:" + lengthFile.getPath()+"\n";
    int numPassedNuc = 0;
    try{
        numPassedNuc=FileOutput.writeNucleosomes(nucleosomes, outFile, FDRTextField.getText(),nSim, info);
        updateRunTextArea(numPassedNuc + " nucleosomes found at " + FDRTextField.getText() +" FDR.");
        FileOutput.writeWig(nucleosomes, outWig,FDRTextField.getText());
    } catch (IOException ex) {
            ex.printStackTrace();
    }
    
    long elapsedTimeMillis = System.currentTimeMillis()-start;
    double elapsedTimeSec = elapsedTimeMillis/1000.0;
    updateRunTextArea("\nOutput written to " + outFile+"\n");
    updateRunTextArea("Time taken: "+Double.toString(elapsedTimeSec));
    updateRunTextArea("\n*** Finished processing. ***\n\n");
    NSeqLabel.setText("Complete.");
    jTabbedPane1.setEnabled(true);
    jTabbedPane1.setEnabledAt(3,true);
    jTabbedPane1.setEnabledAt(2,true);
    displayButton.setEnabled(true);
    DefaultCaret caret = (DefaultCaret)runTextPane.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    chrComboBox.setModel(new javax.swing.DefaultComboBoxModel(FileParser.getOrderedChromosomes().toArray()));
    displayResults(Integer.parseInt(chrStartPosition.getText()), Integer.parseInt(chrEndPosition.getText()));
}

private void displayResults(int startPosition, int endPosition) {
    boolean shouldDisplay = true;
    if (startPosition > endPosition) {
        shouldDisplay = false;
        updateDisplayTextArea("The start position must be less than or equal to the end position.");
    }
    if (Math.abs(startPosition - endPosition) > 100000) {
        shouldDisplay = false;
        updateDisplayTextArea("The maximum interval over which results can be plotted is 10 kb. Reduce interval size.");
    }
    if (shouldDisplay) {
        XYSeriesCollection theSeriesCollections[] = createXYReadNucleosomeSeries(startPosition, endPosition);
        readChart = createReadChart(theSeriesCollections[0]);
        nucleosomeChart = createNucleosomeChart(theSeriesCollections[1]);
        XYPlot readPlot = readChart.getXYPlot();
        XYPlot nucleosomePlot = nucleosomeChart.getXYPlot();
        NumberAxis domain = new NumberAxis("Position");
        domain.setRange(Integer.parseInt(chrStartPosition.getText()), Integer.parseInt(chrEndPosition.getText()));
        domain.setUpperMargin(2);
        readPlot.setDomainAxis(domain);
        XYItemRenderer renderer = readPlot.getRenderer();
        renderer.setSeriesPaint(0, Color.blue);
        renderer.setSeriesPaint(1, Color.red);
        readPlot.setRenderer(renderer);
        nucleosomePlot.setDomainAxis(domain);
        //Add annotations for nucleosome centers
        CandidateNucleosome[] chrNucleosomes = nucleosomes.get((String) chrComboBox.getSelectedItem());
        int nucLocation;
        for (CandidateNucleosome nucleosome : chrNucleosomes) {
            nucLocation = nucleosome.getLoc();
            if (startPosition <= nucLocation && endPosition >= nucLocation) {
                nucleosomePlot.addAnnotation(new XYLineAnnotation(nucLocation,0,nucLocation,100,new BasicStroke(1.5f), Color.red));
            }
        }
        if (readChartPanelContainer.getComponentCount() > 0) {
            readChartPanelContainer.remove(readChartPanelContainer.getComponent(0));
            nucleosomeChartPanelContainer.remove(nucleosomeChartPanelContainer.getComponent(0));
        }
        ChartPanel readChartPanel = new ChartPanel(readChart);
        readChartPanel.setPreferredSize(new java.awt.Dimension(100, 200));
        readChartPanel.setMouseWheelEnabled(true);
        readChartPanelContainer.setLayout(new java.awt.BorderLayout());
        readChartPanelContainer.add(readChartPanel, BorderLayout.CENTER);
        readChartPanelContainer.validate();
        /*XYPlot nucleosomePlot = readChart.getXYPlot();
        nucleosomePlot.setDomainAxis(domain);*/
        ChartPanel nucleosomeChartPanel = new ChartPanel(nucleosomeChart);
        nucleosomeChartPanel.setPreferredSize(new java.awt.Dimension(100, 200));
        nucleosomeChartPanel.setMouseWheelEnabled(true);
        nucleosomeChartPanelContainer.setLayout(new java.awt.BorderLayout());
        nucleosomeChartPanelContainer.add(nucleosomeChartPanel, BorderLayout.CENTER);
        nucleosomeChartPanelContainer.validate();
        updateDisplayTextArea("Plotted " + (String) chrComboBox.getSelectedItem() + " from position " + chrStartPosition.getText() + " to position " + chrEndPosition.getText() + ".");
    }
}

private XYSeriesCollection[] createXYReadNucleosomeSeries(int startPosition, int endPosition) {
    chrStartPosition.setText(Integer.toString(startPosition));
    chrEndPosition.setText(Integer.toString(endPosition));
    XYSeries plusSeries = new XYSeries("Sense strand reads");
    XYSeries minusSeries = new XYSeries("Antisense strand reads");
    XYSeries nucleosomeCoverageSeries = new XYSeries("Bases covered by nucleosomes");
    XYSeries nucleosomeCenterSeries = new XYSeries("Nucleosome centers");
    int[] readHistogramPlus = new int[endPosition-startPosition+1];
    int[] readHistogramMinus = new int[endPosition-startPosition+1];
    int[] nucleosomeCoverage = new int[endPosition-startPosition+1];
    int[] rawListPlus, rawListMinus;
    rawListPlus = br.getChrList().get((String) chrComboBox.getSelectedItem()).getPosRawList();
    rawListMinus = br.getChrList().get((String) chrComboBox.getSelectedItem()).getNegRawList();
    int extendWidth = 75;
    int extendShift = 37;
    for (int i=0; i<rawListPlus.length; i++) {
        if (startPosition <= rawListPlus[i] && endPosition >= rawListPlus[i]) {
            readHistogramPlus[rawListPlus[i]-startPosition]++;
            for (int j=rawListPlus[i]+extendShift; j<rawListPlus[i]+extendWidth+extendShift && j-startPosition<nucleosomeCoverage.length; j++) {
                nucleosomeCoverage[j-startPosition]++;
            }
        }
    }
    for (int i=0; i<rawListMinus.length; i++) {
        if (startPosition <= rawListMinus[i] && endPosition >= rawListMinus[i]) {
            readHistogramMinus[rawListMinus[i]-startPosition]--;
            for (int j=rawListMinus[i]-extendShift; j>rawListMinus[i]-extendWidth-extendShift && j-startPosition>=0; j--) {
                nucleosomeCoverage[j-startPosition]++;
            }
        }
    }
    //Construct read histograms
    for (int i=0; i<readHistogramPlus.length; i++) {
        plusSeries.add(i+startPosition, readHistogramPlus[i]);
    }
    for (int i=0; i<readHistogramMinus.length; i++) {
        minusSeries.add(i+startPosition, readHistogramMinus[i]);
    }
    //Construct nucleosome coverage XY plot
    for (int i=0; i<nucleosomeCoverage.length; i++) {
        nucleosomeCoverageSeries.add(i+startPosition, nucleosomeCoverage[i]);
    }
    XYSeriesCollection[] dataset = new XYSeriesCollection[2];
    dataset[0] = new XYSeriesCollection();
    dataset[1] = new XYSeriesCollection();
    dataset[0].addSeries(plusSeries);
    dataset[0].addSeries(minusSeries);
    dataset[1].addSeries(nucleosomeCenterSeries);
    dataset[1].addSeries(nucleosomeCoverageSeries);
    return dataset;
}

private static JFreeChart createReadChart(XYSeriesCollection aSeriesCollection) {
    JFreeChart chart = ChartFactory.createHistogram(
    "Read counts", // chart title "Release", // domain axis label
    "Position",
    "Reads",
    aSeriesCollection, // data
    PlotOrientation.VERTICAL, // orientation false, // include legend
    true,
    true,// tooltips
    false);
    return chart;
}

private static JFreeChart createNucleosomeChart(XYSeriesCollection aSeriesCollection) {
    JFreeChart chart = ChartFactory.createXYAreaChart(
    "Nucleosome coverage", // chart title "Release", // domain axis label
    "Position",
    "Counts",
    aSeriesCollection, // data
    PlotOrientation.VERTICAL, // orientation false, // include legend
    true,
    true,// tooltips
    false);
    return chart;
}

private void saveResults(HashMap<String,CandidateNucleosome[]> nucleosomes){
    
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JTextField FDRTextField;
    protected javax.swing.JLabel NSeqLabel;
    protected javax.swing.JButton alignmentButton;
    protected javax.swing.JTextField centerWidthTextField;
    protected javax.swing.JComboBox chrComboBox;
    protected javax.swing.JTextField chrEndPosition;
    protected javax.swing.JButton chrLengthButton;
    protected javax.swing.JTextField chrStartPosition;
    protected javax.swing.JButton coverageExportToPDF;
    protected javax.swing.JButton displayButton;
    protected javax.swing.JTextPane displayTextPane;
    protected javax.swing.JButton exportReadPlotToPDF;
    protected javax.swing.JComboBox inputComboBox;
    protected javax.swing.JButton jButton2;
    protected javax.swing.JComboBox jComboBox1;
    protected javax.swing.JFileChooser jFileChooser1;
    protected javax.swing.JFileChooser jFileChooser2;
    protected javax.swing.JLabel jLabel1;
    protected javax.swing.JLabel jLabel10;
    protected javax.swing.JLabel jLabel11;
    protected javax.swing.JLabel jLabel12;
    protected javax.swing.JLabel jLabel13;
    protected javax.swing.JLabel jLabel14;
    protected javax.swing.JLabel jLabel15;
    protected javax.swing.JLabel jLabel16;
    protected javax.swing.JLabel jLabel17;
    protected javax.swing.JLabel jLabel18;
    protected javax.swing.JLabel jLabel19;
    protected javax.swing.JLabel jLabel2;
    protected javax.swing.JLabel jLabel3;
    protected javax.swing.JLabel jLabel4;
    protected javax.swing.JLabel jLabel5;
    protected javax.swing.JLabel jLabel6;
    protected javax.swing.JLabel jLabel7;
    protected javax.swing.JLabel jLabel8;
    protected javax.swing.JLabel jLabel9;
    protected javax.swing.JPanel jPanel1;
    protected javax.swing.JPanel jPanel10;
    protected javax.swing.JPanel jPanel11;
    protected javax.swing.JPanel jPanel2;
    protected javax.swing.JPanel jPanel3;
    protected javax.swing.JPanel jPanel4;
    protected javax.swing.JPanel jPanel5;
    protected javax.swing.JPanel jPanel6;
    protected javax.swing.JPanel jPanel7;
    protected javax.swing.JPanel jPanel8;
    protected javax.swing.JPanel jPanel9;
    protected javax.swing.JScrollPane jScrollPane2;
    protected javax.swing.JScrollPane jScrollPane3;
    protected javax.swing.JScrollPane jScrollPane5;
    protected javax.swing.JScrollPane jScrollPane6;
    protected javax.swing.JScrollPane jScrollPane7;
    protected javax.swing.JTabbedPane jTabbedPane1;
    protected javax.swing.JTextPane jTextArea1;
    protected javax.swing.JTextArea jTextArea2;
    protected javax.swing.JTextArea jTextArea3;
    protected javax.swing.JButton left1Button;
    protected javax.swing.JButton left2Button;
    protected javax.swing.JButton left3Button;
    protected javax.swing.JPanel loadPanel;
    protected javax.swing.JPanel mainPanel;
    protected javax.swing.JMenuBar menuBar;
    protected javax.swing.JTextField nSimTextField;
    protected javax.swing.JPanel nucleosomeChartPanelContainer;
    protected javax.swing.JPanel readChartPanelContainer;
    protected javax.swing.JButton right1Button;
    protected javax.swing.JButton right2Button;
    protected javax.swing.JButton right3Button;
    protected javax.swing.JButton runButton;
    protected javax.swing.JTextPane runTextPane;
    protected javax.swing.JTextField scanWidthTextField;
    protected javax.swing.JTextField threadsTextField;
    protected javax.swing.JTextField tsCutoffTextField;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
}

