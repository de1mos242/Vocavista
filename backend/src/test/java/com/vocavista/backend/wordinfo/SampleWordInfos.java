package com.vocavista.backend.wordinfo;

import java.util.List;
import java.util.Map;

final class SampleWordInfos {

	private SampleWordInfos() {
	}

	static ProviderWordInfo nounInfo() {
		return new ProviderWordInfo(
				"Hausaufgabe",
				"de",
				Map.of("en", List.of("homework"), "ru", List.of("domashnee zadanie")),
				"noun",
				"feminine",
				"die",
				"Hausaufgaben",
				"common",
				true,
				List.of(
						new ProviderWordInfo.CompoundPart("Haus",
								Map.of("en", List.of("house"), "ru", List.of("dom"))),
						new ProviderWordInfo.CompoundPart("Aufgabe",
								Map.of("en", List.of("task"), "ru", List.of("zadanie")))),
				Map.of("en", List.of("A common word for school homework."), "ru",
						List.of("Obychnoe slovo dlya domashnego zadaniya.")),
				List.of(
						new ProviderWordInfo.WordExample("Ich mache meine Hausaufgabe nach dem Abendessen.",
								Map.of("en", List.of("I do my homework after dinner."), "ru",
										List.of("Ya delayu domashnee zadanie posle uzhina."))),
						new ProviderWordInfo.WordExample("Die Hausaufgabe ist heute leicht.",
								Map.of("en", List.of("The homework is easy today."), "ru",
										List.of("Segodnya domashnee zadanie legkoe."))),
						new ProviderWordInfo.WordExample("Hast du die Hausaufgabe schon fertig?",
								Map.of("en", List.of("Have you already finished the homework?"), "ru",
										List.of("Ty uzhe zakonchil domashnee zadanie?")))));
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
