import { useEffect, useRef, useState } from "react";
import type { RefObject } from "react";
import { NavLink, Navigate, Route, Routes } from "react-router";
import { accountRestrictionMessage, ApiError, loginUrl, logout, unwrap } from "./api";
import { articleForGender, articleForMeaning, describeMeaningTranslations, describePhraseTranslations, describeWordTranslations, joinText, normalizeAnswer, smallPronunciationVideoUrl } from "./domain";
import { syncDictionaryVideoCache } from "./mediaCache";
import {
  addDictionaryEntry,
  createPhraseImage,
  createPronunciation,
  getCurrentUser,
  getDictionaryReview,
  getPhraseImage,
  getPronunciation,
  getWordInfo,
  getWordSuggestions,
  listAdminUsers,
  saveVocabularyItem,
  selectPhraseImageCandidate,
  submitDictionaryReview,
  updateAdminUserStatus
} from "./api/generated/sdk.gen";
import type {
  AdminUserResponse,
  CurrentUserResponse,
  DictionaryReviewItem,
  DictionaryReviewSubmitResponse,
  PhraseImageResponse,
  UserStatus,
  VocabularyItemDto,
  WordInfoResponse,
  WordMeaningOption,
  WordSuggestion
} from "./api/generated/types.gen";

type AuthState = "checking" | "signed-out" | "signed-in";


export default function App() {
  const [authState, setAuthState] = useState<AuthState>("checking");
  const [currentUser, setCurrentUser] = useState<CurrentUserResponse>();

  async function reloadCurrentUser() {
    setAuthState("checking");
    try {
      const user = await unwrap(getCurrentUser());
      setCurrentUser(user);
      setAuthState("signed-in");
    }
    catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setCurrentUser(undefined);
        setAuthState("signed-out");
        return;
      }
      setCurrentUser(undefined);
      setAuthState("signed-out");
    }
  }

  async function logoutCurrentUser() {
    await logout();
    setCurrentUser(undefined);
    setAuthState("signed-out");
  }

  function handleAuthError(error: unknown) {
    if (error instanceof ApiError && error.status === 401) {
      setCurrentUser(undefined);
      setAuthState("signed-out");
    }
  }

  useEffect(() => {
    void reloadCurrentUser();
  }, []);

  useEffect(() => {
    if (!currentUser?.functionalAccessAllowed) {
      return;
    }
    syncDictionaryVideoCache().catch(handleAuthError);
  }, [currentUser?.id, currentUser?.functionalAccessAllowed]);

  return (
    <div className="app-shell">
      <header className="topbar">
        <nav className="nav-links" aria-label="Main navigation">
          <NavLink to="/" end>Home</NavLink>
          <NavLink to="/add">Add</NavLink>
          <NavLink to="/review">Review</NavLink>
          {currentUser?.admin ? <NavLink to="/admin">Admin</NavLink> : null}
        </nav>
        <AuthPanel authState={authState} user={currentUser} onLogout={logoutCurrentUser} />
      </header>

      <Routes>
        <Route path="/" element={<HomePage user={currentUser} authState={authState} />} />
        <Route path="/add" element={<AddWordPage user={currentUser} authState={authState} onAuthError={handleAuthError} />} />
        <Route path="/review" element={<ReviewPage user={currentUser} authState={authState} onAuthError={handleAuthError} />} />
        <Route path="/admin" element={<AdminPage user={currentUser} authState={authState} onAuthError={handleAuthError} />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}

function AuthPanel({ authState, user, onLogout }: { authState: AuthState; user?: CurrentUserResponse; onLogout: () => Promise<void> }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submitLogout() {
    setBusy(true);
    setError("");
    try {
      await onLogout();
    }
    catch (logoutError) {
      setError(logoutError instanceof Error ? logoutError.message : "Could not log out.");
    }
    finally {
      setBusy(false);
    }
  }

  if (authState === "checking") {
    return <div className="auth-panel subtle">Checking sign-in...</div>;
  }
  if (!user) {
    return <a className="login-button" href={loginUrl()}><img src="/google-g.svg" alt="" aria-hidden="true" />Sign in</a>;
  }
  return (
    <div className="auth-panel">
      <div>
        <strong>{user.displayName}</strong>
        <small>{user.email} · {user.status}</small>
        {error ? <small className="error-text">{error}</small> : null}
      </div>
      <button type="button" className="secondary small" disabled={busy} onClick={submitLogout}>Logout</button>
    </div>
  );
}

function HomePage({ user, authState }: { user?: CurrentUserResponse; authState: AuthState }) {
  return (
    <main className="home-layout">
      <section className="hero-card home-hero">
        <p className="eyebrow">Vocavista PWA</p>
        <h1>German words that fit in your pocket.</h1>
        <p>Add vocabulary with generated pronunciation video, then review it from your phone whenever you have a minute.</p>
        {authState === "signed-out" ? <a className="login-button wide" href={loginUrl()}><img src="/google-g.svg" alt="" aria-hidden="true" />Sign in with Google</a> : null}
        {user && !user.functionalAccessAllowed ? <AccountNotice user={user} /> : null}
      </section>
      <section className="action-grid" aria-label="Primary actions">
        <NavLink className="action-card add-card" to="/add">
          <span>Add word</span>
          <strong>Search, choose a phrase, generate assets.</strong>
        </NavLink>
        <NavLink className="action-card review-card-link" to="/review">
          <span>Review</span>
          <strong>Recall from English and Russian prompts.</strong>
        </NavLink>
      </section>
    </main>
  );
}

function AddWordPage({ user, authState, onAuthError }: PageProps) {
  const [word, setWord] = useState("");
  const [phrase, setPhrase] = useState("");
  const [status, setStatus] = useState("Sign in with Google to use this page.");
  const [suggestions, setSuggestions] = useState<WordSuggestion[]>([]);
  const [wordInfo, setWordInfo] = useState<WordInfoResponse>();
  const [selectedMeaningId, setSelectedMeaningId] = useState<number>();
  const [selectedVocabularyItem, setSelectedVocabularyItem] = useState<VocabularyItemDto>();
  const [wordInfoId, setWordInfoId] = useState<string>();
  const [videoUrl, setVideoUrl] = useState<string>();
  const [phraseImage, setPhraseImage] = useState<PhraseImageResponse>();
  const [imageStatus, setImageStatus] = useState("");
  const [searchBusy, setSearchBusy] = useState(false);
  const [generateBusy, setGenerateBusy] = useState(false);
  const [imageBusy, setImageBusy] = useState(false);
  const [saveBusy, setSaveBusy] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const canUseFeatures = Boolean(user?.functionalAccessAllowed);
  const canGenerateAssets = canUseFeatures && Boolean(phrase.trim()) && Boolean(wordInfoId || selectedVocabularyItem);
  const selectedMeaning = selectedMeaningId === undefined
    ? undefined
    : wordInfo?.meanings.find((meaning) => meaning.optionId === selectedMeaningId);

  useEffect(() => {
    if (authState === "signed-out") {
      setStatus("Sign in with Google to use this page.");
    }
    else if (user && !user.functionalAccessAllowed) {
      setStatus(accountRestrictionMessage(user.status));
    }
    else if (user) {
      setStatus("Search a word first, choose a German meaning, then choose an example phrase.");
    }
  }, [authState, user]);

  function resetAssets() {
    setVideoUrl(undefined);
    setPhraseImage(undefined);
    setImageStatus("");
  }

  useEffect(() => {
    if (!canUseFeatures || word.trim().length < 2) {
      setSuggestions([]);
      return;
    }
    const timeout = window.setTimeout(() => {
      unwrap(getWordSuggestions({ query: { query: word.trim() } }))
        .then((data) => setSuggestions(data.items))
        .catch((error: unknown) => {
          onAuthError(error);
          setStatus(error instanceof Error ? error.message : "Could not load suggestions.");
        });
    }, 250);
    return () => window.clearTimeout(timeout);
  }, [canUseFeatures, onAuthError, word]);

  async function loadWordInfo() {
    if (!canUseFeatures) {
      setStatus(user ? accountRestrictionMessage(user.status) : "Sign in with Google to use this page.");
      return;
    }
    const trimmedWord = word.trim();
    if (!trimmedWord) {
      setStatus("Enter a word first.");
      return;
    }
    setSearchBusy(true);
    setWordInfo(undefined);
    try {
      setStatus("Loading word info...");
      const info = await unwrap(getWordInfo({ query: { word: trimmedWord } }));
      setSuggestions([]);
      setWordInfo(info);
      setSelectedMeaningId(undefined);
      setSelectedVocabularyItem(undefined);
      setWordInfoId(undefined);
      setPhrase("");
      resetAssets();
      setStatus("Choose the German meaning you want to learn.");
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not load word info.");
    }
    finally {
      setSearchBusy(false);
    }
  }

  async function selectSuggestion(suggestion: WordSuggestion) {
    setSuggestions([]);
    setWordInfo(undefined);
    setSelectedMeaningId(undefined);
    setSelectedVocabularyItem(undefined);
    setWord(suggestion.word);
    setWordInfoId(suggestion.wordInfoId ?? undefined);
    setPhrase(suggestion.phrase ?? "");
    resetAssets();
    if (suggestion.videoUrl) {
      setVideoUrl(suggestion.videoUrl);
      setStatus("Selected an existing pronunciation video. Use Save and generate assets when ready.");
      return;
    }
    setStatus("Selected existing entry. Use Save and generate assets when ready.");
  }

  function selectMeaningOption(meaning: WordMeaningOption) {
    setSelectedMeaningId(meaning.optionId);
    setSelectedVocabularyItem(undefined);
    setWordInfoId(undefined);
    setWord(meaning.word);
    setPhrase("");
    resetAssets();
    setStatus(`Meaning selected for ${meaning.word}. Choose one example phrase.`);
  }

  function selectVocabularyItem(item: VocabularyItemDto) {
    setWord(item.word);
    setPhrase(item.phrase);
    setSelectedVocabularyItem(item);
    setWordInfoId(item.id ?? undefined);
    resetAssets();
    setStatus(`Phrase selected for ${item.word}. Use Save and generate assets when ready.`);
  }

  async function generateAssets() {
    if (!canUseFeatures) {
      setStatus(user ? accountRestrictionMessage(user.status) : "Sign in with Google to use this page.");
      return;
    }
    if (!phrase.trim()) {
      setStatus("Choose a phrase before generating assets.");
      return;
    }
    const targetWordInfoId = await ensureSelectedVocabularyItemSaved();
    if (!targetWordInfoId) {
      setStatus("Search word info before generating assets.");
      return;
    }
    const savedWord = await saveWordToReviseList(targetWordInfoId);
    if (!savedWord) {
      return;
    }
    resetAssets();
    await Promise.allSettled([generateVideo(targetWordInfoId), generateImage(targetWordInfoId)]);
  }

  async function ensureSelectedVocabularyItemSaved() {
    if (wordInfoId) {
      return wordInfoId;
    }
    if (!selectedVocabularyItem) {
      return "";
    }
    setSaveBusy(true);
    try {
      setStatus("Saving selected vocabulary item...");
      const itemToSave = { ...selectedVocabularyItem, word, phrase };
      const saved = await unwrap(saveVocabularyItem({ body: { item: itemToSave } }));
      const savedItemId = saved.item.id ?? undefined;
      if (!savedItemId) {
        throw new Error("Saved vocabulary item did not include an id.");
      }
      setSelectedVocabularyItem(saved.item);
      setWordInfoId(savedItemId);
      setWordInfo((current) => current ? replacePhraseOption(current, selectedVocabularyItem, saved.item) : current);
      return savedItemId;
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not save selected vocabulary item.");
      return "";
    }
    finally {
      setSaveBusy(false);
    }
  }

  async function generateVideo(targetWordInfoId: string) {
    if (!canUseFeatures) {
      setStatus(user ? accountRestrictionMessage(user.status) : "Sign in with Google to use this page.");
      return;
    }
    if (!targetWordInfoId) {
      setStatus("Search word info before generating video.");
      return;
    }
    setGenerateBusy(true);
    try {
      setStatus("Queueing video generation...");
      const queued = await unwrap(createPronunciation({ body: { wordInfoId: targetWordInfoId, word, phrase, language: "de" } }));
      const completed = await pollPronunciation(queued.id, setStatus);
      if (completed.status === "failed") {
        throw new Error(`${completed.errorCode ?? "generation_failed"}: ${completed.errorMessage ?? "Video generation failed."}`);
      }
      if (!completed.videoUrl) {
        throw new Error("Generation completed without videoUrl.");
      }
      setVideoUrl(completed.videoUrl);
      setStatus(`Video ready. id=${completed.id}`);
      window.setTimeout(() => void videoRef.current?.play(), 50);
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not generate video.");
    }
    finally {
      setGenerateBusy(false);
    }
  }

  async function generateImage(targetWordInfoId: string) {
    if (!canUseFeatures) {
      setImageStatus(user ? accountRestrictionMessage(user.status) : "Sign in with Google to use this page.");
      return;
    }
    if (!targetWordInfoId) {
      setImageStatus("Search word info before generating an image.");
      return;
    }
    if (!phrase.trim()) {
      setImageStatus("Choose a phrase before generating an image.");
      return;
    }
    setImageBusy(true);
    try {
      setImageStatus("Queueing cinematic image...");
      const queued = await unwrap(createPhraseImage({ body: { wordInfoId: targetWordInfoId, word, phrase, language: "de" } }));
      setPhraseImage(queued);
      const completed = await pollPhraseImage(queued.id, setImageStatus);
      setPhraseImage(completed);
      if (completed.status === "awaiting_selection") {
        setImageStatus("Choose the best image candidate.");
        return;
      }
      setImageStatus(completed.status === "completed" ? "Image ready." : completed.errorMessage ?? "Image generation failed. Review can continue without it.");
    }
    catch (error) {
      onAuthError(error);
      setImageStatus(error instanceof Error ? error.message : "Could not generate image.");
    }
    finally {
      setImageBusy(false);
    }
  }

  async function selectImageCandidate(candidateIndex: number) {
    if (!phraseImage?.id) {
      return;
    }
    setImageBusy(true);
    try {
      setImageStatus("Saving selected image...");
      const selected = await unwrap(selectPhraseImageCandidate({ path: { id: phraseImage.id, candidateIndex } }));
      setPhraseImage(selected);
      setImageStatus("Image selected.");
    }
    catch (error) {
      onAuthError(error);
      setImageStatus(error instanceof Error ? error.message : "Could not select image.");
    }
    finally {
      setImageBusy(false);
    }
  }

  async function saveWordToReviseList(targetWordInfoId: string) {
    if (!targetWordInfoId) {
      setStatus("Search word info before saving.");
      return "";
    }
    setSaveBusy(true);
    try {
      setStatus("Saving word to revise list...");
      const entry = await unwrap(addDictionaryEntry({ body: { wordInfoId: targetWordInfoId } }));
      return entry.normalizedWord;
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not save to revise list.");
      return "";
    }
    finally {
      setSaveBusy(false);
    }
  }

  return (
    <main className="mobile-workspace add-layout">
      <section className="panel controls-panel">
        <p className="eyebrow">Add word</p>
        <h1>Make a tiny pronunciation lesson.</h1>
        <p>Search in English, Russian, or German. Pick the German meaning first, then choose the phrase you want to practice.</p>
        {user && !user.functionalAccessAllowed ? <AccountNotice user={user} /> : null}
        {authState === "signed-out" ? <SignInCard message="Sign in with Google to search and generate pronunciation video." /> : null}

        <label>
          Word
          <input
            value={word}
            onChange={(event) => { setWordInfo(undefined); setWordInfoId(undefined); setSelectedMeaningId(undefined); setSelectedVocabularyItem(undefined); setPhrase(""); setWord(event.target.value); resetAssets(); }}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                event.preventDefault();
                void loadWordInfo();
              }
            }}
            autoComplete="off"
          />
        </label>

        <div className="suggestions">
          {suggestions.map((suggestion) => (
            <button key={`${suggestion.source}-${suggestion.word}-${suggestion.phrase ?? ""}`} type="button" className="soft-list-button" disabled={!canUseFeatures || saveBusy} onClick={() => void selectSuggestion(suggestion)}>
              <strong>{suggestion.word}</strong>
              <small>{describeSuggestion(suggestion)}</small>
            </button>
          ))}
        </div>

        <button type="button" className="secondary" disabled={!canUseFeatures || searchBusy} onClick={loadWordInfo}>Search word</button>

        {wordInfo ? <WordInfoPanel info={wordInfo} selectedMeaning={selectedMeaning} selectedItem={selectedVocabularyItem} onUseMeaning={selectMeaningOption} onUseItem={selectVocabularyItem} /> : null}

        <label>
          Phrase
          <textarea value={phrase} onChange={(event) => { setPhrase(event.target.value); resetAssets(); }} />
        </label>
        <button type="button" disabled={!canGenerateAssets || generateBusy || imageBusy || saveBusy} onClick={() => void generateAssets()}>Save and generate assets</button>
        <StatusBox>{status}</StatusBox>
      </section>

      <section className="video-stage media-stage">
        <div className="media-stack">
          <PhraseImageCard
            image={phraseImage}
            status={imageStatus}
            busy={imageBusy}
            canGenerate={canUseFeatures}
            onSelectCandidate={(candidateIndex) => void selectImageCandidate(candidateIndex)}
          />
          <PronunciationVideoCard
            videoUrl={videoUrl}
            status={status}
            busy={generateBusy}
            canGenerate={canUseFeatures}
            videoRef={videoRef}
          />
        </div>
      </section>
    </main>
  );
}

function ReviewPage({ user, authState, onAuthError }: PageProps) {
  const [status, setStatus] = useState("Sign in to start reviewing.");
  const [items, setItems] = useState<DictionaryReviewItem[]>([]);
  const [index, setIndex] = useState(0);
  const [answer, setAnswer] = useState("");
  const [answered, setAnswered] = useState(false);
  const [result, setResult] = useState<DictionaryReviewSubmitResponse>();
  const [includeUpcoming, setIncludeUpcoming] = useState(false);
  const canUseFeatures = Boolean(user?.functionalAccessAllowed);
  const item = items[index];

  useEffect(() => {
    if (authState === "signed-out") {
      setStatus("Sign in with Google to review words.");
    }
    else if (user && !user.functionalAccessAllowed) {
      setStatus(accountRestrictionMessage(user.status));
    }
    else if (user) {
      void loadBatch(false);
    }
  }, [authState, user]);

  async function loadBatch(nextIncludeUpcoming: boolean) {
    if (!canUseFeatures) {
      setStatus(user ? accountRestrictionMessage(user.status) : "Sign in with Google to review words.");
      return;
    }
    try {
      const data = await unwrap(getDictionaryReview({ query: { limit: 10, includeUpcoming: nextIncludeUpcoming } }));
      setItems(data.items);
      setIndex(0);
      setAnswer("");
      setAnswered(false);
      setResult(undefined);
      setIncludeUpcoming(nextIncludeUpcoming);
      setStatus(data.items.length === 0 ? "No due words right now." : nextIncludeUpcoming ? "Practicing the next words by due date." : "Review due words.");
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not load review words.");
    }
  }

  async function recordResult(correct: boolean) {
    if (!item || answered) {
      return;
    }
    setAnswered(true);
    try {
      const response = await unwrap(submitDictionaryReview({ path: { entryId: item.entryId }, body: { correct } }));
      setResult(response);
      setStatus("Answer revealed.");
    }
    catch (error) {
      setAnswered(false);
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not submit review.");
    }
  }

  function nextItem() {
    const nextIndex = index + 1;
    if (nextIndex >= items.length) {
      if (includeUpcoming) {
        setItems([]);
        setStatus("Finished practice batch.");
        return;
      }
      void loadBatch(false);
      return;
    }
    setIndex(nextIndex);
    setAnswer("");
    setAnswered(false);
    setResult(undefined);
  }

  useEffect(() => {
    if (item && !answered && normalizeAnswer(answer) === normalizeAnswer(item.expectedAnswer)) {
      void recordResult(true);
    }
  }, [answer, answered, item]);

  return (
    <main className="mobile-workspace review-layout">
      <section className="panel controls-panel">
        <p className="eyebrow">Review</p>
        <h1>Recall the German word.</h1>
        <p>Use the English and Russian prompts, type the German answer, and include the article for nouns.</p>
        {user && !user.functionalAccessAllowed ? <AccountNotice user={user} /> : null}
        {authState === "signed-out" ? <SignInCard message="Sign in with Google to review your dictionary." /> : null}
        <button type="button" disabled={!canUseFeatures} onClick={() => void loadBatch(false)}>Load due words</button>
        <button type="button" className="secondary" disabled={!canUseFeatures} onClick={() => void loadBatch(true)}>Practice more</button>
        <StatusBox>{status}</StatusBox>
      </section>

      <section className="review-stage">
        {!item ? <DoneCard /> : (
          <article className="study-card">
            <p className="meta">Item {index + 1} of {items.length} · {item.partOfSpeech}{item.article ? ` · ${item.article}` : ""}</p>
            <ReviewWordInfo item={item} />
            {item.phrase ? <ReviewPhraseImage item={item} onAuthError={onAuthError} /> : null}
            <label>
              German answer
              <input value={answer} onChange={(event) => setAnswer(event.target.value)} disabled={answered} autoComplete="off" autoFocus />
            </label>
            <button type="button" className="danger" disabled={answered} onClick={() => void recordResult(false)}>I do not remember</button>
            {result ? <ReviewResult item={item} result={result} onNext={nextItem} finalItem={index + 1 >= items.length} onAuthError={onAuthError} /> : null}
          </article>
        )}
      </section>
    </main>
  );
}

function AdminPage({ user, authState, onAuthError }: PageProps) {
  const [status, setStatus] = useState("Checking admin access...");
  const [users, setUsers] = useState<AdminUserResponse[]>([]);

  useEffect(() => {
    if (authState === "signed-out") {
      setStatus("Sign in with an admin Google account to manage users.");
      setUsers([]);
      return;
    }
    if (user && !user.admin) {
      setStatus("Admin access is required for this page.");
      setUsers([]);
      return;
    }
    if (user?.admin) {
      void loadUsers();
    }
  }, [authState, user]);

  async function loadUsers() {
    try {
      setStatus("Loading users...");
      const data = await unwrap(listAdminUsers());
      setUsers(data.items);
      setStatus(`Loaded ${data.items.length} user accounts.`);
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not load users.");
    }
  }

  async function saveStatus(id: string, nextStatus: UserStatus) {
    try {
      await unwrap(updateAdminUserStatus({ path: { id }, body: { status: nextStatus } }));
      await loadUsers();
      setStatus("User status updated.");
    }
    catch (error) {
      onAuthError(error);
      setStatus(error instanceof Error ? error.message : "Could not update user.");
    }
  }

  return (
    <main className="admin-layout">
      <section className="hero-card compact">
        <p className="eyebrow">Admin</p>
        <h1>Manage user access.</h1>
        <p>Approve users for app features. Admin-list users are protected and cannot be edited here.</p>
        {authState === "signed-out" ? <SignInCard message="Sign in with an admin Google account." /> : null}
      </section>
      <StatusBox>{status}</StatusBox>
      <section className="users-list" aria-label="User accounts">
        {users.map((account) => <AdminUserRow key={account.id} user={account} onSave={saveStatus} />)}
      </section>
    </main>
  );
}

type PageProps = {
  user?: CurrentUserResponse;
  authState: AuthState;
  onAuthError: (error: unknown) => void;
};

function WordInfoPanel({ info, selectedMeaning, selectedItem, onUseMeaning, onUseItem }: { info: WordInfoResponse; selectedMeaning?: WordMeaningOption; selectedItem?: VocabularyItemDto; onUseMeaning: (meaning: WordMeaningOption) => void; onUseItem: (item: VocabularyItemDto) => void }) {
  return (
    <div className="word-info">
      <small>Detected input language: {info.inputLanguage}</small>
      <div className="word-info-section">
        <span className="word-info-divider">Choose meaning</span>
        {info.meanings.map((meaning) => (
          <MeaningOptionCard key={`${meaning.optionId}-${meaning.word}`} meaning={meaning} selected={sameMeaningOption(meaning, selectedMeaning)} onUse={() => onUseMeaning(meaning)} />
        ))}
      </div>
      {selectedMeaning ? (
        <div className="word-info-section">
          <span className="word-info-divider">Choose phrase for {selectedMeaning.word}</span>
          {selectedMeaning.phraseOptions.map((item, index) => (
            <VocabularyItemCard key={`${item.id ?? "phrase"}-${item.phrase}`} item={item} label={`Phrase ${index + 1}`} selected={sameVocabularyOption(item, selectedItem)} onUse={() => onUseItem(item)} />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function MeaningOptionCard({ meaning, selected, onUse }: { meaning: WordMeaningOption; selected: boolean; onUse: () => void }) {
  const example = firstPhraseOption(meaning);
  return (
    <article className={`soft-list-button word-info-card${selected ? " selected" : ""}`}>
      <div className="word-info-card-word">
        <strong>{meaning.word}</strong>
        <small>{describeMeaningTranslations(meaning) || "No word translation"}</small>
      </div>
      <small className="word-info-card-meta">{[articleForMeaning(meaning), meaning.partOfSpeech, meaning.frequency].filter(Boolean).join(" · ")}</small>
      {example ? (
        <div className="word-info-card-phrase">
          <strong>{example.phrase}</strong>
          <small>{describePhraseTranslations(example) || "No phrase translation"}</small>
        </div>
      ) : null}
      <button type="button" className="secondary small" onClick={onUse}>{selected ? "Meaning selected" : "Use meaning"}</button>
    </article>
  );
}

function VocabularyItemCard({ item, label, selected, onUse }: { item: VocabularyItemDto; label: string; selected: boolean; onUse: () => void }) {
  return (
    <article className={`soft-list-button word-info-card${selected ? " selected" : ""}`}>
      <div className="word-info-card-word">
        <strong>{item.word}</strong>
        <small>{describeWordTranslations(item) || "No word translation"}</small>
      </div>
      <small className="word-info-card-meta">{[label, articleForGender(item.gender), item.partOfSpeech, item.frequency].filter(Boolean).join(" · ")}</small>
      <div className="word-info-card-phrase">
        <strong>{item.phrase}</strong>
        <small>{describePhraseTranslations(item) || "No phrase translation"}</small>
      </div>
      <button type="button" className="secondary small" onClick={onUse}>{selected ? "Selected" : "Use phrase"}</button>
    </article>
  );
}

function replacePhraseOption(info: WordInfoResponse, previousItem: VocabularyItemDto, nextItem: VocabularyItemDto) {
  return {
    ...info,
    meanings: info.meanings.map((meaning) => ({
      ...meaning,
      phraseOptions: replacePhraseOptions(meaning.phraseOptions, previousItem, nextItem)
    }))
  };
}

function replacePhraseOptions(items: VocabularyItemDto[], previousItem: VocabularyItemDto, nextItem: VocabularyItemDto) {
  return items.map((item) => sameVocabularyOption(item, previousItem) ? nextItem : item);
}

function firstPhraseOption(meaning: WordMeaningOption) {
  return meaning.phraseOptions[0];
}

function sameMeaningOption(meaning?: WordMeaningOption, selectedMeaning?: WordMeaningOption) {
  return Boolean(meaning && selectedMeaning && meaning.optionId === selectedMeaning.optionId);
}

function sameVocabularyOption(item?: VocabularyItemDto, selectedItem?: VocabularyItemDto) {
  if (!item || !selectedItem) {
    return false;
  }
  if (item.id && selectedItem.id) {
    return item.id === selectedItem.id;
  }
  return !item.id && !selectedItem.id && item.word === selectedItem.word && item.phrase === selectedItem.phrase;
}

function PhraseImageCard({ image, status, busy, canGenerate, onSelectCandidate }: { image?: PhraseImageResponse; status: string; busy: boolean; canGenerate: boolean; onSelectCandidate: (candidateIndex: number) => void }) {
  const imageUrl = image?.status === "completed" ? image.imageUrl ?? undefined : undefined;
  const candidateUrls = image?.status === "awaiting_selection" ? image.candidateImageUrls ?? [] : [];
  const [selectedCandidateIndex, setSelectedCandidateIndex] = useState<number>();
  const selectedCandidateUrl = selectedCandidateIndex === undefined ? undefined : candidateUrls[selectedCandidateIndex];
  const previewUrl = selectedCandidateUrl ?? imageUrl;

  useEffect(() => {
    setSelectedCandidateIndex(undefined);
  }, [image?.id, image?.status]);

  function confirmCandidate() {
    if (selectedCandidateIndex !== undefined) {
      onSelectCandidate(selectedCandidateIndex);
    }
  }

  return (
    <article className="phrase-image-card">
      <div className="phrase-image-frame">
        {previewUrl ? <img src={previewUrl} alt={image?.phrase ? `Generated scene for ${image.phrase}` : "Generated vocabulary scene"} /> : <div className="placeholder">{candidateUrls.length > 0 ? "Choose a candidate below to preview it here." : "Cinematic Imagen scene will appear here."}</div>}
      </div>
      {candidateUrls.length > 0 ? <ImageCandidatePicker urls={candidateUrls} busy={!canUseCandidatePicker(canGenerate, busy)} selectedIndex={selectedCandidateIndex} onPreview={setSelectedCandidateIndex} /> : null}
      {candidateUrls.length > 0 ? <div className="phrase-image-actions"><button type="button" className="secondary small" disabled={!canUseCandidatePicker(canGenerate, busy) || selectedCandidateIndex === undefined} onClick={confirmCandidate}>Confirm selected image</button></div> : null}
      {status ? <small>{status}</small> : null}
    </article>
  );
}

function canUseCandidatePicker(canGenerate: boolean, busy: boolean) {
  return canGenerate && !busy;
}

function PronunciationVideoCard({ videoUrl, status, busy, canGenerate, videoRef }: { videoUrl?: string; status: string; busy: boolean; canGenerate: boolean; videoRef: RefObject<HTMLVideoElement | null> }) {
  return (
    <article className="phrase-image-card video-card">
      {videoUrl ? <video ref={videoRef} src={videoUrl} controls playsInline /> : <div className="placeholder">Generated MP4 video will appear here when Veo completes.</div>}
      {canGenerate && busy ? <small>Video generation is running.</small> : null}
      {status ? <small>{status}</small> : null}
    </article>
  );
}

function ImageCandidatePicker({ urls, busy, selectedIndex, onPreview }: { urls: string[]; busy: boolean; selectedIndex?: number; onPreview: (candidateIndex: number) => void }) {
  return (
    <div className="image-candidates" aria-label="Phrase image candidates">
      {urls.map((url, index) => (
        <button key={url} type="button" className={`image-candidate${selectedIndex === index ? " selected" : ""}`} disabled={busy} onClick={() => onPreview(index)}>
          <img src={url} alt={`Generated image candidate ${index + 1}`} />
          <span>{selectedIndex === index ? `Previewing ${index + 1}` : `Preview ${index + 1}`}</span>
        </button>
      ))}
    </div>
  );
}

function ReviewPhraseImage({ item, onAuthError }: { item: DictionaryReviewItem; onAuthError: (error: unknown) => void }) {
  const [imageId, setImageId] = useState(item.phraseImageId ?? undefined);
  const [imageUrl, setImageUrl] = useState(item.phraseImageUrl ?? undefined);
  const [candidateUrls, setCandidateUrls] = useState<string[]>([]);
  const [selectedCandidateIndex, setSelectedCandidateIndex] = useState<number>();
  const [status, setStatus] = useState(item.phraseImageUrl ? "" : "Loading image...");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setImageId(item.phraseImageId ?? undefined);
    setImageUrl(item.phraseImageUrl ?? undefined);
    setCandidateUrls([]);
    setSelectedCandidateIndex(undefined);
    setStatus(item.phraseImageUrl ? "" : "Loading image...");
    if (!item.phrase) {
      return;
    }
    if (item.phraseImageUrl) {
      return;
    }
    void generateReviewImage(item);
  }, [item.entryId]);

  async function generateReviewImage(reviewItem: DictionaryReviewItem) {
    if (!reviewItem.phrase) {
      return;
    }
    setBusy(true);
    try {
      const queued = await unwrap(createPhraseImage({ body: { wordInfoId: reviewItem.wordInfoId, word: reviewItem.normalizedWord, phrase: reviewItem.phrase, language: "de" } }));
      setImageId(queued.id);
      if (queued.imageUrl) {
        setImageUrl(queued.imageUrl);
        setCandidateUrls([]);
        setStatus("");
        return;
      }
      const completed = await pollPhraseImage(queued.id, setStatus);
      setImageId(completed.id);
      if (completed.status === "awaiting_selection") {
        setCandidateUrls(completed.candidateImageUrls ?? []);
        setSelectedCandidateIndex(undefined);
        setStatus("Choose the best visual association image.");
        return;
      }
      setImageUrl(completed.imageUrl ?? undefined);
      setCandidateUrls([]);
      setStatus(completed.status === "completed" ? "" : "Image unavailable. Review continues normally.");
    }
    catch (error) {
      onAuthError(error);
      setStatus("Image unavailable. Review continues normally.");
    }
    finally {
      setBusy(false);
    }
  }

  async function selectReviewImageCandidate(candidateIndex: number) {
    if (!imageId) {
      return;
    }
    setBusy(true);
    try {
      setStatus("Saving selected image...");
      const selected = await unwrap(selectPhraseImageCandidate({ path: { id: imageId, candidateIndex } }));
      setImageUrl(selected.imageUrl ?? undefined);
      setCandidateUrls([]);
      setSelectedCandidateIndex(undefined);
      setStatus("");
    }
    catch (error) {
      onAuthError(error);
      setStatus("Could not select image. Review continues normally.");
    }
    finally {
      setBusy(false);
    }
  }

  function confirmReviewImageCandidate() {
    if (selectedCandidateIndex !== undefined) {
      void selectReviewImageCandidate(selectedCandidateIndex);
    }
  }

  const selectedCandidateUrl = selectedCandidateIndex === undefined ? undefined : candidateUrls[selectedCandidateIndex];
  const previewUrl = selectedCandidateUrl ?? imageUrl;

  return (
    <div className="review-image-block">
      {previewUrl ? <img src={previewUrl} alt={`Generated scene for ${item.phrase}`} /> : <div className="placeholder">{candidateUrls.length > 0 ? "Choose a candidate below to preview it here." : "Visual association image is loading."}</div>}
      {candidateUrls.length > 0 ? <ImageCandidatePicker urls={candidateUrls} busy={busy} selectedIndex={selectedCandidateIndex} onPreview={setSelectedCandidateIndex} /> : null}
      {candidateUrls.length > 0 ? <div className="phrase-image-actions"><button type="button" className="secondary small" disabled={busy || selectedCandidateIndex === undefined} onClick={confirmReviewImageCandidate}>Confirm selected image</button></div> : null}
      {status ? <small>{status}</small> : null}
    </div>
  );
}

function ReviewResult({ item, result, onNext, finalItem, onAuthError }: { item: DictionaryReviewItem; result: DictionaryReviewSubmitResponse; onNext: () => void; finalItem: boolean; onAuthError: (error: unknown) => void }) {
  return (
    <div className={`result ${result.correct ? "correct" : "incorrect"}`}>
      <p><strong>{result.correct ? "Correct" : "Not this time"}</strong></p>
      <p>Answer: <strong>{result.expectedAnswer}</strong></p>
      {item.phrase ? <Prompt label="German example" value={item.phrase} /> : null}
      {item.pronunciationAssetId ? <ReviewVideo assetId={item.pronunciationAssetId} onAuthError={onAuthError} /> : null}
      <small>Next due: {new Date(result.dueAt).toLocaleString()}</small>
      <button type="button" onClick={onNext}>{finalItem ? "Finish batch" : "Next word"}</button>
    </div>
  );
}

function ReviewVideo({ assetId, onAuthError }: { assetId: string; onAuthError: (error: unknown) => void }) {
  const [status, setStatus] = useState("Loading cached video...");
  const videoRef = useRef<HTMLVideoElement>(null);
  const src = smallPronunciationVideoUrl(assetId);

  useEffect(() => {
    setStatus("Loading cached video...");
    window.setTimeout(() => void videoRef.current?.play(), 50);
  }, [src]);

  return <>
    {status ? <small>{status}</small> : null}
    <video
      ref={videoRef}
      className="review-video"
      src={src}
      controls
      playsInline
      onCanPlay={() => setStatus("")}
      onError={() => {
        onAuthError(new Error("Could not load pronunciation video."));
        setStatus("Could not load video.");
      }}
    />
  </>;
}

function AdminUserRow({ user, onSave }: { user: AdminUserResponse; onSave: (id: string, status: UserStatus) => Promise<void> }) {
  const [status, setStatus] = useState<UserStatus>(user.status);
  const [busy, setBusy] = useState(false);

  async function save() {
    setBusy(true);
    try {
      await onSave(user.id, status);
    }
    finally {
      setBusy(false);
    }
  }

  return (
    <article className={`user-row${user.adminListUser ? " protected" : ""}`}>
      <div>
        <strong>{user.email}</strong>
        <small>{user.displayName}</small>
        {user.adminListUser ? <span className="badge">Admin-list protected</span> : null}
      </div>
      <select value={status} disabled={user.adminListUser || busy} aria-label="User status" onChange={(event) => setStatus(event.target.value as UserStatus)}>
        <option value="pending">pending</option>
        <option value="active">active</option>
        <option value="deactivated">deactivated</option>
      </select>
      <button type="button" disabled={user.adminListUser || busy} onClick={save}>Save</button>
    </article>
  );
}

function AccountNotice({ user }: { user: CurrentUserResponse }) {
  return <div className="notice">{accountRestrictionMessage(user.status)} App features are disabled.</div>;
}

function SignInCard({ message }: { message: string }) {
  return (
    <div className="signin-card">
      <p>{message}</p>
      <a className="login-button wide" href={loginUrl()}><img src="/google-g.svg" alt="" aria-hidden="true" />Sign in with Google</a>
    </div>
  );
}

function StatusBox({ children }: { children: string }) {
  return <div className="status-box">{children}</div>;
}

function Prompt({ label, value }: { label: string; value: string }) {
  return (
    <div className="prompt">
      <small>{label}</small>
      <strong>{value}</strong>
    </div>
  );
}

function ReviewWordInfo({ item }: { item: DictionaryReviewItem }) {
  const englishExample = joinText(item.phraseTranslations.en);
  const russianExample = joinText(item.phraseTranslations.ru);

  return (
    <div className="review-word-info">
      <div className="prompt-grid">
        <Prompt label="English" value={joinText(item.translations.en) || "No English translation"} />
        <Prompt label="Russian" value={joinText(item.translations.ru) || "No Russian translation"} />
      </div>
      {englishExample || russianExample ? (
        <div className="example-grid">
          <Prompt label="English example" value={englishExample || "No English example"} />
          <Prompt label="Russian example" value={russianExample || "No Russian example"} />
        </div>
      ) : null}
    </div>
  );
}

function DoneCard() {
  return (
    <article className="study-card done-card">
      <h2>You are done, congrats!</h2>
      <p>No words are currently due. You can practice more to review the next words by due date.</p>
    </article>
  );
}

function describeSuggestion(item: WordSuggestion) {
  if (item.source === "pronunciation") {
    return `${item.phrase ?? "No phrase"}${item.status ? ` · ${item.status}` : ""}`;
  }
  return "Cached word info";
}

async function pollPronunciation(id: string, setStatus: (message: string) => void) {
  for (let attempt = 0; attempt < 180; attempt += 1) {
    const asset = await unwrap(getPronunciation({ path: { id } }));
    setStatus(`id=${asset.id}\nstatus=${asset.status}\nwaiting=${attempt * 2}s`);
    if (asset.status === "completed" || asset.status === "failed") {
      return asset;
    }
    await sleep(2000);
  }
  throw new Error("Timed out waiting for video generation.");
}

async function pollPhraseImage(id: string, setStatus: (message: string) => void) {
  for (let attempt = 0; attempt < 90; attempt += 1) {
    const image = await unwrap(getPhraseImage({ path: { id } }));
    setStatus(`image=${image.id}\nstatus=${image.status}\nwaiting=${attempt * 2}s`);
    if (image.status === "completed" || image.status === "awaiting_selection" || image.status === "failed") {
      return image;
    }
    await sleep(2000);
  }
  throw new Error("Timed out waiting for image generation.");
}

function sleep(millis: number) {
  return new Promise((resolve) => window.setTimeout(resolve, millis));
}
