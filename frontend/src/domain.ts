import type { DictionaryVideoManifestItem, VocabularyItemDto, WordMeaningOption } from "./api/generated/types.gen";

export function joinText(values?: string[]) {
  return values?.join(" · ") ?? "";
}

export function normalizeAnswer(value: string) {
  return value
    .toLowerCase()
    .replaceAll("ä", "ae")
    .replaceAll("ö", "oe")
    .replaceAll("ü", "ue")
    .replaceAll("ß", "ss")
    .replace(/[^\p{L}\p{N}\s]/gu, " ")
    .trim()
    .replace(/\s+/g, " ");
}

export function articleForGender(gender?: VocabularyItemDto["gender"]) {
  if (gender === "masculine") return "der";
  if (gender === "feminine") return "die";
  if (gender === "neuter") return "das";
  return "";
}

export function articleForMeaning(meaning: WordMeaningOption) {
  return meaning.article ?? articleForGender(meaning.gender);
}

export function describeWordTranslations(item: VocabularyItemDto) {
  return item.translations
    .map((translation) => `${translation.language}: ${translation.wordTranslation}`)
    .join(" · ");
}

export function describeMeaningTranslations(meaning: WordMeaningOption) {
  return Object.entries(meaning.translations)
    .flatMap(([language, values]) => values.map((value) => `${language}: ${value}`));
}

export function describeMeaningGloss(meaning: WordMeaningOption) {
  return Object.entries(meaning.gloss)
    .flatMap(([language, values]) => values.map((value) => `${language}: ${value}`));
}

export function describePhraseTranslations(item: VocabularyItemDto) {
  return item.translations
    .map((translation) => `${translation.language}: ${translation.phraseTranslation}`)
    .join(" · ");
}

export function smallPronunciationVideoUrl(assetId: string) {
  return `/api/v1/media/pronunciations/${assetId}/video/small`;
}

export function absoluteUrl(value: DictionaryVideoManifestItem["videoUrl"]) {
  return new URL(value, window.location.origin).toString();
}
