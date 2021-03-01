/**
 * 
 */
package com.digkas.refactoringminer;

import java.io.*;
import java.util.*;

import com.digkas.refactoringminer.api.interest.InterestIndicatorsResponseEntity;
import com.google.gson.Gson;
import com.mashape.unirest.http.Unirest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

/**
 * @author George Digkas <digasgeo@gmail.com>
 *
 */
public class Main {

	private static final String GIT_SERVICE_URL = "https://github.com/";
	private static final String OWNER = "cbeust";
	private static final String REPOSITORY = "jcommander";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		Gson gson = new Gson();
		Unirest.setTimeouts(0, 0);
		InterestIndicatorsResponseEntity response = gson.fromJson(Unirest.get("http://195.251.210.147:7070/interestIndicators/search?projectID=jcommander&language=java")
				.asJson().getBody().toString(), InterestIndicatorsResponseEntity.class);

		GitService gitService = new GitServiceImpl();

		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

		Repository repo = gitService.cloneIfNotExists("C:/Users/Dimitris/Desktop/" + REPOSITORY, GIT_SERVICE_URL + OWNER + "/" + REPOSITORY);

		List<String> commits = readCSV();

		StringBuilder output = new StringBuilder();

		response.getInterestIndicators().getRows().forEach(row -> System.out.println(row.getName() + "\t" + row.getInterest()));

//		List<String> hasRefactorings = new ArrayList<>();
//		hasRefactorings.add("\tHas Refactorings");

		miner.detectAll(repo, "master", new CustomRefactoringHandler(response));
//		commits.forEach(commit -> {
//			miner.detectAtCommit(repo, commit, new CustomRefactoringHandler() {
//				@Override
//				public void handle(String commitId, List<Refactoring> refactorings) {
//					if (!refactorings.isEmpty())
//						for (Refactoring r : refactorings){
//							r.getInvolvedClassesAfterRefactoring().forEach(c -> System.out.println(c.getLeft()));
//						}
//					if (refactorings.isEmpty())
//						hasRefactorings.add("\tNo");
//					else
//						hasRefactorings.add("\tYes, Refactoring Type: "
//								+ refactorings.stream().map(Refactoring::getRefactoringType).collect(Collectors.toList())
//								+ ", Involved Classes After: " + refactorings.stream().map(Refactoring::getInvolvedClassesAfterRefactoring).collect(Collectors.toList()));
//				}
//			});
//		} );

//		addColumn("C:/Users/Dimitris/Desktop/", "Zeppelin2.csv", hasRefactorings);

		writeCSV(output);

		System.exit(0);
	}

	public static List<String> readCSV() throws IOException {
		List<String> revisions = new ArrayList<>();
		Reader in = new FileReader("Zeppelin2.csv");
		Iterable<CSVRecord> records = CSVFormat.TDF
				.withFirstRecordAsHeader()
				.parse(in);
		records.forEach(record -> revisions.add(record.get("Revision")));
		return revisions;
	}

	public static void writeCSV(StringBuilder out) throws IOException {

		FileWriter csvWriter = new FileWriter("C:/Users/Dimitris/Desktop/new.csv");
		csvWriter.append("CommitId\t");
		csvWriter.append("InvolvedFile\t");
		csvWriter.append("TypeOfChange\t");
		csvWriter.append("Granularity\t");
		csvWriter.append("TDContributionInterest\t");
		csvWriter.append("Comment\n");

		csvWriter.append(out);

		csvWriter.flush();
		csvWriter.close();
	}

	public static void addColumn(String path, String fileName, List<String> lst) throws IOException{
		BufferedReader br=null;
		BufferedWriter bw=null;
		final String lineSep=System.lineSeparator();

		try {
			File file = new File(path, fileName);
			File file2 = new File(path, fileName+".1");//so the
			//names don't conflict or just use different folders

			br = new BufferedReader(new InputStreamReader(new FileInputStream(file))) ;
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file2)));
			String line = null;
			int i=0;
			for ( line = br.readLine(); line != null; line = br.readLine(),i++)
			{

				String addedColumn = lst.get(i);
				bw.write(line+addedColumn+lineSep);
			}

		}catch(Exception e){
			System.out.println(e);
		}finally  {
			if(br!=null)
				br.close();
			if(bw!=null)
				bw.close();
		}

	}
}
