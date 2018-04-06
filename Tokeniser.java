package preprocessor;

import lex_rank.CosineSimilarity;
import Sentence_Scoring.*;
import Sentence_Selection.*;
import lex_rank.Stop_word_elimination;
import lex_rank.Frequency;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;
import UserInterface.First;

public class Tokeniser extends Tika_Extraction {

	public Tokeniser() {
		// super(path);
	}

	// public class try_tokenise{

	public String tokenise(String SampleFileName, String title,int output_req) throws Exception {

		// extract(); //extract raw text apache tika
		Locale locale = Locale.US;
		BreakIterator wordIterator = BreakIterator.getWordInstance(locale); // it breaks the word first and last
																			// position of break(space)
		String temp = "";
		String Summary = "";
		int count = 0;
		Scanner sc = new Scanner(System.in);
		// System.out.println("Enter title of document");
		// String title= sc.nextLine();

		TitleResemblance tit = new TitleResemblance("D", title);
		tit.Title_creation();

		// RandomAccessFile file1 = new RandomAccessFile(new
		// File("F:\\Project(Summarization)\\Summarised.txt"), "rw");
		// file1.writeBytes(title+".");
		// file1.close();

		Scanner scan = new Scanner(new File(SampleFileName));

		String line;
		FileReader file = new FileReader(SampleFileName);
		BufferedReader br1 = new BufferedReader(file);
		while ((line = br1.readLine()) != null) {
			temp += line;

		}
		br1.close();
		file.close();

		// System.out.println("Sample file
		// -------------------------temp------------------");
		// System.out.println(temp);

		String text = "";
		wordIterator.setText(temp);
		int wordstart = wordIterator.first();
		for (int wordend = wordIterator
				.next(); wordend != BreakIterator.DONE; wordstart = wordend, wordend = wordIterator.next()) {

			String value = Normalizer.normalize(temp.substring(wordstart, wordend), Normalizer.Form.NFD); // value
																											// =single
																											// word ,
																											// normalizes
																											// unicode
																											// to
																											// suitable
																											// for
																											// sorting
																											// and
																											// searching
			value = value.replaceAll(" ", ""); // replaces all string not in ASCII format to null
			text += " " + value;
			text = text.replaceAll("  ", " ");
			count++;
		}
		
		// System.out.println("Sample file -------------------------text------------------");
		// System.out.println(text);

	//	temp = "" + text; // lines followed by words
		BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US); // breaking sentences using english
																				// grammer
		iterator.setText(temp); // pointing to temp for scanning

		int start = iterator.first();
		int index = 0;
		FileOutputStream out = new FileOutputStream(new File("F:\\Project(Summarization)\\Tokens.txt"));

		for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
			int spaces = 0;

			String finaltext = temp.substring(start, end).trim().replaceAll("		", " ");

			for (int i = 0; i < finaltext.length(); i++) {

				if (Character.isWhitespace(finaltext.charAt(i)))
					spaces++;
			}

			if ((spaces + 1) < 3)
				continue; // minimal 3 words in a line

			index++;
			finaltext+=" ";
			out.write(finaltext.getBytes());
			out.write("\n".getBytes());

		}

		out.flush();

		int noOfLines = index;
		double[][] sent_score_matrix = new double[noOfLines][noOfLines];
		double[][] sent_rank = new double[noOfLines][2];
		double noOfWords;
		double totalNoOfCuePhrases = new CuePhrase("F:\\Project(Summarization)\\Tokens.txt").totalCuePhrases();
		double totalNoOftitleResemblences = new TitleResemblance(("F:\\Project(Summarization)\\Tokens.txt"), title)
				.resemblence();
		scan = new Scanner(new File("F:\\Project(Summarization)\\Tokens.txt"));
		index = 0;
		int i = 0;
		
		if(output_req> noOfLines){
			return "null";
		}
		
		RandomAccessFile f = new RandomAccessFile("F:\\Project(Summarization)\\StopWordOutput.txt", "rw");
		String text1 = f.readLine();
		int tcount = 0;
		while (text1 != null) {
			// freqWords=getWordCounts(text1);
			text1 = f.readLine();
			tcount++;	//total no of words without stop-words
			// System.out.println("totalNoOfWords"+totalNoOfWords);
		}
		//System.out.println(tcount);

		while (scan.hasNext()) {

			// this loop

			temp = scan.nextLine();
		//	temp = temp.replaceAll("[^\\p{ASCII}]", "");
			Frequency f1 = new Frequency();
			double sentenceScore = f1.sentence_score(temp, tcount);
			sent_score_matrix[i][i] = sentenceScore;
			i++;

		}
		for (i = 0; i < noOfLines - 1; i++) {
			// System.out.println("\n");
			for (int j = i + 1; j < noOfLines; j++) {
				// System.out.print(" "+sent_score_matrix[i][j]);

				CosineSimilarity cs = new CosineSimilarity();
				sent_score_matrix[i][j] = sent_score_matrix[j][i] = cs.calc_cosineSimilarity(sent_score_matrix[i][i],
						sent_score_matrix[j][j]);

			}
		}

		System.out.println("------------------------MATRIX--------------------------");
		for (i = 0; i < noOfLines; i++) {
			System.out.println("\n");
			for (int j = 0; j < noOfLines; j++) {
				System.out.print("  " + sent_score_matrix[i][j]);
			}
		}
		
		//CALCULATING RANKS
		for (i = 0; i < noOfLines; i++) {
			//System.out.println("\n");
			sent_rank[i][0] = i+1;
			sent_rank[i][1] = 0;
			for (int j = 0; j < noOfLines; j++) {
			//	System.out.print("  " + sent_score_matrix[i][j]);
				sent_rank[i][1]+=sent_score_matrix[i][j];
			}
		}
		
		/*System.out.println("------------------------RANKS--------------------------");
		for (i = 0; i < noOfLines; i++) 
			System.out.println((i+1)+"-->"+sent_rank[i][1]+"\n");
		*/
		
		int j,k, min_idx;
		 double te[] = new double[2];
	    for (j = 0; j < noOfLines-1; j++)
	    {
	        
	        min_idx = j;
	        for (k = j+1; k < noOfLines; k++)
	          if (sent_rank[k][1] > sent_rank[min_idx][1])
	            min_idx = k;
	        
	       te[0] = sent_rank[min_idx][0];	//sentence number
	       te[1] = sent_rank[min_idx][1];	//rank_score
	       sent_rank[min_idx][0]=sent_rank[j][0];
	       sent_rank[min_idx][1]=sent_rank[j][1];
	       sent_rank[j][0]=te[0];
	       sent_rank[j][1]=te[1];
	       
	        
	    }
		
	    System.out.println("\n------------------------SORTED RANKS--------------------------");
		for (i = 0; i < noOfLines; i++) 
			System.out.println((i+1)+"-->"+sent_rank[i][1]+"-->"+sent_rank[i][0]+"\n");
		
		//output_req=3;
		double out_req_ranks[] = new double[output_req];
		for (i = 0; i < output_req; i++) {
			
			out_req_ranks[i] = sent_rank[i][0];
			//out_req_ranks[i][1] = sent_rank[i][1];
			//System.out.println((i+1)+"-->"+sent_rank[i][1]+"\n");
		}
		
		for(j=0;j<output_req-1;j++) {
			min_idx = j;
			for(k=j+1;k<output_req;k++) 
				if(out_req_ranks[k]<out_req_ranks[min_idx])
					min_idx=k;
			te[0] = out_req_ranks[j];
			out_req_ranks[j]=out_req_ranks[min_idx];
			out_req_ranks[min_idx]=te[0];
		}
		System.out.println("\n------------------------SORTED REQUIRED RANKS--------------------------");
		for (i = 0; i < output_req; i++) 
			System.out.println((i+1)+"-->"+out_req_ranks[i]+"\n");
		
		// totalNoOfWords=t;
		// System.out.println(t);
		scan = new Scanner(new File("F:\\Project(Summarization)\\Tokens.txt"));
		index = 0;
		int lcount = 0;
		i = 0;
		// Summary = title +". \n";
		while (scan.hasNext()) {

			// this loop

			
			temp = scan.nextLine();
			//temp = temp.replaceAll("[^\\p{ASCII}]", "");
			if (SentenceLength.noOfWords(temp) < 2)
				continue;
			Stop_word_elimination s = new Stop_word_elimination();
			count = s.Eliminate();
			noOfWords = count;
			Frequency f1 = new Frequency();
			double sentenceScore = f1.sentence_score(temp, tcount);
			// sent_score_matrix[i][i]=sentenceScore;
			// i++;
			// System.out.println("NOOFLINESSSS-----"+noOfLines);
			for (i = 0; i < noOfLines; i++) {

				if (sent_score_matrix[lcount][i] > 2.0 && sent_score_matrix[lcount][i] < 999) {
					sent_score_matrix[lcount][i] = sent_score_matrix[i][lcount] = 999;
					System.out.println("REMOVED  LINE NO  -----  " + (i + 1));
				}

			}

			lcount++;

			Sentence sentence = new Sentence(++index, temp,
					SentenceLength.sentenceLengthScore(SentenceLength.noOfWords(temp), 15.0),
					SentencePosition.sentencePositionScore(index, noOfLines),
					new CuePhrase(SampleFileName).cuePhraseScore(temp, totalNoOfCuePhrases),
					new TitleResemblance(null, null).titlePhraseScore(temp, totalNoOftitleResemblences), sentenceScore,
					noOfWords);
			System.out.println(sentence.toString());
			Threshold t = new Threshold();
			boolean flag = t.applyThreshold(SentenceLength.sentenceLengthScore(SentenceLength.noOfWords(temp), 15.0),
					SentencePosition.sentencePositionScore(index, noOfLines),
					new CuePhrase(SampleFileName).cuePhraseScore(temp, totalNoOfCuePhrases),
					new TitleResemblance(null, null).titlePhraseScore(temp, totalNoOftitleResemblences), sentenceScore,
					noOfWords);

			if (flag) { // System.out.println(temp);

			//	Summary += temp;
				//Summary = Summary + "\n";
			}
		}
		
		scan = new Scanner(new File("F:\\Project(Summarization)\\Tokens.txt"));  
		count=1;
		boolean flag;
		while(scan.hasNext()){
			flag=false;
			temp=scan.nextLine();
			for (i = 0; i < output_req; i++) 
			{
				if(count==out_req_ranks[i]) {flag=true;} 
			}
			if(flag) {
				Summary+=temp;
				Summary+="\n";
			}
			count++;
		}
		
		
		out.close();

		RandomAccessFile f1 = new RandomAccessFile(new File("F:\\Project(Summarization)\\Summarised.txt"), "rw");
		// f1.flush();
		f1.setLength(0);
		// f1.write(title.getBytes());
		// f1.write("\n".getBytes());
		f1.getFD().sync();
		f1.write(Summary.getBytes());
		// f1.write(System.getProperty("line.separator").getBytes());

		f1.close();

		System.out.println(Summary);
		return Summary;

	}

}
/*
 * public static void main(String[] args) {
 * 
 * Tokeniser t=new Tokeniser(); try { t.tokenise(); } catch (Exception e) { //
 * TODO Auto-generated catch block e.printStackTrace(); }
 * 
 * }
 */
