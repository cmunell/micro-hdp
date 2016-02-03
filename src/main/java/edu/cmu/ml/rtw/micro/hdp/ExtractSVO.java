package edu.cmu.ml.rtw.micro.hdp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse.Dependency;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse.Node;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;

public class ExtractSVO {
	private static String[] SUBJECT_DEPENDENCIES = { "compound" };
	private static String[] VERB_DEPENDENCIES = { "aux" };
	private static String[] OBJECT_DEPENDENCIES = { "compound" };

	private static Dependency findDependency(DependencyParse parse, int node, String type) {
		List<Dependency> deps = parse.getGovernedDependencies(node);
		for (Dependency dep : deps) {
			if (dep.getType().equals(type))
				return dep;
		}
		return null;
	}

	private static List<Dependency> findDependencies(DependencyParse parse, int node, String[] types) {
		List<Dependency> found = new ArrayList<Dependency>();
		List<Dependency> deps = parse.getGovernedDependencies(node);
		for (Dependency dep : deps) {
			for (int i = 0; i < types.length; i++)
				if (dep.getType().equals(types[i]))
					found.add(dep);
		}
		return found;
	}

	private static List<Integer> toDependentIndices(DependencyParse parse, List<Dependency> deps) {
		List<Integer> indices = new ArrayList<Integer>();
		for (Dependency dep : deps)
			indices.add(dep.getDependentTokenIndex());
		return indices;
	}

	private static String concatenateTokens(DocumentNLP document, DependencyParse parse, List<Integer> indices) {
		if (indices.size() == 0)
			return "";
		StringBuilder string = new StringBuilder();
		string.append(document.getTokenStr(parse.getSentenceIndex(), indices.get(0)));
		for (int i = 1; i < indices.size(); i++) {
			string.append(' ');
			string.append(document.getTokenStr(parse.getSentenceIndex(), indices.get(i)));
		}
		return string.toString();
	}

	private static String extract(DocumentNLP document, DependencyParse parse, Dependency head, String[] subDepTypes) {
		List<Dependency> subDeps = findDependencies(parse, head.getDependentTokenIndex(), subDepTypes);
		List<Integer> indices = toDependentIndices(parse, subDeps);
		indices.add(head.getDependentTokenIndex());
		Collections.sort(indices);
		return concatenateTokens(document, parse, indices);
	}

	private static String extract(DocumentNLP document, DependencyParse parse, Dependency root, String headType, String[] subDepTypes) {
		Dependency head = findDependency(parse, root.getDependentTokenIndex(), headType);
		if (head == null)
			return null;
		return extract(document, parse, head, subDepTypes);
	}

	public static String extract(DocumentNLP document, DependencyParse parse) {
		if (parse == null)
			return null;
		Dependency root = findDependency(parse, -1, "root");
		if (root == null)
			return null;

		String subject = extract(document, parse, root, "nsubj", SUBJECT_DEPENDENCIES);
		String verb = extract(document, parse, root, VERB_DEPENDENCIES);
		String object = extract(document, parse, root, "dobj", OBJECT_DEPENDENCIES);
		if (subject == null || verb == null || object == null)
			return null;

		return subject + " " + verb + " " + object;
	}
}
