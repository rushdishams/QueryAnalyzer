package com.sustainalytics.ict;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

/**
 * Class to create index of the files
 * 
 * @author Sustainalytics, Rushdi Shams
 * @version 1.0 31/03/2015
 *
 */
public class Indexing {
	// -----------------------------------------------------------------
	// instance variables
	// -----------------------------------------------------------------
	private final String FILES_TO_INDEX_DIRECTORY = "filesToIndex";
	private final String INDEX_DIRECTORY = "indexDirectory";

	private final String FIELD_PATH = "path";
	private final String FIELD_CONTENTS = "contents";

	private Hits hits = null;
	private int paragraphCount = 0;
	private int documentsHaveQueryTerms = 0;
	private StringBuilder outputParagraphs;
	private StringBuilder output;
	
	private final String OUTPUT_PARAGRAPHS = "out/allparagraphs.txt";
	private final String OUTPUT = "out/individualparagraphs.txt";

	// -----------------------------------------------------------------
	// Method section
	// -----------------------------------------------------------------
	/**
	 * Method to create index of documents in a directory
	 */
	public void createIndex() {
		Analyzer analyzer = new StandardAnalyzer();
		boolean recreateIndexIfExists = true;
		IndexWriter indexWriter = null;
		try {
			indexWriter = new IndexWriter(INDEX_DIRECTORY, analyzer,
					recreateIndexIfExists);
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		File dir = new File(FILES_TO_INDEX_DIRECTORY);
		File[] files = dir.listFiles();
		for (File file : files) {
			Document document = new Document();

			String path = null;
			try {
				path = file.getCanonicalPath();
			} catch (IOException e) {
				e.printStackTrace();
			}
			document.add(new Field(FIELD_PATH, path, Field.Store.YES,
					Field.Index.UN_TOKENIZED,   
				    Field.TermVector.WITH_POSITIONS_OFFSETS));

			Reader reader = null;
			try {
				reader = new FileReader(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			document.add(new Field(FIELD_CONTENTS, reader));

			try {
				indexWriter.addDocument(document);
			} catch (CorruptIndexException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			indexWriter.optimize();
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			indexWriter.close();
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}// end of method to create index

	/**
	 * Method to search the indexes
	 * 
	 * @param searchString
	 *            is the query term
	 */
	public void searchIndex(String searchString) {
		System.out.println("Searching for '" + searchString + "'");
		Directory directory = null;
		try {
			directory = FSDirectory.getDirectory(INDEX_DIRECTORY);
		} catch (IOException e) {
			e.printStackTrace();
		}
		IndexReader indexReader = null;
		try {
			indexReader = IndexReader.open(directory);
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		Analyzer analyzer = new StandardAnalyzer();
		QueryParser queryParser = new QueryParser(FIELD_CONTENTS, analyzer);
		Query query = null;
		try {
			query = queryParser.parse(searchString);
			Set<Term> map = new HashSet<Term>();
			query.extractTerms(map);
			System.out.println(map.size());
			for(int i = 0; i < map.size(); i++)
				System.out.println(map.toString());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		try {
			hits = indexSearcher.search(query);
			documentsHaveQueryTerms = hits.length();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}// end method to search the indexes for query term

	/**
	 * Method to read documents that contain the search term
	 * 
	 * @param searchItem
	 *            is the term that needs to be searched within the document
	 */
	public void readDocuments(String searchItem) {
		Iterator<Hit> it = hits.iterator();
		String[] documentPaths = new String[documentsHaveQueryTerms];
		int pathIndex = 0;
		while (it.hasNext()) {
			Hit hit = it.next();
			Document document = null;
			try {
				document = hit.getDocument();
			} catch (CorruptIndexException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			documentPaths[pathIndex] = document.get(FIELD_PATH);
			pathIndex++;
		}// end looping through all the hits
		outputParagraphs = new StringBuilder();
		output = new StringBuilder();
		for (pathIndex = 0; pathIndex < documentPaths.length; pathIndex++) {
			File dataFile = new File(documentPaths[pathIndex]);
			String dataContent = "";
			try {
				dataContent = FileUtils.readFileToString(dataFile);
			} catch (IOException e) {
				System.out
						.println("Error from readDocuments() method: Cannot read files");
				e.printStackTrace();
			}
			String[] paragraphs = dataContent.split("\r\n");
			boolean found;
			for (int paragraphIndex = 0; paragraphIndex < paragraphs.length; paragraphIndex++) {
				found = StringUtils.containsIgnoreCase(
						paragraphs[paragraphIndex].trim(), searchItem);
				if (found) {
					paragraphCount++;
					outputParagraphs.append(paragraphs[paragraphIndex].trim()
							+ "\n--------------------------------\n");
					//System.out.println("Found in " + documentPaths[pathIndex]);
					output.append("Found in " + documentPaths[pathIndex] + "\n");
					//System.out.println("Paragraph no: " + (paragraphIndex + 1));
					output.append("Paragraph no: " + (paragraphIndex + 1) + "\n");
					//System.out.println(paragraphs[paragraphIndex].trim());
					output.append(paragraphs[paragraphIndex].trim() + "\n\n");
				}// end if the query is found in a paragraph
			}// end looping through paragraphs
		}// end looping through all the hits
	}// end method to read documents
	
	/**
	 * Method to write all paragraphs and individual paragraphs that contain query term into file
	 */
	public void writeOutput(){
		/*
		 * Writing all paragraphs that contain the query term(s) into file
		 */
		File allParagraphs = new File(OUTPUT_PARAGRAPHS);
		if (!allParagraphs.exists()) {
			try {
				allParagraphs.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		FileWriter fw = null;
		try {
			fw = new FileWriter(allParagraphs);
		} catch (IOException e) {
			e.printStackTrace();
		}
		BufferedWriter bw = new BufferedWriter(fw);
		try {
			bw.write(outputParagraphs.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		 * Writing individual paragraphs that contain the query term(s) into file
		 */
		File individualParagraphs = new File(OUTPUT);
		if (!individualParagraphs.exists()) {
			try {
				individualParagraphs.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			fw = new FileWriter(individualParagraphs);
		} catch (IOException e) {
			e.printStackTrace();
		}
		bw = new BufferedWriter(fw);
		try {
			bw.write(output.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}//end method to write information to output files
	/**
	 * Method to get all paragraphs that contain the query term
	 * @return all paragraphs that contain the query term as string
	 */
	public String getOutputParagraphs() {
		return outputParagraphs.toString();
	}
	/**
	 * Method to get individual paragraphs that contain the query term
	 * @return the individual paragraphs as string
	 */
	public String getOutput(){
		return output.toString();
	}
	/**
	 * Method to get the document path and name that contains the query term
	 * @return document name and path that contain the query term as string
	 */
	public int getDocumentsHaveQueryTerms() {
		return documentsHaveQueryTerms;
	}
	/**
	 * Method to get the # of paragraphs that contain the query term
	 * @return # of paragraphs containing the query term as integer
	 */
	public int getParagraphCount() {
		return paragraphCount;
	}
	/**
	 * Method to get the # of documents that contain the query term
	 * @return # of documents that contain the query term as integer
	 */
	public int getHits(){
		return hits.length();
	}
	/**
	 * Driver class
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Indexing index = new Indexing();
		index.createIndex();
		String searchItem = "Management Actions";
		index.searchIndex(searchItem);
		index.readDocuments(searchItem);
		
		System.out.println("Total documents containing " + searchItem + ": "
				+ index.getHits());
		System.out.println("Total Paragraphs containing " + searchItem + ": "
				+ index.getParagraphCount());
		System.out.println("Paragraphs that contain " + searchItem + ":\n"
				+ index.getOutputParagraphs());
		System.out.println("Document-wise paragraphs that contain " + searchItem + ":\n"
				+ index.getOutput());
		
		index.writeOutput();
	}// End Driver method
}//end class