/*
 This isn't the best code I've ever written. I wrote it in high school, when I had just learned Java.
 If I had written it now, I would have used better variable names.
 */

import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.security.CodeSource;

public class CrossSpeciesGeneFinder {

    static PrintStream o = null;
    static PrintWriter wInfo = null;
    static ArrayList<String> errorList = new ArrayList<String>();

    private static void printlnFlush(String s) {
        if(wInfo != null) {
            wInfo.println(s);
            wInfo.flush();
        }
    }

    private static void fail(String geneQuery, String msg) {
        msg = "ERROR: "+msg;
        if(o != null) o.println(msg);
        if(!errorList.contains(geneQuery)) {
            errorList.add(geneQuery);
        }
        printlnFlush("*** "+msg.toUpperCase()+" ***");
        wInfo.close();
    }

    public static void main(String[] args){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // oh well.
        }

        JTextArea textArea = new JTextArea (24, 80);
        textArea.setFont(new Font("monospaced", Font.PLAIN, 12));
        textArea.setEditable (false);

        final JFrame frame = new JFrame ("Cross-Species Gene Finder");
        frame.setDefaultCloseOperation (JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if(JOptionPane.showConfirmDialog(frame, "Are you sure you want to quit? The search process will be stopped.",
                                                 "Confirm",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE ) == JOptionPane.OK_OPTION){
                    System.exit(0);
                }
            }
        });

        Container contentPane = frame.getContentPane ();
        contentPane.setLayout (new BorderLayout ());
        contentPane.add (new JScrollPane (textArea,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),BorderLayout.CENTER);
        frame.pack ();
        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH); // maximize
        frame.setVisible (true);

        JTextAreaOutputStream out = new JTextAreaOutputStream (textArea);
        System.setOut (new PrintStream (out));
        System.setErr (new PrintStream (out));
        o = System.out;
        HttpURLConnection.setFollowRedirects(true);

        String species = null;
        String evalueStr = null;
        String buffStr = null;
        boolean batch = false;
        o.println("Cross-Species Gene Finder for NCBI\nBy Ethan Nelson-Moore, (C) "+Calendar.getInstance().get(Calendar.YEAR));
        o.println("It will play a \"ta-da!\" sound when it's done, or an error sound if there are any errors.\n");
        ArrayList<String> queryList = new ArrayList<String>();
        int option = JOptionPane.showConfirmDialog(null, "Do you want to use batch mode?\n\nPress Cancel to quit.", "CSGF Mode Selection", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (option == JOptionPane.YES_OPTION) { // batch
            batch = true;
            o.print("Using batch mode. ");
            FileDialog fd = new FileDialog((Frame)null, "Choose accession number list file", FileDialog.LOAD);
            fd.setFile("*.txt");
            fd.setVisible(true);
            String filename = fd.getDirectory()+fd.getFile();
            if (filename == null || filename.equals("")) {
                System.exit(0);
                return;
            }
            o.println("Accession number list file: " + filename);
            try {
                Scanner scQL = new Scanner(new File(filename));
                String line1 = scQL.nextLine();
                if(line1 != null && line1.startsWith("!CSGFBatchV1")) {
                    if(line1.contains(":") && (line1.split(":").length == 3) || line1.split(":").length == 4) {
                        species = line1.split(":")[1].trim();
                        evalueStr = line1.split(":")[2].trim();
                        if(line1.split(":").length == 4) buffStr = line1.split(":")[3].trim();
                    }
                    while(scQL.hasNextLine()) {
                        String line = scQL.nextLine().trim();
                        if(line.contains("#")) line = line.split("#",-1)[0].trim();
                        if(line.equals("")) continue;
                        queryList.add(line);
                    }
                } else {
                    scQL.close();
                    JOptionPane.showMessageDialog(null, "Not a CSGF batch file!", "CSGF Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                    return;
                }
                scQL.close();
            } catch(IOException e) {
                JOptionPane.showMessageDialog(null, "Error loading file! "+e.getMessage(), "CSGF Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return;
            }
        } else if(option == JOptionPane.NO_OPTION) {
            String input = JOptionPane.showInputDialog(null, "Enter the NCBI protein accession number for the gene you want to find.", "CSGF Input", JOptionPane.QUESTION_MESSAGE);
            if(input == null || input.equals("")) {
                System.exit(0);
                return;
            }
            queryList.add(input.trim());
        } else {
            System.exit(0);
            return;
        }
        if(species == null || species.equals("")) species = JOptionPane.showInputDialog(null, "Enter the species you want to search for the gene in.", "CSGF Input", JOptionPane.QUESTION_MESSAGE).trim();
        if(species == null || species.equals("")) {
            System.exit(0);
            return;
        }
        double maxEvalue = 0;
        try {
            if(evalueStr != null) {
                maxEvalue = Double.parseDouble(evalueStr);
            } else {
                maxEvalue = Double.parseDouble(JOptionPane.showInputDialog(null, "Enter the maximum evalue to show results for.\n(Example: 1e-30)", "CSGF Input", JOptionPane.QUESTION_MESSAGE).trim());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "You didn't enter a valid number for the evalue!", "CSGF Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
            return;
        } catch(NullPointerException e) {
            System.exit(0);
            return;
        }
        long bufferLeft = 1000;
        long bufferRight = 1000;
        try {
            if(buffStr == null) {
                buffStr = JOptionPane.showInputDialog(null, "Enter the maximum buffer to save on either side of the match.\n(Example: 1000 or 2000,5000)", "CSGF Input", JOptionPane.QUESTION_MESSAGE).trim();
            }
            if(buffStr.contains(",")) {
                bufferLeft = Long.parseLong(buffStr.split(",")[0].trim());
                bufferRight = Long.parseLong(buffStr.split(",")[1].trim());
            } else {
                bufferLeft = bufferRight = Long.parseLong(buffStr.trim());
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "You didn't enter a valid number for the buffer option!", "CSGF Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
            return;
        } catch(NullPointerException e) {
            System.exit(0);
            return;
        } catch(ArrayIndexOutOfBoundsException e) {
            JOptionPane.showMessageDialog(null, "You didn't enter a valid number for the buffer option!", "CSGF Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
            return;
        }
        URL url3 = null;
        Scanner sc2 = null;
        Scanner sc3 = null;
        String assembly = "";
        String txid = "";
        String niceTitle = "";
        try {
            URL url2 = new URL("https://www.ncbi.nlm.nih.gov/genome/?term="+URLEncoder.encode(species, "UTF-8"));
            sc2 = new Scanner(url2.openConnection().getInputStream());
            while(sc2.hasNextLine()) {
                String line = sc2.nextLine().trim();
                if(line.startsWith("<title>") && line.endsWith("</title>")) {
                    niceTitle =  line.split(">")[1].split("-")[0].trim();
                    String id = niceTitle.split("\\(ID ")[1].split("\\)")[0];
                    o.println("Loaded basic info for " + niceTitle);
                    url3 = new URL("https://www.ncbi.nlm.nih.gov/assembly?LinkName=genome_assembly&from_uid="+URLEncoder.encode(id, "UTF-8"));
                    break;
                }
            }

            sc3 = new Scanner(url3.openConnection().getInputStream());
            while(sc3.hasNextLine()) {
                String line = sc3.nextLine().trim();

                if(line.contains("GenBank assembly accession: </dt><dd>")) {
                    assembly = line.split("GenBank assembly accession: </dt><dd>")[1].split(" ")[0].trim();
                    break;
                }
            }
            if(sc3 != null) sc3.close();
            URL url4 = new URL("https://www.ncbi.nlm.nih.gov/assembly/"+URLEncoder.encode(assembly,"UTF-8"));
            sc3 = new Scanner(url4.openConnection().getInputStream());
            while(sc3.hasNextLine()) {
                String line = sc3.nextLine().trim();

                if(line.contains("href=\"/genome/?term=txid")) {
                    txid = line.split("href=\"/genome/\\?term=txid")[1].split("\\[")[0].trim();
                    break;
                }
            }
        } catch (MalformedURLException e) {
            o.println("ERROR: Species search makes invalid URL!");
            return;
        } catch (UnsupportedEncodingException e) {
            o.println("ERROR: Failed to URL-encode species name!");
            return;
        } catch (IOException e) {
            o.println("ERROR: Failed to load species info!");
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            o.println("ERROR: Species not found in NCBI database! (or they changed their HTML)");
            return;
        } catch (NullPointerException e) {
            o.println("ERROR: NPE (you shouldn't see this)");
            return;
        } finally {
            if(sc2 != null) sc2.close();
            if(sc3 != null) sc3.close();
        }
        o.println("Got TXID " + txid);
        for(String geneQuery : queryList) {
            try {
                new File("Results/"+species+"/"+geneQuery).mkdirs();
                wInfo = new PrintWriter("Results/"+species+"/"+geneQuery+"/Info.txt", "UTF-8");
            } catch (UnsupportedEncodingException e) {
                o.println("ERROR: Failed to create results file!");
                continue;
            } catch(IOException e) {
                o.println("ERROR: Failed to create or write to results file or folder!");
                continue;
            } catch (NullPointerException e) {
                o.println("ERROR: NPE (you shouldn't see this)");
                continue;
            }

            printlnFlush("Searching in Species Genome: " + niceTitle);
            printlnFlush("Maximum evalue: " + maxEvalue);

            String aminoSeq = null;
            String geneName = null;
            Scanner sc1 = null;
            Scanner scGetFASTALink = null;
            String fastaLink = null;
            String fastaParam = null;
            String fastaId = null;
            try {
                URL urlGetFASTALink = new URL("https://www.ncbi.nlm.nih.gov/gene/?term="+URLEncoder.encode(geneQuery, "UTF-8"));
                scGetFASTALink = new Scanner(urlGetFASTALink.openConnection().getInputStream());
                boolean hasLink = false;
                boolean hasId = false;
                while(scGetFASTALink.hasNextLine()) {
                    String line = scGetFASTALink.nextLine().trim();
                    if(line.contains("<a title=\"Nucleotide FASTA report\"")) {
                        hasLink = true;
                        fastaParam = line.split("\"")[3].replaceAll("&amp;","&").split("\\?")[1];
                        o.println("Got FASTA start/end points for " + geneQuery);
                    } else if(line.contains("&amp;nuc_gi=")){
                        hasId = true;
                        fastaId = line.split("&amp;nuc_gi=")[1].split("&")[0];
                        o.println("Got FASTA nuc_gi ID for " + geneQuery);
                    }
                    if(hasLink && hasId) {
                        fastaLink= "https://www.ncbi.nlm.nih.gov/sviewer/viewer.cgi?"+fastaParam+"&id="+fastaId+"&tool=portal&save=file&log$=seqview&db=nuccore&extrafeat=null&conwithfeat=on&hide-cdd=on";
                        break;
                    }
                }
                if(!hasLink || !hasId) {
                    throw new IOException(); // hacky
                }
                URL url1 = new URL(fastaLink);
                sc1 = new Scanner(url1.openConnection().getInputStream());
                sc1.useDelimiter("\0");
                String tNxt = sc1.next().trim();
                aminoSeq = tNxt.split("\n",2)[1].replaceAll("\n","");
                geneName = tNxt.split("\n",2)[0].split(" ",2)[1].trim();
                o.println("Gene name: "+ geneName);
                printlnFlush("NCBI Gene Query Used: " + geneQuery + " - " + geneName);
            } catch (MalformedURLException e) {
                fail(geneQuery,"Gene query makes invalid URL!");
                continue;
            } catch (NullPointerException e) {
                fail(geneQuery,"NPE (you shouldn't see this)");
                continue;
            } catch (UnsupportedEncodingException e) {
                fail(geneQuery,"Failed to URL-encode gene query!");
                continue;
            } catch (IOException e) {
                fail(geneQuery,"Failed to load gene query info!");
                continue;
            } catch (ArrayIndexOutOfBoundsException e) {
                fail(geneQuery,"Gene query not found in NCBI database! (or they changed their HTML)");
                continue;
            } finally {
                if(scGetFASTALink != null) scGetFASTALink.close();
                if(sc1 != null) sc1.close();
            }
            String tblastnRID = "";
            try {
                o.print("Starting TBLASTN... ");
                // problem here
                String tblastnQuery = "CMD=Put&QUERY="+URLEncoder.encode(aminoSeq, "UTF-8")+"&BLAST_SPEC=Assembly&PROGRAM=tblastn&FORMAT_TYPE=text&DATABASE=nr&EQ_MENU="+URLEncoder.encode(txid, "UTF-8");
                System.err.println(tblastnQuery);
                tblastnRID = doBlastAndGetRID(tblastnQuery,wInfo);
            } catch (UnsupportedEncodingException e) {
                fail(geneQuery,"Failed to URL-encode TBLASTN query!");
                continue;
            } catch (ArrayIndexOutOfBoundsException e) {
                fail(geneQuery,"Invalid TBLASTN query (or they changed their HTML)");
                continue;
            }
            catch(IOException e) {
                fail(geneQuery,"Failed to upload TBLASTN query!");
                continue;
            } catch (NullPointerException e) {
                fail(geneQuery,"NPE (you shouldn't see this)");
                continue;
            }
            if(tblastnRID.equals("")) {
                fail(geneQuery,"Invalid TBLASTN result (or they changed their HTML)");
                continue;
            }

            Map<String,String> mapIDToDescription = new TreeMap<String,String>();
            Map<String,Long> mapLowestForEachID = new TreeMap<String,Long>();
            Map<String,Long> mapHighestForEachID = new TreeMap<String,Long>();
            Map<String,Double> mapEvalueForEachID = new TreeMap<String,Double>();
            Map<String,Long> mapLengthForEachID = new TreeMap<String,Long>();
            Scanner sc6 = null;
            try {
                o.print("Searching for results in genome of " + species + " (TXID " + txid + ")...");
                URL url6 = new URL("https://blast.ncbi.nlm.nih.gov/Blast.cgi?RESULTS_FILE=on&RID="+tblastnRID+"&FORMAT_TYPE=Text&FORMAT_OBJECT=Alignment&ALIGNMENT_VIEW=Tabular&CMD=Get");
                sc6 = new Scanner(url6.openConnection().getInputStream());

                while(sc6.hasNextLine()) {
                    String line = sc6.nextLine().trim();
                    if(line.startsWith("#")) continue;
                    if(line.equals("")) continue;
                    boolean isRightSpecies = true;
                    //NEW: # Fields: query acc.ver, subject acc.ver, % identity, alignment length, mismatches, gap opens, q. start, q. end, s. start, s. end, evalue, bit score, % positives, query/sbjct frames
                    String someKindaID = line.split("\t")[1];
                    String description = mapIDToDescription.get(someKindaID);
                    if(description == null) {
                        // Find description of organism, because not in text file
                        URL url7 = new URL("https://www.ncbi.nlm.nih.gov/nucleotide/" + someKindaID);
                        Scanner sc7 = new Scanner(url7.openConnection().getInputStream());
                        while(sc7.hasNextLine()) {
                            String line2 = sc7.nextLine().trim();
                            if(line2.contains("?ORGANISM=")) {
                                if(line2.contains("?ORGANISM="+txid+"&")) {
                                    isRightSpecies = true;
                                } else {
                                    isRightSpecies = false;
                                    mapIDToDescription.put(someKindaID,"");
                                    break;
                                }
                            }
                            if(line2.startsWith("<h1>") && line2.endsWith("</h1>")) {
                                description = line2.split("<h1>")[1].split("</h1>")[0];
                                mapIDToDescription.put(someKindaID,description);
                                mapLowestForEachID.put(someKindaID,Long.MAX_VALUE);
                                mapHighestForEachID.put(someKindaID,(long)-1);
                            }
                            if(line2.contains("SequenceSize=\"")) {
                                long size = Long.parseLong(line2.split("SequenceSize=\"")[1].split("\"")[0]);
                                mapLengthForEachID.put(someKindaID,size);
                            }
                        }
                        o.print(".");
                        sc7.close();
                    }
                    if(description.equals("")) isRightSpecies = false;
                    if(isRightSpecies){
                        long subjectStart = Long.parseLong(line.split("\t")[8]);
                        long subjectEnd = Long.parseLong(line.split("\t")[9]);
                        double evalue = Double.parseDouble(line.split("\t")[10]);

                        if(evalue <= maxEvalue) {
                            if((mapEvalueForEachID.containsKey(someKindaID) && mapEvalueForEachID.get(someKindaID) < evalue) || (!mapEvalueForEachID.containsKey(someKindaID))) {
                                mapEvalueForEachID.put(someKindaID,evalue);
                            }
                            if(subjectStart < mapLowestForEachID.get(someKindaID)) mapLowestForEachID.put(someKindaID,subjectStart);
                            if(subjectEnd > mapHighestForEachID.get(someKindaID)) mapHighestForEachID.put(someKindaID,subjectEnd);
                        }
                    }
                }
                sc6.close();
                o.println(" Finished!");
            } catch (MalformedURLException e) {
                fail(geneQuery,"Gene search makes invalid URL!");
                continue;
            } catch (UnsupportedEncodingException e) {
                fail(geneQuery,"Failed to URL-encode gene name!");
                continue;
            } catch (IOException e) {
                fail(geneQuery,"Failed to load gene info!");
                continue;
            } catch (ArrayIndexOutOfBoundsException e) {
                fail(geneQuery,"Gene description not found in NCBI database! (or they changed their HTML)");
                continue;
            } catch (NullPointerException e) {
                fail(geneQuery,"NPE (you shouldn't see this)");
                continue;
            } finally {
                if(sc6 != null) sc6.close();
            }

            printlnFlush("");
            mapEvalueForEachID = ValueComparator.sortByValue(mapEvalueForEachID);
            int doneGenes = 1;
            for (String currID : mapEvalueForEachID.keySet()) {
                if(doneGenes > 5) break;
                doneGenes++;
                String currDesc = mapIDToDescription.get(currID);
                double evalue = mapEvalueForEachID.get(currID);
                long currStart = mapLowestForEachID.get(currID);
                long currEnd = mapHighestForEachID.get(currID);
                long currLen = mapLengthForEachID.get(currID);
                printlnFlush("TBLASTN Match Name: " + currDesc);
                printlnFlush("NCBI ID: " + currID);
                printlnFlush("True Match Range (unpadded): " + currStart + "-" + currEnd);
                // Add buffer on either side, if possible
                if((currStart - bufferLeft) < 1) {
                    currStart = 1;
                } else {
                    currStart -= bufferLeft;
                }
                if((currEnd + bufferRight) > currLen) {
                    currEnd = currLen;
                } else {
                    currEnd += bufferRight;
                }
                o.println("Downloading FASTA for ID " + currID + ": " + currDesc +" {" + currStart + "-" + currEnd+"}, evalue " + evalue);
                printlnFlush("FASTA File Range (padded by "+buffStr+" bases): " + currStart + "-" + currEnd);
                printlnFlush("Match evalue: " + evalue);
                String strFASTA = null;
                try {
                    URL urlFASTA = new URL("https://www.ncbi.nlm.nih.gov/projects/sviewer/sequence.cgi?id="+currID+"&format=fasta&ranges=" + (currStart - 1) + "-" + (currEnd - 1));
                    Scanner sc8 = new Scanner(urlFASTA.openConnection().getInputStream());
                    sc8.useDelimiter("\0");
                    strFASTA = sc8.next().trim();
                    sc8.close();
                } catch (MalformedURLException e) {
                    fail(geneQuery,"FASTA download makes invalid URL!");
                    continue;
                }  catch (NullPointerException e) {
                    fail(geneQuery,"NPE (you shouldn't see this)");
                    continue;
                }
                catch (IOException e) {
                    fail(geneQuery,"Failed to download FASTA!");
                    continue;
                }
                if(strFASTA == null || strFASTA.equals("")) {
                    fail(geneQuery,"Failed to download FASTA!");
                    continue;
                }
                try {
                    Utils.saveTextFile(strFASTA,"Results/"+species+"/"+geneQuery+"/FASTA_ID" + currID + "_" + currStart + "-" + currEnd + ".fa");
                } catch (IOException e) {
                    fail(geneQuery,"Failed to save FASTA file!");
                    continue;
                }
                o.print("Starting BLASTX to verify results... ");
                String blastxRID = "";
                try {
                    String blastxQuery = "CMD=Put&QUERY="+URLEncoder.encode(strFASTA, "UTF-8")+"&PROGRAM=blastx&FORMAT_TYPE=text&DATABASE=nr";
                    blastxRID = doBlastAndGetRID(blastxQuery,wInfo);
                    printlnFlush("BLASTX Results Link: https://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Get&RID="+blastxRID);
                } catch (UnsupportedEncodingException e) {
                    fail(geneQuery,"Failed to URL-encode BLASTX query!");
                    continue;
                } catch (ArrayIndexOutOfBoundsException e) {
                    fail(geneQuery,"Invalid BLASTX query (or they changed their HTML)");
                    continue;
                }
                catch(IOException e) {
                    fail(geneQuery,"Failed to upload BLASTX query!");
                    continue;
                }
                catch (NullPointerException e) {
                    fail(geneQuery,"NPE (you shouldn't see this)");
                    continue;
                }
                if(blastxRID.equals("")) {
                    fail(geneQuery,"Invalid BLASTX result (or they changed their HTML)");
                    continue;
                }
                printlnFlush("");
            }
            printlnFlush("");
            printlnFlush("[QUERY COMPLETE AT DATE: "+new Date().toString()+"]");
            wInfo.close();
            o.println("");
        }
        if(batch) {
            try {
                String l = System.lineSeparator();
                String dt = new Date().toString().replace(":","-");
                String joinedStr = "";
                for(String s : errorList) {
                    joinedStr+=s+l;
                }
                Utils.saveTextFile("Total Number of Errors: " + errorList.size() + l + "Date: " + dt+l+l+joinedStr, "Results/"+species+"/ErrorList_"+dt+".txt");
            } catch (IOException e) {
                o.println("ERROR: Failed to save error list file!");

            }
        }
        if(errorList.size() > 0) {
            o.println("*** Total Number of Errors: " + errorList.size());
            if(batch)
                o.print(" *** Errors in Genes: ");
            for(String s : errorList) {
                o.print(s+" ");
            }
            o.println();
            Audio.playWav("resources/error.wav");
        } else {
            Audio.playWav("resources/tada.wav");
        }
        o.println("All done!");
        int option2 = JOptionPane.showConfirmDialog(null, "All done! Do you want to open the results folder?", "CSGF Finished", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (option2 == JOptionPane.YES_OPTION) {
            try {
                Desktop.getDesktop().open(new File("Results/"));
            } catch(Exception e) {
                JOptionPane.showMessageDialog(null, "Error opening results folder!\n\n"+e.getMessage(), "CSGF Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        System.exit(0);
    }

    private static String doBlastAndGetRID(String query,PrintWriter wInfo) throws IOException, MalformedURLException, ArrayIndexOutOfBoundsException, NullPointerException {
        boolean blastx = query.contains("&PROGRAM=blastx&");
        String blastRID = null;
        int i = 0;
        do { // no retry if blastx
            InputStream postStream = Utils.doPost("https://blast.ncbi.nlm.nih.gov/blast/Blast.cgi",query);
            Scanner sc4 = new Scanner(postStream);
            blastRID = null;
            while(sc4.hasNextLine()) {
                String line = sc4.nextLine().trim();
                if(line.startsWith("RID = ")) {
                    blastRID = line.split("=")[1].trim();
                    break;
                }
            }
            sc4.close();
            if(blastRID == null) {
                throw new IOException(); // hacky
            }
            o.println("Query RID is " + blastRID);

            String blastStatus = "WAITING";
            URL url5 = new URL("https://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Get&NOHEADER=true&RID="+blastRID);
            o.print("Waiting for results");
            int totalSlept = 0;
            while(blastStatus.equals("WAITING")) {
                Scanner sc5 = new Scanner(url5.openConnection().getInputStream());
                while(sc5.hasNextLine()) {
                    String line = sc5.nextLine().trim();
                    if(line.startsWith("Status=")) {
                        blastStatus = line.split("=")[1].trim();
                        if(blastStatus.equals("READY")) {
                            o.println(" Finished!");
                            sc5.close();
                            return blastRID;
                        }
                        if(blastStatus.equals("FAILED")) {
                            o.println(" BLAST Server Said Failed!");
                            printlnFlush("*** BLAST SERVER SAID FAILED! ***");

                            sc5.close();
                            o.println("Waiting 30 seconds to be nice to NCBI's server...");
                            MyTime.sleep(30000);
                            return "";
                        }
                    }
                    else if(line.startsWith("var tm = \"") && !line.contains("\"\"")) {
                        int time = Integer.parseInt(line.split("\"")[1].trim());
                        o.print(".");
                        MyTime.sleep(time); // in ms
                        totalSlept += time;
                        if((!blastx && totalSlept > 300000) || (blastx && totalSlept > 900000)) { // 5 min or 15 if blastx
                            o.println(" ERROR!\nBLAST server seems to have hung. Canceling query, waiting 30 sec and retrying. (Try "+(i+1)+"/5)");
                            URL urlCancel = new URL("https://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Cancel&RID="+blastRID);
                            Scanner sc5a = new Scanner(urlCancel.openConnection().getInputStream());
                            sc5a.nextLine(); // just read something
                            sc5a.close();
                            MyTime.sleep(30000);
                            blastStatus = ""; // cheat
                            totalSlept = 0;
                            break;
                        }
                    }
                }
                sc5.close();
                MyTime.sleepAlways(2000);
            }
            i++;
        } while(i < 5 && !blastx);
        return "";
    }
}
