package edu.cmu.ml.rtw.micro.hdp;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.AnnotatorSentence;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.micro.hdp.NarSystem;

/**
 * TODO: Instead of returning a string for each annotation
 * (serialized semantic and syntactic structures), create
 * a serializable Java structure that holds the information.
 *
 * TODO: Move the KB, ontology gz, and possibly the HDP
 * files into /nell/data/micro/.
 *
 * TODO: Move the grammar file into the resources directory
 * of this repository.
 */
public class HDPParser implements AnnotatorSentence<String> {
	private static final AnnotationType<?>[] REQUIRED_ANNOTATIONS = new AnnotationType<?>[] {
		AnnotationTypeNLP.SENTENCE

		// TODO: extract simpler subsentences from sentences that are too complex using dependency trees
		//AnnotationTypeNLP.DEPENDENCY_PARSE
	};

	public static final AnnotationTypeNLP<String> SEMANTIC_PARSE = new AnnotationTypeNLP<String>("nell-hdp", String.class, Target.SENTENCE);

	private static final String DEFAULT_KB_PATH = "/home/asaparov/SVOReader/NELL.08m.905.esv.csv.gz";
	private static final String DEFAULT_ONTOLOGY_PATH = "/home/asaparov/SVOReader/NELL.08m.905.ontology.csv.gz";
	private static final String DEFAULT_GRAMMAR_PATH = "/home/asaparov/SVOReader/verb_morphology.gram";
	private static final String DEFAULT_HDP_DIRECTORY = "/home/asaparov/SVOReader/data";
	private static final int DEFAULT_MEMORY_LIMIT = 0; /* in gigabytes; OS will kill the program if this limit is exceeded (zero is infinite) */

	private static class Singleton {
		private static final HDPParser INSTANCE = new HDPParser(DEFAULT_KB_PATH, DEFAULT_ONTOLOGY_PATH, DEFAULT_GRAMMAR_PATH, DEFAULT_HDP_DIRECTORY, DEFAULT_MEMORY_LIMIT);
	}

	public static HDPParser getInstance() {
		return Singleton.INSTANCE;
	}

	private HDPParser(String kbPath, String ontologyPath, String grammarPath, String hdpDirectory, int memoryLimitGB) {
		this(
			String.format("load_kb --kb=%s --ontology=%s\n", kbPath, ontologyPath)
		  + String.format("load_grammar %s quiet", grammarPath), hdpDirectory, memoryLimitGB);
	}

	private HDPParser(String initScript, String hdpDirectory, int memoryLimitGB) {
		NarSystem.loadLibrary();
		if (!initialize(initScript, hdpDirectory, memoryLimitGB))
			throw new IllegalStateException("Unable to initialize parser.");
	}

	private native boolean initialize(String initScript, String hdpDirectory, int memoryLimitGB);
	private native Pair<String, Double> annotate(String sentence, String rootSymbol, int k);

	@Override
	public String getName() {
		return "cmunell_hdp-0.0.1";
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
	public Map<Integer, Pair<String, Double>> annotate(DocumentNLP document) {
		HashMap<Integer, Pair<String, Double>> annotations = new HashMap<Integer, Pair<String, Double>>();
		for (int i = 0; i < document.getSentenceCount(); i++) {
			String sentence = document.getSentence(i).replace(" .", "").toLowerCase();
			if (sentence.split("\\s+").length > 12)
				continue;
			Pair<String, Double> annotation = annotate(sentence, "S", 1);
			if (annotation != null)
				annotations.put(i, annotation);
		}
		return annotations;
	}
}
