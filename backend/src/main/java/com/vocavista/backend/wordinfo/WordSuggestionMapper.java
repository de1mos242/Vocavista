package com.vocavista.backend.wordinfo;

import com.vocavista.backend.api.model.PronunciationStatus;
import com.vocavista.backend.api.model.WordSuggestion;
import com.vocavista.backend.media.MediaAssetQueryService;
import com.vocavista.backend.vocabulary.VocabularyItem;
import org.springframework.stereotype.Component;

@Component
class WordSuggestionMapper {

	WordSuggestion toVocabularyItemSuggestion(VocabularyItem item) {
		return new WordSuggestion(item.getWord(), WordSuggestion.SourceEnum.WORD_INFO)
				.phrase(item.getPhrase())
				.wordInfoId(item.getId());
	}

	WordSuggestion toPronunciationSuggestion(MediaAssetQueryService.PronunciationSuggestion pronunciation) {
		return new WordSuggestion(pronunciation.word(), WordSuggestion.SourceEnum.PRONUNCIATION)
				.phrase(pronunciation.phrase())
				.wordInfoId(pronunciation.wordInfoId())
				.pronunciationId(pronunciation.pronunciationId())
				.status(pronunciation.status() == null ? null : PronunciationStatus.fromValue(pronunciation.status()))
				.videoUrl(pronunciation.videoUrl())
				.fullVideoUrl(pronunciation.fullVideoUrl());
	}

}
