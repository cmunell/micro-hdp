package edu.cmu.ml.rtw.micro.hdp;

import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorTokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.util.Triple;

public class HDPParser implements AnnotatorTokenSpan<String> {
	private static final AnnotationType<?>[] REQUIRED_ANNOTATIONS = new AnnotationType<?>[] {
		AnnotationTypeNLP.SENTENCE

		// TODO: extract simpler subsentences from sentences that are too complex using dependency trees
		//AnnotationTypeNLP.DEPENDENCY_PARSE
	};

	private static final AnnotationTypeNLP<String> SEMANTIC_PARSE = new AnnotationTypeNLP<String>("hdp-parse", String.class, Target.SENTENCE);
	private static final String DEFAULT_KB_PATH = "/home/asaparov/SVOReader/NELL.08m.905.esv.csv.gz";
	private static final String DEFAULT_ONTOLOGY_PATH = "/home/asaparov/SVOReader/NELL.08m.905.ontology.csv.gz";
	private static final String DEFAULT_GRAMMAR_PATH = "/home/asaparov/SVOReader/correlated_svo_with_prepositions.gram";
	private static final String DEFAULT_HDP_DIRECTORY = "/home/asaparov/SVOReader/data_correlated";
	private static final int DEFAULT_MEMORY_LIMIT = 80; /* in gigabytes; OS will kill the program if this limit is exceeded */

	private static class Singleton {
		private static final HDPParser INSTANCE = new HDPParser(DEFAULT_KB_PATH, DEFAULT_ONTOLOGY_PATH, DEFAULT_GRAMMAR_PATH, DEFAULT_HDP_DIRECTORY, DEFAULT_MEMORY_LIMIT);
	}

	public static HDPParser getInstance() {
		return Singleton.INSTANCE;
	}

	private HDPParser(String kbPath, String ontologyPath, String grammarPath, String hdpDirectory, int memoryLimitGB) {
		this(
			String.format("load_kb --kb=%s --ontology=%s\n", kbPath, ontologyPath)
		  + String.format("load_grammar %s", grammarPath), hdpDirectory, memoryLimitGB);
	}

	private HDPParser(String initScript, String hdpDirectory, int memoryLimitGB) {
		System.loadLibrary("libsvo_reader");
		if (!initialize(initScript, hdpDirectory, memoryLimitGB))
			throw new IllegalStateException("Unable to initialize parser.");
	}

	private native boolean initialize(String initScript, String hdpDirectory, int memoryLimitGB);

	@Override
	public String getName() {
		return "micro-hdp";
	}

	@Override
	public AnnotationType<String> produces() {
		return SEMANTIC_PARSE;
	}

	@Override
	public AnnotationType<?>[] requires() {
		return REQUIRED_ANNOTATIONS;
	}

	@Override
	public boolean measuresConfidence() {
		return true;
	}

	@Override
	public List<Triple<TokenSpan, String, Double>> annotate(DocumentNLP document) {
		return null;
	}
}

