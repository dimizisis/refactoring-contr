/**
 * 
 */
package com.digkas.refactoringminer;

import java.io.*;
import java.util.*;

import com.digkas.refactoringminer.api.interest.InterestIndicatorsResponseEntity;
import com.digkas.refactoringminer.api.principal.PrincipalResponseEntity;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

/**
 * @author George Digkas <digasgeo@gmail.com>
 *
 */
public class Main {

	private static final String GIT_SERVICE_URL = "https://github.com/";
	private static final String OWNER = "apache";
	private static final String REPOSITORY = "commons-io";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		Repository repo = cloneRepository();
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
//		PrincipalResponseEntity[] principalResponse = Objects.requireNonNull(getPrincipalApiResponse());
//		InterestIndicatorsResponseEntity interestResponse = getInterestApiResponse();
		InterestIndicatorsResponseEntity interestResponse = null; // tmp
		PrincipalResponseEntity[] principalResponse = null; // tmp

		List<String> commits = Objects.requireNonNull(getCommitIds());

//		commits.forEach(commit -> {
//			miner.detectAtCommit(repo, commit, new RefactoringHandler() {
//				@Override
//				public void handle(String commitId, List<Refactoring> refactorings) {
////					if (!refactorings.isEmpty())
////						for (Refactoring r : refactorings){
////							r.getInvolvedClassesAfterRefactoring().forEach(c -> {
////								System.out.println(c.getLeft());
////							});
////						}
//					if (!refactorings.isEmpty())
//						refactorings.forEach(r -> r.leftSide().forEach(co -> System.out.println("File: " + co.getFilePath() + " | Start Line: " + co.getStartLine() + " | End Line: " + co.getEndLine())));
////					if (refactorings.isEmpty())
////						hasRefactorings.add("\tNo");
////					else
////						hasRefactorings.add("\tYes, Refactoring Type: "
////								+ refactorings.stream().map(Refactoring::getRefactoringType).collect(Collectors.toList())
////								+ ", Involved Classes After: " + refactorings.stream().map(Refactoring::getInvolvedClassesAfterRefactoring).collect(Collectors.toList()));
//				}
//			});
//		} );

		commits
				.forEach(commit -> miner.detectAtCommit(repo, commit, new CustomRefactoringHandler(principalResponse, interestResponse)));

		writeCSV();
		System.exit(0);
	}

	private static Repository cloneRepository() throws Exception {
		return new GitServiceImpl().cloneIfNotExists("C:/Users/Dimitris/Desktop/" + REPOSITORY, GIT_SERVICE_URL + OWNER + "/" + REPOSITORY);
	}

	private static List<String> readCSV() throws IOException {
		List<String> revisions = new ArrayList<>();
		Reader in = new FileReader("Zeppelin2.csv");
		Iterable<CSVRecord> records = CSVFormat.TDF
				.withFirstRecordAsHeader()
				.parse(in);
		records.forEach(record -> revisions.add(record.get("Revision")));
		return revisions;
	}

	private static void writeCSV() throws IOException {

		FileWriter csvWriter = new FileWriter("C:/Users/Dimitris/Desktop/neww.csv");
		csvWriter.append("CommitId\t");
		csvWriter.append("InvolvedFile\t");
		csvWriter.append("TypeOfChange\t");
		csvWriter.append("Granularity\t");
		csvWriter.append("TDContributionPrincipal\t");
		csvWriter.append("TDContributionInterest\t");
		csvWriter.append("Comment\n");

		csvWriter.append(Globals.output);

		csvWriter.flush();
		csvWriter.close();
	}

	private static List<String> getCommitIds() {
		HttpResponse<JsonNode> httpResponse = null;
		Unirest.setTimeouts(0, 0);
		try {
			httpResponse = Unirest.get("http://195.251.210.147:8989/api/dzisis/commits?url=https://github.com/apache/commons-io").asJson();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return Objects.nonNull(httpResponse) ? Arrays.asList(new Gson().fromJson(httpResponse.getBody().toString(), String[].class)) : null;
	}

	private static InterestIndicatorsResponseEntity getInterestApiResponse() {
		HttpResponse<JsonNode> httpResponse = null;
		Unirest.setTimeouts(0, 0);
		try {
			httpResponse = Unirest.get("http://195.251.210.147:7070/interestIndicators/search?projectID=jcommander&language=java").asJson();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return Objects.nonNull(httpResponse) ? new Gson().fromJson(httpResponse.getBody().toString(), InterestIndicatorsResponseEntity.class) : null;
	}

	private static PrincipalResponseEntity[] getPrincipalApiResponse() {
		HttpResponse<JsonNode> httpResponse = null;
		Unirest.setTimeouts(0, 0);
		try {
			httpResponse = Unirest.get("http://195.251.210.147:8989/api/dzisis/study?url=https://github.com/apache/commons-io").asJson();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
		return Objects.nonNull(httpResponse) ? new Gson().fromJson(httpResponse.getBody().toString(), PrincipalResponseEntity[].class) : null;
	}

}
