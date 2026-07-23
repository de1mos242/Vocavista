import { describeMeaningGloss, describeMeaningTranslations } from "./domain";
import type { WordMeaningOption } from "./api/generated/types.gen";
import { describe, expect, it } from "vitest";

const polysemousMeaning: WordMeaningOption = {
  optionId: 0,
  word: "Bank",
  language: "de",
  gloss: {
    en: ["financial institution for money"],
    ru: ["finansovoe uchrezhdenie dlya deneg"]
  },
  translations: {
    en: ["bank", "lender"],
    ru: ["bank"]
  },
  phraseOptions: [],
  partOfSpeech: "noun",
  frequency: "common",
  isCompound: false
};

describe("meaning disambiguation", () => {
  it("keeps a polysemous sense gloss primary and translation alternatives separate", () => {
    expect(describeMeaningGloss(polysemousMeaning)).toEqual([
      "en: financial institution for money",
      "ru: finansovoe uchrezhdenie dlya deneg"
    ]);
    expect(describeMeaningTranslations(polysemousMeaning)).toEqual(["en: bank", "en: lender", "ru: bank"]);
  });
});
