package com.teslamotors.artifacts.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.catalina.util.RequestUtil;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.teslamotors.artifacts.files.FileNavigableObject;
import com.teslamotors.artifacts.files.NavigableObject;
import com.teslamotors.artifacts.files.SortUtils;
import com.teslamotors.artifacts.files.SortUtils.SortCriteria;
import com.teslamotors.artifacts.files.ZipMemberNavigableObject;
import com.teslamotors.artifacts.files.ZipParentNavigableObject;

/**
 * Based on DefaultServlet:
 * http://svn.apache.org/viewvc/tomcat/trunk/java/org/apache/catalina/servlets/DefaultServlet.java?view=markup
 * 
 * and on FileServlet by BalusC:
 * http://balusc.blogspot.com/2007/07/fileservlet.html
 */
@SuppressWarnings("serial")
public class FileServlet extends HttpServlet {

	/**
	 * The debugging detail level for this servlet.
	 */
	protected int debug = 0;

	/**
	 * Allow customized directory listing per directory.
	 */
	protected String  localXsltFile = null;

	/** 
	 * Allow customized directory listing per context. 
	 */ 
	protected String contextXsltFile = null; 

	/**
	 * Allow customized directory listing per instance.
	 */
	protected String  globalXsltFile = null;

	private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.

	private String file_basepath;

	public void init() throws ServletException {

		// Set our properties from the initialization parameters
		String value = null;
		try {
			value = getServletConfig().getInitParameter("debug");
			if (value != null) {
				debug = Integer.parseInt(value);
			}
		} catch (Exception e) {
			log("DefaultServlet.init: couldn't read debug from " + value);
		}

		this.file_basepath = getServletConfig().getInitParameter("base-path");
		this.globalXsltFile = getServletConfig().getInitParameter("globalXsltFile");
		this.contextXsltFile = getServletConfig().getInitParameter("contextXsltFile"); 
		this.localXsltFile = getServletConfig().getInitParameter("localXsltFile");
	}

	String getBaseRelativePath(File parent_path, File child_path) {
		return parent_path.toURI().relativize(child_path.toURI()).getPath();
	}
	
	String getBaseRelativePath(File path) {
		return getBaseRelativePath(new File(this.file_basepath), path);
	}

	public boolean shouldHide(NavigableObject nav) {
		
		String resourceName = nav.getName();
		return resourceName.equalsIgnoreCase("WEB-INF") ||
				resourceName.equalsIgnoreCase("META-INF") ||
				resourceName.equalsIgnoreCase(localXsltFile) ||
				resourceName.equalsIgnoreCase(contextXsltFile);
	}


	NavigableObject getTargetNavigableFromPath(File filesystem_file) throws IOException {
		File zip_file_prefix = getZipPathPrefix(filesystem_file);
		if (zip_file_prefix != null) {
			ZipParentNavigableObject zip_parent = ZipParentNavigableObject.createNew(zip_file_prefix);
			if (zip_file_prefix.equals(filesystem_file))
				return zip_parent;
			else
				return new ZipMemberNavigableObject(zip_parent, getBaseRelativePath(zip_file_prefix, filesystem_file));
		} else {
			return new FileNavigableObject(filesystem_file);
		}
	}
	
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
			{
		// Get requested file by path info.
		// XXX Note that this automatically adjusts for any servlet-mapping
		// prefix matching rule specified in web.xml
		String requestedFile = request.getPathInfo();

		// Check if file is actually supplied to the request URI.
		if (requestedFile == null) {
			// Do your thing if the file is not supplied to the request URI.
			// Throw an exception, or send 404, or show default/warning page, or just ignore it.
			response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
			return;
		}

		// Decode the file name (might contain spaces and on) and prepare file object.
		File filesystem_file = new File(this.file_basepath, URLDecoder.decode(requestedFile, "UTF-8"));

		// Get content type by filename.
		String contentType = getServletContext().getMimeType(filesystem_file.getName());

		boolean is_downloading = request.getParameter("download") != null;

		// Can force downloading instead of displaying.
		if (contentType == null || is_downloading) {
			contentType = "application/octet-stream";
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filesystem_file.getName() + "\"");
		}

		final NavigableObject navigable_object = getTargetNavigableFromPath(filesystem_file);

		emitContent(
				request.getContextPath(),
				response,
				navigable_object,
				contentType,
				SortUtils.getSortersFromRequest(request),
				is_downloading);
	}


	File getZipPathPrefix(File file) {
		
		Collection<String> segments = Lists.newArrayList();
		for (String path_segment : file.getPath().split("/")) {
			segments.add(path_segment);
			if (path_segment.endsWith(ZipParentNavigableObject.ZIP_EXTENSION))
				return new File(Joiner.on("/").join(segments));
		}
		
		return null;
	}
	
	
	void emitContent(String context_path,
			HttpServletResponse response,
			NavigableObject nav, String content_type,
			Collection<SortCriteria> sorters,
			boolean is_downloading) throws IOException {
		
		// Check if file actually exists in filesystem.
		if (!nav.exists()) {
			// Do your thing if the file appears to be non-existing.
			// Throw an exception, or send 404, or show default/warning page, or just ignore it.
			response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
			return;
		}
		
		// Init servlet response.
		response.reset();
		
		if (nav.isDirectory() && !is_downloading) {
			
			respondWithDirectoryListing(nav, response, context_path, sorters);

		} else {
			
			response.setBufferSize(DEFAULT_BUFFER_SIZE);
			response.setContentType(content_type);
			long entrySize = nav.getSize();
			response.setHeader("Content-Length", String.valueOf(entrySize));
			// Prepare streams.
			BufferedInputStream input = null;
			BufferedOutputStream output = null;

			try {
				// Open streams.
				input = new BufferedInputStream(nav.getInputStream());
				output = new BufferedOutputStream(response.getOutputStream());
				long bytesCopied = ByteStreams.copy(input, output);
				output.flush();

			} finally {
				// Gently close streams.
				close(output);
				close(input);
			}
		}
	}
	
	
	
	String getUrlPath(String contextPath, NavigableObject nav) {

		String url_path = new File(contextPath, getBaseRelativePath(nav.asFile())).getPath();
		return url_path;
	}
	

	/**
	 * Return an InputStream to an HTML representation of the contents
	 * of this directory.
	 *
	 * @param contextPath Context path to which our internal paths are
	 *  relative
	 * @throws TransformerException 
	 * @throws IOException 
	 */
	protected byte[] listDirectoryContents(String contextPath, NavigableObject directory,
			InputStream xsltInputStream, Collection<SortCriteria> sorters) throws TransformerException, IOException {

		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\"?>");
		sb.append("<listing ");
		sb.append(" contextPath='");
		sb.append(contextPath);
		sb.append("'");
		sb.append(" directory='");
		sb.append( getBaseRelativePath(directory.asFile()) );
		sb.append("'>");

		sb.append("<entries>");

		Collection<NavigableObject> children_and_parent = directory.listChildren(sorters);
		
		for (NavigableObject nav : children_and_parent) {
			
			// Hidden files
			if (shouldHide(nav))
				continue;

			sb.append("<entry");
			sb.append(" type='")
			.append( nav.isDirectory() ? "dir" : "file" )
			.append("'");

			String url_path = getUrlPath(contextPath, nav);
			sb.append(" urlPath='")
			.append(url_path)
			.append("'");

			if (!nav.isDirectory()) {
				sb.append(" size='")
				.append(renderSize(nav.getSize()))
				.append("'");
			}

			sb.append(" date='")
			.append( nav.getIsParentLink() ? "" : nav.renderAge())
			.append("'");
			
			sb.append(" icon='")
			.append( new File(new File(contextPath), new File("static/images", nav.getIconFilename()).getPath()).getPath() )
			.append("'");

			sb.append(" dlicon='")
			.append( new File(new File(contextPath), "static/images/download.png").getPath() )
			.append("'");
			
			sb.append(" downloadLink='")
			.append(nav.isDownloadable() ? "true" : "")
			.append("'");
			
			sb.append(">");
			sb.append(RequestUtil.filter(nav.getDisplayName()));
			sb.append("</entry>");

		}

		sb.append("</entries>");
		sb.append("</listing>");

		TransformerFactory tFactory = TransformerFactory.newInstance();
		Source xmlSource = new StreamSource(new StringReader(sb.toString()));

		Source xslSource = new StreamSource(xsltInputStream);
		Transformer transformer = tFactory.newTransformer(xslSource);

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		OutputStreamWriter osWriter = new OutputStreamWriter(stream, "UTF8");
		StreamResult out = new StreamResult(osWriter);
		transformer.transform(xmlSource, out);
		osWriter.flush();
		return stream.toByteArray();
	}
	
	private void respondWithDirectoryListing(NavigableObject nav, HttpServletResponse response, String context_path, Collection<SortCriteria> sorters)
			throws IOException {

		InputStream xsltInputStream = findXsltInputStream(nav.asFile());
		response.setContentType("text/html");

		try {
			byte[] directory_listing_page_bytes = listDirectoryContents(
					context_path,
					nav,
					xsltInputStream,
					sorters);
			response.setHeader("Content-Length", String.valueOf(directory_listing_page_bytes.length));

			// Open streams.
			InputStream input = new BufferedInputStream(new ByteArrayInputStream(directory_listing_page_bytes), DEFAULT_BUFFER_SIZE);
			OutputStream output = new BufferedOutputStream(response.getOutputStream(), DEFAULT_BUFFER_SIZE);

			ByteStreams.copy(input, output);
			// Gently close streams.
			close(output);
			close(input);

		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}



	private static void close(Closeable resource) {
		if (resource != null) {
			try {
				resource.close();
			} catch (IOException e) {
				// Do your thing with the exception. Print it, log it or mail it.
				e.printStackTrace();
			}
		}
	}

	/**
	 * Render the specified file size (in bytes).
	 *
	 * @param size File size (in bytes)
	 */
	protected String renderSize(long size) {

		long leftSide = size / 1024;
		long rightSide = (size % 1024) / 103;   // Makes 1 digit
		if ((leftSide == 0) && (rightSide == 0) && (size > 0))
			rightSide = 1;

		return ("" + leftSide + "." + rightSide + " kb");

	}



	/**
	 * Return the xsl template inputstream (if possible)
	 */
	protected InputStream findXsltInputStream(File directory) {

		if (localXsltFile!=null) {
			try {
				File local_xlst_file = new File(directory, localXsltFile);
				if (local_xlst_file.isFile()) {
					InputStream is = new FileInputStream(local_xlst_file);
					if (is!=null)
						return is;
				}
			} catch(IOException ioe) {
				log("DefaultServlet.findXsltInputStream: IO exception: " + ioe.getMessage());
			}
		}

		if (contextXsltFile != null) { 
			InputStream is = 
					getServletContext().getResourceAsStream(contextXsltFile); 
			if (is != null) 
				return is; 

			if (debug > 10) 
				log("contextXsltFile '" + contextXsltFile + "' not found"); 
		} 		


		/*  Open and read in file in one fell swoop to reduce chance
		 *  chance of leaving handle open.
		 */
		if (globalXsltFile!=null) {
			FileInputStream fis = null;

			try {
				File f = new File(globalXsltFile);
				if (f.exists()){
					fis =new FileInputStream(f);
					byte b[] = new byte[(int)f.length()]; /* danger! */
					fis.read(b);
					return new ByteArrayInputStream(b);
				}
			} catch(Exception e) {
				log("DefaultServlet.findXsltInputStream: can't read "
						+ globalXsltFile);
				return null;
			} finally {
				try {
					if (fis!=null)
						fis.close();
				} catch(Exception e) {
					log("DefaultServlet.findXsltInputStream: "
							+ " exception closing input stream: " + e.getMessage());
				}
			}
		}

		return null;

	}
}