package com.vocavista.backend.wordinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
		ProviderWordInfo malformedInfo = new ProviderWordInfo("Hausaufgabe", ProviderWordInfo.Language.de, null,
				ProviderWordInfo.ProviderPartOfSpeech.noun, Optional.of(ProviderWordInfo.ProviderGender.feminine),
				Optional.of(ProviderWordInfo.ProviderArticle.die), Optional.of("Hausaufgaben"),
				ProviderWordInfo.ProviderFrequency.common, true, List.of(), null, List.of());
		WordInfoService service = new WordInfoService(word -> malformedInfo, providerWordInfoValidator, wordInfoMapper);

		assertThatThrownBy(() -> service.getWordInfo("Hausaufgabe")).isInstanceOf(AiProviderBadGatewayException.class);
	}

}
