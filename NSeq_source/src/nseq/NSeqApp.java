/*
 * NSeqApp.java
 */

package nseq;

import java.io.File;
import java.io.FileNotFoundException;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class NSeqApp extends SingleFrameApplication {
    static String lengthFilename;
    static String seqFilename;
    static String fileType;
    static String assembly = "none";
    static File lengthFile;
    static File seqFile;
    static boolean rawGUIQ = true;
    static int numberOfThreads = 4;
    static double fdrCutoff = 0.01;
    static int numberOfSims = 10;
    static int windowWidth = 200;
    static int centerWidth = 50;
    static double tsCutoff = 1.7;
    
    
    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        NSeqView nsv;
        if (rawGUIQ) {
            nsv = new NSeqView(this);
            show(nsv);
            nsv.jTabbedPane1.setSelectedIndex(0);
        } else {
            nsv = new NSeqView(this, lengthFilename, seqFilename, fileType, numberOfThreads, fdrCutoff, numberOfSims, windowWidth, centerWidth, tsCutoff, assembly);
            show(nsv);
            nsv.jTabbedPane1.setSelectedIndex(2);
        }
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of NSeqApp
     */
    public static NSeqApp getApplication() {
        return Application.getInstance(NSeqApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args){
        
        Option help = new Option("h", "help", false, "print this message" );
        Option quiet = new Option("q", "quiet", false, "suppress console output in command-line mode" );
        Option gui = new Option("G", "gui", false, "open gui to search for nucleosomes with parameters from command line; use this option to view plots of results");
        Option alignmentfile = OptionBuilder.withArgName("file").hasArg().withLongOpt("alignment").withDescription("alignment file in sam, bam, or bed format").create("a");
        Option alignmentformat = OptionBuilder.withArgName("format").hasArg().withLongOpt("input").withDescription("input format (bam, sam, bed)").create("i");
        Option genomeassembly = OptionBuilder.withArgName("name").hasArg().withLongOpt("assembly").withDescription("genome assembly (ce10, mm9, mm10, hg18, hg19)").create("g");
        
        Option lengthfile = OptionBuilder.withArgName("file").hasArg().withLongOpt("lengths").withDescription("chromosome length file with tab-separated values; the first column should contain reference sequence names (e.g., chr1), and the second column should contain chromosome lengths in genome coordinates (e.g., 249250621)").create("l");
        Option threads = OptionBuilder.withArgName("number").hasArg().withLongOpt("threads").withDescription("(default 4) number of threads to use in nucleosome identification and processing; using twice the number of available processor cores is recommended").create("t");
        Option fdr = OptionBuilder.withArgName("number").hasArg().withLongOpt("fdrcutoff").withDescription("(default 0.01) false discovery rate cutoff; candidate nucleosomes whose false discovery rates exceed this value are not included in output files").create("f");
        Option sims = OptionBuilder.withArgName("number").hasArg().withLongOpt("simulations").withDescription("(default 10) number of simulations to use in computation of false discovery rates; higher values increase accuracy at the expense of performance").create("s");
        Option windowwidth = OptionBuilder.withArgName("number").hasArg().withLongOpt("window").withDescription("(default 200) the genomic-coordinate width of the scanning window for the triangle statistic computation; changing the default value of 200 is not recommended").create("w");
        Option centerwidth = OptionBuilder.withArgName("number").hasArg().withLongOpt("center").withDescription("(default 50) the genomic-coordinate width of the triangle statistic's central bin; changing the default value of 50 is not recommended").create("c");
        Option trianglecutoff = OptionBuilder.withArgName("number").hasArg().withLongOpt("tscutoff").withDescription("(default 1.7) the value of the triangle statistic below which the N-statistic is always 0; changing the default value of 1.7 is not recommended").create("T");

        
        
        Options options = new Options();

        options.addOption(help);
        options.addOption(gui );
        options.addOption(quiet);
        options.addOption(alignmentfile);
        options.addOption(alignmentformat);
        options.addOption(genomeassembly);
        options.addOption(lengthfile);
        options.addOption(threads );
        options.addOption(fdr);
        options.addOption(sims);
        options.addOption(windowwidth);
        options.addOption(centerwidth);
        options.addOption(trianglecutoff);
        
        HelpFormatter formatter = new HelpFormatter();
        String helpUsage = "java -jar NSeq.jar [OPTIONS]\nexclude [OPTIONS] to run gui, or use -G to search for nucleosomes in gui using parameters from command line";
        
        CommandLineParser parser = new GnuParser();
        
        // parse the command line arguments
        String errorSource = "none";
        try {
            CommandLine line = parser.parse( options, args );
            boolean executeQ = true;
            boolean printHelpQ = false;
            if (args.length == 0) {
                launch(NSeqApp.class, args);
            } else {
                if (line.hasOption("help")) {
                    formatter.printHelp(helpUsage, options);
                }
                else {
                    if (line.hasOption("lengths")) {
                        lengthFilename = line.getOptionValue("lengths");
                        lengthFile = new File(lengthFilename);
                        if (!(lengthFile.exists() && !lengthFile.isDirectory())) {
                            throw new FileNotFoundException("The chromosome length file entered was not found.");
                        }
                    } else if (line.hasOption("assembly")) {
                        assembly = line.getOptionValue("assembly");
                        if (!(assembly.toLowerCase().equals("ce10") || assembly.toLowerCase().equals("hg18") || assembly.toLowerCase().equals("hg19") || assembly.toLowerCase().equals("mm9") || assembly.toLowerCase().equals("mm10"))) {
                            throw new ParseException("The genome assembly should be ce10, hg18, hg19, mm9 or mm10.");
                        }
                    } else {
                        System.out.println("If command-line options are specified, either length file or genome assembly is required.");
                        printHelpQ = true;
                        executeQ = false;
                    }
                
                    if (line.hasOption("alignment")) {
                        seqFilename = line.getOptionValue("alignment");
                        seqFile = new File(seqFilename);
                        if (!(seqFile.exists() && !seqFile.isDirectory())) {
                            throw new FileNotFoundException("The alignment file entered was not found.");
                        }
                        //String[] splitFilename = seqFilename.split("\\.");
                        //fileType = splitFilename[splitFilename.length-1].toLowerCase();
                    } else {
                        System.out.println("If command-line options are specified, alignment file is required.");
                        printHelpQ = true;
                        executeQ = false;
                    }
                    
                    if (line.hasOption("input")){
                        String format = line.getOptionValue("input");
                        format = format.toUpperCase();
                        if (format.toLowerCase().equals("bam")){
                            fileType = "bam";
                        } else if (format.toLowerCase().equals("bed")){
                            fileType = "bed";
                        }
                         else if (format.equals("sam")){
                            fileType = "sam";
                         }
                         else {
                             throw new ParseException("The input format should be bam, sam, or bed.");
                         }
                    } else {
                        System.out.println("If command-line options are specified, input-file format is required.");
                        printHelpQ = true;
                        executeQ = false;
                    }
                    
                    if (line.hasOption("threads")) {
                        errorSource = "threads";
                        numberOfThreads = Integer.parseInt(line.getOptionValue("threads"));
                    }
                
                    if (line.hasOption("fdrcutoff")) {
                        errorSource = "fdrcutoff";
                        fdrCutoff = Double.parseDouble(line.getOptionValue("fdrcutoff"));
                        if (fdrCutoff < 0 || fdrCutoff > 1) {
                            throw new NumberFormatException();
                        }
                    }
                
                    if (line.hasOption("simulations")) {
                        errorSource = "simulations";
                        numberOfSims = Integer.parseInt(line.getOptionValue("simulations"));
                    }
                
                    if (line.hasOption("window")) {
                        errorSource = "window";
                        windowWidth = Integer.parseInt(line.getOptionValue("window"));
                        if (windowWidth < 3) {
                            throw new NumberFormatException();
                        }
                    }
                
                    if (line.hasOption("center")) {
                        errorSource = "center";
                        centerWidth = Integer.parseInt(line.getOptionValue("center"));
                        if (centerWidth > windowWidth - 2) {
                            throw new NumberFormatException();
                        }
                    }
                    
                    if (line.hasOption("center")) {
                        errorSource = "center";
                        centerWidth = Integer.parseInt(line.getOptionValue("center"));
                        if (centerWidth > windowWidth - 2) {
                            throw new NumberFormatException();
                        }
                    }
                    
                    if (line.hasOption("tscutoff")) {
                        errorSource = "tscutoff";
                        tsCutoff = Double.parseDouble(line.getOptionValue("tscutoff"));
                        if (tsCutoff <= 0) {
                            throw new NumberFormatException();
                        }
                    }
                
                    if (executeQ) {
                        if (line.hasOption("gui")) {
                            rawGUIQ = false;
                            launch(NSeqApp.class, args);
                        } else {
                            // COMMAND-LINE VERSION OF APP
                            NSeqCommand nsc = new NSeqCommand(lengthFilename, seqFilename, fileType, numberOfThreads, fdrCutoff, numberOfSims, windowWidth, centerWidth, tsCutoff, assembly, !line.hasOption("quiet"));
                        }
                    
                    } else if (printHelpQ) {
                        formatter.printHelp(helpUsage, options);
                    }
                }
            }
        }
        catch( ParseException e ) {
            System.out.println(e.getMessage());
            formatter.printHelp(helpUsage, options);
        }
        catch (NumberFormatException e) {
            if (errorSource.equals("threads")) {
                System.out.println("The thread number should be a positive integer.");
            } else if (errorSource.equals("fdrcutoff")) {
                System.out.println("The FDR cutoff should be a positive real number between 0 and 1.");
            } else if (errorSource.equals("sims")) {
                System.out.println("The number of simulations should be a positive integer.");
            } else if (errorSource.equals("center")) {
                System.out.println("The center width should be a positive integer at most 2 less than the scan window width.");
            } else if (errorSource.equals("window")) {
                System.out.println("The scan-window width should be a positive integer greater than 2.");
            } else if (errorSource.equals("tscutoff")) {
                System.out.println("The triangle-statistic cutoff should be greater than 0.");
            }
        }
        catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
}
