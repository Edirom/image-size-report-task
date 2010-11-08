import org.apache.tools.ant.taskdefs.UpToDate;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.LogLevel;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

public class ImageSizeTask extends Task{
  
  static {
    System.setProperty("java.awt.headless", "true");
  }
  
  private FileSet imageFiles;
  private String reportFile;
  private boolean checkUpToDate = true;
  private boolean failOnUnreadable = false;
  
  public void addConfiguredFileSet(FileSet files) {
    if (imageFiles == null) {
      imageFiles = files;
    }
    else {
      fail("You can only specify one fileset.");
    }
  }
  
  public void setOutFile(String name) {
    log("Warning: you should use 'reportFile' instead of 'outFile'", LogLevel.WARN.getLevel());
    checkAndSetReportFile(name);
  }
  
  public void setReportFile(String name) {
    checkAndSetReportFile(name);
  }
  
  private void checkAndSetReportFile(String name) {
    if (reportFile == null) {
      reportFile = name;
    }
    else {
      fail("Only set one of 'reportFile' or 'outFile' (use 'reportFile')");
    }
  }
  
  public void setCheckUpToDate(boolean check) {
    checkUpToDate = check;
  }
  
  public void setFailOnUnreadable(boolean fail) {
    failOnUnreadable = fail;
  }
  
  private boolean upToDate() {
    UpToDate utd = new UpToDate();
    utd.setProject(getProject());
    utd.addSrcfiles(imageFiles);
    utd.setTargetFile(new File(reportFile));
    
    return utd.eval();
  }
  
  public void execute() throws BuildException {
    
    validate();
    
    if (checkUpToDate && upToDate()) {
      log("Report file " + reportFile + " is up to date. Skipping report generation.");
      return;
    }
    
    PrintWriter out = null;
    
    try {
      out = new PrintWriter(new BufferedWriter(new FileWriter(reportFile)));
      
      DirectoryScanner ds = imageFiles.getDirectoryScanner();
      String[] fileNames = ds.getIncludedFiles();
      
      for (String fileName : fileNames) {
        log("Processing " + fileName, LogLevel.VERBOSE.getLevel());
        
        File imageFile = new File(ds.getBasedir(), fileName);
        ImageInputStream iis = new FileImageInputStream(imageFile);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        
        ImageReader reader = readers.hasNext() ? readers.next() : null;
        
        if (reader == null) {
          if (failOnUnreadable) {
            fail("Image file is not readable: " + imageFile.getPath());
          }
          else {
            log("Image file is not readable: " + imageFile.getPath() + ", skipping.", LogLevel.WARN.getLevel());
            continue;
          }
        }
        
        reader.setInput(iis);
        
        int height = reader.getHeight(reader.getMinIndex());
        int width = reader.getWidth(reader.getMinIndex());
        
        out.printf("%s,%d,%d%n", fileName, height, width);
      }
      
      log("Image size report written to " + reportFile);
      
    }
    catch (IOException e) {
      throw new BuildException(e);
    }
    finally {
      if (out != null) {
        out.close();
      }
    }
  }
  
  private void validate() {
    if (reportFile == null) {
      fail("You must specify a report file.");
    }
    
    if (imageFiles == null) {
      fail("You must specify a fileset to generate the report against.");
    }
  }
  
  private void fail(String message){
    throw new BuildException(getTaskName() + ": " + message);
  }
  
}