package edu.cmu.ml.rtw.micro.hdp;

import java.io.Console;

import org.junit.Test;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPInMemory;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLPStanford;
import edu.cmu.ml.rtw.generic.util.Pair;

public class HDPParserTest {
	private class Pipeline extends PipelineNLP {
		public Pipeline(HDPParser parser) {
			super();
			addAnnotator(parser.produces(), parser);
		}
	}

	@Test
 	public void testParser() {
		HDPParser parser = HDPParser.getInstance();
		if (parser == null) {
			System.out.println("ERROR: getInstance returned null.\n");
			return;
		}
		PipelineNLPStanford pipe = new PipelineNLPStanford(30);
		Pipeline hdpPipe = new Pipeline(parser);
		PipelineNLP welded = pipe.weld(hdpPipe);

		DocumentNLPMutable document = new DocumentNLPInMemory(new DataTools(), "test_document", "And a trip to Baghdad 's looter market only bears witness to the fact that criminals are ruling the roost in the post - Saddam era. The president would have exceeded his authority in giving to Cuba the returns from the export of oil. Ramirez will have been playing baseball. John McCain represents Arizona, too.");
		document = welded.run(document);
		for (int i = 0; i < document.getSentenceCount(); i++) {
			String annotation = document.getSentenceAnnotation(HDPParser.SEMANTIC_PARSE, i);
			if (annotation == null)
				System.out.println("[sentence " + i + "] <null>");
			else System.out.println("[sentence " + i + "] " + annotation);
		}
	}
}
