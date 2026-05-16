package com.vocavista.backend.wordinfo;

import java.util.List;
import java.util.Optional;

final class SampleWordInfos {

	private SampleWordInfos() {
	}

	static ProviderWordInfo nounInfo() {
		return new ProviderWordInfo(
				"Hausaufgabe",
				ProviderWordInfo.Language.de,
				localizedText(List.of("homework"), List.of("domashnee zadanie")),
				ProviderWordInfo.ProviderPartOfSpeech.noun,
				Optional.of(ProviderWordInfo.ProviderGender.feminine),
				Optional.of(ProviderWordInfo.ProviderArticle.die),
				Optional.of("Hausaufgaben"),
				ProviderWordInfo.ProviderFrequency.common,
				true,
				List.of(
						new ProviderWordInfo.CompoundPart("Haus",
								localizedText(List.of("house"), List.of("dom"))),
						new ProviderWordInfo.CompoundPart("Aufgabe",
								localizedText(List.of("task"), List.of("zadanie")))),
				localizedText(List.of("A common word for school homework."),
						List.of("Obychnoe slovo dlya domashnego zadaniya.")),
				List.of(
						new ProviderWordInfo.WordExample("Ich mache meine Hausaufgabe nach dem Abendessen.",
								localizedText(List.of("I do my homework after dinner."),
										List.of("Ya delayu domashnee zadanie posle uzhina."))),
						new ProviderWordInfo.WordExample("Die Hausaufgabe ist heute leicht.",
								localizedText(List.of("The homework is easy today."),
										List.of("Segodnya domashnee zadanie legkoe."))),
						new ProviderWordInfo.WordExample("Hast du die Hausaufgabe schon fertig?",
								localizedText(List.of("Have you already finished the homework?"),
										List.of("Ty uzhe zakonchil domashnee zadanie?")))));
	}

	private static ProviderWordInfo.LocalizedText localizedText(List<String> en, List<String> ru) {
		return new ProviderWordInfo.LocalizedText(en, ru);
	}

	static String nounInfoJson() {
		return """
				{
				  "normalizedWord": "Hausaufgabe",
				  "language": "de",
				  "translations": {
				    "en": ["homework"],
				    "ru": ["domashnee zadanie"]
				  },
				  "partOfSpeech": "noun",
				  "gender": "feminine",
				  "article": "die",
				  "plural": "Hausaufgaben",
				  "frequency": "common",
				  "isCompound": true,
				  "compoundParts": [
				    {
				      "word": "Haus",
				      "meanings": {
				        "en": ["house"],
				        "ru": ["dom"]
				      }
				    },
				    {
				      "word": "Aufgabe",
				      "meanings": {
				        "en": ["task"],
				        "ru": ["zadanie"]
				      }
				    }
				  ],
				  "shortNote": {
				    "en": ["A common word for school homework."],
				    "ru": ["Obychnoe slovo dlya domashnego zadaniya."]
				  },
				  "examples": [
				    {
				      "sentence": "Ich mache meine Hausaufgabe nach dem Abendessen.",
				      "translations": {
				        "en": ["I do my homework after dinner."],
				        "ru": ["Ya delayu domashnee zadanie posle uzhina."]
				      }
				    },
				    {
				      "sentence": "Die Hausaufgabe ist heute leicht.",
				      "translations": {
				        "en": ["The homework is easy today."],
				        "ru": ["Segodnya domashnee zadanie legkoe."]
				      }
				    },
				    {
				      "sentence": "Hast du die Hausaufgabe schon fertig?",
				      "translations": {
				        "en": ["Have you already finished the homework?"],
				        "ru": ["Ty uzhe zakonchil domashnee zadanie?"]
				      }
				    }
				  ]
				}
				""";
	}

}
