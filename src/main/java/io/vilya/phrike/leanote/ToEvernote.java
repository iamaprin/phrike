package io.vilya.phrike.leanote;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 用于合并evernote备份文件<br>
 * 工作以来一直有记笔记的习惯，长时间以来，一直使用的leanote，感觉还是不错的。
 * 但是公司里面用有道云笔记的比较多，所以就有笔记迁移的打算，但是leanote没有
 * 批量导出的功能，只能自己将其导出的笔记合并导入到有道云笔记中。使用了evernote
 * 作为中间格式。
 * @author iamaprin
 * @time 2017年9月28日 上午12:23:04
 */
public class ToEvernote {

	private static final Logger LOGGER = Logger.getLogger("ToYoudao");
	
	private DocumentBuilder builder;
	
	private Transformer transformer;
	
	public static void main(String[] args) {
		ToEvernote instance = new ToEvernote();
		
		File sourceDir = new File("leanote.bak.d");
		File targetFile = new File("all.enex");
		try {
			instance.combineNotes(sourceDir, targetFile);
		} catch (TransformerFactoryConfigurationError | ParserConfigurationException | TransformerException e) {
			LOGGER.log(Level.ALL, "fail to combine notes!", e);
		}
	}
	
	/**
	 * combine notes
	 * @param sourceDir
	 * @param targetFile
	 * @throws TransformerFactoryConfigurationError
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	private void combineNotes(File sourceDir, File targetFile) throws TransformerFactoryConfigurationError, ParserConfigurationException, TransformerException {
		checkNotNull(sourceDir);
		checkNotNull(targetFile);
		
		if (!sourceDir.exists() || !sourceDir.isDirectory()) {
			LOGGER.warning("source dir don't exist, or is not a directory!");
			return;
		}
		
		Document targetDoc = createDocument();
		Node enExportNode = createEnExportElement(targetDoc);
		targetDoc.appendChild(enExportNode);
		
		// do combine
		File[] notes = sourceDir.listFiles(new EvernoteFilenameFilter());
		for (File note: notes) {
			Document document;
			try {
				document = getBuilder().parse(note);				
			} catch (Exception e) {
				LOGGER.warning(note.getName());
				continue;
			}
			NodeList nodeList = document.getElementsByTagName("note");
			
			Node noteNode = nodeList.item(0);
			enExportNode.appendChild(targetDoc.importNode(noteNode, true));
		}
		
		Source source = new DOMSource(targetDoc);
		Result result = new StreamResult(targetFile);
		getTransformer().transform(source, result);		// document -> file
		
		LOGGER.info("over!");
	}
	
	/**
	 * create <code>en-export</code> element
	 * @param document
	 * @return
	 */
	private Element createEnExportElement(Document document) {
		return document.createElement("en-export");
	}
	
	/**
	 * create document
	 * @return
	 * @throws ParserConfigurationException
	 */
	private Document createDocument() throws ParserConfigurationException {
		return getBuilder().newDocument();
	}
	
	/**
	 * get document builder
	 * @return
	 * @throws ParserConfigurationException
	 */
	private DocumentBuilder getBuilder() throws ParserConfigurationException {
		if (builder == null) {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		return builder;
	}
	
	/**
	 * get transformer
	 * @return
	 * @throws TransformerConfigurationException
	 * @throws TransformerFactoryConfigurationError
	 */
	private Transformer getTransformer() throws TransformerConfigurationException, TransformerFactoryConfigurationError {
		if (transformer == null) {
			transformer = TransformerFactory.newInstance().newTransformer();
		}
		return transformer;
	}
	
	
	/**
	 * evernote filename filter
	 * @author iamaprin
	 * @time 2017年10月2日 下午8:24:52
	 */
	private static class EvernoteFilenameFilter implements FilenameFilter {
		
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith("enex");
		}
		
	}
	
}
