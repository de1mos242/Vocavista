package com.vocavista.backend.wordinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vocavista.backend.api.model.PartOfSpeech;
import com.vocavista.backend.api.model.WordInfoResponse;
import org.junit.jupiter.api.Test;

class WordInfoServiceTest {

	private final WordInfoMapper wordInfoMapper = new WordInfoMapperImpl();
	private final ProviderWordInfoValidator providerWordInfoValidator = new ProviderWordInfoValidator();

	@Test
	void trimsWordAndMapsProviderResponse() {
		WordInfoService service = new WordInfoService(word -> {
			assertThat(word).isEqualTo("Hausaufgabe");
			return SampleWordInfos.nounInfo();
		}, providerWordInfoValidator, wordInfoMapper);

		WordInfoResponse response = service.getWordInfo("  Hausaufgabe  ");

		assertThat(response.getNormalizedWord()).isEqualTo("Hausaufgabe");
		assertThat(response.getPartOfSpeech()).isEqualTo(PartOfSpeech.NOUN);
		assertThat(response.getExamples()).hasSize(3);
	}

	@Test
	void rejectsBlankWordAfterTrimming() {
		WordInfoService service = new WordInfoService(word -> SampleWordInfos.nounInfo(), providerWordInfoValidator,
				wordInfoMapper);

		assertThatThrownBy(() -> service.getWordInfo("   ")).isInstanceOf(WordInfoValidationException.class);
	}

	@Test
	void rejectsMalformedProviderResponse() {
		ProviderWordInfo malformedInfo = new ProviderWordInfo("Hausaufgabe", "de", null, "noun", "feminine", "die",
				"Hausaufgaben", "common", true, java.util.List.of(), java.util.Map.of(), java.util.List.of());
		WordInfoService service = new WordInfoService(word -> malformedInfo, providerWordInfoValidator, wordInfoMapper);

		assertThatThrownBy(() -> service.getWordInfo("Hausaufgabe")).isInstanceOf(AiProviderBadGatewayException.class);
	}

}
