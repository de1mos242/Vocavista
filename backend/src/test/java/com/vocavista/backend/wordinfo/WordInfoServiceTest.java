package com.vocavista.backend.wordinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.WordInfoResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WordInfoServiceTest {

	private final WordInfoMapper wordInfoMapper = new WordInfoMapperImpl();
	private final ProviderWordInfoValidator providerWordInfoValidator = new ProviderWordInfoValidator();

	@Test
	void trimsWordAndMapsProviderResponse() {
		WordInfoRepository wordInfoRepository = emptyRepository();
		WordInfoService service = new WordInfoService(word -> {
			assertThat(word).isEqualTo("Hausaufgabe");
			return SampleWordInfos.nounInfo();
		}, providerWordInfoValidator, wordInfoMapper, wordInfoRepository);

		WordInfoResponse response = service.getWordInfo("  Hausaufgabe  ");

		assertThat(response.getNormalizedWord()).isEqualTo("Hausaufgabe");
		assertThat(response.getPartOfSpeech()).isEqualTo(PartOfSpeech.NOUN);
		assertThat(response.getExamples()).hasSize(3);
		verify(wordInfoRepository).save(any());
	}

	@Test
	void rejectsBlankWordAfterTrimming() {
		WordInfoService service = new WordInfoService(word -> SampleWordInfos.nounInfo(), providerWordInfoValidator,
				wordInfoMapper, emptyRepository());

		assertThatThrownBy(() -> service.getWordInfo("   ")).isInstanceOf(WordInfoValidationException.class);
	}

	@Test
	void rejectsMalformedProviderResponse() {
		ProviderWordInfo malformedInfo = new ProviderWordInfo("Hausaufgabe", ProviderWordInfo.Language.de,
				new ProviderWordInfo.LocalizedText(List.of("homework"), List.of("домашнее задание")),
				ProviderWordInfo.ProviderPartOfSpeech.noun, Optional.of(ProviderWordInfo.ProviderGender.feminine),
				Optional.of(ProviderWordInfo.ProviderArticle.die), Optional.of("Hausaufgaben"),
				ProviderWordInfo.ProviderFrequency.common, false, List.of(),
				new ProviderWordInfo.LocalizedText(List.of("note"), List.of("заметка")),
				List.of(new ProviderWordInfo.WordExample("Ich mache meine Hausaufgabe.",
						new ProviderWordInfo.LocalizedText(List.of("I do my homework."),
								List.of("Я делаю домашнее задание.")))));
		WordInfoService service = new WordInfoService(word -> malformedInfo, providerWordInfoValidator, wordInfoMapper,
				emptyRepository());

		assertThatThrownBy(() -> service.getWordInfo("Hausaufgabe"))
				.isInstanceOf(AiProviderBadGatewayException.class)
				.hasMessageContaining("examples must contain exactly 3 items");
	}

	private static WordInfoRepository emptyRepository() {
		WordInfoRepository wordInfoRepository = mock(WordInfoRepository.class);
		when(wordInfoRepository.findByNormalizedQuery(anyString())).thenReturn(Optional.empty());
		return wordInfoRepository;
	}

}
