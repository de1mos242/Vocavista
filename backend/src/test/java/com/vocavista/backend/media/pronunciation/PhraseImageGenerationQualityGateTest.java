package com.vocavista.backend.media.pronunciation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vocavista.backend.vocabulary.VocabularyItem;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhraseImageGenerationQualityGateTest {

	@Mock PhraseImageRepository repository;
	@Mock SpringAiPhraseImageSceneDescriber describer;
	@Mock PhraseImageGenerator generator;
	@Mock PhraseImageQualityReviewer reviewer;
	@Mock MediaStorageService storage;

	@Test
	void retriesWhenMockedSceneReviewRejectsFirstCandidateThenPersistsReviewMetadata() {
		PhraseImageAsset asset = PhraseImageAsset.queued(new VocabularyItem(), "Bank", "Er sitzt auf der Bank.", "de", "v5", OffsetDateTime.now());
		when(repository.findById(asset.getId())).thenReturn(Optional.of(asset));
		PhraseImageScenePlan plan = new PhraseImageScenePlan("a park bench for sitting", List.of("wooden slats", "park path"),
				"a person sits on the bench", "wide park view", "natural documentary photo");
		when(describer.describe(any(), any(), any())).thenReturn(plan);
		GeneratedImage image = new GeneratedImage(new byte[] { 1 }, "image/png");
		when(generator.generate(any())).thenReturn(List.of(image));
		when(reviewer.review(any(), any())).thenReturn(new ImageQualityReview(.2, false, false, true, false, "wrong sense"),
				new ImageQualityReview(.9, true, true, true, true, "anchors visible"));

		new PhraseImageGenerationProcessor(repository, describer, generator, reviewer, storage).process(asset.getId());

		verify(generator, times(2)).generate(any());
		verify(storage).store(any(), any(), any());
		org.assertj.core.api.Assertions.assertThat(asset.getQualityMetadata()).contains("0.90", "anchors visible");
	}
}
