
package com.digkas.refactoringminer;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
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
	 * @param args program arguments
	 * @throws Exception possible exception thrown by repo cloning
	 */
	public static void main(String[] args) throws Exception {

		Repository repo = cloneRepository();
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		List<String> commits = Objects.requireNonNull(getCommitIds());

		Thread progressThread = new Thread(new ProgressReport(commits.size()));
		progressThread.start();

		commits
				.parallelStream()
				.forEach(commit -> {
					miner.detectAtCommit(repo, commit, new CustomRefactoringHandler());
					Globals.increaseProgress();
				});
		writeCSV();
		progressThread.join();
		System.exit(0);
	}

	private static Repository cloneRepository() throws Exception {
		return new GitServiceImpl().cloneIfNotExists("C:/Users/Dimitris/Desktop/" + REPOSITORY, GIT_SERVICE_URL + OWNER + "/" + REPOSITORY);
	}

	private static void writeCSV() throws IOException {

		FileWriter csvWriter = new FileWriter("C:/Users/Dimitris/Desktop/neww_parallel.csv");

		for (String header : Globals.outputHeaders)
			csvWriter.append(header);
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

	private static class ProgressReport implements Runnable {

		private final int size;
		private static final int SECONDS_DELAY = 1;

		public ProgressReport(int commitListSize) {
			this.size = commitListSize;
		}

		@Override
		public void run() {
			try { reportProgress(); } catch (InterruptedException ignored) {}
		}

		private void reportProgress() throws InterruptedException {
			int progress = 0;
			do {
				System.out.printf("Processing commits... %d%%\r", progress);
				TimeUnit.SECONDS.sleep(SECONDS_DELAY);
			} while ((progress = (Globals.progress.get() * 100) / size) < 100);
			System.out.printf("Processing commits... %d%%\r", progress);
		}

	}
}
