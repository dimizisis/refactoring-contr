
package com.digkas.refactoringminer;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

/**
 * @author George Digkas <digasgeo@gmail.com>
 *
 */
public class Main {
	/**
	 * @param args program arguments
	 * @throws Exception possible exception thrown by repo cloning
	 */
	public static void main(String[] args) throws Exception {

		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.OFF);

		Unirest.config()
				.socketTimeout(Integer.MAX_VALUE-1)
				.connectTimeout(Integer.MAX_VALUE-1);

		if (args.length < 3) {
			System.out.println("Usage: java -jar RefactoringContribution.jar <Repo URL> <Clone Path> <Output File Path>");
			System.exit(1);
		}
		String url = Objects.requireNonNull(args[0]);
		String clonePath = Objects.requireNonNull(args[1]);
		String outFilePath = Objects.requireNonNull(args[2]);
		if (!outputPathIsDir(outFilePath)) {
			System.out.println("Not a valid output file path provided");
			System.exit(-1);
		}
		Repository repo = cloneRepository(url, clonePath);
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		List<String> commits = Objects.requireNonNull(getCommitIds(url));
		Collections.reverse(commits);
//		 List<String> commits = new ArrayList<>();
//		 commits.add("e7e3907b1343f45b45a3742f0c7a5d103f383a28");

		Thread progressThread = new Thread(new ProgressReport(commits.size()));
		progressThread.start();

//		for (String commit : commits) {
//			miner.detectAtCommit(repo, commit, new CustomRefactoringHandler(url));
//			Globals.increaseProgress();
//		}

		 commits.parallelStream().forEach(commit -> {
		 			miner.detectAtCommit(repo, commit, new CustomRefactoringHandler(url));
		 			Globals.increaseProgress();
		 		});

		writeCSV(outFilePath);
		progressThread.join();
		System.exit(0);

	}

	private static boolean outputPathIsDir(String outFilePath) {
		return new File(outFilePath).isDirectory();
	}

	private static Repository cloneRepository(String url, String clonePath) throws Exception {
		return new GitServiceImpl().cloneIfNotExists(clonePath, url);
	}

	private static void writeCSV(String filePath) throws IOException {

		FileWriter csvWriter = new FileWriter(filePath.replace("\\", "/") + "/out.csv");

		for (String header : Globals.outputHeaders)
			csvWriter.append(header);
		csvWriter.append(Globals.output);

		csvWriter.flush();
		csvWriter.close();
	}

	private static List<String> getCommitIds(String url) {
		HttpResponse<JsonNode> httpResponse = null;
//		Unirest.setTimeouts(0, 0);
		try {
			httpResponse = Unirest.get("http://195.251.210.147:8989/api/dzisis/commits?url=" + url).asJson();
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
