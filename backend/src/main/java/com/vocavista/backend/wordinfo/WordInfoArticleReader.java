package com.vocavista.backend.wordinfo;

import com.vocavista.backend.vocabulary.VocabularyItem;
import org.springframework.stereotype.Service;

@Service
public class WordInfoArticleReader {

	public String nounArticle(VocabularyItem item) {
		if (item == null || !"noun".equals(item.getPartOfSpeech()) || item.getGender() == null) {
			return null;
		}
		return switch (item.getGender()) {
			case "masculine" -> "der";
			case "feminine" -> "die";
			case "neuter" -> "das";
			default -> null;
		};
	}

}
