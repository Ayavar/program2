/**
 * Web worker: an object of this class executes in its own new thread
 * to receive and respond to a single HTTP request. After the constructor
 * the object executes on its "run" method, and leaves when it is done.
 *
 * One WebWorker object is only responsible for one client connection. 
 * This code uses Java threads to parallelize the handling of clients:
 * each WebWorker runs in its own thread. This means that you can essentially
 * just think about what is happening on one client at a time, ignoring 
 * the fact that the entirety of the webserver execution might be handling
 * other clients, too. 
 *
 * This WebWorker class (i.e., an object of this class) is where all the
 * client interaction is done. The "run()" method is the beginning -- think
 * of it as the "main()" for a client interaction. It does three things in
 * a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it
 * writes out some HTML content for the response content. HTTP requests and
 * responses are just lines of text (in a very particular format). 
 *
 **/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;
//import java.net.URL;
//import java.awt.image.BufferedImage;
//import javax.imageio.ImageIO;
//import java.io.IOException;
//import java.io.File;
//import javax.imageio.ImageIO;
//import java.util.Scanner;




public class WebWorker implements Runnable
{
  String location;
  String loc;
  //String path = "";
  String type= null;
  //BufferedImage image = null;
  //BufferedImage bImage = null;
  String dataContent = "";
  
  
  private Socket socket;
  
  /**
   * Constructor: must have a valid open socket
   **/
  public WebWorker(Socket s)
  {
    socket = s;
  }
  
  /**
   * Worker thread starting point. Each worker handles just one HTTP 
   * request and then returns, which destroys the thread. This method
   * assumes that whoever created the worker created it with a valid
   * open socket object.
   **/
  public void run()
  {
    System.err.println("Handling connection...");
    try
    {
      
      InputStream is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      
      readHTTPRequest(is);
      dataContent = getDataContent(location);
      
      if(location!=null)
      {
        if(location.contains("html"))
        {
          if(fileExists(location))
          {
            writeHTTPHeader(os, dataContent);
            writeFileContent(os, dataContent);
          }
          else
          {
            write404Header(os,"text/html");
          }
        }
        else if(location.equals("/"))
        {
          writeHTTPHeader(os,"text/html");
          writeContent(os);
        }
        else if(location.contains("<cs371server>")||location.contains("%3Ccs371server%3E"))
        {
          writeHTTPHeader(os,"text/html");
          writeCustomContent(os);
        }
        else if (location.contains("<cs371date>")||location.contains("%3Ccs371date%3E"))
        {
          writeHTTPHeader(os,"text/html");
          writeDateContent(os);
        }
        else if(location.contains("gif") || location.contains("png") || location.contains("jpg"))
        {
          if(fileExists(location))
          {
            writeHTTPHeader(os,dataContent);
            writeFileContent(os, dataContent);
          }
          else
          {
            write404Header(os,"text/html");
          }
        }
        else
        {
          write404Header(os,"text/html");
        }
      }//end of 1st if
      else
      {//Start of 1st else
        write404Header(os,"text/html");
      }//end of 1st else
      
      os.flush();
      socket.close();
    }
    catch (Exception e) 
    {
      System.err.println("Output error: "+e);
    }
    System.err.println("Done handling connection.");
    return;
  }
  
  private String getDataContent (String l)
  {
    if(l.contains("jpg") || l.contains("gif") || l.contains("png") || l.contains("jpeg"))
      return "image/html";
//    else if (l.contains("ico"))
//    {
//      return "image/x-icon";
//    }
    else
      return "text/html";
  }
//This method will return a true or false boolean, after looking for the file, depending on the file existing or not existing.
  private boolean fileExists (String location2)
  {
    location2 = (System.getProperty("user.dir")+(location2)).trim();
    loc=location2;
    File file = new File (location2);
    return file.exists() && !file.isDirectory() && file.isFile();
  }
  
  /**
   * Read the HTTP request header.
   **/
  private void readHTTPRequest(InputStream is)
  {
    String line;
    String lineCopy;
    BufferedReader r = new BufferedReader(new InputStreamReader(is));
    while (true) {
      try {
        while (!r.ready()) Thread.sleep(1);
        line = r.readLine();
        //line is copied to a new string "lineCopy" to prevent damageing the source "line."
        lineCopy=line;
        
        System.err.println("Request line: ("+lineCopy+")");
        
        //Looks for the line containing "GET", to see what the user is looking for.
        if (lineCopy.contains("GET ") && !lineCopy.contains(".ico")) 
        {
          lineCopy = lineCopy.substring(3);
          String[] lineCopyArray = lineCopy.split(" ");
          String lineCopy3 = lineCopyArray[1];
          location=lineCopy3;  
        }
        
        
        if (lineCopy.length()==0)
        {
          //System.err.println("Request line: ("+Error+404+")");
          break;
        }
        
        
      } catch (Exception e) {
        System.err.println("Request error: "+e);
        break;
      }
    }
    
    return;
  }
  
  /**
   * Write the HTTP header lines to the client network connection.
   * @param os is the OutputStream object to write to
   * @param contentType is the string MIME content type (e.g. "text/html")
   **/
  private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
  {
    
    Date d = new Date();
    DateFormat df = DateFormat.getDateTimeInstance();
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    os.write("HTTP/1.1 200 OK\n".getBytes());
    os.write("Date: ".getBytes());
    os.write((df.format(d)).getBytes());
    os.write("\n".getBytes());
    os.write("Server: Jon's very own server\n".getBytes());
    //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
    //os.write("Content-Length: 438\n".getBytes()); 
    os.write("Connection: close\n".getBytes());
    os.write("Content-Type: ".getBytes());
    os.write(contentType.getBytes());
    os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
    return;
  }
  
//This method will print "Not Found" if the file is not found.
  private void write404Header(OutputStream os, String contentType) throws Exception
  {
    Date d = new Date();
    DateFormat df = DateFormat.getDateTimeInstance();
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    os.write("HTTP/1.1 404 not found\n".getBytes());
    os.write("Date: ".getBytes());
    os.write((df.format(d)).getBytes());
    os.write("\n".getBytes());
    os.write("Server: Jon's very own server\n".getBytes());
    //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
    //os.write("Content-Length: 438\n".getBytes()); 
    os.write("Connection: close\n".getBytes());
    os.write("Content-Type: ".getBytes());
    os.write(contentType.getBytes());
    os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
    
    os.write("<html><head>\n".getBytes());
    os.write("<title>404 Not Found</title>\n".getBytes());
    os.write("</head><body>\n".getBytes());
    os.write("<h1>Not Found</h1>\n".getBytes());
    os.write("<p>The requested URL http://localhost:8080".getBytes());
    os.write(location.getBytes());
    os.write(" was not found on this server.".getBytes());
    os.write("</body></html>\n".getBytes());
    
    //os.write("<html><head></head><body>\n".getBytes());
    //os.write("<h3>Error 404</h3>\n".getBytes());
    //os.write("</body></html>\n".getBytes());
    return;
  }
  
  
  /**
   * Write the data content to the client network connection. This MUST
   * be done after the HTTP header has been written out.
   * @param os is the OutputStream object to write to
   **/
//This method prints "My web server works!" when the user is not looking for any files
//(Example: http://localhost:8080).
  private void writeContent(OutputStream os) throws Exception
  {
    os.write("<html><head></head><body>\n".getBytes());
    os.write("<h3>My web server works!</h3>\n".getBytes());
    os.write("</body></html>\n".getBytes());
  }
//This method prints custom message when the user types http://localhost:8080/<cs371server>.
  private void writeCustomContent(OutputStream os) throws Exception
  {
    os.write("<html><head></head><body>\n".getBytes());
    os.write("<h3>Welcome to Jonathan's server!</h3>\n".getBytes());
    os.write("</body></html>\n".getBytes());
  }
  
//This method prints the date and time when the user types http://localhost:8080/<cs371date>.
  private void writeDateContent(OutputStream os) throws Exception
  {
    Date d = new Date();
    DateFormat df = DateFormat.getDateTimeInstance();
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    os.write("Date: ".getBytes());
    os.write((df.format(d)).getBytes());
    os.write("\n".getBytes());
    os.write("Server: Jonathan's server\n".getBytes());
    //os.write("Connection: close\n".getBytes());
    //os.write("Content-Type: ".getBytes());
    //os.write(contentType.getBytes());
    os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
  }
  
//This method will print the contents of the desired file. 
  private void writeFileContent(OutputStream os, String contentType) throws Exception
  {
    
    Date d = new Date();
    DateFormat df = DateFormat.getDateTimeInstance();
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    
    
    File file = new File(loc);
    Scanner input = new Scanner (file);
    
    if(contentType.equals("text/html"))
    {
      while(input.hasNextLine())
      {
        os.write(input.nextLine().getBytes());
      }
      System.out.println("Locationn"+location);
    }
    else
    {
      
      FileInputStream imageReader = new FileInputStream( file );
      byte imageArray[] = new byte [ (int) file.length() ];
      imageReader.read( imageArray );
      DataOutputStream imageOut = new DataOutputStream( os );
      imageOut.write(imageArray);
      
      
    }
    System.out.println("Locationn"+location);
    System.out.println(loc);   
  }
  
  private void writeUnsupportedType(OutputStream os) throws Exception
  {
    os.write("<html><head></head><body>\n".getBytes());
    os.write("<h3>The format ''".getBytes());
    os.write(type.getBytes());
    os.write("'' is not yet supported.  Try a different format or check the spelling.</h3>\n".getBytes());
    os.write("</body></html>\n".getBytes());
    
  }
}// end class
